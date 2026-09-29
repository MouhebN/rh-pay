package com.rhpaie.payrollvariable.entity;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payroll_variables",
        uniqueConstraints = @UniqueConstraint(name = "uk_payroll_variables_employee_month_year", columnNames = {"employee_id", "month", "year"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal unpaidAbsenceDays;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal paidLeaveDays;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal sickLeaveDays;

    @Column(nullable = false)
    private Integer lateMinutes;

    @Column(nullable = false)
    private Integer overtimeMinutes;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal workedDays;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal absentDays;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal bonusAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deductionAmount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PayrollVariableStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_id")
    private UserAccount generatedBy;

    private LocalDateTime generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_id")
    private UserAccount validatedBy;

    private LocalDateTime validatedAt;

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
            status = PayrollVariableStatus.DRAFT;
        }
        if (unpaidAbsenceDays == null) unpaidAbsenceDays = BigDecimal.ZERO;
        if (paidLeaveDays == null) paidLeaveDays = BigDecimal.ZERO;
        if (sickLeaveDays == null) sickLeaveDays = BigDecimal.ZERO;
        if (lateMinutes == null) lateMinutes = 0;
        if (overtimeMinutes == null) overtimeMinutes = 0;
        if (workedDays == null) workedDays = BigDecimal.ZERO;
        if (absentDays == null) absentDays = BigDecimal.ZERO;
        if (bonusAmount == null) bonusAmount = BigDecimal.ZERO;
        if (advanceAmount == null) advanceAmount = BigDecimal.ZERO;
        if (deductionAmount == null) deductionAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
