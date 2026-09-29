package com.rhpaie.settings.entity;

import com.rhpaie.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "app_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSettings {

    public static final String DEFAULT_COMPANY_NAME = "RH PaiePro";
    public static final LocalTime DEFAULT_WORK_START_TIME = LocalTime.of(9, 0);
    public static final LocalTime DEFAULT_WORK_END_TIME = LocalTime.of(17, 0);
    public static final BigDecimal DEFAULT_EXPECTED_DAILY_HOURS = BigDecimal.valueOf(8);
    public static final BigDecimal DEFAULT_MONTHLY_REFERENCE_HOURS = BigDecimal.valueOf(173.33);
    public static final BigDecimal DEFAULT_ANNUAL_LEAVE_DAYS = BigDecimal.valueOf(24);
    public static final BigDecimal DEFAULT_OVERTIME_MULTIPLIER = BigDecimal.valueOf(1.25);
    public static final String DEFAULT_CURRENCY = "TND";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(length = 255)
    private String companyAddress;

    @Column(length = 120)
    private String companyEmail;

    @Column(length = 50)
    private String companyPhone;

    @Column(nullable = false)
    private LocalTime workStartTime;

    @Column(nullable = false)
    private LocalTime workEndTime;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal expectedDailyHours;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal monthlyReferenceHours;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultAnnualLeaveDays;

    @Column(nullable = false)
    private Boolean allowNegativeLeaveBalance;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal overtimeMultiplier;

    @Column(nullable = false)
    private Boolean deductLateMinutesFromSalary;

    @Column(nullable = false, length = 10)
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private UserAccount updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
