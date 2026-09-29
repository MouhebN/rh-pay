package com.rhpaie.settings.service;

import com.rhpaie.settings.dto.AppSettingsResponse;
import com.rhpaie.settings.dto.AppSettingsUpdateRequest;
import com.rhpaie.settings.entity.AppSettings;
import com.rhpaie.settings.mapper.AppSettingsMapper;
import com.rhpaie.settings.repository.AppSettingsRepository;
import com.rhpaie.user.entity.UserAccount;
import com.rhpaie.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final AppSettingsRepository appSettingsRepository;
    private final UserRepository userRepository;
    private final AppSettingsMapper appSettingsMapper;

    @Override
    @Transactional
    public AppSettings getCurrentSettings() {
        return appSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> appSettingsRepository.save(defaultSettings(null)));
    }

    @Override
    @Transactional
    public AppSettingsResponse get() {
        return appSettingsMapper.toResponse(getCurrentSettings());
    }

    @Override
    @Transactional
    public AppSettingsResponse update(AppSettingsUpdateRequest request, String username) {
        validate(request);
        AppSettings settings = getCurrentSettings();
        apply(settings, request);
        settings.setUpdatedBy(findUser(username));
        return appSettingsMapper.toResponse(appSettingsRepository.save(settings));
    }

    @Override
    @Transactional
    public AppSettingsResponse resetDefaults(String username) {
        AppSettings settings = getCurrentSettings();
        AppSettings defaults = defaultSettings(findUser(username));
        settings.setCompanyName(defaults.getCompanyName());
        settings.setCompanyAddress(defaults.getCompanyAddress());
        settings.setCompanyEmail(defaults.getCompanyEmail());
        settings.setCompanyPhone(defaults.getCompanyPhone());
        settings.setWorkStartTime(defaults.getWorkStartTime());
        settings.setWorkEndTime(defaults.getWorkEndTime());
        settings.setExpectedDailyHours(defaults.getExpectedDailyHours());
        settings.setMonthlyReferenceHours(defaults.getMonthlyReferenceHours());
        settings.setDefaultAnnualLeaveDays(defaults.getDefaultAnnualLeaveDays());
        settings.setAllowNegativeLeaveBalance(defaults.getAllowNegativeLeaveBalance());
        settings.setOvertimeMultiplier(defaults.getOvertimeMultiplier());
        settings.setDeductLateMinutesFromSalary(defaults.getDeductLateMinutesFromSalary());
        settings.setCurrency(defaults.getCurrency());
        settings.setUpdatedBy(defaults.getUpdatedBy());
        return appSettingsMapper.toResponse(appSettingsRepository.save(settings));
    }

    private void apply(AppSettings settings, AppSettingsUpdateRequest request) {
        settings.setCompanyName(request.companyName().trim());
        settings.setCompanyAddress(clean(request.companyAddress()));
        settings.setCompanyEmail(clean(request.companyEmail()));
        settings.setCompanyPhone(clean(request.companyPhone()));
        settings.setWorkStartTime(request.workStartTime());
        settings.setWorkEndTime(request.workEndTime());
        settings.setExpectedDailyHours(scale(request.expectedDailyHours()));
        settings.setMonthlyReferenceHours(scale(request.monthlyReferenceHours()));
        settings.setDefaultAnnualLeaveDays(scale(request.defaultAnnualLeaveDays()));
        settings.setAllowNegativeLeaveBalance(request.allowNegativeLeaveBalance());
        settings.setOvertimeMultiplier(scale(request.overtimeMultiplier()));
        settings.setDeductLateMinutesFromSalary(request.deductLateMinutesFromSalary());
        settings.setCurrency(request.currency().trim().toUpperCase());
    }

    private void validate(AppSettingsUpdateRequest request) {
        if (!request.workStartTime().isBefore(request.workEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure de début doit être avant l'heure de fin.");
        }
        if (request.currency() == null || request.currency().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La devise est obligatoire.");
        }
    }

    private AppSettings defaultSettings(UserAccount updatedBy) {
        return AppSettings.builder()
                .companyName(AppSettings.DEFAULT_COMPANY_NAME)
                .companyAddress("")
                .companyEmail("")
                .companyPhone("")
                .workStartTime(AppSettings.DEFAULT_WORK_START_TIME)
                .workEndTime(AppSettings.DEFAULT_WORK_END_TIME)
                .expectedDailyHours(AppSettings.DEFAULT_EXPECTED_DAILY_HOURS)
                .monthlyReferenceHours(AppSettings.DEFAULT_MONTHLY_REFERENCE_HOURS)
                .defaultAnnualLeaveDays(AppSettings.DEFAULT_ANNUAL_LEAVE_DAYS)
                .allowNegativeLeaveBalance(false)
                .overtimeMultiplier(AppSettings.DEFAULT_OVERTIME_MULTIPLIER)
                .deductLateMinutesFromSalary(false)
                .currency(AppSettings.DEFAULT_CURRENCY)
                .updatedBy(updatedBy)
                .build();
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
