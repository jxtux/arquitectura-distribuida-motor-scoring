import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AuthFlowStoreService } from '../services/auth-flow-store.service';
import { TokenStoreService } from '../services/token-store.service';

@Component({
  standalone: true,
  selector: 'app-mfa-verify',
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <main class="auth-page">
      <section class="auth-panel" aria-labelledby="mfa-verify-title">
        <div class="auth-brand">
          <div class="brand-logo" aria-hidden="true">
            <svg viewBox="0 0 64 64" role="img">
              <path d="M14 40a18 18 0 1 1 36 0" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round"/>
              <path d="M32 40 44 27" fill="none" stroke="currentColor" stroke-width="5" stroke-linecap="round"/>
              <circle cx="32" cy="40" r="4.5" fill="currentColor"/>
            </svg>
          </div>
          <div class="brand-name">Motor Scoring</div>
        </div>
        <h1 id="mfa-verify-title">Verificación en dos pasos</h1>
        <p class="auth-subtitle">Ingresa el código actual de Google Authenticator.</p>

        <label for="mfaCode">Código de 6 dígitos</label>
        <input id="mfaCode" class="otp-input" [(ngModel)]="code" maxlength="6"
               inputmode="numeric" autocomplete="one-time-code" placeholder="000000">

        <div class="error-message" *ngIf="error">{{ error }}</div>

        <button class="primary-auth-button" type="button" [disabled]="loading" (click)="verify()">
          {{ loading ? 'Verificando...' : 'Verificar' }}
        </button>
        <p class="auth-switch"><a routerLink="/login">Usar otra cuenta</a></p>
      </section>
    </main>
  `
})
export class MfaVerifyComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly flow = inject(AuthFlowStoreService);
  private readonly router = inject(Router);
  private readonly tokens = inject(TokenStoreService);

  challengeToken = '';
  code = '';
  error = '';
  loading = false;

  ngOnInit(): void {
    this.challengeToken =
      (history.state.token ?? this.flow.getChallengeToken() ?? '') as string;
    if (!this.challengeToken) {
      this.router.navigate(['/login']);
      return;
    }
    this.flow.setChallengeToken(this.challengeToken);
  }

  verify(): void {
    this.error = '';
    if (!/^\d{6}$/.test(this.code)) {
      this.error = 'Ingresa un código de 6 dígitos.';
      return;
    }

    this.loading = true;
    this.auth.verifyMfa(this.challengeToken, this.code).subscribe({
      next: () => {
        this.flow.clearTransient();
        this.router.navigate([this.tokens.hasRole('ADMIN') ? '/admin/operaciones' : '/scoring']);
      },
      error: error => {
        this.error = error?.error?.message ?? 'El código TOTP no es válido.';
        this.loading = false;
      }
    });
  }
}
