import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AuthFlowStoreService } from '../services/auth-flow-store.service';

@Component({
  standalone: true,
  selector: 'app-verify-email',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-page">
      <section class="auth-panel" aria-labelledby="verify-title">
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
        <h1 id="verify-title">Verifica tu correo</h1>
        <p class="auth-subtitle">
          Ingresa el código de 6 dígitos enviado a <strong>{{ email }}</strong>.
        </p>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label for="verificationCode">Código de verificación</label>
          <input id="verificationCode" class="otp-input" inputmode="numeric" maxlength="6"
                 autocomplete="one-time-code" formControlName="code" placeholder="000000">

          <div class="error-message" *ngIf="error">{{ error }}</div>

          <button class="primary-auth-button" type="submit" [disabled]="loading">
            {{ loading ? 'Validando...' : 'Verificar y continuar' }}
          </button>
        </form>

        <p class="auth-help">
          Si interrumpes el proceso, puedes volver a Crear cuenta con el mismo correo y contraseña para reanudarlo.
        </p>
        <p class="auth-switch"><a routerLink="/login">Volver al inicio de sesión</a></p>
      </section>
    </main>
  `
})
export class VerifyEmailComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly flow = inject(AuthFlowStoreService);

  readonly email =
    this.route.snapshot.queryParamMap.get('email') ??
    this.flow.getPendingEmail() ??
    '';

  error = '';
  loading = false;

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  submit(): void {
    this.form.markAllAsTouched();
    this.error = '';

    if (!this.email) {
      this.error = 'No se encontró el correo del registro. Vuelve a Crear cuenta para reanudar.';
      return;
    }
    if (this.form.invalid) {
      this.error = 'El código debe contener exactamente 6 dígitos.';
      return;
    }

    this.loading = true;
    this.auth.verifyEmail(this.email, this.form.getRawValue().code).subscribe({
      next: result => {
        this.flow.setSetupToken(result.setupToken);
        this.flow.clearPendingEmail();
        this.router.navigate(['/mfa/setup'], { state: { setupToken: result.setupToken } });
      },
      error: error => {
        this.error = error?.error?.message ?? 'No se pudo verificar el correo.';
        this.loading = false;
      }
    });
  }
}
