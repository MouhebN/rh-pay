import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { AttendanceAdjustRequest } from '../models/attendance-adjust-request.model';
import { AttendanceFilter, AttendanceRecord, TodayAttendance } from '../models/attendance-record.model';
import { AttendanceSummary } from '../models/attendance-summary.model';

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/attendance';

  checkIn(): Observable<ApiResponse<AttendanceRecord>> {
    return this.http.post<ApiResponse<AttendanceRecord>>(`${this.baseUrl}/check-in`, {});
  }

  checkOut(): Observable<ApiResponse<AttendanceRecord>> {
    return this.http.post<ApiResponse<AttendanceRecord>>(`${this.baseUrl}/check-out`, {});
  }

  today(): Observable<ApiResponse<TodayAttendance>> {
    return this.http.get<ApiResponse<TodayAttendance>>(`${this.baseUrl}/today`);
  }

  myRecords(filter: AttendanceFilter = {}): Observable<ApiResponse<AttendanceRecord[]>> {
    return this.http.get<ApiResponse<AttendanceRecord[]>>(`${this.baseUrl}/my-records`, { params: this.toParams(filter) });
  }

  findAll(filter: AttendanceFilter = {}): Observable<ApiResponse<AttendanceRecord[]>> {
    return this.http.get<ApiResponse<AttendanceRecord[]>>(this.baseUrl, { params: this.toParams(filter) });
  }

  findById(id: number): Observable<ApiResponse<AttendanceRecord>> {
    return this.http.get<ApiResponse<AttendanceRecord>>(`${this.baseUrl}/${id}`);
  }

  adjust(id: number, request: AttendanceAdjustRequest): Observable<ApiResponse<AttendanceRecord>> {
    return this.http.patch<ApiResponse<AttendanceRecord>>(`${this.baseUrl}/${id}/adjust`, request);
  }

  summary(): Observable<ApiResponse<AttendanceSummary>> {
    return this.http.get<ApiResponse<AttendanceSummary>>(`${this.baseUrl}/summary`);
  }

  generateDailyAbsences(date: string): Observable<ApiResponse<void>> {
    const params = new HttpParams().set('date', date);
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/generate-daily-absences`, {}, { params });
  }

  private toParams(filter: AttendanceFilter): HttpParams {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return params;
  }
}
