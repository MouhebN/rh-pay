import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { httpErrorFallback, httpErrorTitle, markHttpErrorNotified } from '../http/http-error';
import { NotificationService } from '../services/notification.service';

export const SKIP_ERROR_TOAST = new HttpContextToken(() => false);

const SILENT_URLS = ['/api/auth/refresh', '/api/notifications/recent', '/api/notifications/read'];
const PUBLIC_AUTH_URLS = ['/api/auth/login', '/api/auth/register'];

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const notificationService = inject(NotificationService);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (!request.context.get(SKIP_ERROR_TOAST) && shouldToast(request.url, error)) {
        notificationService.errorFromHttp(error, httpErrorFallback(error), httpErrorTitle(error));
      } else {
        markHttpErrorNotified(error);
      }
      return throwError(() => error);
    })
  );
};

function shouldToast(requestUrl: string, error: unknown): boolean {
  if (!(error instanceof HttpErrorResponse)) {
    return false;
  }

  if (matchesAnyUrl(requestUrl, SILENT_URLS) || matchesAnyUrl(error.url, SILENT_URLS)) {
    return false;
  }

  if (error.status === 401 && !matchesAnyUrl(requestUrl, PUBLIC_AUTH_URLS) && !matchesAnyUrl(error.url, PUBLIC_AUTH_URLS)) {
    return false;
  }

  return true;
}

function matchesAnyUrl(url: string | null | undefined, paths: string[]): boolean {
  return Boolean(url && paths.some((path) => url.includes(path)));
}
