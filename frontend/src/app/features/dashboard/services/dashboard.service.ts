import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { AdminDashboardSummary, EmployeeDashboardSummary } from '../models/dashboard-summary.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/dashboard';

  adminSummary(): Observable<ApiResponse<AdminDashboardSummary>> {
    return this.http.get<ApiResponse<AdminDashboardSummary>>(`${this.baseUrl}/admin-summary`);
  }

  employeeSummary(): Observable<ApiResponse<EmployeeDashboardSummary>> {
    return this.http.get<ApiResponse<EmployeeDashboardSummary>>(`${this.baseUrl}/employee-summary`);
  }
}
