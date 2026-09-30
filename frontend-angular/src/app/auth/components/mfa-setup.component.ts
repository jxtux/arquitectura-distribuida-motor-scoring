import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import * as QRCode from 'qrcode';
import { AuthService } from '../services/auth.service';
import { AuthFlowStoreService } from '../services/auth-flow-store.service';
import { TokenStoreService } from '../services/token-store.service';

@Component({
  standalone: true,
  selector: 'app-mfa-setup',
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <main class="auth-page">
      <section class="auth-panel auth-panel-wide" aria-labelledby="mfa-title">
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
        <h1 id="mfa-title">Protege tu cuenta</h1>
        <p class="auth-subtitle">
          Escanea este QR con Google Authenticator. Después escribe el código actual de 6 dígitos.
        </p>

        <div class="qr-state" *ngIf="loadingQr">Generando código QR...</div>
        <div class="error-message" *ngIf="error">{{ error }}</div>
        <button class="secondary-auth-button" type="button" *ngIf="error && !qrDataUrl && setupToken"
                [disabled]="loadingQr" (click)="loadQr()">
          {{ loadingQr ? 'Reintentando...' : 'Reintentar generar QR' }}
        </button>

        <div class="qr-box" *ngIf="qrDataUrl">
          <img [src]="qrDataUrl" alt="QR para enlazar Google Authenticator">
        </div>

        <details *ngIf="secret" class="manual-secret">
          <summary>No puedo escanear el QR</summary>
          <p>Agrega una clave de configuración manual en Google Authenticator:</p>
          <code>{{ secret }}</code>
        </details>

        <label for="totpCode">Código de Google Authenticator</label>
        <input id="totpCode" class="otp-input" [(ngModel)]="code" maxlength="6"
               inputmode="numeric" autocomplete="one-time-code" placeholder="000000">

        <button class="primary-auth-button" type="button" [disabled]="loadingQr || confirming || !qrDataUrl" (click)="confirm()">
          {{ confirming ? 'Confirmando...' : 'Confirmar y entrar' }}
        </button>

        <p class="auth-help">
          El QR se mantiene estable si recargas esta pantalla mientras el MFA no haya sido confirmado.
        </p>
        <p class="auth-switch"><a routerLink="/login">Volver al inicio de sesión</a></p>
      </section>
    </main>
  `
})
export class MfaSetupComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly flow = inject(AuthFlowStoreService);
  private readonly router = inject(Router);
  private readonly tokens = inject(TokenStoreService);

  setupToken = '';
  qrDataUrl = '';
  secret = '';
  code = '';
  error = '';
  loadingQr = true;
  confirming = false;

  ngOnInit(): void {
    this.setupToken =
      (history.state.setupToken ?? history.state.token ?? this.flow.getSetupToken() ?? '') as string;

    if (!this.setupToken) {
      this.loadingQr = false;
      this.router.navigate(['/login']);
      return;
    }

    this.flow.setSetupToken(this.setupToken);
    this.loadQr();
  }

  loadQr(): void {
    if (!this.setupToken) {
      return;
    }
    this.error = '';
    this.qrDataUrl = '';
    this.loadingQr = true;

    this.auth.setupMfa(this.setupToken).subscribe({
      next: result => this.renderQr(result.secret, result.otpAuthUri),
      error: error => {
        this.loadingQr = false;
        const code = error?.error?.code;
        this.error = error?.error?.message ?? 'No se pudo configurar MFA.';
        if (code === 'INVALID_MFA_SETUP_TOKEN') {
          this.flow.clearSetupToken();
          this.setupToken = '';
        }
      }
    });
  }

  private async renderQr(secret: string, otpAuthUri: string): Promise<void> {
    try {
      this.secret = secret;
      this.qrDataUrl = await QRCode.toDataURL(otpAuthUri, {
        errorCorrectionLevel: 'M',
        margin: 2,
        width: 280
      });
    } catch {
      this.error = 'El secreto TOTP fue creado, pero no se pudo dibujar el QR. Usa la clave manual mostrada abajo.';
    } finally {
      this.loadingQr = false;
    }
  }

  confirm(): void {
    this.error = '';
    if (!/^\d{6}$/.test(this.code)) {
      this.error = 'Ingresa un código de 6 dígitos.';
      return;
    }

    this.confirming = true;
    this.auth.confirmMfa(this.setupToken, this.code).subscribe({
      next: () => {
        this.flow.clearTransient();
        this.router.navigate([this.tokens.hasRole('ADMIN') ? '/admin/operaciones' : '/scoring']);
      },
      error: error => {
        this.error = error?.error?.message ?? 'El código TOTP no es válido.';
        this.confirming = false;
      }
    });
  }
}
