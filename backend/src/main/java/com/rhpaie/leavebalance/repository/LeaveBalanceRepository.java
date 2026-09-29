package com.rhpaie.leavebalance.repository;

import com.rhpaie.leavebalance.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByYearOrderByEmployeeLastNameAscEmployeeFirstNameAsc(Integer year);

    Optional<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);
}
