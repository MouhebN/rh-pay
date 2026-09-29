package com.rhpaie.settings.service;

import com.rhpaie.settings.dto.AppSettingsResponse;
import com.rhpaie.settings.dto.AppSettingsUpdateRequest;
import com.rhpaie.settings.entity.AppSettings;

public interface SettingsService {

    AppSettings getCurrentSettings();

    AppSettingsResponse get();

    AppSettingsResponse update(AppSettingsUpdateRequest request, String username);

    AppSettingsResponse resetDefaults(String username);
}
