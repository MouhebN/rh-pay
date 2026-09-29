import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { UserCreateRequest } from '../models/user-create-request.model';
import { UserResponse } from '../models/user-response.model';
import { UserUpdateRequest } from '../models/user-update-request.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  findAll(): Observable<ApiResponse<UserResponse[]>> {
    return this.http.get<ApiResponse<UserResponse[]>>('/api/users');
  }

  findById(id: number): Observable<ApiResponse<UserResponse>> {
    return this.http.get<ApiResponse<UserResponse>>(`/api/users/${id}`);
  }

  create(request: UserCreateRequest): Observable<ApiResponse<UserResponse>> {
    return this.http.post<ApiResponse<UserResponse>>('/api/users', request);
  }

  update(id: number, request: UserUpdateRequest): Observable<ApiResponse<UserResponse>> {
    return this.http.put<ApiResponse<UserResponse>>(`/api/users/${id}`, request);
  }

  updateStatus(id: number, enabled: boolean): Observable<ApiResponse<UserResponse>> {
    return this.http.patch<ApiResponse<UserResponse>>(`/api/users/${id}/status`, { enabled });
  }

  unlock(id: number): Observable<ApiResponse<UserResponse>> {
    return this.http.patch<ApiResponse<UserResponse>>(`/api/users/${id}/unlock`, {});
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/users/${id}`);
  }
}
