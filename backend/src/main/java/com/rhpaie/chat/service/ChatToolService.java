package com.rhpaie.chat.service;

import com.rhpaie.attendance.dto.TodayAttendanceResponse;
import com.rhpaie.attendance.service.AttendanceService;
import com.rhpaie.chat.dto.ChatToolResult;
import com.rhpaie.chat.entity.ChatIntent;
import com.rhpaie.contract.dto.ContractMonitoringResponse;
import com.rhpaie.contract.dto.ContractResponse;
import com.rhpaie.contract.service.ContractService;
import com.rhpaie.employeeexit.dto.EmployeeExitDocumentResponse;
import com.rhpaie.employeeexit.dto.EmployeeExitResponse;
import com.rhpaie.employeeexit.dto.EmployeeExitSummaryResponse;
import com.rhpaie.employeeexit.service.EmployeeExitDocumentService;
import com.rhpaie.employeeexit.service.EmployeeExitService;
import com.rhpaie.leave.dto.AdminLeaveSummaryResponse;
import com.rhpaie.leave.dto.EmployeeLeaveSummaryResponse;
import com.rhpaie.leave.dto.LeaveCalendarEventResponse;
import com.rhpaie.leave.dto.LeaveRequestResponse;
import com.rhpaie.leave.service.LeaveRequestService;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leavebalance.dto.LeaveBalanceSummaryResponse;
import com.rhpaie.leavebalance.service.LeaveBalanceService;
import com.rhpaie.payroll.dto.PayrollCalculationResponse;
import com.rhpaie.payroll.dto.PayrollCalculationSummaryResponse;
import com.rhpaie.payroll.service.PayrollCalculationService;
import com.rhpaie.payslip.dto.PayslipResponse;
import com.rhpaie.payslip.service.PayslipService;
import com.rhpaie.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.rhpaie.chat.context.ReadFollowUpResolver.FILTER_NEAREST_EXPIRING;
import static com.rhpaie.chat.context.ReadFollowUpResolver.FILTER_STATUS;

@Service
@RequiredArgsConstructor
public class ChatToolService {

    private final LeaveBalanceService leaveBalanceService;
    private final LeaveRequestService leaveRequestService;
    private final PayslipService payslipService;
    private final ContractService contractService;
    private final AttendanceService attendanceService;
    private final PayrollCalculationService payrollCalculationService;
    private final EmployeeExitService employeeExitService;
    private final EmployeeExitDocumentService employeeExitDocumentService;

    public ChatToolResult execute(ChatIntent intent, String username, Integer year, Role role) {
        return execute(intent, username, year, role, Map.of());
    }

    public ChatToolResult execute(
            ChatIntent intent,
            String username,
            Integer year,
            Role role,
            Map<String, String> filters
    ) {
        int resolvedYear = year != null ? year : LocalDate.now().getYear();
        Map<String, String> safeFilters = filters == null ? Map.of() : filters;
        try {
            return switch (intent) {
                case LEAVE_BALANCE -> leaveBalance(username, resolvedYear);
                case MY_LEAVE_REQUESTS -> myLeaveRequests(username, safeFilters);
                case MY_PAYSLIPS -> myPayslips(username, resolvedYear);
                case MY_CONTRACTS -> myContracts(username, safeFilters);
                case MY_ATTENDANCE -> myAttendance(username);
                case LEAVE_CALENDAR -> leaveCalendar(username, role, year);
                case MY_SALARIES -> mySalaries(username, resolvedYear);
                case MY_EXIT_DOCUMENTS -> myExitDocuments(username);
                case PENDING_LEAVES -> pendingLeaves();
                case EXPIRING_CONTRACTS -> expiringContracts();
                case PAYROLL_STATUS -> payrollStatus(resolvedYear);
                case EXIT_SUMMARY -> exitSummary();
                default -> ChatToolResult.reply("Je ne peux pas récupérer cette information.");
            };
        } catch (ResponseStatusException exception) {
            String reason = exception.getReason() == null ? "Information indisponible." : exception.getReason();
            if (reason.contains("No employee profile")) {
                return ChatToolResult.reply("Aucun profil employé n'est lié à votre compte. Contactez le service RH.");
            }
            return ChatToolResult.reply(reason);
        }
    }

