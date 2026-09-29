package com.rhpaie.employeeexit.repository;

import com.rhpaie.employeeexit.entity.EmployeeExit;
import com.rhpaie.employeeexit.entity.ExitStatus;
import com.rhpaie.employeeexit.entity.ExitType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface EmployeeExitRepository extends JpaRepository<EmployeeExit, Long>, JpaSpecificationExecutor<EmployeeExit> {

    boolean existsByEmployeeIdAndStatusIn(Long employeeId, Collection<ExitStatus> statuses);

    long countByStatus(ExitStatus status);

    long countByType(ExitType type);

    long countByStatusAndExitDateBetween(ExitStatus status, LocalDate start, LocalDate end);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.jobPosition", "createdBy", "completedBy", "cancelledBy"})
    List<EmployeeExit> findByEmployeeIdOrderByExitDateDesc(Long employeeId);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.jobPosition", "createdBy", "completedBy", "cancelledBy"})
    List<EmployeeExit> findByStatusOrderByExitDateAsc(ExitStatus status);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.jobPosition", "createdBy", "completedBy", "cancelledBy"})
    @Query("""
            select e from EmployeeExit e
            where e.status = :status and e.exitDate between :start and :end
            order by e.exitDate asc
            """)
    List<EmployeeExit> findUpcoming(@Param("status") ExitStatus status, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<EmployeeExit> findByStatusAndExitDateLessThanEqual(ExitStatus status, LocalDate date);

    @Query("""
            select e.employee.id from EmployeeExit e
            where e.status = :status and e.exitDate >= :monthStart
            """)
    List<Long> findEmployeeIdsCompletedOnOrAfter(@Param("status") ExitStatus status, @Param("monthStart") LocalDate monthStart);
}
