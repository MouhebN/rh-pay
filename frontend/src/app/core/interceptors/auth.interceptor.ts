import {
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { AuthService } from '../../features/auth/services/auth.service';
import { AppNotificationService } from '../services/app-notification.service';
import { NotificationService } from '../services/notification.service';
import { TokenService } from '../services/token.service';

const SKIP_AUTH_REFRESH = new HttpContextToken(() => false);

const PUBLIC_AUTH_URLS = [
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/refresh',
  '/api/auth/health',
  '/api/auth/logout'
];

let refreshRequest$: Observable<string> | null = null;

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);
  const appNotificationService = inject(AppNotificationService);
  const notificationService = inject(NotificationService);
  const accessToken = tokenService.getAccessToken();
  const outgoingRequest =
    !accessToken || isPublicAuthRequest(request.url)
      ? request
      : withAccessToken(request, accessToken);

  return next(outgoingRequest).pipe(
    catchError((error: unknown) => {
      if (!shouldRefresh(request, error) || !tokenService.getRefreshToken()) {
        if (isUnauthorized(error) && !isPublicAuthRequest(request.url) && !tokenService.getRefreshToken()) {
          endSession(tokenService, appNotificationService, notificationService, router);
        }
        return throwError(() => error);
      }

      if (!refreshRequest$) {
        refreshRequest$ = authService.refresh().pipe(
          tap(() => appNotificationService.reconnect()),
          map((response) => response.data.token),
          catchError((refreshError: unknown) => {
            endSession(tokenService, appNotificationService, notificationService, router);
            return throwError(() => refreshError);
          }),
          finalize(() => {
            refreshRequest$ = null;
          }),
          shareReplay(1)
        );
      }

      return refreshRequest$.pipe(
        switchMap((token) => next(withAccessToken(request, token, true)))
      );
    })
  );
};

function isPublicAuthRequest(url: string): boolean {
  return PUBLIC_AUTH_URLS.some((path) => url.includes(path));
}

function isUnauthorized(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 401;
}

function shouldRefresh(request: HttpRequest<unknown>, error: unknown): boolean {
  return (
    isUnauthorized(error) &&
    !isPublicAuthRequest(request.url) &&
    !request.context.get(SKIP_AUTH_REFRESH)
  );
}

function withAccessToken<T>(request: HttpRequest<T>, accessToken: string, skipRefresh = false): HttpRequest<T> {
  return request.clone({
    setHeaders: {
      Authorization: `Bearer ${accessToken}`
    },
    context: skipRefresh ? request.context.set(SKIP_AUTH_REFRESH, true) : request.context
  });
}

function endSession(
  tokenService: TokenService,
  appNotificationService: AppNotificationService,
  notificationService: NotificationService,
  router: Router
): void {
  tokenService.clearSession();
  appNotificationService.disconnect();
  notificationService.error('Session expirée, veuillez vous reconnecter.', 'Session');
  void router.navigate(['/login']);
}
