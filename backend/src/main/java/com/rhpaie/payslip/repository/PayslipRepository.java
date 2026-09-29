package com.rhpaie.payslip.repository;

import com.rhpaie.payslip.entity.Payslip;
import com.rhpaie.payslip.entity.PayslipStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long>, JpaSpecificationExecutor<Payslip> {

    Optional<Payslip> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    List<Payslip> findByEmployeeIdAndStatusOrderByYearDescMonthDesc(Long employeeId, PayslipStatus status);

    long countByMonthAndYear(Integer month, Integer year);

    long countByMonthAndYearAndStatus(Integer month, Integer year, PayslipStatus status);

    @Query("select coalesce(sum(p.netSalary), 0) from Payslip p where p.month = :month and p.year = :year and p.status = :status")
    BigDecimal sumNet(@Param("month") Integer month, @Param("year") Integer year, @Param("status") PayslipStatus status);

    @Query("select coalesce(sum(p.grossSalary), 0) from Payslip p where p.month = :month and p.year = :year and p.status = :status")
    BigDecimal sumGross(@Param("month") Integer month, @Param("year") Integer year, @Param("status") PayslipStatus status);

    @Query("select coalesce(sum(p.totalDeductions), 0) from Payslip p where p.month = :month and p.year = :year and p.status = :status")
    BigDecimal sumDeductions(@Param("month") Integer month, @Param("year") Integer year, @Param("status") PayslipStatus status);
}
