package com.rhpaie.leave.service;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.EmploymentStatus;
import com.rhpaie.leave.dto.LeaveValidationResponse;
import com.rhpaie.leave.entity.LeaveRequest;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import com.rhpaie.leave.repository.LeaveRequestRepository;
import com.rhpaie.leavebalance.service.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveValidationServiceImpl implements LeaveValidationService {

    private static final List<LeaveStatus> BLOCKING_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceService leaveBalanceService;

    @Override
    public LeaveValidationResponse validateBeforeApproval(LeaveRequest leaveRequest) {
        List<String> blockingErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Employee employee = leaveRequest.getEmployee();
        Double remainingBalance = null;
        boolean affectsPayroll = leaveRequest.getType() == LeaveType.UNPAID_LEAVE;

        if (leaveRequest.getStartDate().isAfter(leaveRequest.getEndDate())) {
            blockingErrors.add("La date de début doit être avant ou égale à la date de fin.");
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            blockingErrors.add("La demande doit être en attente pour être approuvée.");
        }

        if (leaveRequestRepository.existsOverlappingRequestExcludingCurrent(
                employee.getId(),
                leaveRequest.getId(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                BLOCKING_STATUSES
        )) {
            blockingErrors.add("Une autre demande en attente ou approuvée chevauche cette période.");
        }

        if (employee.getStatus() != EmploymentStatus.ACTIVE) {
            blockingErrors.add("L'employé doit être actif.");
        }

        if (leaveRequest.getType() == LeaveType.PAID_LEAVE) {
            remainingBalance = leaveBalanceService.getRemainingDays(employee, leaveRequest.getStartDate().getYear());
            if (remainingBalance < leaveRequest.getTotalDays()) {
                blockingErrors.add("Le solde de congé est insuffisant.");
            }
        }

        if (affectsPayroll) {
            warnings.add("Cette absence sans solde impactera la paie.");
        }

        LocalDate monthStart = leaveRequest.getStartDate().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = leaveRequest.getStartDate().with(TemporalAdjusters.lastDayOfMonth());
        Double approvedDaysThisMonth = leaveRequestRepository.sumDaysByEmployeeAndStatusInMonth(
                employee.getId(),
                LeaveStatus.APPROVED,
                monthStart,
                monthEnd
        );
        if (approvedDaysThisMonth > 5) {
            warnings.add("L'employé a déjà plus de 5 jours d'absence approuvés ce mois.");
        }

        if (leaveRequestRepository.existsCloseApprovedRequest(
                employee.getId(),
                leaveRequest.getId(),
                LeaveStatus.APPROVED,
                leaveRequest.getStartDate().minusDays(7),
                leaveRequest.getEndDate().plusDays(7)
        )) {
            warnings.add("L'employé a déjà une absence approuvée proche de cette période.");
        }

        if (includesWeekend(leaveRequest.getStartDate(), leaveRequest.getEndDate())) {
            warnings.add("La période contient des jours de weekend.");
        }

        if (leaveRequest.getTotalDays() > 10) {
            warnings.add("La durée demandée dépasse 10 jours.");
        }

        if (leaveRequest.getType() == LeaveType.SICK_LEAVE && leaveRequest.getDocumentStoragePath() == null) {
            warnings.add("Un justificatif maladie est recommandé.");
        }

        return LeaveValidationResponse.builder()
                .valid(blockingErrors.isEmpty())
                .blockingErrors(blockingErrors)
                .warnings(warnings)
                .requestedDays(leaveRequest.getTotalDays())
                .remainingLeaveBalance(remainingBalance)
                .affectsPayroll(affectsPayroll)
                .build();
    }

    private boolean includesWeekend(LocalDate startDate, LocalDate endDate) {
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            if (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
                return true;
            }
            cursor = cursor.plusDays(1);
        }
        return false;
    }
}
