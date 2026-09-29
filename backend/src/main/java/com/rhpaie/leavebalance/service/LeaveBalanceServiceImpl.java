package com.rhpaie.leavebalance.service;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.repository.EmployeeRepository;
import com.rhpaie.leavebalance.dto.LeaveBalanceResponse;
import com.rhpaie.leavebalance.dto.LeaveBalanceSummaryResponse;
import com.rhpaie.leavebalance.dto.LeaveBalanceUpdateRequest;
import com.rhpaie.leavebalance.entity.LeaveBalance;
import com.rhpaie.leavebalance.mapper.LeaveBalanceMapper;
import com.rhpaie.leavebalance.repository.LeaveBalanceRepository;
import com.rhpaie.settings.entity.AppSettings;
import com.rhpaie.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveBalanceServiceImpl implements LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final SettingsService settingsService;

    @Override
    @Transactional
    public List<LeaveBalanceResponse> findAll(Integer year) {
        Integer resolvedYear = resolveYear(year);
        employeeRepository.findAll().forEach(employee -> initializeBalanceIfMissing(employee, resolvedYear));
        return leaveBalanceRepository.findByYearOrderByEmployeeLastNameAscEmployeeFirstNameAsc(resolvedYear)
                .stream()
                .map(leaveBalanceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public LeaveBalanceResponse findByEmployee(Long employeeId, Integer year) {
        Employee employee = findEmployee(employeeId);
        return leaveBalanceMapper.toResponse(initializeBalanceIfMissing(employee, resolveYear(year)));
    }

    @Override
    @Transactional
    public LeaveBalanceResponse updateAcquiredDays(Long employeeId, LeaveBalanceUpdateRequest request) {
        Employee employee = findEmployee(employeeId);
        LeaveBalance balance = initializeBalanceIfMissing(employee, resolveYear(request.year()));
        balance.setAcquiredDays(request.acquiredDays() == null ? defaultAcquiredDays() : request.acquiredDays());
        balance.recalculateRemainingDays();
        return leaveBalanceMapper.toResponse(leaveBalanceRepository.save(balance));
    }

    @Override
    @Transactional
    public LeaveBalanceSummaryResponse findMyBalance(String username, Integer year) {
        Employee employee = employeeRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No employee profile is linked to the current account"));
        return leaveBalanceMapper.toSummaryResponse(initializeBalanceIfMissing(employee, resolveYear(year)));
    }

    @Override
    @Transactional
    public void reservePendingDays(Employee employee, Integer year, Double days) {
        LeaveBalance balance = initializeBalanceIfMissing(employee, year);
        checkSufficientBalance(balance, days);
        balance.setPendingDays(balance.getPendingDays() + days);
        balance.recalculateRemainingDays();
        leaveBalanceRepository.save(balance);
    }

    @Override
    @Transactional
    public void approvePendingDays(Employee employee, Integer year, Double days) {
        LeaveBalance balance = initializeBalanceIfMissing(employee, year);
        balance.setPendingDays(Math.max(0.0, balance.getPendingDays() - days));
        balance.setConsumedDays(balance.getConsumedDays() + days);
        balance.recalculateRemainingDays();
        leaveBalanceRepository.save(balance);
    }

    @Override
    @Transactional
    public void consumeApprovedDays(Employee employee, Integer year, Double days) {
        LeaveBalance balance = initializeBalanceIfMissing(employee, year);
        checkSufficientBalance(balance, days);
        balance.setConsumedDays(balance.getConsumedDays() + days);
        balance.recalculateRemainingDays();
        leaveBalanceRepository.save(balance);
    }

    @Override
    @Transactional
    public void releasePendingDays(Employee employee, Integer year, Double days) {
        LeaveBalance balance = initializeBalanceIfMissing(employee, year);
        balance.setPendingDays(Math.max(0.0, balance.getPendingDays() - days));
        balance.recalculateRemainingDays();
        leaveBalanceRepository.save(balance);
    }

    @Override
    @Transactional(readOnly = true)
    public void checkSufficientBalance(Employee employee, Integer year, Double days) {
        checkSufficientBalance(initializeBalanceIfMissing(employee, year), days);
    }

    @Override
    @Transactional
    public Double getRemainingDays(Employee employee, Integer year) {
        LeaveBalance balance = initializeBalanceIfMissing(employee, year);
        balance.recalculateRemainingDays();
        return balance.getRemainingDays();
    }

    private LeaveBalance initializeBalanceIfMissing(Employee employee, Integer year) {
        double defaultDays = defaultAcquiredDays();
        return leaveBalanceRepository.findByEmployeeIdAndYear(employee.getId(), year)
                .orElseGet(() -> leaveBalanceRepository.save(LeaveBalance.builder()
                        .employee(employee)
                        .year(year)
                        .acquiredDays(defaultDays)
                        .consumedDays(0.0)
                        .pendingDays(0.0)
                        .remainingDays(defaultDays)
                        .build()));
    }

    private void checkSufficientBalance(LeaveBalance balance, Double days) {
        balance.recalculateRemainingDays();
        if (!settingsService.getCurrentSettings().getAllowNegativeLeaveBalance() && balance.getRemainingDays() < days) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient leave balance");
        }
    }

    private double defaultAcquiredDays() {
        AppSettings settings = settingsService.getCurrentSettings();
        return settings.getDefaultAnnualLeaveDays().doubleValue();
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private Integer resolveYear(Integer year) {
        return year == null ? LocalDate.now().getYear() : year;
    }
}