    private ChatToolResult leaveBalance(String username, int year) {
        LeaveBalanceSummaryResponse balance = leaveBalanceService.findMyBalance(username, year);
        String facts = "année=" + balance.year()
                + ", acquis=" + formatNumber(balance.acquiredDays())
                + ", consommés=" + formatNumber(balance.consumedDays())
                + ", en attente=" + formatNumber(balance.pendingDays())
                + ", restants=" + formatNumber(balance.remainingDays());
        String reply = "Il vous reste " + formatNumber(balance.remainingDays())
                + " jour(s) de congé en " + balance.year()
                + " (acquis " + formatNumber(balance.acquiredDays())
                + ", consommés " + formatNumber(balance.consumedDays())
                + ", en attente " + formatNumber(balance.pendingDays()) + ").";
        return ChatToolResult.reply(facts, reply, "/employee/my-leave-requests", "Voir mes congés");
    }

    private ChatToolResult myLeaveRequests(String username, Map<String, String> filters) {
        EmployeeLeaveSummaryResponse summary = leaveRequestService.mySummary(username);
        List<LeaveRequestResponse> requests = leaveRequestService.findMyRequests(username);
        LeaveStatus statusFilter = parseStatusFilter(filters.get(FILTER_STATUS));
        if (statusFilter != null) {
            List<LeaveRequestResponse> filtered = requests.stream()
                    .filter(request -> request.status() == statusFilter)
                    .toList();
            String latest = filtered.stream().limit(5)
                    .map(request -> request.type() + " " + request.startDate() + " → " + request.endDate()
                            + " (n°" + request.id() + ")")
                    .collect(Collectors.joining(" ; "));
            String statusLabel = statusLabelFr(statusFilter);
            String facts = "filtre=" + statusFilter.name() + ", nombre=" + filtered.size()
                    + (latest.isBlank() ? "" : ", liste=[" + latest + "]");
            String reply = filtered.isEmpty()
                    ? "Aucune demande " + statusLabel + " pour le moment."
                    : "Voici " + filtered.size() + " demande(s) " + statusLabel + " : "
                    + (latest.isBlank() ? "" : latest + ".");
            return ChatToolResult.reply(facts, reply, "/employee/my-leave-requests", "Voir mes demandes");
        }
        String latest = requests.stream().limit(3)
                .map(request -> request.type() + " " + request.startDate() + " → " + request.endDate() + " (" + request.status() + ")")
                .collect(Collectors.joining(" ; "));
        String facts = "enAttente=" + summary.pendingRequests()
                + ", approuvéesCetteAnnée=" + summary.approvedThisYear()
                + ", refuséesCetteAnnée=" + summary.rejectedThisYear()
                + (latest.isBlank() ? "" : ", dernières=[" + latest + "]");
        String reply = "Vous avez " + summary.pendingRequests() + " demande(s) en attente, "
                + summary.approvedThisYear() + " approuvée(s) et "
                + summary.rejectedThisYear() + " refusée(s) cette année.";
        return ChatToolResult.reply(facts, reply, "/employee/my-leave-requests", "Voir mes demandes");
    }

