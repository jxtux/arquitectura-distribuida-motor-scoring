import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, finalize, tap } from 'rxjs';
import {
  AccessTokenResponse,
  AuthStage,
  CurrentUserProfile,
  MfaSetup,
  RegistrationStage
} from '../models/auth.models';
import { TokenStoreService } from './token-store.service';
import { API_BASE_URL } from '../../config/api.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokens = inject(TokenStoreService);
  private readonly base = `${API_BASE_URL}/api/v1/auth`;

  register(body: {
    displayName: string;
    email: string;
    password: string;
  }): Observable<RegistrationStage> {
    return this.http.post<RegistrationStage>(`${this.base}/register`, body);
  }

  verifyEmail(email: string, code: string) {
    return this.http.post<{
      setupToken: string;
      nextStep: 'MFA_SETUP';
    }>(`${this.base}/verify-email`, { email, code });
  }

  login(email: string, password: string): Observable<AuthStage> {
    return this.http.post<AuthStage>(
      `${this.base}/login`,
      { email, password }
    );
  }

  setupMfa(setupToken: string): Observable<MfaSetup> {
    return this.http.post<MfaSetup>(
      `${this.base}/mfa/setup`,
      { setupToken }
    );
  }

  confirmMfa(setupToken: string, code: string): Observable<AccessTokenResponse> {
    return this.http.post<AccessTokenResponse>(
      `${this.base}/mfa/confirm`,
      { setupToken, code },
      { withCredentials: true }
    ).pipe(
      tap(result => this.tokens.set(result.accessToken))
    );
  }

  verifyMfa(challengeToken: string, code: string) {
    return this.http.post<AccessTokenResponse>(
      `${this.base}/mfa/verify`,
      { challengeToken, code },
      { withCredentials: true }
    ).pipe(
      tap(result => this.tokens.set(result.accessToken))
    );
  }

  getTikTokStartUrl(): Observable<{ url: string }> {
    return this.http.get<{ url: string }>(
      `${this.base}/social/tiktok/start-url`
    );
  }

  getCurrentUser(): Observable<CurrentUserProfile> {
    return this.http.get<CurrentUserProfile>(`${API_BASE_URL}/api/v1/users/me`);
  }

  refresh() {
    return this.http.post<AccessTokenResponse>(
      `${this.base}/refresh`,
      {},
      { withCredentials: true }
    ).pipe(
      tap(result => this.tokens.set(result.accessToken))
    );
  }

  logout() {
    return this.http.post(
      `${this.base}/logout`,
      {},
      { withCredentials: true }
    ).pipe(
      // Aunque el servidor no estuviera disponible, el navegador no debe
      // conservar un Access JWT utilizable después de pulsar Cerrar sesión.
      finalize(() => this.tokens.clear())
    );
  }

  hasAccessToken(): boolean {
    return this.tokens.hasUsableAccessToken();
  }
}
