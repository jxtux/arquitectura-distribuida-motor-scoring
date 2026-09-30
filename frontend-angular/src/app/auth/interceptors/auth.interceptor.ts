import {
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';
import { inject } from '@angular/core';
import {
  catchError,
  switchMap,
  throwError
} from 'rxjs';
import { TokenStoreService } from '../services/token-store.service';
import { AuthService } from '../services/auth.service';

/**
 * Adjunta el JWT y un correlation id por petición. El mismo correlation id se
 * conserva si el interceptor debe refrescar el access token y reintentar.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const tokenStore = inject(TokenStoreService);
  const auth = inject(AuthService);

  const token = tokenStore.get();
  const isAuthEndpoint = request.url.includes('/api/v1/auth/');
  const correlationId = request.headers.get('X-Correlation-Id') ?? crypto.randomUUID();

  const baseHeaders: Record<string, string> = {
    'X-Correlation-Id': correlationId
  };
  if (token && !isAuthEndpoint) {
    baseHeaders['Authorization'] = `Bearer ${token}`;
  }

  const securedRequest = request.clone({ setHeaders: baseHeaders });

  return next(securedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isAuthEndpoint) {
        return throwError(() => error);
      }

      return auth.refresh().pipe(
        switchMap(result =>
          next(
            request.clone({
              setHeaders: {
                Authorization: `Bearer ${result.accessToken}`,
                'X-Correlation-Id': correlationId
              }
            })
          )
        ),
        catchError(refreshError => {
          tokenStore.clear();
          return throwError(() => refreshError);
        })
      );
    })
  );
};
