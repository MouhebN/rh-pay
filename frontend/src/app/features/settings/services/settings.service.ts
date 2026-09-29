import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { AppSettings } from '../models/app-settings.model';
import { AppSettingsUpdateRequest } from '../models/app-settings-update-request.model';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/settings';

  get(): Observable<ApiResponse<AppSettings>> {
    return this.http.get<ApiResponse<AppSettings>>(this.baseUrl);
  }

  update(request: AppSettingsUpdateRequest): Observable<ApiResponse<AppSettings>> {
    return this.http.put<ApiResponse<AppSettings>>(this.baseUrl, request);
  }

  resetDefaults(): Observable<ApiResponse<AppSettings>> {
    return this.http.post<ApiResponse<AppSettings>>(`${this.baseUrl}/reset-defaults`, {});
  }
}
