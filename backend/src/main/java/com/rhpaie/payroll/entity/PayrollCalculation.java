package com.rhpaie.payroll.entity;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.payrollvariable.entity.PayrollVariable;
import com.rhpaie.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payroll_calculations",
        uniqueConstraints = @UniqueConstraint(name = "uk_payroll_calculations_employee_month_year", columnNames = {"employee_id", "month", "year"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_variable_id", nullable = false)
    private PayrollVariable payrollVariable;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal unpaidAbsenceDays;

    @Column(nullable = false)
    private Integer lateMinutes;

    @Column(nullable = false)
    private Integer overtimeMinutes;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal bonusAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deductionAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unpaidAbsenceDeduction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lateDeduction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal overtimeAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDeductions;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayrollCalculationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculated_by_id")
    private UserAccount calculatedBy;

    private LocalDateTime calculatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalized_by_id")
    private UserAccount finalizedBy;

    private LocalDateTime finalizedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = PayrollCalculationStatus.DRAFT;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
