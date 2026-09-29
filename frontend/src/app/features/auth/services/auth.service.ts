import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap, throwError } from 'rxjs';
import { ApiResponse } from '../../../core/models/api-response.model';
import { TokenService } from '../../../core/services/token.service';
import { CurrentUser } from '../models/current-user.model';
import { LoginRequest } from '../models/login-request.model';
import { LoginResponse } from '../models/login-response.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenService = inject(TokenService);

  login(request: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>('/api/auth/login', request).pipe(
      tap((response) => this.tokenService.saveSession(response.data))
    );
  }

  currentUser(): Observable<ApiResponse<CurrentUser>> {
    return this.http.get<ApiResponse<CurrentUser>>('/api/auth/me');
  }

  refresh(): Observable<ApiResponse<LoginResponse>> {
    const refreshToken = this.tokenService.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('Missing refresh token'));
    }

    return this.http.post<ApiResponse<LoginResponse>>('/api/auth/refresh', { refreshToken }).pipe(
      tap((response) => this.tokenService.saveSession(response.data))
    );
  }

  logout(): Observable<ApiResponse<void>> {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.http.post<ApiResponse<void>>('/api/auth/logout', { refreshToken }).pipe(
      tap(() => this.tokenService.clearSession())
    );
  }
}
