import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthFlowStoreService } from '../services/auth-flow-store.service';

@Component({
  standalone: true,
  selector: 'app-social-callback',
  template: `
    <main class="auth-page">
      <section class="auth-panel">
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

        <ng-container *ngIf="!errorMessage; else socialError">
          <h1>Continuando autenticación...</h1>
          <p class="auth-subtitle">Estamos preparando la verificación de Google Authenticator.</p>
        </ng-container>

        <ng-template #socialError>
          <h1>No se pudo completar el acceso</h1>
          <div class="error-message" role="alert">{{ errorMessage }}</div>
          <button class="primary-auth-button" type="button" (click)="backToLogin()">
            Volver al inicio de sesión
          </button>
        </ng-template>
      </section>
    </main>
  `
})
export class SocialCallbackComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly flow = inject(AuthFlowStoreService);

  errorMessage = '';

  ngOnInit(): void {
    this.route.fragment.subscribe(fragment => {
      const params = new URLSearchParams(fragment ?? '');
      const token = params.get('token');
      const next = params.get('next');
      const error = params.get('error');
      const message = params.get('message');

      if (error) {
        this.errorMessage = message || 'No se pudo completar el inicio de sesión social.';
        this.flow.clearTransient();
        return;
      }

      if (!token || (next !== 'MFA_SETUP' && next !== 'MFA_VERIFY')) {
        this.errorMessage = 'La respuesta del proveedor de identidad no es válida.';
        this.flow.clearTransient();
        return;
      }

      if (next === 'MFA_SETUP') {
        this.flow.setSetupToken(token);
        this.flow.clearChallengeToken();
      } else {
        this.flow.setChallengeToken(token);
        this.flow.clearSetupToken();
      }

      this.router.navigate(
        [next === 'MFA_SETUP' ? '/mfa/setup' : '/mfa/verify'],
        { state: { token }, replaceUrl: true }
      );
    });
  }

  backToLogin(): void {
    this.router.navigate(['/login']);
  }
}
