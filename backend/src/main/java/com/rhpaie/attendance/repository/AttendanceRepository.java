package com.rhpaie.attendance.repository;

import com.rhpaie.attendance.entity.AttendanceRecord;
import com.rhpaie.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long>, JpaSpecificationExecutor<AttendanceRecord> {

    Optional<AttendanceRecord> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    List<AttendanceRecord> findByEmployeeIdOrderByWorkDateDesc(Long employeeId);

    List<AttendanceRecord> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(Long employeeId, LocalDate startDate, LocalDate endDate);

    boolean existsByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    long countByWorkDateAndStatus(LocalDate workDate, AttendanceStatus status);

    @Query("""
            select coalesce(sum(a.overtimeMinutes), 0)
            from AttendanceRecord a
            where a.workDate between :startDate and :endDate
            """)
    Integer sumOvertimeMinutesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            select coalesce(sum(a.workedHours), 0)
            from AttendanceRecord a
            where a.workDate between :startDate and :endDate
            """)
    BigDecimal sumWorkedHoursBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
