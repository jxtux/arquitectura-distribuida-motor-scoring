import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TokenStoreService {
  /*
   * Access JWT corto: sessionStorage.
   * Refresh token: nunca se expone a JavaScript; vive en cookie HttpOnly.
   *
   * FIXED15:
   * Guardamos únicamente un "session hint" no sensible en localStorage.
   * Permite que una nueva pestaña sepa que debe intentar /refresh sin guardar
   * el refresh token ni el JWT en almacenamiento persistente.
   */
  private readonly key = 'fs_access_token';
  private readonly sessionHintKey = 'fs_session_active';

  get(): string | null {
    return sessionStorage.getItem(this.key);
  }

  set(token: string): void {
    sessionStorage.setItem(this.key, token);
    try {
      localStorage.setItem(this.sessionHintKey, '1');
    } catch {
      // localStorage puede estar bloqueado; la sesión sigue funcionando en la pestaña actual.
    }
  }

  clear(): void {
    sessionStorage.removeItem(this.key);
    try {
      localStorage.removeItem(this.sessionHintKey);
    } catch {
      // Sin acción adicional.
    }
  }

  hasSessionHint(): boolean {
    try {
      return localStorage.getItem(this.sessionHintKey) === '1';
    } catch {
      return !!this.get();
    }
  }

  roles(): string[] {
    const token = this.get();
    if (!token) return [];
    try {
      const payload = this.decodePayload(token) as { roles?: string[] };
      return Array.isArray(payload.roles) ? payload.roles : [];
    } catch { return []; }
  }

  hasRole(role: string): boolean {
    return this.roles().some(r => r.toUpperCase() === role.toUpperCase());
  }

  private decodePayload(token: string): any {
    const parts = token.split('.');
    if (parts.length !== 3) throw new Error('JWT inválido');
    const normalized = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(normalized.length + ((4 - normalized.length % 4) % 4), '=');
    return JSON.parse(atob(padded));
  }

  hasUsableAccessToken(skewSeconds = 15): boolean {
    const token = this.get();
    if (!token) return false;

    try {
      const payload = this.decodePayload(token) as { exp?: number; token_use?: string; mfa?: boolean };
      const now = Math.floor(Date.now() / 1000);

      return payload.token_use === 'ACCESS'
        && payload.mfa === true
        && typeof payload.exp === 'number'
        && payload.exp > now + skewSeconds;
    } catch {
      return false;
    }
  }
}
