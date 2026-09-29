package com.rhpaie.payroll.service;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.notification.entity.NotificationType;
import com.rhpaie.notification.service.AppNotificationService;
import com.rhpaie.payroll.dto.*;
import com.rhpaie.payroll.entity.*;
import com.rhpaie.payroll.mapper.PayrollCalculationMapper;
import com.rhpaie.payroll.repository.PayrollCalculationRepository;
import com.rhpaie.payrollvariable.entity.*;
import com.rhpaie.payrollvariable.repository.PayrollVariableRepository;
import com.rhpaie.settings.entity.AppSettings;
import com.rhpaie.settings.service.SettingsService;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollCalculationServiceImpl implements PayrollCalculationService {

    private static final BigDecimal DAYS_PER_MONTH = BigDecimal.valueOf(30);

    private final PayrollCalculationRepository calculationRepository;
    private final PayrollVariableRepository variableRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollCalculationMapper mapper;
    private final AppNotificationService notificationService;
    private final SettingsService settingsService;

    @Override
    @Transactional
    public PayrollCalculationGenerationSummaryResponse generate(PayrollCalculationGenerateRequest request, String username) {
        validatePeriod(request.month(), request.year());
        UserAccount calculatedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        int generated = 0;
        int updated = 0;
        int skippedFinalized = 0;
        int skippedInvalid = 0;

        for (PayrollVariable variable : variableRepository.findByMonthAndYearAndStatus(request.month(), request.year(), PayrollVariableStatus.VALIDATED)) {
            Employee employee = variable.getEmployee();
            if (employee.getBaseSalary() == null) {
                skippedInvalid++;
                continue;
            }
            PayrollCalculation existing = calculationRepository
                    .findByEmployeeIdAndMonthAndYear(employee.getId(), request.month(), request.year())
                    .orElse(null);
            if (existing != null && existing.getStatus() == PayrollCalculationStatus.FINALIZED) {
                skippedFinalized++;
                continue;
            }

            PayrollCalculation target = existing == null ? new PayrollCalculation() : existing;
            applyCalculation(target, variable, calculatedBy);
            calculationRepository.save(target);
            variable.setStatus(PayrollVariableStatus.LOCKED);
            variableRepository.save(variable);
            if (existing == null) {
                generated++;
            } else {
                updated++;
            }
        }

        return PayrollCalculationGenerationSummaryResponse.builder()
                .generatedCount(generated)
                .updatedDraftCount(updated)
                .skippedFinalizedCount(skippedFinalized)
                .skippedInvalidVariablesCount(skippedInvalid)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollCalculationResponse> findAll(PayrollCalculationFilterRequest filter) {
        return calculationRepository.findAll(buildSpecification(filter))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollCalculationDetailsResponse findById(Long id) {
        return mapper.toDetails(findCalculation(id));
    }

    @Override
    @Transactional
    public PayrollCalculationDetailsResponse finalizeCalculation(Long id, String username) {
        PayrollCalculation calculation = findCalculation(id);
        if (calculation.getStatus() != PayrollCalculationStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les paies en brouillon peuvent être finalisées.");
        }
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        calculation.setStatus(PayrollCalculationStatus.FINALIZED);
        calculation.setFinalizedBy(user);
        calculation.setFinalizedAt(LocalDateTime.now());
        PayrollCalculation saved = calculationRepository.save(calculation);
        notifyEmployee(saved);
        return mapper.toDetails(saved);
    }

    @Override
    @Transactional
    public List<PayrollCalculationResponse> finalizeBulk(PayrollCalculationBulkFinalizeRequest request, String username) {
        return request.ids().stream()
                .map(id -> mapper.toResponse(findAndFinalize(id, username)))
                .toList();
    }

    @Override
    @Transactional
    public PayrollCalculationDetailsResponse cancel(Long id) {
        PayrollCalculation calculation = findCalculation(id);
        if (calculation.getStatus() != PayrollCalculationStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les paies en brouillon peuvent être annulées.");
        }
        calculation.setStatus(PayrollCalculationStatus.CANCELLED);
        PayrollVariable variable = calculation.getPayrollVariable();
        if (variable.getStatus() == PayrollVariableStatus.LOCKED) {
            variable.setStatus(PayrollVariableStatus.VALIDATED);
            variableRepository.save(variable);
        }
        return mapper.toDetails(calculationRepository.save(calculation));
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollCalculationSummaryResponse summary(Integer month, Integer year) {
        validatePeriod(month, year);
        return PayrollCalculationSummaryResponse.builder()
                .totalEmployeesCalculated(calculationRepository.countByMonthAndYear(month, year))
                .draftCount(calculationRepository.countByMonthAndYearAndStatus(month, year, PayrollCalculationStatus.DRAFT))
                .finalizedCount(calculationRepository.countByMonthAndYearAndStatus(month, year, PayrollCalculationStatus.FINALIZED))
                .cancelledCount(calculationRepository.countByMonthAndYearAndStatus(month, year, PayrollCalculationStatus.CANCELLED))
                .totalGrossSalary(calculationRepository.sumGross(month, year))
                .totalNetSalary(calculationRepository.sumNet(month, year))
                .totalDeductions(calculationRepository.sumDeductions(month, year))
                .totalBonuses(calculationRepository.sumBonuses(month, year))
                .totalAdvances(calculationRepository.sumAdvances(month, year))
                .totalOvertimeAmount(calculationRepository.sumOvertimeAmount(month, year))
                .totalUnpaidAbsenceDeduction(calculationRepository.sumUnpaidAbsenceDeduction(month, year))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollCalculationResponse> findMine(String username, Integer year) {
        Employee employee = employeeRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No employee profile is linked to the current account"));
        return calculationRepository.findByEmployeeIdAndStatusOrderByYearDescMonthDesc(employee.getId(), PayrollCalculationStatus.FINALIZED)
                .stream()
                .filter(c -> year == null || c.getYear().equals(year))
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollCalculationDetailsResponse findMineById(String username, Long id) {
        Employee employee = employeeRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No employee profile is linked to the current account"));
        PayrollCalculation calculation = findCalculation(id);
        if (!calculation.getEmployee().getId().equals(employee.getId()) || calculation.getStatus() != PayrollCalculationStatus.FINALIZED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez consulter que vos paies finalisées.");
        }
        return mapper.toDetails(calculation);
    }

    private PayrollCalculation findAndFinalize(Long id, String username) {
        finalizeCalculation(id, username);
        return findCalculation(id);
    }

    private void applyCalculation(PayrollCalculation c, PayrollVariable v, UserAccount calculatedBy) {
        AppSettings settings = settingsService.getCurrentSettings();
        BigDecimal baseSalary = money(v.getEmployee().getBaseSalary());
        BigDecimal dailyRate = divide(baseSalary, DAYS_PER_MONTH);
        BigDecimal hourlyRate = divide(baseSalary, settings.getMonthlyReferenceHours());
        BigDecimal overtimeHours = divide(BigDecimal.valueOf(v.getOvertimeMinutes()), BigDecimal.valueOf(60));
        BigDecimal lateHours = divide(BigDecimal.valueOf(v.getLateMinutes()), BigDecimal.valueOf(60));

        // Simplified academic formula. Future versions can replace this block with legal payroll rules.
        BigDecimal unpaidAbsenceDeduction = money(v.getUnpaidAbsenceDays().multiply(dailyRate));
        BigDecimal lateDeduction = Boolean.TRUE.equals(settings.getDeductLateMinutesFromSalary())
                ? money(lateHours.multiply(hourlyRate))
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal overtimeAmount = money(overtimeHours.multiply(hourlyRate).multiply(settings.getOvertimeMultiplier()));
        BigDecimal grossSalary = money(baseSalary.add(v.getBonusAmount()).add(overtimeAmount));
        BigDecimal totalDeductions = money(unpaidAbsenceDeduction.add(v.getAdvanceAmount()).add(v.getDeductionAmount()).add(lateDeduction));
        BigDecimal netSalary = money(grossSalary.subtract(totalDeductions).max(BigDecimal.ZERO));

        c.setEmployee(v.getEmployee());
        c.setPayrollVariable(v);
        c.setMonth(v.getMonth());
        c.setYear(v.getYear());
        c.setBaseSalary(baseSalary);
        c.setDailyRate(dailyRate);
        c.setHourlyRate(hourlyRate);
        c.setUnpaidAbsenceDays(v.getUnpaidAbsenceDays());
        c.setLateMinutes(v.getLateMinutes());
        c.setOvertimeMinutes(v.getOvertimeMinutes());
        c.setBonusAmount(money(v.getBonusAmount()));
        c.setAdvanceAmount(money(v.getAdvanceAmount()));
        c.setDeductionAmount(money(v.getDeductionAmount()));
        c.setUnpaidAbsenceDeduction(unpaidAbsenceDeduction);
        c.setLateDeduction(lateDeduction);
        c.setOvertimeAmount(overtimeAmount);
        c.setGrossSalary(grossSalary);
        c.setTotalDeductions(totalDeductions);
        c.setNetSalary(netSalary);
        c.setStatus(PayrollCalculationStatus.DRAFT);
        c.setCalculatedBy(calculatedBy);
        c.setCalculatedAt(LocalDateTime.now());
        c.setFinalizedBy(null);
        c.setFinalizedAt(null);
    }

    private Specification<PayrollCalculation> buildSpecification(PayrollCalculationFilterRequest filter) {
        return (root, query, cb) -> {
            root.fetch("employee", JoinType.LEFT);
            root.fetch("payrollVariable", JoinType.LEFT);
            if (query != null) {
                query.orderBy(cb.desc(root.get("year")), cb.desc(root.get("month")), cb.asc(root.get("employee").get("lastName")));
            }
            var predicates = cb.conjunction();
            if (filter.month() != null) predicates = cb.and(predicates, cb.equal(root.get("month"), filter.month()));
            if (filter.year() != null) predicates = cb.and(predicates, cb.equal(root.get("year"), filter.year()));
            if (filter.employeeId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("id"), filter.employeeId()));
            if (filter.departmentId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("department").get("id"), filter.departmentId()));
            if (filter.jobPositionId() != null) predicates = cb.and(predicates, cb.equal(root.get("employee").get("jobPosition").get("id"), filter.jobPositionId()));
            if (filter.status() != null) predicates = cb.and(predicates, cb.equal(root.get("status"), filter.status()));
            return predicates;
        };
    }

    private PayrollCalculation findCalculation(Long id) {
        return calculationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll calculation not found"));
    }

    private void notifyEmployee(PayrollCalculation calculation) {
        UserAccount user = calculation.getEmployee().getUserAccount();
        if (user != null) {
            notificationService.notifyUsername(
                    user.getUsername(),
                    "Paie disponible",
                    "Votre paie du mois " + calculation.getMonth() + "/" + calculation.getYear() + " est disponible.",
                    NotificationType.SUCCESS,
                    "/employee/my-salaries"
            );
        }
    }

    private void validatePeriod(Integer month, Integer year) {
        if (month == null || month < 1 || month > 12 || year == null || year < 2020 || year > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Période invalide.");
        }
    }

    private BigDecimal divide(BigDecimal value, BigDecimal divisor) {
        return value.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
