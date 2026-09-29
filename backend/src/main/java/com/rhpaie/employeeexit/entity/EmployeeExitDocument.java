package com.rhpaie.employeeexit.entity;

import com.rhpaie.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_exit_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeExitDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_exit_id", nullable = false)
    private EmployeeExit employeeExit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EmployeeExitDocumentType type;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false, length = 500)
    private String storagePath;

    private Long fileSize;

    @Builder.Default
    @Column(nullable = false)
    private Boolean visibleToEmployee = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private UserAccount uploadedBy;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (visibleToEmployee == null) {
            visibleToEmployee = false;
        }
    }
}
