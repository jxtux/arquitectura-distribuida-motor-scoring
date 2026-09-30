import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AuthFlowStoreService } from '../services/auth-flow-store.service';
import { GOOGLE_LOGIN_URL } from '../../config/api.config';

@Component({
  standalone: true,
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-page">
      <section class="auth-panel auth-panel-login" aria-labelledby="login-title">
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

        <h1 id="login-title">Iniciar sesión</h1>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="auth-field">
            <label for="email">Correo electrónico</label>
            <div class="field-control"
                 [class.field-control-invalid]="form.controls.email.touched && form.controls.email.invalid">
              <input id="email" type="email" autocomplete="email" formControlName="email"
                     placeholder="tu@correo.com"
                     [attr.aria-invalid]="form.controls.email.touched && form.controls.email.invalid"
                     aria-describedby="login-email-error">
            </div>
            <div id="login-email-error" class="field-error"
                 *ngIf="form.controls.email.touched && form.controls.email.errors?.['required']">
              <span aria-hidden="true">!</span> El correo electrónico es obligatorio.
            </div>
            <div id="login-email-error" class="field-error"
                 *ngIf="form.controls.email.touched && !form.controls.email.errors?.['required'] && form.controls.email.errors?.['email']">
              <span aria-hidden="true">!</span> Ingresa un correo electrónico válido.
            </div>
          </div>

          <div class="auth-field">
            <label for="password">Contraseña</label>
            <div class="field-control field-control-password"
                 [class.field-control-invalid]="form.controls.password.touched && form.controls.password.invalid">
              <input id="password" [type]="showPassword ? 'text' : 'password'"
                     autocomplete="current-password" formControlName="password"
                     placeholder="Ingresa tu contraseña"
                     [attr.aria-invalid]="form.controls.password.touched && form.controls.password.invalid"
                     aria-describedby="login-password-error">
              <button class="password-toggle" type="button" (click)="togglePasswordVisibility()"
                      [attr.aria-label]="showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                <svg *ngIf="!showPassword" viewBox="0 0 24 24" aria-hidden="true"><path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z"/><circle cx="12" cy="12" r="2.6"/></svg>
                <svg *ngIf="showPassword" viewBox="0 0 24 24" aria-hidden="true"><path d="m3 3 18 18M10.6 6.1A9.9 9.9 0 0 1 12 6c6 0 9.5 6 9.5 6a17 17 0 0 1-3 3.6M14.1 14.1A3 3 0 0 1 9.9 9.9M6.3 7.2A17.2 17.2 0 0 0 2.5 12S6 18 12 18a9.8 9.8 0 0 0 3.2-.5"/></svg>
              </button>
            </div>
            <div id="login-password-error" class="field-error"
                 *ngIf="form.controls.password.touched && form.controls.password.errors?.['required']">
              <span aria-hidden="true">!</span> La contraseña es obligatoria.
            </div>
          </div>

          <div class="error-message" *ngIf="error" role="alert">{{ error }}</div>

          <button class="primary-auth-button" type="submit" [disabled]="loading">
            {{ loading ? 'Validando...' : 'Ingresar' }}
          </button>
        </form>

        <div class="auth-divider"><span>o</span></div>

        <a class="social-button" [href]="googleLoginUrl" aria-label="Continuar con Google">
          <svg class="social-icon google-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="#4285F4" d="M21.6 12.2c0-.7-.1-1.4-.2-2H12v3.9h5.4a4.6 4.6 0 0 1-2 3v2.5h3.3c1.9-1.8 2.9-4.4 2.9-7.4Z"/>
            <path fill="#34A853" d="M12 22c2.7 0 5-.9 6.7-2.4l-3.3-2.5c-.9.6-2.1 1-3.4 1-2.6 0-4.8-1.8-5.6-4.2H3v2.6A10 10 0 0 0 12 22Z"/>
            <path fill="#FBBC05" d="M6.4 13.9A6 6 0 0 1 6.1 12c0-.7.1-1.3.3-1.9V7.5H3A10 10 0 0 0 2 12c0 1.6.4 3.1 1 4.5l3.4-2.6Z"/>
            <path fill="#EA4335" d="M12 5.9c1.5 0 2.8.5 3.9 1.5l2.9-2.9A9.8 9.8 0 0 0 12 2a10 10 0 0 0-9 5.5l3.4 2.6C7.2 7.7 9.4 5.9 12 5.9Z"/>
          </svg>
          <span>Continuar con Google</span>
        </a>

        <button class="social-button" type="button" (click)="continueWithTikTok()" [disabled]="socialLoading" aria-label="Continuar con TikTok">
          <svg class="social-icon tiktok-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path class="tiktok-cyan" d="M14.7 3.2v10.1a4.2 4.2 0 1 1-3.6-4.1v3a1.4 1.4 0 1 0 .7 1.2V3.2h2.9Z"/>
            <path class="tiktok-red" d="M16.5 3.2c.4 2 1.5 3.2 3.5 3.7v2.9a8 8 0 0 1-3.5-1.2v4.7a4.2 4.2 0 0 1-5.2 4.1 4.2 4.2 0 0 0 6.1-3.8V8.9a7.7 7.7 0 0 0 3.3 1V7c-2-.4-3.2-1.7-3.7-3.8h-.5Z"/>
            <path class="tiktok-black" d="M13.9 2.5h2.4c.3 2.2 1.6 3.5 3.7 4v2.4a7.3 7.3 0 0 1-3.2-.9v5.3a4.8 4.8 0 1 1-4.8-4.8c.4 0 .8 0 1.2.2v2.5a2.4 2.4 0 1 0 1.5 2.2V2.5h-.8Z"/>
          </svg>
          <span>{{ socialLoading ? 'Abriendo TikTok...' : 'Continuar con TikTok' }}</span>
        </button>
        <p class="tiktok-sandbox-hint">Sandbox TikTok: selecciona una cuenta configurada como <strong>Target User</strong>.</p>

        <p class="auth-switch">
          ¿Nuevo en Motor Scoring?
          <a routerLink="/register">Crear cuenta</a>
        </p>
      </section>
    </main>
  `
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly flow = inject(AuthFlowStoreService);
  private readonly router = inject(Router);

  error = '';
  loading = false;
  socialLoading = false;
  showPassword = false;
  readonly googleLoginUrl = GOOGLE_LOGIN_URL;

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  submit(): void {
    this.form.markAllAsTouched();
    this.error = '';

    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    const value = this.form.getRawValue();

    this.auth.login(value.email.trim(), value.password).subscribe({
      next: result => {
        if (result.nextStep === 'MFA_SETUP') {
          this.flow.setSetupToken(result.token);
          this.flow.clearChallengeToken();
          this.router.navigate(['/mfa/setup'], { state: { token: result.token } });
        } else {
          this.flow.setChallengeToken(result.token);
          this.flow.clearSetupToken();
          this.router.navigate(['/mfa/verify'], { state: { token: result.token } });
        }
      },
      error: error => {
        this.error = error?.error?.message ?? 'No se pudo iniciar sesión.';
        this.loading = false;
      }
    });
  }

  continueWithTikTok(): void {
    this.error = '';
    this.socialLoading = true;
    this.auth.getTikTokStartUrl().subscribe({
      next: result => window.location.assign(result.url),
      error: error => {
        this.error = error?.error?.message ?? 'No se pudo iniciar el login con TikTok.';
        this.socialLoading = false;
      }
    });
  }
}
