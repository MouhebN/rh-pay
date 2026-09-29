package com.rhpaie.employeeexit.repository;

import com.rhpaie.employeeexit.entity.EmployeeExitDocument;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeExitDocumentRepository extends JpaRepository<EmployeeExitDocument, Long> {

    @EntityGraph(attributePaths = {"employeeExit", "uploadedBy"})
    List<EmployeeExitDocument> findByEmployeeExitIdOrderByUploadedAtDesc(Long employeeExitId);

    @EntityGraph(attributePaths = {"employeeExit", "employeeExit.employee", "uploadedBy"})
    Optional<EmployeeExitDocument> findByIdAndEmployeeExitId(Long id, Long employeeExitId);

    @EntityGraph(attributePaths = {"employeeExit", "employeeExit.employee", "uploadedBy"})
    @Query("""
            select d from EmployeeExitDocument d
            where d.visibleToEmployee = true and d.employeeExit.employee.userAccount.username = :username
            order by d.uploadedAt desc
            """)
    List<EmployeeExitDocument> findVisibleForEmployee(@Param("username") String username);

    @EntityGraph(attributePaths = {"employeeExit", "employeeExit.employee", "uploadedBy"})
    @Query("""
            select d from EmployeeExitDocument d
            where d.id = :id and d.visibleToEmployee = true and d.employeeExit.employee.userAccount.username = :username
            """)
    Optional<EmployeeExitDocument> findVisibleForEmployeeById(@Param("username") String username, @Param("id") Long id);
}
