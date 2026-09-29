package com.rhpaie.payrollvariable.repository;

import com.rhpaie.payrollvariable.entity.PayrollVariable;
import com.rhpaie.payrollvariable.entity.PayrollVariableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayrollVariableRepository extends JpaRepository<PayrollVariable, Long>, JpaSpecificationExecutor<PayrollVariable> {

    Optional<PayrollVariable> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    List<PayrollVariable> findByMonthAndYearAndStatus(Integer month, Integer year, PayrollVariableStatus status);

    long countByMonthAndYear(Integer month, Integer year);

    long countByMonthAndYearAndStatus(Integer month, Integer year, PayrollVariableStatus status);

    @Query("""
            select coalesce(sum(p.bonusAmount), 0)
            from PayrollVariable p
            where p.month = :month and p.year = :year
            """)
    BigDecimal sumBonusAmount(@Param("month") Integer month, @Param("year") Integer year);

    @Query("""
            select coalesce(sum(p.advanceAmount), 0)
            from PayrollVariable p
            where p.month = :month and p.year = :year
            """)
    BigDecimal sumAdvanceAmount(@Param("month") Integer month, @Param("year") Integer year);

    @Query("""
            select coalesce(sum(p.deductionAmount), 0)
            from PayrollVariable p
            where p.month = :month and p.year = :year
            """)
    BigDecimal sumDeductionAmount(@Param("month") Integer month, @Param("year") Integer year);

    @Query("""
            select coalesce(sum(p.unpaidAbsenceDays), 0)
            from PayrollVariable p
            where p.month = :month and p.year = :year
            """)
    BigDecimal sumUnpaidAbsenceDays(@Param("month") Integer month, @Param("year") Integer year);

    @Query("""
            select coalesce(sum(p.lateMinutes), 0)
            from PayrollVariable p
            where p.month = :month and p.year = :year
            """)
    Integer sumLateMinutes(@Param("month") Integer month, @Param("year") Integer year);

    @Query("""
            select coalesce(sum(p.overtimeMinutes), 0)
            from PayrollVariable p
            where p.month = :month and p.year = :year
            """)
    Integer sumOvertimeMinutes(@Param("month") Integer month, @Param("year") Integer year);
}
