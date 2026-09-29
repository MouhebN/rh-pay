package com.rhpaie.contract.repository;

import com.rhpaie.contract.entity.Contract;
import com.rhpaie.contract.entity.ContractStatus;
import com.rhpaie.contract.entity.ContractType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {

    boolean existsByEmployeeIdAndStatus(Long employeeId, ContractStatus status);

    List<Contract> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    long countByStatus(ContractStatus status);

    long countByType(ContractType type);

    long countByStatusAndEndDateBetween(ContractStatus status, LocalDate start, LocalDate end);

    long countByStatusAndEndDateBefore(ContractStatus status, LocalDate date);

    long countByDocumentStoragePathIsNull();

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.jobPosition", "createdBy", "updatedBy", "terminatedBy"})
    @Query("select c from Contract c where c.employee.id = :employeeId order by c.startDate desc")
    List<Contract> findDetailedByEmployeeId(@Param("employeeId") Long employeeId);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.jobPosition", "createdBy", "updatedBy", "terminatedBy"})
    @Query("""
            select c from Contract c
            where c.status = :status and c.endDate is not null and c.endDate between :start and :end
            order by c.endDate asc
            """)
    List<Contract> findExpiringSoon(@Param("status") ContractStatus status, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.jobPosition", "createdBy", "updatedBy", "terminatedBy"})
    @Query("""
            select c from Contract c
            where (c.status = :activeStatus and c.endDate is not null and c.endDate < :today)
               or c.status = :expiredStatus
            order by c.endDate asc
            """)
    List<Contract> findExpiredForMonitoring(
            @Param("activeStatus") ContractStatus activeStatus,
            @Param("expiredStatus") ContractStatus expiredStatus,
            @Param("today") LocalDate today
    );

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.jobPosition", "createdBy", "updatedBy", "terminatedBy"})
    List<Contract> findByDocumentStoragePathIsNullOrderByStartDateDesc();

    @Query("select c from Contract c where c.status = :status and c.endDate is not null and c.endDate < :today")
    List<Contract> findExpiredActiveContracts(@Param("status") ContractStatus status, @Param("today") LocalDate today);

    long countByEmployeeId(Long employeeId);
}
