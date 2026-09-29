import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { DepartmentRequest } from '../models/department-request.model';
import { DepartmentResponse } from '../models/department-response.model';

@Injectable({ providedIn: 'root' })
export class DepartmentService {
  private readonly http = inject(HttpClient);

  findAll(): Observable<ApiResponse<DepartmentResponse[]>> {
    return this.http.get<ApiResponse<DepartmentResponse[]>>('/api/departments');
  }

  findById(id: number): Observable<ApiResponse<DepartmentResponse>> {
    return this.http.get<ApiResponse<DepartmentResponse>>(`/api/departments/${id}`);
  }

  create(request: DepartmentRequest): Observable<ApiResponse<DepartmentResponse>> {
    return this.http.post<ApiResponse<DepartmentResponse>>('/api/departments', request);
  }

  update(id: number, request: DepartmentRequest): Observable<ApiResponse<DepartmentResponse>> {
    return this.http.put<ApiResponse<DepartmentResponse>>(`/api/departments/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/departments/${id}`);
  }
}
