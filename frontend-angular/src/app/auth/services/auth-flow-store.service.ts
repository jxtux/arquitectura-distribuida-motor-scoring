import { Injectable } from '@angular/core';

/**
 * Guarda únicamente tokens temporales de continuación del flujo IAM.
 * El refresh token sigue siendo HttpOnly y el Access JWT vive en TokenStoreService.
 */
@Injectable({ providedIn: 'root' })
export class AuthFlowStoreService {
  private readonly setupKey = 'fs_mfa_setup_token';
  private readonly challengeKey = 'fs_mfa_challenge_token';
  private readonly emailKey = 'fs_pending_email';

  setSetupToken(token: string): void {
    sessionStorage.setItem(this.setupKey, token);
  }

  getSetupToken(): string | null {
    return sessionStorage.getItem(this.setupKey);
  }

  clearSetupToken(): void {
    sessionStorage.removeItem(this.setupKey);
  }

  setChallengeToken(token: string): void {
    sessionStorage.setItem(this.challengeKey, token);
  }

  getChallengeToken(): string | null {
    return sessionStorage.getItem(this.challengeKey);
  }

  clearChallengeToken(): void {
    sessionStorage.removeItem(this.challengeKey);
  }

  setPendingEmail(email: string): void {
    sessionStorage.setItem(this.emailKey, email);
  }

  getPendingEmail(): string | null {
    return sessionStorage.getItem(this.emailKey);
  }

  clearPendingEmail(): void {
    sessionStorage.removeItem(this.emailKey);
  }

  clearTransient(): void {
    this.clearSetupToken();
    this.clearChallengeToken();
    this.clearPendingEmail();
  }
}
