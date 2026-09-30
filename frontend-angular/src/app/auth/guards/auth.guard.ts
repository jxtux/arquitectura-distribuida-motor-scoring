import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { TokenStoreService } from '../services/token-store.service';

/**
 * Protege las páginas privadas. Si el Access JWT ya no está disponible pero
 * existe una sesión HttpOnly activa, intenta recuperarla mediante /refresh.
 */
export const authGuard: CanActivateFn = () => {
  const tokens = inject(TokenStoreService);
  const auth = inject(AuthService);
  const router = inject(Router);

  if (tokens.hasUsableAccessToken()) {
    return true;
  }

  if (!tokens.hasSessionHint()) {
    tokens.clear();
    return router.createUrlTree(['/login']);
  }

  return auth.refresh().pipe(
    map(() => true),
    catchError(() => {
      tokens.clear();
      return of(router.createUrlTree(['/login']));
    })
  );
};

/**
 * Impide volver a login, registro, verificación o MFA cuando ya existe una
 * sesión autenticada. Si hay refresh cookie pero no Access JWT (p. ej. nueva
 * pestaña), recupera la sesión y redirige al panel de scoring.
 */
export const guestGuard: CanActivateFn = () => {
  const tokens = inject(TokenStoreService);
  const auth = inject(AuthService);
  const router = inject(Router);

  if (tokens.hasUsableAccessToken()) {
    return router.createUrlTree([tokens.hasRole('ADMIN') ? '/admin/operaciones' : '/scoring']);
  }

  if (!tokens.hasSessionHint()) {
    return true;
  }

  return auth.refresh().pipe(
    map(() => router.createUrlTree([tokens.hasRole('ADMIN') ? '/admin/operaciones' : '/scoring'])),
    catchError(() => {
      tokens.clear();
      return of(true);
    })
  );
};

/** Restringe /admin a JWT con ROLE ADMIN. */
export const adminGuard: CanActivateFn = () => {
  const tokens = inject(TokenStoreService);
  const auth = inject(AuthService);
  const router = inject(Router);
  const authorize = () => tokens.hasRole('ADMIN') ? true : router.createUrlTree(['/evaluacion']);
  if (tokens.hasUsableAccessToken()) return authorize();
  if (!tokens.hasSessionHint()) return router.createUrlTree(['/login']);
  return auth.refresh().pipe(
    map(() => authorize()),
    catchError(() => { tokens.clear(); return of(router.createUrlTree(['/login'])); })
  );
};