    private ChatToolResult myContracts(String username, Map<String, String> filters) {
        List<ContractResponse> contracts = contractService.findMine(username);
        if (contracts.isEmpty()) {
            return ChatToolResult.reply(
                    "contrats=0",
                    "Aucun contrat n'est associé à votre profil pour le moment.",
                    "/employee/my-contracts",
                    "Voir mes contrats"
            );
        }
        if ("true".equalsIgnoreCase(filters.getOrDefault(FILTER_NEAREST_EXPIRING, ""))) {
            LocalDate today = LocalDate.now();
            ContractResponse nearest = contracts.stream()
                    .filter(contract -> contract.endDate() != null && !contract.endDate().isBefore(today))
                    .min(Comparator.comparing(ContractResponse::endDate))
                    .orElse(null);
            if (nearest == null) {
                return ChatToolResult.reply(
                        "contrats=" + contracts.size() + ", prochainExpirant=aucun",
                        "Aucun de vos contrats n'a de date de fin à venir.",
                        "/employee/my-contracts",
                        "Voir mes contrats"
                );
            }
            String facts = "contratId=" + nearest.id()
                    + ", type=" + nearest.type()
                    + ", statut=" + nearest.status()
                    + ", fin=" + nearest.endDate();
            String reply = "Le contrat qui expire le plus tôt est un " + nearest.type()
                    + " (" + nearest.status() + "), fin le " + nearest.endDate() + ".";
            return ChatToolResult.reply(facts, reply, "/employee/my-contracts", "Voir mes contrats");
        }
        ContractResponse last = contracts.getFirst();
        String facts = "nombre=" + contracts.size()
                + ", dernierType=" + last.type()
                + ", statut=" + last.status()
                + ", debut=" + last.startDate()
                + ", fin=" + last.endDate();
        String reply = "Vous avez " + contracts.size() + " contrat(s). Le plus récent est un "
                + last.type() + " (" + last.status() + "), du " + last.startDate()
                + (last.endDate() == null ? "." : " au " + last.endDate() + ".");
        return ChatToolResult.reply(facts, reply, "/employee/my-contracts", "Voir mes contrats");
    }

    private static LeaveStatus parseStatusFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LeaveStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String statusLabelFr(LeaveStatus status) {
        return switch (status) {
            case PENDING -> "en attente";
            case APPROVED -> "approuvée(s)";
            case REJECTED -> "refusée(s)";
            case CANCELLED -> "annulée(s)";
        };
    }

    private ChatToolResult myPayslips(String username, int year) {
        List<PayslipResponse> payslips = payslipService.findMine(username, year);
        if (payslips.isEmpty()) {
            return ChatToolResult.reply(
                    "bulletins=" + year + ":0",
                    "Aucun bulletin de paie n'est disponible pour " + year + ".",
                    "/employee/my-payslips",
                    "Voir mes bulletins"
            );
        }
        PayslipResponse last = payslips.getFirst();
        String facts = "année=" + year
                + ", nombre=" + payslips.size()
                + ", dernier=" + last.month() + "/" + last.year()
                + ", net=" + formatMoney(last.netSalary())
                + ", statut=" + last.status();
        String reply = "Votre dernier bulletin est " + last.month() + "/" + last.year()
                + " (net " + formatMoney(last.netSalary()) + "). "
                + payslips.size() + " bulletin(s) sont disponibles pour " + year + ".";
        return ChatToolResult.reply(facts, reply, "/employee/my-payslips", "Télécharger mes bulletins");
    }

    private ChatToolResult myAttendance(String username) {
        TodayAttendanceResponse today = attendanceService.today(username);
        String facts = "date=" + today.workDate()
                + ", statut=" + today.status()
                + ", arrivee=" + today.checkInTime()
                + ", sortie=" + today.checkOutTime()
                + ", heures=" + formatNumber(today.workedHours())
                + ", retardMinutes=" + today.lateMinutes()
                + ", canCheckIn=" + today.canCheckIn()
                + ", canCheckOut=" + today.canCheckOut();
        String reply = today.message() != null && !today.message().isBlank()
                ? today.message()
                : "Pointage du " + today.workDate() + " : statut " + today.status()
                + (today.checkInTime() == null ? "" : ", arrivée " + today.checkInTime())
                + (today.checkOutTime() == null ? "" : ", sortie " + today.checkOutTime()) + ".";
        return ChatToolResult.reply(facts, reply, "/employee/my-attendance", "Voir mon pointage");
    }

