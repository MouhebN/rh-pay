import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { AdminLeaveSummary, EmployeeLeaveSummary, LeaveRequest, LeaveRequestFilter } from '../models/leave-request.model';
import { LeaveCalendarEvent } from '../models/leave-calendar-event.model';
import { LeaveRequestCreateRequest } from '../models/leave-request-create-request.model';
import { LeaveReviewRequest } from '../models/leave-review-request.model';
import { AdminLeaveStatistics, EmployeeLeaveStatistics } from '../models/leave-statistics.model';
import { LeaveStatus, LeaveType } from '../models/leave-status.model';
import { LeaveValidation } from '../models/leave-validation.model';

@Injectable({ providedIn: 'root' })
export class LeaveRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/leave-requests';

  create(request: LeaveRequestCreateRequest): Observable<ApiResponse<LeaveRequest>> {
    return this.http.post<ApiResponse<LeaveRequest>>(this.baseUrl, request);
  }

  findAll(filter: LeaveRequestFilter = {}): Observable<ApiResponse<LeaveRequest[]>> {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<ApiResponse<LeaveRequest[]>>(this.baseUrl, { params });
  }

  findMyRequests(): Observable<ApiResponse<LeaveRequest[]>> {
    return this.http.get<ApiResponse<LeaveRequest[]>>(`${this.baseUrl}/my-requests`);
  }

  findById(id: number): Observable<ApiResponse<LeaveRequest>> {
    return this.http.get<ApiResponse<LeaveRequest>>(`${this.baseUrl}/${id}`);
  }

  approve(id: number): Observable<ApiResponse<LeaveRequest>> {
    return this.http.patch<ApiResponse<LeaveRequest>>(`${this.baseUrl}/${id}/approve`, {});
  }

  validate(id: number): Observable<ApiResponse<LeaveValidation>> {
    return this.http.get<ApiResponse<LeaveValidation>>(`${this.baseUrl}/${id}/validate`);
  }

  reject(id: number, request: LeaveReviewRequest): Observable<ApiResponse<LeaveRequest>> {
    return this.http.patch<ApiResponse<LeaveRequest>>(`${this.baseUrl}/${id}/reject`, request);
  }

  cancel(id: number): Observable<ApiResponse<LeaveRequest>> {
    return this.http.patch<ApiResponse<LeaveRequest>>(`${this.baseUrl}/${id}/cancel`, {});
  }

  uploadDocument(id: number, file: File): Observable<ApiResponse<LeaveRequest>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<LeaveRequest>>(`${this.baseUrl}/${id}/document`, formData);
  }

  downloadDocument(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/document`, { responseType: 'blob' });
  }

  deleteDocument(id: number): Observable<ApiResponse<LeaveRequest>> {
    return this.http.delete<ApiResponse<LeaveRequest>>(`${this.baseUrl}/${id}/document`);
  }

  adminSummary(): Observable<ApiResponse<AdminLeaveSummary>> {
    return this.http.get<ApiResponse<AdminLeaveSummary>>(`${this.baseUrl}/summary`);
  }

  mySummary(): Observable<ApiResponse<EmployeeLeaveSummary>> {
    return this.http.get<ApiResponse<EmployeeLeaveSummary>>(`${this.baseUrl}/my-summary`);
  }

  adminStatistics(year?: number): Observable<ApiResponse<AdminLeaveStatistics>> {
    const params = year ? new HttpParams().set('year', year) : undefined;
    return this.http.get<ApiResponse<AdminLeaveStatistics>>(`${this.baseUrl}/statistics/admin`, { params });
  }

  myStatistics(year?: number): Observable<ApiResponse<EmployeeLeaveStatistics>> {
    const params = year ? new HttpParams().set('year', year) : undefined;
    return this.http.get<ApiResponse<EmployeeLeaveStatistics>>(`${this.baseUrl}/statistics/me`, { params });
  }

  adminCalendar(filter: {
    startDate: string;
    endDate: string;
    departmentId?: number | null;
    status?: LeaveStatus | '';
    type?: LeaveType | '';
  }): Observable<ApiResponse<LeaveCalendarEvent[]>> {
    let params = new HttpParams()
      .set('startDate', filter.startDate)
      .set('endDate', filter.endDate);

    Object.entries(filter).forEach(([key, value]) => {
      if (key !== 'startDate' && key !== 'endDate' && value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });

    return this.http.get<ApiResponse<LeaveCalendarEvent[]>>(`${this.baseUrl}/calendar`, { params });
  }

  myCalendar(startDate: string, endDate: string): Observable<ApiResponse<LeaveCalendarEvent[]>> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<ApiResponse<LeaveCalendarEvent[]>>(`${this.baseUrl}/calendar/me`, { params });
  }
}
