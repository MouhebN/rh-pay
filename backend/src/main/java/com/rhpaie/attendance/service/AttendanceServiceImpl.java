package com.rhpaie.attendance.service;

import com.rhpaie.attendance.dto.AttendanceAdjustRequest;
import com.rhpaie.attendance.dto.AttendanceFilterRequest;
import com.rhpaie.attendance.dto.AttendanceResponse;
import com.rhpaie.attendance.dto.AttendanceSummaryResponse;
import com.rhpaie.attendance.dto.TodayAttendanceResponse;
import com.rhpaie.attendance.entity.AttendanceRecord;
import com.rhpaie.attendance.entity.AttendanceSource;
import com.rhpaie.attendance.entity.AttendanceStatus;
import com.rhpaie.attendance.mapper.AttendanceMapper;
import com.rhpaie.attendance.repository.AttendanceRepository;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.EmploymentStatus;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.repository.LeaveRequestRepository;
import com.rhpaie.notification.entity.NotificationType;
import com.rhpaie.notification.service.AppNotificationService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceMapper attendanceMapper;
    private final AppNotificationService notificationService;
    private final SettingsService settingsService;

    @Override
    @Transactional
    public AttendanceResponse checkIn(String username) {
        Employee employee = findEmployeeForCurrentUser(username);
        validateEmployeeActive(employee);
        LocalDate today = LocalDate.now();
        if (hasApprovedLeave(employee.getId(), today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous avez une absence approuvée aujourd’hui.");
        }
        if (attendanceRepository.existsByEmployeeIdAndWorkDate(employee.getId(), today)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà pointé l'arrivée aujourd'hui.");
        }

        LocalTime now = LocalTime.now().withNano(0);
        AppSettings settings = settingsService.getCurrentSettings();
        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .workDate(today)
                .checkInTime(now)
                .lateMinutes(calculateLateMinutes(now, settings))
                .overtimeMinutes(0)
                .status(AttendanceStatus.INCOMPLETE)
                .source(AttendanceSource.WEB)
                .build();
        AttendanceRecord savedRecord = attendanceRepository.save(record);

        if (savedRecord.getLateMinutes() > 0) {
            notificationService.notifyAdmins(
                    "Pointage en retard",
                    employee.getFirstName() + " " + employee.getLastName() + " a pointé avec " + savedRecord.getLateMinutes() + " minute(s) de retard.",
                    NotificationType.WARNING,
                    "/admin/attendance"
            );
        }

        return attendanceMapper.toResponse(savedRecord);
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(String username) {
        Employee employee = findEmployeeForCurrentUser(username);
        validateEmployeeActive(employee);
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous devez pointer l'arrivée avant la sortie."));
        if (record.getCheckOutTime() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous avez déjà pointé la sortie aujourd'hui.");
        }

        record.setCheckOutTime(LocalTime.now().withNano(0));
        recalculate(record, false);
        return attendanceMapper.toResponse(attendanceRepository.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public TodayAttendanceResponse today(String username) {
        Employee employee = findEmployeeForCurrentUser(username);
        LocalDate today = LocalDate.now();
        if (isInactiveForAttendance(employee)) {
            return TodayAttendanceResponse.builder()
                    .workDate(today)
                    .canCheckIn(false)
                    .canCheckOut(false)
                    .onApprovedLeave(false)
                    .message("Votre compte employé n’est plus actif.")
                    .build();
        }
        boolean onLeave = hasApprovedLeave(employee.getId(), today);
        return attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today)
                .map(record -> TodayAttendanceResponse.builder()
                        .workDate(today)
                        .checkInTime(record.getCheckInTime())
                        .checkOutTime(record.getCheckOutTime())
                        .workedHours(record.getWorkedHours() == null ? null : record.getWorkedHours().doubleValue())
                        .lateMinutes(record.getLateMinutes())
                        .overtimeMinutes(record.getOvertimeMinutes())
                        .status(record.getStatus())
                        .canCheckIn(false)
                        .canCheckOut(record.getCheckInTime() != null && record.getCheckOutTime() == null)
                        .onApprovedLeave(onLeave || record.getStatus() == AttendanceStatus.ON_LEAVE)
                        .message(record.getCheckOutTime() != null ? "Pointage terminé" : "Pointage en cours")
                        .build())
                .orElseGet(() -> TodayAttendanceResponse.builder()
                        .workDate(today)
                        .status(onLeave ? AttendanceStatus.ON_LEAVE : null)
                        .canCheckIn(!onLeave)
                        .canCheckOut(false)
                        .onApprovedLeave(onLeave)
                        .message(onLeave ? "Absence approuvée aujourd’hui" : "Aucun pointage aujourd'hui")
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> myRecords(String username, AttendanceFilterRequest filter) {
        Employee employee = findEmployeeForCurrentUser(username);
        AttendanceFilterRequest scopedFilter = new AttendanceFilterRequest(
                employee.getId(),
                null,
                null,
                filter.startDate(),
                filter.endDate(),
                filter.status(),
                null
        );
        return attendanceRepository.findAll(buildSpecification(scopedFilter))
                .stream()
                .map(attendanceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> findAll(AttendanceFilterRequest filter) {
        return attendanceRepository.findAll(buildSpecification(filter))
                .stream()
                .map(attendanceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse findById(Long id) {
        return attendanceMapper.toResponse(findRecord(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> findByEmployee(Long employeeId) {
        return attendanceRepository.findByEmployeeIdOrderByWorkDateDesc(employeeId)
                .stream()
                .map(attendanceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AttendanceResponse adjust(Long id, AttendanceAdjustRequest request, String username) {
        AttendanceRecord record = findRecord(id);
        UserAccount adjustedBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        record.setWorkDate(request.workDate());
        record.setCheckInTime(request.checkInTime());
        record.setCheckOutTime(request.checkOutTime());
        record.setStatus(request.status());
        record.setAdminNote(request.adminNote());
        record.setSource(AttendanceSource.MANUAL);
        record.setAdjustedBy(adjustedBy);
        record.setAdjustedAt(LocalDateTime.now());
        recalculate(record, true);

        AttendanceRecord savedRecord = attendanceRepository.save(record);
        notifyEmployee(savedRecord, "Pointage ajusté", "Votre pointage du " + savedRecord.getWorkDate() + " a été ajusté par le service RH.");
        return attendanceMapper.toResponse(savedRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        return AttendanceSummaryResponse.builder()
                .presentTodayCount(attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.PRESENT))
                .lateTodayCount(attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.LATE))
                .absentTodayCount(attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.ABSENT))
                .onLeaveTodayCount(attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.ON_LEAVE))
                .incompleteTodayCount(attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.INCOMPLETE))
                .totalOvertimeMinutesThisMonth(attendanceRepository.sumOvertimeMinutesBetween(monthStart, monthEnd))
                .totalWorkedHoursThisMonth(attendanceRepository.sumWorkedHoursBetween(monthStart, monthEnd).doubleValue())
                .build();
    }

    @Override
    @Transactional
    public void generateDailyAbsences(LocalDate date) {
        LocalDate resolvedDate = date == null ? LocalDate.now() : date;
        List<Employee> activeEmployees = employeeRepository.findByStatus(EmploymentStatus.ACTIVE);
        for (Employee employee : activeEmployees) {
            if (attendanceRepository.existsByEmployeeIdAndWorkDate(employee.getId(), resolvedDate)) {
                continue;
            }
            boolean onLeave = hasApprovedLeave(employee.getId(), resolvedDate);
            attendanceRepository.save(AttendanceRecord.builder()
                    .employee(employee)
                    .workDate(resolvedDate)
                    .status(onLeave ? AttendanceStatus.ON_LEAVE : AttendanceStatus.ABSENT)
                    .source(AttendanceSource.MANUAL)
                    .lateMinutes(0)
                    .overtimeMinutes(0)
                    .workedHours(BigDecimal.ZERO)
                    .build());
        }
    }

    private Specification<AttendanceRecord> buildSpecification(AttendanceFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            root.fetch("employee", JoinType.LEFT);
            root.fetch("adjustedBy", JoinType.LEFT);
            if (query != null) {
                query.distinct(true);
                query.orderBy(criteriaBuilder.desc(root.get("workDate")), criteriaBuilder.desc(root.get("createdAt")));
            }

            var predicates = criteriaBuilder.conjunction();
            if (filter.employeeId() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("employee").get("id"), filter.employeeId()));
            }
            if (filter.departmentId() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("employee").get("department").get("id"), filter.departmentId()));
            }
            if (filter.jobPositionId() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("employee").get("jobPosition").get("id"), filter.jobPositionId()));
            }
            if (filter.startDate() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.greaterThanOrEqualTo(root.get("workDate"), filter.startDate()));
            }
            if (filter.endDate() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.lessThanOrEqualTo(root.get("workDate"), filter.endDate()));
            }
            if (filter.status() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("status"), filter.status()));
            }
            if (filter.source() != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("source"), filter.source()));
            }
            return predicates;
        };
    }

    private void recalculate(AttendanceRecord record, boolean keepManualStatus) {
        AppSettings settings = settingsService.getCurrentSettings();
        if (record.getCheckInTime() == null || record.getCheckOutTime() == null) {
            record.setWorkedHours(record.getStatus() == AttendanceStatus.ABSENT || record.getStatus() == AttendanceStatus.ON_LEAVE ? BigDecimal.ZERO : null);
            record.setLateMinutes(record.getCheckInTime() == null ? 0 : calculateLateMinutes(record.getCheckInTime(), settings));
            record.setOvertimeMinutes(0);
            if (!keepManualStatus || record.getStatus() == null) {
                record.setStatus(record.getCheckInTime() == null ? record.getStatus() : AttendanceStatus.INCOMPLETE);
            }
            return;
        }

        if (!record.getCheckOutTime().isAfter(record.getCheckInTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure de sortie doit être après l'heure d'arrivée.");
        }

        double expectedWorkHours = settings.getExpectedDailyHours().doubleValue();
        double workedHours = round(Duration.between(record.getCheckInTime(), record.getCheckOutTime()).toMinutes() / 60.0);
        int lateMinutes = calculateLateMinutes(record.getCheckInTime(), settings);
        int overtimeMinutes = Math.max(0, (int) Math.round((workedHours - expectedWorkHours) * 60));
        record.setWorkedHours(BigDecimal.valueOf(workedHours));
        record.setLateMinutes(lateMinutes);
        record.setOvertimeMinutes(overtimeMinutes);

        if (keepManualStatus && (record.getStatus() == AttendanceStatus.ABSENT || record.getStatus() == AttendanceStatus.ON_LEAVE)) {
            return;
        }
        if (workedHours < 4.0) {
            record.setStatus(AttendanceStatus.HALF_DAY);
        } else if (lateMinutes > 0) {
            record.setStatus(AttendanceStatus.LATE);
        } else if (workedHours >= expectedWorkHours) {
            record.setStatus(AttendanceStatus.PRESENT);
        } else {
            record.setStatus(AttendanceStatus.HALF_DAY);
        }
    }

    private int calculateLateMinutes(LocalTime checkInTime, AppSettings settings) {
        if (checkInTime == null || !checkInTime.isAfter(settings.getWorkStartTime())) {
            return 0;
        }
        return (int) Duration.between(settings.getWorkStartTime(), checkInTime).toMinutes();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private boolean hasApprovedLeave(Long employeeId, LocalDate date) {
        return leaveRequestRepository.existsByEmployeeIdAndStatusCoveringDate(employeeId, LeaveStatus.APPROVED, date);
    }

    private AttendanceRecord findRecord(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance record not found"));
    }

    private Employee findEmployeeForCurrentUser(String username) {
        return employeeRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No employee profile is linked to the current account"));
    }

    private void validateEmployeeActive(Employee employee) {
        if (isInactiveForAttendance(employee)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Votre compte employé n’est plus actif.");
        }
    }

    private boolean isInactiveForAttendance(Employee employee) {
        return employee.getStatus() == EmploymentStatus.LEFT || employee.getStatus() == EmploymentStatus.INACTIVE;
    }

    private void notifyEmployee(AttendanceRecord record, String title, String message) {
        UserAccount userAccount = record.getEmployee().getUserAccount();
        if (userAccount == null) {
            return;
        }
        notificationService.notifyUsername(userAccount.getUsername(), title, message, NotificationType.INFO, "/employee/my-attendance");
    }
}
