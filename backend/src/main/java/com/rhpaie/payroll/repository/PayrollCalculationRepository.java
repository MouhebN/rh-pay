package com.rhpaie.payroll.repository;

import com.rhpaie.payroll.entity.PayrollCalculation;
import com.rhpaie.payroll.entity.PayrollCalculationStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayrollCalculationRepository extends JpaRepository<PayrollCalculation, Long>, JpaSpecificationExecutor<PayrollCalculation> {

    Optional<PayrollCalculation> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    List<PayrollCalculation> findByEmployeeIdAndStatusOrderByYearDescMonthDesc(Long employeeId, PayrollCalculationStatus status);

    List<PayrollCalculation> findByMonthAndYearAndStatus(Integer month, Integer year, PayrollCalculationStatus status);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.jobPosition"})
    @Query("""
            select p from PayrollCalculation p
            where p.month = :month and p.year = :year and p.status = :status
            order by p.employee.lastName asc, p.employee.firstName asc
            """)
    List<PayrollCalculation> findFinalizedForExport(@Param("month") Integer month, @Param("year") Integer year, @Param("status") PayrollCalculationStatus status);

    long countByMonthAndYear(Integer month, Integer year);

    long countByMonthAndYearAndStatus(Integer month, Integer year, PayrollCalculationStatus status);

    @Query("select coalesce(sum(p.grossSalary), 0) from PayrollCalculation p where p.month = :month and p.year = :year")
    BigDecimal sumGross(@Param("month") Integer month, @Param("year") Integer year);

    @Query("select coalesce(sum(p.netSalary), 0) from PayrollCalculation p where p.month = :month and p.year = :year")
    BigDecimal sumNet(@Param("month") Integer month, @Param("year") Integer year);

    @Query("select coalesce(sum(p.grossSalary), 0) from PayrollCalculation p where p.month = :month and p.year = :year and p.status = :status")
    BigDecimal sumGrossByStatus(@Param("month") Integer month, @Param("year") Integer year, @Param("status") PayrollCalculationStatus status);

    @Query("select coalesce(sum(p.netSalary), 0) from PayrollCalculation p where p.month = :month and p.year = :year and p.status = :status")
    BigDecimal sumNetByStatus(@Param("month") Integer month, @Param("year") Integer year, @Param("status") PayrollCalculationStatus status);

    @Query("select coalesce(sum(p.totalDeductions), 0) from PayrollCalculation p where p.month = :month and p.year = :year")
    BigDecimal sumDeductions(@Param("month") Integer month, @Param("year") Integer year);

    @Query("select coalesce(sum(p.bonusAmount), 0) from PayrollCalculation p where p.month = :month and p.year = :year")
    BigDecimal sumBonuses(@Param("month") Integer month, @Param("year") Integer year);

    @Query("select coalesce(sum(p.advanceAmount), 0) from PayrollCalculation p where p.month = :month and p.year = :year")
    BigDecimal sumAdvances(@Param("month") Integer month, @Param("year") Integer year);

    @Query("select coalesce(sum(p.overtimeAmount), 0) from PayrollCalculation p where p.month = :month and p.year = :year")
    BigDecimal sumOvertimeAmount(@Param("month") Integer month, @Param("year") Integer year);

    @Query("select coalesce(sum(p.unpaidAbsenceDeduction), 0) from PayrollCalculation p where p.month = :month and p.year = :year")
    BigDecimal sumUnpaidAbsenceDeduction(@Param("month") Integer month, @Param("year") Integer year);
}