    private ChatToolResult pendingLeaves() {
        AdminLeaveSummaryResponse summary = leaveRequestService.adminSummary();
        String facts = "enAttente=" + summary.pendingCount()
                + ", approuvéesCeMois=" + summary.approvedThisMonth()
                + ", refuséesCeMois=" + summary.rejectedThisMonth()
                + ", annuléesCeMois=" + summary.cancelledThisMonth();
        String reply = "Il y a " + summary.pendingCount() + " demande(s) de congé en attente. "
                + "Ce mois-ci : " + summary.approvedThisMonth() + " approuvée(s), "
                + summary.rejectedThisMonth() + " refusée(s).";
        return ChatToolResult.reply(facts, reply, "/admin/leave-requests", "Traiter les congés");
    }

    private ChatToolResult expiringContracts() {
        ContractMonitoringResponse monitoring = contractService.monitoring();
        List<ContractResponse> expiring = contractService.expiringSoon(30);
        String names = expiring.stream().limit(5)
                .map(contract -> contract.employeeFullName() + " (" + contract.endDate() + ")")
                .collect(Collectors.joining(", "));
        String facts = "actifs=" + monitoring.activeContractsCount()
                + ", expirentBientôt=" + monitoring.expiringSoonCount()
                + ", expirés=" + monitoring.expiredContractsCount()
                + ", documentsManquants=" + monitoring.missingDocumentCount()
                + (names.isBlank() ? "" : ", liste=[" + names + "]");
        String reply = monitoring.expiringSoonCount() + " contrat(s) arrivent à échéance bientôt. "
                + monitoring.expiredContractsCount() + " déjà expiré(s), "
                + monitoring.missingDocumentCount() + " sans document.";
        return ChatToolResult.reply(facts, reply, "/admin/contracts/monitoring", "Suivi des contrats");
    }

    private ChatToolResult payrollStatus(int year) {
        int month = YearMonth.now().getMonthValue();
        PayrollCalculationSummaryResponse summary = payrollCalculationService.summary(month, year);
        String facts = "période=" + month + "/" + year
                + ", calculés=" + summary.totalEmployeesCalculated()
                + ", brouillons=" + summary.draftCount()
                + ", finalisés=" + summary.finalizedCount()
                + ", annulés=" + summary.cancelledCount()
                + ", brut=" + formatMoney(summary.totalGrossSalary())
                + ", net=" + formatMoney(summary.totalNetSalary());
        String reply = "Paie " + month + "/" + year + " : "
                + summary.totalEmployeesCalculated() + " calcul(s), "
                + summary.draftCount() + " brouillon(s), "
                + summary.finalizedCount() + " finalisé(s).";
        return ChatToolResult.reply(facts, reply, "/admin/payroll-calculations", "Voir la paie");
    }

    private ChatToolResult leaveCalendar(String username, Role role, Integer year) {
        LocalDate start;
        LocalDate end;
        if (year != null) {
            start = LocalDate.of(year, 1, 1);
            end = LocalDate.of(year, 12, 31);
        } else {
            start = LocalDate.now().withDayOfMonth(1);
            end = start.plusMonths(2).minusDays(1);
        }
        boolean admin = role == Role.ADMIN_RH;
        List<LeaveCalendarEventResponse> events = admin
                ? leaveRequestService.adminCalendar(start, end, null, null, null)
                : leaveRequestService.myCalendar(username, start, end);
        String preview = events.stream().limit(4)
                .map(event -> event.employeeFullName() + " " + event.startDate() + " → " + event.endDate()
                        + " (" + event.status() + ")")
                .collect(Collectors.joining(" ; "));
        String facts = "du=" + start + ", au=" + end + ", absences=" + events.size()
                + (preview.isBlank() ? "" : ", liste=[" + preview + "]");
        String reply = events.isEmpty()
                ? "Aucune absence sur le calendrier du " + start + " au " + end + "."
                : events.size() + " absence(s) du " + start + " au " + end + "."
                + (preview.isBlank() ? "" : " Exemples : " + preview + ".");
        String link = admin ? "/admin/leave-calendar" : "/employee/leave-calendar";
        return ChatToolResult.reply(facts, reply, link, "Ouvrir le calendrier");
    }

