import { Injectable, signal } from '@angular/core';
import { CurrentUser } from '../../features/auth/models/current-user.model';
import { LoginResponse } from '../../features/auth/models/login-response.model';

const ACCESS_TOKEN_KEY = 'rh_paie_access_token';
const REFRESH_TOKEN_KEY = 'rh_paie_refresh_token';
const CURRENT_USER_KEY = 'rh_paie_current_user';

@Injectable({ providedIn: 'root' })
export class TokenService {
  readonly currentUser = signal<CurrentUser | null>(this.readCurrentUser());

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  saveSession(loginResponse: LoginResponse): void {
    const user: CurrentUser = {
      id: loginResponse.userId,
      username: loginResponse.username,
      email: loginResponse.email,
      role: loginResponse.role
    };

    localStorage.setItem(ACCESS_TOKEN_KEY, loginResponse.token);
    localStorage.setItem(REFRESH_TOKEN_KEY, loginResponse.refreshToken);
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  clearSession(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(CURRENT_USER_KEY);
    this.currentUser.set(null);
  }

  isAuthenticated(): boolean {
    return Boolean(this.getAccessToken());
  }

  private readCurrentUser(): CurrentUser | null {
    const rawUser = localStorage.getItem(CURRENT_USER_KEY);
    if (!rawUser) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as CurrentUser;
    } catch {
      return null;
    }
  }
}
