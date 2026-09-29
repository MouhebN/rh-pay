package com.rhpaie.employee.repository;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.time.LocalDateTime;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByMatricule(String matricule);

    boolean existsByEmail(String email);

    Optional<Employee> findByMatricule(String matricule);

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByUserAccountUsername(String username);

    Optional<Employee> findByUserAccountId(Long userAccountId);

    List<Employee> findByStatus(EmploymentStatus status);

    List<Employee> findByStatusIn(Collection<EmploymentStatus> statuses);

    long countByStatus(EmploymentStatus status);

    long countByStatusIn(Collection<EmploymentStatus> statuses);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByRibIsNullOrRib(String rib);
}
