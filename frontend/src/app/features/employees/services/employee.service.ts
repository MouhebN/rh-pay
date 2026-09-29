import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { EmployeeRequest } from '../models/employee-request.model';
import { EmployeeResponse } from '../models/employee-response.model';
import { EmploymentStatus } from '../models/employment-status.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);

  findAll(): Observable<ApiResponse<EmployeeResponse[]>> {
    return this.http.get<ApiResponse<EmployeeResponse[]>>('/api/employees');
  }

  findById(id: number): Observable<ApiResponse<EmployeeResponse>> {
    return this.http.get<ApiResponse<EmployeeResponse>>(`/api/employees/${id}`);
  }

  create(request: EmployeeRequest): Observable<ApiResponse<EmployeeResponse>> {
    return this.http.post<ApiResponse<EmployeeResponse>>('/api/employees', request);
  }

  update(id: number, request: EmployeeRequest): Observable<ApiResponse<EmployeeResponse>> {
    return this.http.put<ApiResponse<EmployeeResponse>>(`/api/employees/${id}`, request);
  }

  updateStatus(id: number, status: EmploymentStatus): Observable<ApiResponse<EmployeeResponse>> {
    return this.http.patch<ApiResponse<EmployeeResponse>>(`/api/employees/${id}/status`, { status });
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/employees/${id}`);
  }
}
