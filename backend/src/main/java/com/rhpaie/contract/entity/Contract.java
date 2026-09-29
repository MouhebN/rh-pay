package com.rhpaie.contract.entity;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContractType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContractStatus status;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate trialPeriodEndDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salaryAtSignature;

    @Column(unique = true, length = 100)
    private String contractReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String documentFileName;

    private String documentContentType;

    private String documentStoragePath;

    private Long documentSize;

    private LocalDateTime documentUploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private UserAccount updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminated_by_id")
    private UserAccount terminatedBy;

    private LocalDate terminationDate;

    @Column(columnDefinition = "TEXT")
    private String terminationReason;

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
            status = ContractStatus.DRAFT;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
