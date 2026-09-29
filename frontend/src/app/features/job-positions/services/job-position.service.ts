import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { JobPositionRequest } from '../models/job-position-request.model';
import { JobPositionResponse } from '../models/job-position-response.model';

@Injectable({ providedIn: 'root' })
export class JobPositionService {
  private readonly http = inject(HttpClient);

  findAll(): Observable<ApiResponse<JobPositionResponse[]>> {
    return this.http.get<ApiResponse<JobPositionResponse[]>>('/api/job-positions');
  }

  findById(id: number): Observable<ApiResponse<JobPositionResponse>> {
    return this.http.get<ApiResponse<JobPositionResponse>>(`/api/job-positions/${id}`);
  }

  create(request: JobPositionRequest): Observable<ApiResponse<JobPositionResponse>> {
    return this.http.post<ApiResponse<JobPositionResponse>>('/api/job-positions', request);
  }

  update(id: number, request: JobPositionRequest): Observable<ApiResponse<JobPositionResponse>> {
    return this.http.put<ApiResponse<JobPositionResponse>>(`/api/job-positions/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/job-positions/${id}`);
  }
}
