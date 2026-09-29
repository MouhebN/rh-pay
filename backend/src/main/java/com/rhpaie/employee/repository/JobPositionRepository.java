package com.rhpaie.employee.repository;

import com.rhpaie.employee.entity.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {

    boolean existsByTitle(String title);

    Optional<JobPosition> findByTitle(String title);
}
