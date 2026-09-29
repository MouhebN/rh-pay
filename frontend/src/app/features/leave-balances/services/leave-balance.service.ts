import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { LeaveBalance, LeaveBalanceSummary, LeaveBalanceUpdateRequest } from '../models/leave-balance.model';

@Injectable({ providedIn: 'root' })
export class LeaveBalanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/leave-balances';

  findAll(year?: number): Observable<ApiResponse<LeaveBalance[]>> {
    const params = year ? new HttpParams().set('year', year) : undefined;
    return this.http.get<ApiResponse<LeaveBalance[]>>(this.baseUrl, { params });
  }

  findByEmployee(employeeId: number, year?: number): Observable<ApiResponse<LeaveBalance>> {
    const params = year ? new HttpParams().set('year', year) : undefined;
    return this.http.get<ApiResponse<LeaveBalance>>(`${this.baseUrl}/employee/${employeeId}`, { params });
  }

  updateAcquiredDays(employeeId: number, request: LeaveBalanceUpdateRequest): Observable<ApiResponse<LeaveBalance>> {
    return this.http.put<ApiResponse<LeaveBalance>>(`${this.baseUrl}/employee/${employeeId}`, request);
  }

  findMine(year?: number): Observable<ApiResponse<LeaveBalanceSummary>> {
    const params = year ? new HttpParams().set('year', year) : undefined;
    return this.http.get<ApiResponse<LeaveBalanceSummary>>(`${this.baseUrl}/me`, { params });
  }
}
