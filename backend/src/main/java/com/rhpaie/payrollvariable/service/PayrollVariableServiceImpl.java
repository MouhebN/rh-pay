package com.rhpaie.payrollvariable.service;

import com.rhpaie.attendance.entity.AttendanceRecord;
import com.rhpaie.attendance.entity.AttendanceStatus;
import com.rhpaie.attendance.repository.AttendanceRepository;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.EmploymentStatus;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.employeeexit.entity.ExitStatus;
import com.rhpaie.employeeexit.repository.EmployeeExitRepository;
import com.rhpaie.leave.entity.LeaveRequest;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import com.rhpaie.leave.repository.LeaveRequestRepository;
import com.rhpaie.payrollvariable.dto.PayrollVariableBulkValidateRequest;
import com.rhpaie.payrollvariable.dto.PayrollVariableFilterRequest;
import com.rhpaie.payrollvariable.dto.PayrollVariableGenerateRequest;
import com.rhpaie.payrollvariable.dto.PayrollVariableGenerationSummaryResponse;
import com.rhpaie.payrollvariable.dto.PayrollVariableResponse;
import com.rhpaie.payrollvariable.dto.PayrollVariableSummaryResponse;
import com.rhpaie.payrollvariable.dto.PayrollVariableUpdateRequest;
import com.rhpaie.payrollvariable.entity.PayrollVariable;
import com.rhpaie.payrollvariable.entity.PayrollVariableStatus;
import com.rhpaie.payrollvariable.mapper.PayrollVariableMapper;
import com.rhpaie.payrollvariable.repository.PayrollVariableRepository;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayrollVariableServiceImpl implements PayrollVariableService {

    private final PayrollVariableRepository payrollVariableRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollVariableMapper payrollVariableMapper;
    private final EmployeeExitRepository employeeExitRepository;

    @Override
    @Transactional
    public PayrollVariableGenerationSummaryResponse generate(PayrollVariableGenerateRequest request, String username) {
        validatePeriod(request.month(), request.year());
        UserAccount generatedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        LocalDate monthStart = monthStart(request.month(), request.year());
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());

        int generatedCount = 0;
        int skippedValidatedCount = 0;
        int updatedDraftCount = 0;

        for (Employee employee : employeesEligibleForPayrollMonth(monthStart)) {
            PayrollSnapshot snapshot = calculateSnapshot(employee, monthStart, monthEnd);
            PayrollVariable variable = payrollVariableRepository
                    .findByEmployeeIdAndMonthAndYear(employee.getId(), request.month(), request.year())
                    .orElse(null);

            if (variable == null) {
                variable = PayrollVariable.builder()
                        .employee(employee)
                        .month(request.month())
                        .year(request.year())
                        .bonusAmount(BigDecimal.ZERO)
                        .advanceAmount(BigDecimal.ZERO)
                        .deductionAmount(BigDecimal.ZERO)
                        .status(PayrollVariableStatus.DRAFT)
                        .build();
                applySnapshot(variable, snapshot);
                variable.setGeneratedBy(generatedBy);
                variable.setGeneratedAt(LocalDateTime.now());
                payrollVariableRepository.save(variable);
                generatedCount++;
                continue;
            }

            if (variable.getStatus() != PayrollVariableStatus.DRAFT) {
                skippedValidatedCount++;
                continue;
            }

            applySnapshot(variable, snapshot);
            variable.setGeneratedBy(generatedBy);
            variable.setGeneratedAt(LocalDateTime.now());
            payrollVariableRepository.save(variable);
            updatedDraftCount++;
        }

        return PayrollVariableGenerationSummaryResponse.builder()
                .generatedCount(generatedCount)
                .skippedValidatedCount(skippedValidatedCount)
                .updatedDraftCount(updatedDraftCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollVariableResponse> findAll(PayrollVariableFilterRequest filter) {
        return payrollVariableRepository.findAll(buildSpecification(filter))
                .stream()
                .map(payrollVariableMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollVariableResponse findById(Long id) {
        return payrollVariableMapper.toResponse(findVariable(id));
    }

    @Override
    @Transactional
    public PayrollVariableResponse update(Long id, PayrollVariableUpdateRequest request) {
        PayrollVariable variable = findVariable(id);
        if (variable.getStatus() != PayrollVariableStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les variables en brouillon peuvent être modifiées.");
        }
        validateNonNegative(request.bonusAmount(), "Prime");
        validateNonNegative(request.advanceAmount(), "Avance");
        validateNonNegative(request.deductionAmount(), "Retenue");

        variable.setBonusAmount(normalizeAmount(request.bonusAmount()));
        variable.setAdvanceAmount(normalizeAmount(request.advanceAmount()));
        variable.setDeductionAmount(normalizeAmount(request.deductionAmount()));
        variable.setNotes(request.notes());
        return payrollVariableMapper.toResponse(payrollVariableRepository.save(variable));
    }

    @Override
    @Transactional
    public PayrollVariableResponse validate(Long id, String username) {
        PayrollVariable variable = findVariable(id);
        validateDraft(variable);
        UserAccount validatedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        variable.setStatus(PayrollVariableStatus.VALIDATED);
        variable.setValidatedBy(validatedBy);
        variable.setValidatedAt(LocalDateTime.now());
        return payrollVariableMapper.toResponse(payrollVariableRepository.save(variable));
    }

    @Override
    @Transactional
    public List<PayrollVariableResponse> validateBulk(PayrollVariableBulkValidateRequest request, String username) {
        return request.ids()
                .stream()
                .map(id -> validate(id, username))
                .toList();
    }

    @Override
    @Transactional
    public PayrollVariableResponse reopen(Long id) {
        PayrollVariable variable = findVariable(id);
        if (variable.getStatus() == PayrollVariableStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une variable verrouillée ne peut pas être réouverte.");
        }
        if (variable.getStatus() != PayrollVariableStatus.VALIDATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les variables validées peuvent être réouvertes.");
        }
        variable.setStatus(PayrollVariableStatus.DRAFT);
        variable.setValidatedBy(null);
        variable.setValidatedAt(null);
        return payrollVariableMapper.toResponse(payrollVariableRepository.save(variable));
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollVariableSummaryResponse summary(Integer month, Integer year) {
        validatePeriod(month, year);
        return PayrollVariableSummaryResponse.builder()
                .totalEmployees(payrollVariableRepository.countByMonthAndYear(month, year))
                .draftCount(payrollVariableRepository.countByMonthAndYearAndStatus(month, year, PayrollVariableStatus.DRAFT))
                .validatedCount(payrollVariableRepository.countByMonthAndYearAndStatus(month, year, PayrollVariableStatus.VALIDATED))
                .lockedCount(payrollVariableRepository.countByMonthAndYearAndStatus(month, year, PayrollVariableStatus.LOCKED))
                .totalBonusAmount(payrollVariableRepository.sumBonusAmount(month, year))
                .totalAdvanceAmount(payrollVariableRepository.sumAdvanceAmount(month, year))
                .totalDeductionAmount(payrollVariableRepository.sumDeductionAmount(month, year))
                .totalUnpaidAbsenceDays(payrollVariableRepository.sumUnpaidAbsenceDays(month, year))
                .totalLateMinutes(payrollVariableRepository.sumLateMinutes(month, year))
                .totalOvertimeMinutes(payrollVariableRepository.sumOvertimeMinutes(month, year))
                .build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PayrollVariable variable = findVariable(id);
        if (variable.getStatus() != PayrollVariableStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les variables en brouillon peuvent être supprimées.");
        }
        payrollVariableRepository.delete(variable);
    }

    private PayrollSnapshot calculateSnapshot(Employee employee, LocalDate monthStart, LocalDate monthEnd) {
        BigDecimal unpaidAbsenceDays = BigDecimal.ZERO;
        BigDecimal paidLeaveDays = BigDecimal.ZERO;
        BigDecimal sickLeaveDays = BigDecimal.ZERO;

        for (LeaveRequest leave : leaveRequestRepository.findEmployeeApprovedOverlappingMonth(employee.getId(), LeaveStatus.APPROVED, monthStart, monthEnd)) {
            BigDecimal days = overlapDays(leave, monthStart, monthEnd);
            if (leave.getType() == LeaveType.UNPAID_LEAVE) {
                unpaidAbsenceDays = unpaidAbsenceDays.add(days);
            } else if (leave.getType() == LeaveType.PAID_LEAVE) {
                paidLeaveDays = paidLeaveDays.add(days);
            } else if (leave.getType() == LeaveType.SICK_LEAVE) {
                sickLeaveDays = sickLeaveDays.add(days);
            }
        }

        int lateMinutes = 0;
        int overtimeMinutes = 0;
        BigDecimal workedDays = BigDecimal.ZERO;
        BigDecimal absentDays = BigDecimal.ZERO;

        for (AttendanceRecord attendance : attendanceRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(employee.getId(), monthStart, monthEnd)) {
            lateMinutes += attendance.getLateMinutes() == null ? 0 : attendance.getLateMinutes();
            overtimeMinutes += attendance.getOvertimeMinutes() == null ? 0 : attendance.getOvertimeMinutes();
            if (attendance.getStatus() == AttendanceStatus.PRESENT || attendance.getStatus() == AttendanceStatus.LATE) {
                workedDays = workedDays.add(BigDecimal.ONE);
            } else if (attendance.getStatus() == AttendanceStatus.ABSENT) {
                absentDays = absentDays.add(BigDecimal.ONE);
            }
        }

        return new PayrollSnapshot(
                scale(unpaidAbsenceDays),
                scale(paidLeaveDays),
                scale(sickLeaveDays),
                lateMinutes,
                overtimeMinutes,
                scale(workedDays),
                scale(absentDays)
        );
    }

    private List<Employee> employeesEligibleForPayrollMonth(LocalDate monthStart) {
        Map<Long, Employee> employees = new LinkedHashMap<>();
        employeeRepository.findByStatus(EmploymentStatus.ACTIVE)
                .forEach(employee -> employees.put(employee.getId(), employee));

        // Academic simplification: employees who left during the selected month are still included.
        // A future final-settlement module can handle precise partial-month calculations.
        List<Long> recentlyExitedEmployeeIds = employeeExitRepository.findEmployeeIdsCompletedOnOrAfter(ExitStatus.COMPLETED, monthStart);
        if (!recentlyExitedEmployeeIds.isEmpty()) {
            employeeRepository.findAllById(recentlyExitedEmployeeIds)
                    .forEach(employee -> employees.put(employee.getId(), employee));
        }
        return List.copyOf(employees.values());
    }

    private void applySnapshot(PayrollVariable variable, PayrollSnapshot snapshot) {
        variable.setUnpaidAbsenceDays(snapshot.unpaidAbsenceDays());
        variable.setPaidLeaveDays(snapshot.paidLeaveDays());
        variable.setSickLeaveDays(snapshot.sickLeaveDays());
        variable.setLateMinutes(snapshot.lateMinutes());
        variable.setOvertimeMinutes(snapshot.overtimeMinutes());
        variable.setWorkedDays(snapshot.workedDays());
        variable.setAbsentDays(snapshot.absentDays());
    }

    private Specification<PayrollVariable> buildSpecification(PayrollVariableFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            root.fetch("employee", JoinType.LEFT);
            root.fetch("generatedBy", JoinType.LEFT);
            root.fetch("validatedBy", JoinType.LEFT);
            if (query != null) {
                query.orderBy(
                        criteriaBuilder.desc(root.get("year")),
                        criteriaBuilder.desc(root.get("month")),
                        criteriaBuilder.asc(root.get("employee").get("lastName"))
                );
            }

            var predicates = criteriaBuilder.conjunction();
            if (filter.month() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("month"), filter.month()));
            }
            if (filter.year() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("year"), filter.year()));
            }
            if (filter.employeeId() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("employee").get("id"), filter.employeeId()));
            }
            if (filter.departmentId() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("employee").get("department").get("id"), filter.departmentId()));
            }
            if (filter.jobPositionId() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("employee").get("jobPosition").get("id"), filter.jobPositionId()));
            }
            if (filter.status() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("status"), filter.status()));
            }
            return predicates;
        };
    }

    private BigDecimal overlapDays(LeaveRequest leave, LocalDate monthStart, LocalDate monthEnd) {
        LocalDate start = leave.getStartDate().isBefore(monthStart) ? monthStart : leave.getStartDate();
        LocalDate end = leave.getEndDate().isAfter(monthEnd) ? monthEnd : leave.getEndDate();
        return BigDecimal.valueOf(ChronoUnit.DAYS.between(start, end) + 1L);
    }

    private PayrollVariable findVariable(Long id) {
        return payrollVariableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll variable not found"));
    }

    private void validateDraft(PayrollVariable variable) {
        if (variable.getStatus() != PayrollVariableStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les variables en brouillon peuvent être validées.");
        }
    }

    private void validatePeriod(Integer month, Integer year) {
        if (month == null || month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mois doit être compris entre 1 et 12.");
        }
        if (year == null || year < 2020 || year > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'année doit être comprise entre 2020 et 2100.");
        }
    }

    private LocalDate monthStart(Integer month, Integer year) {
        return LocalDate.of(year, month, 1);
    }

    private void validateNonNegative(BigDecimal value, String label) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " ne peut pas être négatif.");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return scale(value == null ? BigDecimal.ZERO : value);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record PayrollSnapshot(
            BigDecimal unpaidAbsenceDays,
            BigDecimal paidLeaveDays,
            BigDecimal sickLeaveDays,
            Integer lateMinutes,
            Integer overtimeMinutes,
            BigDecimal workedDays,
            BigDecimal absentDays
    ) {
    }
}
