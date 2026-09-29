package com.rhpaie.settings.mapper;

import com.rhpaie.settings.dto.AppSettingsResponse;
import com.rhpaie.settings.entity.AppSettings;
import com.rhpaie.user.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class AppSettingsMapper {

    public AppSettingsResponse toResponse(AppSettings settings) {
        UserAccount updatedBy = settings.getUpdatedBy();
        return AppSettingsResponse.builder()
                .id(settings.getId())
                .companyName(settings.getCompanyName())
                .companyAddress(settings.getCompanyAddress())
                .companyEmail(settings.getCompanyEmail())
                .companyPhone(settings.getCompanyPhone())
                .workStartTime(settings.getWorkStartTime())
                .workEndTime(settings.getWorkEndTime())
                .expectedDailyHours(settings.getExpectedDailyHours())
                .monthlyReferenceHours(settings.getMonthlyReferenceHours())
                .defaultAnnualLeaveDays(settings.getDefaultAnnualLeaveDays())
                .allowNegativeLeaveBalance(settings.getAllowNegativeLeaveBalance())
                .overtimeMultiplier(settings.getOvertimeMultiplier())
                .deductLateMinutesFromSalary(settings.getDeductLateMinutesFromSalary())
                .currency(settings.getCurrency())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .updatedById(updatedBy == null ? null : updatedBy.getId())
                .updatedByUsername(updatedBy == null ? null : updatedBy.getUsername())
                .build();
    }
}
