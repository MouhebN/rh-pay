package com.rhpaie.leave.repository;

import com.rhpaie.leave.entity.LeaveRequest;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<LeaveRequest> findByStatus(LeaveStatus status);

    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    @Query("""
            select l
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status = :status
              and l.startDate <= :monthEnd
              and l.endDate >= :monthStart
            """)
    List<LeaveRequest> findEmployeeApprovedOverlappingMonth(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd
    );

    long countByStatus(LeaveStatus status);

    long countByStatusAndCreatedAtBetween(LeaveStatus status, LocalDateTime start, LocalDateTime end);

    long countByEmployeeIdAndStatusAndCreatedAtBetween(Long employeeId, LeaveStatus status, LocalDateTime start, LocalDateTime end);

    long countByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    @Query("""
            select count(l) > 0
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status in :statuses
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    boolean existsOverlappingRequest(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<LeaveStatus> statuses
    );

    @Query("""
            select count(l) > 0
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.id <> :excludedId
              and l.status in :statuses
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    boolean existsOverlappingRequestExcludingCurrent(
            @Param("employeeId") Long employeeId,
            @Param("excludedId") Long excludedId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<LeaveStatus> statuses
    );

    @Query("""
            select coalesce(sum(l.totalDays), 0)
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status = :status
              and l.startDate <= :monthEnd
              and l.endDate >= :monthStart
            """)
    Double sumDaysByEmployeeAndStatusInMonth(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd
    );

    @Query("""
            select count(l) > 0
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.id <> :excludedId
              and l.status = :status
              and l.startDate <= :closeEnd
              and l.endDate >= :closeStart
            """)
    boolean existsCloseApprovedRequest(
            @Param("employeeId") Long employeeId,
            @Param("excludedId") Long excludedId,
            @Param("status") LeaveStatus status,
            @Param("closeStart") LocalDate closeStart,
            @Param("closeEnd") LocalDate closeEnd
    );

    @Query("""
            select coalesce(sum(l.totalDays), 0)
            from LeaveRequest l
            where l.status = :status
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    Double sumDaysByStatusAndDateRange(
            @Param("status") LeaveStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select coalesce(sum(l.totalDays), 0)
            from LeaveRequest l
            where l.status = :status
              and l.type = :type
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    Double sumDaysByStatusAndTypeAndDateRange(
            @Param("status") LeaveStatus status,
            @Param("type") LeaveType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select count(distinct l.employee.id)
            from LeaveRequest l
            where l.status = :status
              and l.startDate <= :today
              and l.endDate >= :today
            """)
    long countEmployeesOnLeaveToday(@Param("status") LeaveStatus status, @Param("today") LocalDate today);

    @Query("""
            select new com.rhpaie.leave.dto.MonthlyLeaveStatResponse(
                month(l.startDate),
                coalesce(sum(l.totalDays), 0),
                count(l)
            )
            from LeaveRequest l
            where l.status = :status
              and year(l.startDate) = :year
            group by month(l.startDate)
            order by month(l.startDate)
            """)
    List<com.rhpaie.leave.dto.MonthlyLeaveStatResponse> findMonthlyStats(
            @Param("status") LeaveStatus status,
            @Param("year") Integer year
    );

    @Query("""
            select new com.rhpaie.leave.dto.LeaveTypeStatResponse(
                l.type,
                count(l),
                coalesce(sum(l.totalDays), 0)
            )
            from LeaveRequest l
            where l.status = :status
              and year(l.startDate) = :year
            group by l.type
            order by l.type
            """)
    List<com.rhpaie.leave.dto.LeaveTypeStatResponse> findLeaveTypeStats(
            @Param("status") LeaveStatus status,
            @Param("year") Integer year
    );

    @Query("""
            select new com.rhpaie.leave.dto.DepartmentLeaveStatResponse(
                coalesce(d.name, 'Non affecté'),
                count(l),
                coalesce(sum(l.totalDays), 0)
            )
            from LeaveRequest l
            left join l.employee.department d
            where l.status = :status
              and year(l.startDate) = :year
            group by d.name
            order by d.name
            """)
    List<com.rhpaie.leave.dto.DepartmentLeaveStatResponse> findDepartmentStats(
            @Param("status") LeaveStatus status,
            @Param("year") Integer year
    );

    @Query("""
            select coalesce(sum(l.totalDays), 0)
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status = :status
              and year(l.startDate) = :year
            """)
    Double sumEmployeeDaysByStatusAndYear(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("year") Integer year
    );

    @Query("""
            select coalesce(sum(l.totalDays), 0)
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status = :status
              and l.type = :type
              and year(l.startDate) = :year
            """)
    Double sumEmployeeDaysByStatusTypeAndYear(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("type") LeaveType type,
            @Param("year") Integer year
    );

    @Query("""
            select new com.rhpaie.leave.dto.MonthlyLeaveStatResponse(
                month(l.startDate),
                coalesce(sum(l.totalDays), 0),
                count(l)
            )
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status = :status
              and year(l.startDate) = :year
            group by month(l.startDate)
            order by month(l.startDate)
            """)
    List<com.rhpaie.leave.dto.MonthlyLeaveStatResponse> findEmployeeMonthlyStats(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("year") Integer year
    );

    @Query("""
            select new com.rhpaie.leave.dto.LeaveTypeStatResponse(
                l.type,
                count(l),
                coalesce(sum(l.totalDays), 0)
            )
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status = :status
              and year(l.startDate) = :year
            group by l.type
            order by l.type
            """)
    List<com.rhpaie.leave.dto.LeaveTypeStatResponse> findEmployeeLeaveTypeStats(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("year") Integer year
    );

    @Query("""
            select l
            from LeaveRequest l
            left join fetch l.employee e
            left join fetch e.department
            left join fetch e.jobPosition
            where l.startDate <= :endDate
              and l.endDate >= :startDate
              and l.status in :statuses
              and (:departmentId is null or e.department.id = :departmentId)
              and (:type is null or l.type = :type)
            order by l.startDate asc, e.lastName asc, e.firstName asc
            """)
    List<LeaveRequest> findCalendarEvents(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<LeaveStatus> statuses,
            @Param("departmentId") Long departmentId,
            @Param("type") LeaveType type
    );

    @Query("""
            select l
            from LeaveRequest l
            left join fetch l.employee e
            left join fetch e.department
            left join fetch e.jobPosition
            where e.id = :employeeId
              and l.startDate <= :endDate
              and l.endDate >= :startDate
              and l.status in :statuses
            order by l.startDate asc
            """)
    List<LeaveRequest> findEmployeeCalendarEvents(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<LeaveStatus> statuses
    );

    @Query("""
            select count(l) > 0
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status = :status
              and l.startDate <= :workDate
              and l.endDate >= :workDate
            """)
    boolean existsByEmployeeIdAndStatusCoveringDate(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("workDate") LocalDate workDate
    );
}