    private ChatToolResult mySalaries(String username, int year) {
        List<PayrollCalculationResponse> salaries = payrollCalculationService.findMine(username, year);
        if (salaries.isEmpty()) {
            return ChatToolResult.reply(
                    "salaires=" + year + ":0",
                    "Aucun calcul de salaire finalisé n'est disponible pour " + year + ".",
                    "/employee/my-salaries",
                    "Voir mes salaires"
            );
        }
        PayrollCalculationResponse last = salaries.getFirst();
        String facts = "année=" + year
                + ", nombre=" + salaries.size()
                + ", dernier=" + last.month() + "/" + last.year()
                + ", net=" + formatMoney(last.netSalary())
                + ", brut=" + formatMoney(last.grossSalary())
                + ", statut=" + last.status();
        String reply = "Votre dernier salaire finalisé est " + last.month() + "/" + last.year()
                + " (net " + formatMoney(last.netSalary()) + "). "
                + salaries.size() + " calcul(s) sont disponibles pour " + year + ".";
        return ChatToolResult.reply(facts, reply, "/employee/my-salaries", "Voir mes salaires");
    }

    private ChatToolResult myExitDocuments(String username) {
        List<EmployeeExitDocumentResponse> documents = employeeExitDocumentService.findVisibleMine(username);
        if (documents.isEmpty()) {
            return ChatToolResult.reply(
                    "documentsSortie=0",
                    "Aucun document de sortie n'est visible sur votre espace pour le moment.",
                    "/employee/my-exit-documents",
                    "Voir mes documents de sortie"
            );
        }
        String preview = documents.stream().limit(3)
                .map(document -> document.type() + " (" + document.originalFileName() + ")")
                .collect(Collectors.joining(" ; "));
        String facts = "nombre=" + documents.size() + ", liste=[" + preview + "]";
        String reply = "Vous avez " + documents.size() + " document(s) de sortie visible(s). "
                + "Derniers : " + preview + ".";
        return ChatToolResult.reply(facts, reply, "/employee/my-exit-documents", "Ouvrir mes documents de sortie");
    }

    private ChatToolResult exitSummary() {
        EmployeeExitSummaryResponse summary = employeeExitService.summary();
        List<EmployeeExitResponse> upcoming = employeeExitService.upcoming(30);
        String names = upcoming.stream().limit(5)
                .map(exit -> exit.employeeFullName() + " (" + exit.exitDate() + ", " + exit.status() + ")")
                .collect(Collectors.joining(", "));
        String facts = "total=" + summary.totalExits()
                + ", brouillons=" + summary.draftCount()
                + ", planifiés=" + summary.scheduledCount()
                + ", terminés=" + summary.completedCount()
                + ", àVenir30j=" + summary.upcomingCount()
                + (names.isBlank() ? "" : ", liste=[" + names + "]");
        String reply = summary.upcomingCount() + " départ(s) dans les 30 jours, "
                + summary.scheduledCount() + " planifié(s), "
                + summary.draftCount() + " brouillon(s).";
        return ChatToolResult.reply(facts, reply, "/admin/employee-exits", "Suivi des sorties");
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "0";
        }
        if (value == Math.rint(value)) {
            return String.valueOf(value.longValue());
        }
        return String.format(Locale.FRANCE, "%.1f", value);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return String.format(Locale.FRANCE, "%,.2f", value);
    }
}
