package com.rhpaie.dashboard.service;

import com.rhpaie.attendance.entity.AttendanceRecord;
import com.rhpaie.attendance.entity.AttendanceStatus;
import com.rhpaie.attendance.repository.AttendanceRepository;
import com.rhpaie.contract.entity.Contract;
import com.rhpaie.contract.entity.ContractStatus;
import com.rhpaie.contract.repository.ContractRepository;
import com.rhpaie.dashboard.dto.AdminDashboardSummaryResponse;
import com.rhpaie.dashboard.dto.DashboardAlertResponse;
import com.rhpaie.dashboard.dto.EmployeeDashboardSummaryResponse;
import com.rhpaie.dashboard.dto.QuickStatResponse;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.EmploymentStatus;
import com.rhpaie.employee.repository.DepartmentRepository;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.employee.repository.JobPositionRepository;
import com.rhpaie.employeeexit.entity.ExitStatus;
import com.rhpaie.employeeexit.repository.EmployeeExitRepository;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.repository.LeaveRequestRepository;
import com.rhpaie.leavebalance.service.LeaveBalanceService;
import com.rhpaie.payroll.entity.PayrollCalculation;
import com.rhpaie.payroll.entity.PayrollCalculationStatus;
import com.rhpaie.payroll.repository.PayrollCalculationRepository;
import com.rhpaie.payrollvariable.entity.PayrollVariableStatus;
import com.rhpaie.payrollvariable.repository.PayrollVariableRepository;
import com.rhpaie.payslip.entity.PayslipStatus;
import com.rhpaie.payslip.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final JobPositionRepository jobPositionRepository;
    private final ContractRepository contractRepository;
    private final EmployeeExitRepository employeeExitRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollVariableRepository payrollVariableRepository;
    private final PayrollCalculationRepository payrollCalculationRepository;
    private final PayslipRepository payslipRepository;
    private final LeaveBalanceService leaveBalanceService;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse adminSummary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime monthStartTime = monthStart.atStartOfDay();
        LocalDateTime monthEndTime = monthEnd.atTime(23, 59, 59);
        int month = today.getMonthValue();
        int year = today.getYear();

        long activeEmployees = employeeRepository.countByStatus(EmploymentStatus.ACTIVE);
        long inactiveEmployees = employeeRepository.countByStatusIn(List.of(EmploymentStatus.INACTIVE, EmploymentStatus.LEFT, EmploymentStatus.TERMINATED));
        long pendingLeaves = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);
        long expiringSoonContracts = contractRepository.countByStatusAndEndDateBetween(ContractStatus.ACTIVE, today, today.plusDays(30));
        long expiredContracts = contractRepository.countByStatusAndEndDateBefore(ContractStatus.ACTIVE, today)
                + contractRepository.countByStatus(ContractStatus.EXPIRED);
        long missingContractDocuments = contractRepository.countByDocumentStoragePathIsNull();
        long missingRib = employeeRepository.countByRibIsNullOrRib("");
        long finalizedCalculations = payrollCalculationRepository.countByMonthAndYearAndStatus(month, year, PayrollCalculationStatus.FINALIZED);
        long generatedPayslips = payslipRepository.countByMonthAndYearAndStatus(month, year, PayslipStatus.GENERATED);
        long ungeneratedPayslips = Math.max(0, finalizedCalculations - generatedPayslips);

        BigDecimal totalNetPayroll = payrollCalculationRepository.sumNetByStatus(month, year, PayrollCalculationStatus.FINALIZED);
        BigDecimal totalGrossPayroll = payrollCalculationRepository.sumGrossByStatus(month, year, PayrollCalculationStatus.FINALIZED);

        return AdminDashboardSummaryResponse.builder()
                .activeEmployeesCount(activeEmployees)
                .inactiveEmployeesCount(inactiveEmployees)
                .newEmployeesThisMonth(employeeRepository.countByCreatedAtBetween(monthStartTime, monthEndTime))
                .departmentsCount(departmentRepository.count())
                .jobPositionsCount(jobPositionRepository.count())
                .activeContractsCount(contractRepository.countByStatus(ContractStatus.ACTIVE))
                .expiringSoonContractsCount(expiringSoonContracts)
                .expiredContractsCount(expiredContracts)
                .contractsMissingDocumentCount(missingContractDocuments)
                .scheduledExitsCount(employeeExitRepository.countByStatus(ExitStatus.SCHEDULED))
                .pendingLeaveRequestsCount(pendingLeaves)
                .approvedLeaveRequestsThisMonth(leaveRequestRepository.countByStatusAndCreatedAtBetween(LeaveStatus.APPROVED, monthStartTime, monthEndTime))
                .employeesOnLeaveToday(leaveRequestRepository.countEmployeesOnLeaveToday(LeaveStatus.APPROVED, today))
                .leaveRequestsToReviewCount(pendingLeaves)
                .presentTodayCount(attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.PRESENT))
                .lateTodayCount(attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.LATE))
                .absentTodayCount(attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.ABSENT))
                .incompleteAttendanceTodayCount(attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.INCOMPLETE))
                .totalOvertimeMinutesThisMonth(attendanceRepository.sumOvertimeMinutesBetween(monthStart, monthEnd))
                .draftPayrollVariablesThisMonth(payrollVariableRepository.countByMonthAndYearAndStatus(month, year, PayrollVariableStatus.DRAFT))
                .validatedPayrollVariablesThisMonth(payrollVariableRepository.countByMonthAndYearAndStatus(month, year, PayrollVariableStatus.VALIDATED))
                .draftPayrollCalculationsThisMonth(payrollCalculationRepository.countByMonthAndYearAndStatus(month, year, PayrollCalculationStatus.DRAFT))
                .finalizedPayrollCalculationsThisMonth(finalizedCalculations)
                .generatedPayslipsThisMonth(generatedPayslips)
                .totalNetPayrollThisMonth(totalNetPayroll)
                .totalGrossPayrollThisMonth(totalGrossPayroll)
                .mainStats(mainStats(activeEmployees, pendingLeaves, generatedPayslips, totalNetPayroll, today, month, year))
                .alerts(alerts(expiredContracts, expiringSoonContracts, missingContractDocuments, missingRib, pendingLeaves, ungeneratedPayslips))
                .build();
    }

    @Override
    @Transactional
    public EmployeeDashboardSummaryResponse employeeSummary(String username) {
        Employee employee = employeeRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No employee profile is linked to the current account"));
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        Contract contract = contractRepository.findDetailedByEmployeeId(employee.getId()).stream()
                .filter(item -> item.getStatus() == ContractStatus.ACTIVE)
                .findFirst()
                .orElse(null);
        AttendanceRecord attendance = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today).orElse(null);
        PayrollCalculation salary = payrollCalculationRepository.findByEmployeeIdAndStatusOrderByYearDescMonthDesc(employee.getId(), PayrollCalculationStatus.FINALIZED)
                .stream()
                .findFirst()
                .orElse(null);

        return EmployeeDashboardSummaryResponse.builder()
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .departmentName(employee.getDepartment() == null ? null : employee.getDepartment().getName())
                .jobPositionName(employee.getJobPosition() == null ? null : employee.getJobPosition().getTitle())
                .currentContractType(contract == null ? null : contract.getType())
                .currentContractEndDate(contract == null ? null : contract.getEndDate())
                .leaveBalanceRemaining(leaveBalanceService.getRemainingDays(employee, year))
                .pendingLeaveRequestsCount(leaveRequestRepository.countByEmployeeIdAndStatus(employee.getId(), LeaveStatus.PENDING))
                .approvedLeaveRequestsThisYear(leaveRequestRepository.countByEmployeeIdAndStatusAndCreatedAtBetween(
                        employee.getId(),
                        LeaveStatus.APPROVED,
                        LocalDate.of(year, 1, 1).atStartOfDay(),
                        LocalDate.of(year, 12, 31).atTime(23, 59, 59)
                ))
                .todayAttendanceStatus(attendance == null ? null : attendance.getStatus())
                .todayCheckInTime(attendance == null ? null : attendance.getCheckInTime())
                .todayCheckOutTime(attendance == null ? null : attendance.getCheckOutTime())
                .workedHoursToday(attendance == null || attendance.getWorkedHours() == null ? null : attendance.getWorkedHours().doubleValue())
                .latestNetSalary(salary == null ? null : salary.getNetSalary())
                .latestSalaryMonth(salary == null ? null : salary.getMonth())
                .latestSalaryYear(salary == null ? null : salary.getYear())
                .latestPayslipAvailable(!payslipRepository.findByEmployeeIdAndStatusOrderByYearDescMonthDesc(employee.getId(), PayslipStatus.GENERATED).isEmpty())
                .build();
    }

    private List<QuickStatResponse> mainStats(long activeEmployees, long pendingLeaves, long generatedPayslips, BigDecimal totalNetPayroll, LocalDate today, int month, int year) {
        return List.of(
                QuickStatResponse.builder().label("Employés actifs").value(String.valueOf(activeEmployees)).helper("Effectif opérationnel").icon("users").severity("success").route("/admin/employees").build(),
                QuickStatResponse.builder().label("Demandes en attente").value(String.valueOf(pendingLeaves)).helper("À traiter par RH").icon("calendar").severity("warning").route("/admin/leave-requests").build(),
                QuickStatResponse.builder().label("Présents aujourd’hui").value(String.valueOf(attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.PRESENT))).helper("Pointage du jour").icon("clock").severity("success").route("/admin/attendance").build(),
                QuickStatResponse.builder().label("Paies finalisées").value(String.valueOf(payrollCalculationRepository.countByMonthAndYearAndStatus(month, year, PayrollCalculationStatus.FINALIZED))).helper("Mois courant").icon("calculator").severity("primary").route("/admin/payroll-calculations").build(),
                QuickStatResponse.builder().label("Bulletins générés").value(String.valueOf(generatedPayslips)).helper("PDF disponibles").icon("file").severity("info").route("/admin/payslips").build(),
                QuickStatResponse.builder().label("Masse salariale nette").value(money(totalNetPayroll)).helper("Paies finalisées").icon("wallet").severity("primary").route("/admin/payroll-calculations").build()
        );
    }

    private List<DashboardAlertResponse> alerts(long expiredContracts, long expiringSoonContracts, long missingContractDocuments, long missingRib, long pendingLeaves, long ungeneratedPayslips) {
        return List.of(
                alert("CONTRACT_EXPIRED", "Contrats expirés", expiredContracts, "Contrats à renouveler ou sortie à préparer.", "danger", "/admin/contracts/monitoring", "Ouvrir"),
                alert("CONTRACT_EXPIRING_SOON", "Contrats expirant bientôt", expiringSoonContracts, "Échéances à surveiller sur 30 jours.", "warning", "/admin/contracts/monitoring", "Voir"),
                alert("MISSING_CONTRACT_DOCUMENT", "Documents de contrat manquants", missingContractDocuments, "Contrats sans PDF signé.", "danger", "/admin/contracts/monitoring", "Corriger"),
                alert("MISSING_RIB", "RIB employé manquant", missingRib, "Informations de paiement à compléter.", "warning", "/admin/employees", "Corriger"),
                alert("PENDING_LEAVE_REQUEST", "Demandes d’absence en attente", pendingLeaves, "Demandes RH à vérifier.", "warning", "/admin/leave-requests", "Traiter"),
                alert("PAYSLIP_NOT_GENERATED", "Bulletins non générés", ungeneratedPayslips, "Paies finalisées sans bulletin PDF.", "info", "/admin/payslips", "Générer")
        );
    }

    private DashboardAlertResponse alert(String type, String title, long count, String message, String severity, String route, String actionLabel) {
        return DashboardAlertResponse.builder()
                .type(type)
                .title(title)
                .count(count)
                .message(message)
                .severity(severity)
                .route(route)
                .actionLabel(actionLabel)
                .build();
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
