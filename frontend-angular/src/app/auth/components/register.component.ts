import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AuthFlowStoreService } from '../services/auth-flow-store.service';
import { GOOGLE_LOGIN_URL } from '../../config/api.config';

@Component({
  standalone: true,
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly flow = inject(AuthFlowStoreService);
  private readonly router = inject(Router);

  error = '';
  info = '';
  loading = false;
  socialLoading = false;
  showPassword = false;
  showConfirmPassword = false;
  readonly googleLoginUrl = GOOGLE_LOGIN_URL;

  readonly form = this.fb.nonNullable.group({
    displayName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(128)]],
    confirmPassword: ['', Validators.required]
  });

  get passwordsMismatch(): boolean {
    const value = this.form.getRawValue();
    return !!value.confirmPassword && value.password !== value.confirmPassword;
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  submit(): void {
    this.form.markAllAsTouched();
    this.error = '';
    this.info = '';

    if (this.form.invalid) {
      this.error = 'Corrige los campos marcados antes de continuar.';
      return;
    }

    const value = this.form.getRawValue();
    if (value.password !== value.confirmPassword) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }

    this.loading = true;
    this.flow.setPendingEmail(value.email.trim());

    this.auth.register({
      displayName: value.displayName.trim(),
      email: value.email.trim(),
      password: value.password
    }).subscribe({
      next: result => {
        this.info = result.message;
        if (result.nextStep === 'MFA_SETUP' && result.token) {
          this.flow.setSetupToken(result.token);
          this.router.navigate(['/mfa/setup'], { state: { token: result.token } });
          return;
        }
        this.router.navigate(['/verify-email'], { queryParams: { email: value.email.trim() } });
      },
      error: error => {
        this.error = error?.error?.message ?? 'No se pudo registrar el usuario.';
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
        this.error = error?.error?.message ?? 'No se pudo iniciar el registro con TikTok.';
        this.socialLoading = false;
      }
    });
  }
}
