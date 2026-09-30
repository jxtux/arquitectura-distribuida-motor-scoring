import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, switchMap, takeWhile, timer } from 'rxjs';
import { AuthService } from '../auth/services/auth.service';
import { CurrentUserProfile } from '../auth/models/auth.models';
import { CreditRequest, ScoringApiService } from './scoring-api.service';

const notAllZeros: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const digits = String(control.value ?? '').replace(/\D/g, '');
  return digits.length > 0 && /^0+$/.test(digits) ? { allZeros: true } : null;
};

const futureExpiry: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = String(control.value ?? '');
  const match = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(value);
  if (!match) return null; // el pattern informa el formato
  const month = Number(match[1]);
  const year = 2000 + Number(match[2]);
  const now = new Date();
  const currentMonth = now.getMonth() + 1;
  const currentYear = now.getFullYear();
  return year < currentYear || (year === currentYear && month < currentMonth)
    ? { expired: true }
    : null;
};

type WorkflowStageKey = 'payment' | 'submitted' | 'scoring' | 'report' | 'email' | 'completed';
type WorkflowVisualStatus = 'PENDING' | 'IN_PROGRESS' | 'SUCCESS' | 'ERROR';

interface WorkflowStageView {
  key: WorkflowStageKey;
  label: string;
  status: WorkflowVisualStatus;
  errorMessage?: string;
}

@Component({
  selector: 'app-scoring',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './scoring.component.html'
})
export class ScoringComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private api = inject(ScoringApiService);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  user?: CurrentUserProfile;
  step = 1;
  request?: CreditRequest;
  quote: any;
  error = '';
  loading = false;
  showConfirm = false;
  poll?: Subscription;
  paymentSuccess = false;
  readonly workflowStages: WorkflowStageView[] = [
    { key: 'payment', label: 'Pago aprobado', status: 'PENDING' },
    { key: 'submitted', label: 'Solicitud enviada', status: 'PENDING' },
    { key: 'scoring', label: 'Evaluación de riesgo / scoring', status: 'PENDING' },
    { key: 'report', label: 'Generación de reporte PDF', status: 'PENDING' },
    { key: 'email', label: 'Envío de correo', status: 'PENDING' },
    { key: 'completed', label: 'Completado', status: 'PENDING' }
  ];
  private visualTargetSuccessCount = 0;
  private visualTargetCurrentIndex: number | null = null;
  private visualTargetErrorIndex: number | null = null;
  private visualTargetErrorMessage = '';
  private visualAnimationRunning = false;
  private visualSequenceToken = 0;
  private paymentIdempotencyKey?: string;
  private readonly productRules: Record<string, {minAmount:number; maxAmount:number; minTerm:number; maxTerm:number}> = {
    PRESTAMO_PERSONAL: { minAmount: 1000, maxAmount: 50000, minTerm: 6, maxTerm: 48 },
    CREDITO_EDUCATIVO: { minAmount: 1000, maxAmount: 80000, minTerm: 6, maxTerm: 60 },
    CREDITO_VEHICULAR: { minAmount: 5000, maxAmount: 150000, minTerm: 12, maxTerm: 72 },
    CAPITAL_TRABAJO: { minAmount: 2000, maxAmount: 100000, minTerm: 6, maxTerm: 48 }
  };

  readonly application = this.fb.nonNullable.group({
    productCode: ['PRESTAMO_PERSONAL', Validators.required],
    amount: [15000, [Validators.required]],
    termMonths: [24, [Validators.required]],
    purpose: ['Estudios', [Validators.required, Validators.maxLength(150)]],
    consent: [false, Validators.requiredTrue]
  });

  readonly payment = this.fb.nonNullable.group({
    serviceCode: ['EVALUACION_CREDITICIA', Validators.required],
    currency: ['PEN', Validators.required],
    cardBrand: ['VISA', Validators.required],
    cardNumber: ['4111111111111111', [Validators.required, Validators.pattern(/^\d{13,19}$/), notAllZeros]],
    expiry: ['12/30', [Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])\/\d{2}$/), futureExpiry]],
    cvv: ['123', [Validators.required, Validators.pattern(/^\d{3,4}$/)]]
  });

  ngOnInit(): void {
    this.auth.getCurrentUser().subscribe({
      next: u => this.user = u,
      error: () => this.router.navigate(['/login'])
    });

    const resume = this.route.snapshot.queryParamMap.get('requestId');
    if (resume) {
      this.api.get(resume).subscribe(r => {
        this.request = r;
        this.restoreIdempotencyKey(r.id);
        this.step = ['AWAITING_PAYMENT', 'PAYMENT_REJECTED'].includes(r.status) ? 2 : 3;
        if (this.step === 3) {
          this.syncWorkflowVisuals(r);
          if (!['COMPLETED', 'FAILED'].includes(r.status)) this.startPolling();
        }
      });
    }

    this.applyProductRules();
    this.application.controls.productCode.valueChanges.subscribe(() => this.applyProductRules());
    this.refreshQuote();
    this.payment.controls.currency.valueChanges.subscribe(() => this.refreshQuote());
  }

  ngOnDestroy(): void {
    this.poll?.unsubscribe();
    this.visualSequenceToken++;
  }

  refreshQuote(): void {
    this.api.quote(this.payment.controls.currency.value).subscribe({
      next: q => this.quote = q,
      error: e => this.error = e?.error?.message ?? 'No se pudo calcular el importe.'
    });
  }

  private applyProductRules(): void {
    const rule = this.productRules[this.application.controls.productCode.value];
    if (!rule) return;
    this.application.controls.amount.setValidators([Validators.required, Validators.min(rule.minAmount), Validators.max(rule.maxAmount)]);
    this.application.controls.termMonths.setValidators([Validators.required, Validators.min(rule.minTerm), Validators.max(rule.maxTerm)]);
    this.application.controls.amount.updateValueAndValidity({ emitEvent: false });
    this.application.controls.termMonths.updateValueAndValidity({ emitEvent: false });
  }

  productHint(): string {
    const r = this.productRules[this.application.controls.productCode.value];
    return r ? `Monto ${r.minAmount}–${r.maxAmount} PEN · plazo ${r.minTerm}–${r.maxTerm} meses` : '';
  }

  create(): void {
    this.error = '';
    this.application.markAllAsTouched();
    if (this.application.invalid) return;
    this.loading = true;
    this.api.create(this.application.getRawValue()).subscribe({
      next: r => {
        this.request = r;
        this.restoreIdempotencyKey(r.id, true);
        this.step = 2;
        this.loading = false;
      },
      error: e => {
        this.error = e?.error?.message ?? 'No se pudo registrar la solicitud.';
        this.loading = false;
      }
    });
  }

  confirmPayment(): void {
    this.error = '';
    this.payment.markAllAsTouched();
    if (!this.request) return;
    if (this.payment.controls.cardNumber.hasError('allZeros')) {
      this.error = 'Pago no válido: el número de tarjeta no puede estar formado solo por ceros.';
      return;
    }
    if (this.payment.invalid) {
      this.error = 'Revise los datos de pago antes de continuar.';
      return;
    }
    this.showConfirm = true;
  }

  cancelPayment(): void {
    this.showConfirm = false;
  }

  pay(): void {
    if (!this.request) return;
    this.showConfirm = false;
    this.loading = true;
    this.error = '';
    const value = this.payment.getRawValue();
    const key = this.paymentIdempotencyKey ?? this.restoreIdempotencyKey(this.request.id, true);

    this.api.pay({
      ...value,
      requestId: this.request.id,
      correlationId: this.request.correlationId
    }, key).subscribe({
      next: r => {
        this.loading = false;
        if (r.status === 'APPROVED') {
          this.paymentSuccess = true;
          sessionStorage.removeItem(this.idempotencyStorageKey(this.request!.id));
          setTimeout(() => {
            if (this.request) {
              this.request = { ...this.request, status: 'PAYMENT_APPROVED' };
              this.resetWorkflowVisuals();
              this.syncWorkflowVisuals(this.request);
            }
            this.step = 3;
            this.startPolling();
          }, 700);
        } else {
          this.error = 'Pago no válido';
          this.resetPaymentIdempotencyKey();
        }
      },
      error: e => {
        this.loading = false;
        // En errores 4xx la operación fue rechazada antes de quedar aprobada y
        // el siguiente intento es una nueva operación. Ante 5xx/red se conserva
        // la misma key porque no sabemos si el backend alcanzó a procesarla.
        if (e?.status >= 400 && e?.status < 500) this.resetPaymentIdempotencyKey();
        this.error = e?.error?.message ?? 'Pago no válido';
      }
    });
  }

  startPolling(): void {
    if (!this.request) return;
    const id = this.request.id;
    this.poll?.unsubscribe();
    this.poll = timer(0, 1200).pipe(
      switchMap(() => this.api.get(id)),
      takeWhile(x => !['COMPLETED', 'FAILED', 'PAYMENT_REJECTED'].includes(x.status), true)
    ).subscribe({
      next: x => {
        this.request = x;
        this.syncWorkflowVisuals(x);
      },
      error: e => {
        this.error = e?.status === 401
          ? 'Tu sesión expiró o ya no es válida. Inicia sesión nuevamente para continuar consultando el estado.'
          : 'No se pudo actualizar el estado de la evaluación. El procesamiento del backend no se cancela por este error de consulta.';
      }
    });
  }

  logout(): void {
    this.auth.logout().subscribe(() => this.router.navigate(['/login']));
  }

  friendly(status?: string): string | undefined {
    return ({
      AWAITING_PAYMENT: 'Esperando pago',
      PAYMENT_APPROVED: 'Pago aprobado',
      PAYMENT_REJECTED: 'Pago rechazado',
      SUBMITTED: 'Solicitud enviada',
      SCORING_IN_PROGRESS: 'Evaluando solicitud',
      REPORT_GENERATING: 'Generando reporte',
      NOTIFICATION_PENDING: 'Enviando resultado',
      COMPLETED: 'Completada',
      FAILED: 'Fallida'
    } as any)[status ?? ''] ?? status;
  }

  stageIcon(stage: WorkflowStageView): string {
    if (stage.status === 'SUCCESS') return '✓';
    if (stage.status === 'ERROR') return '✕';
    if (stage.status === 'IN_PROGRESS') return '';
    return '○';
  }

  isVisualComplete(): boolean {
    return this.workflowStages[this.workflowStages.length - 1].status === 'SUCCESS';
  }

  hasVisualError(): boolean {
    return this.workflowStages.some(stage => stage.status === 'ERROR');
  }

  visualHeadline(): string {
    if (this.hasVisualError()) return 'El proceso se detuvo por un error';
    if (this.isVisualComplete()) return 'Proceso completado correctamente';
    const current = this.workflowStages.find(stage => stage.status === 'IN_PROGRESS');
    return current ? current.label : (this.friendly(this.request?.status) ?? 'Procesando solicitud');
  }

  private resetWorkflowVisuals(): void {
    this.visualSequenceToken++;
    this.visualAnimationRunning = false;
    this.visualTargetSuccessCount = 0;
    this.visualTargetCurrentIndex = null;
    this.visualTargetErrorIndex = null;
    this.visualTargetErrorMessage = '';
    for (const stage of this.workflowStages) {
      stage.status = 'PENDING';
      stage.errorMessage = undefined;
    }
  }

  private syncWorkflowVisuals(request: CreditRequest): void {
    const target = this.visualTargetFor(request);

    // En un fallo, la etapa reportada por backend tiene prioridad absoluta.
    // No conservamos un target de éxito más avanzado de polls anteriores porque
    // eso podía dejar la fila fallida como pendiente aunque el estado general
    // ya fuera FAILED. Las etapas anteriores quedan verdes, la etapa exacta
    // queda roja y las posteriores vuelven a pendiente.
    if (target.errorIndex !== null) {
      this.visualTargetSuccessCount = target.successCount;
      this.visualTargetCurrentIndex = null;
      this.visualTargetErrorIndex = target.errorIndex;
      this.visualTargetErrorMessage = target.errorMessage;
      this.applyFailureVisuals(target.errorIndex, target.errorMessage);
      return;
    }

    this.visualTargetSuccessCount = Math.max(this.visualTargetSuccessCount, target.successCount);
    this.visualTargetCurrentIndex = target.currentIndex;
    this.visualTargetErrorIndex = null;
    this.visualTargetErrorMessage = '';
    void this.runVisualSequence();
  }

  private applyFailureVisuals(errorIndex: number, message: string): void {
    this.visualSequenceToken++;
    this.visualAnimationRunning = false;

    this.workflowStages.forEach((stage, index) => {
      stage.errorMessage = undefined;
      if (index < errorIndex) {
        stage.status = 'SUCCESS';
      } else if (index === errorIndex) {
        stage.status = 'ERROR';
        stage.errorMessage = message || 'No se pudo completar esta etapa del proceso.';
      } else {
        stage.status = 'PENDING';
      }
    });
  }

  private visualTargetFor(request: CreditRequest): { successCount: number; currentIndex: number | null; errorIndex: number | null; errorMessage: string } {
    if (request.status === 'FAILED') {
      const errorIndex = this.failureStageIndex(request.failureStage);
      return {
        successCount: Math.max(0, errorIndex),
        currentIndex: null,
        errorIndex,
        errorMessage: request.failureReason || 'No se pudo completar esta etapa del proceso.'
      };
    }

    switch (request.status) {
      case 'PAYMENT_APPROVED': return { successCount: 1, currentIndex: 1, errorIndex: null, errorMessage: '' };
      case 'SUBMITTED': return { successCount: 2, currentIndex: 2, errorIndex: null, errorMessage: '' };
      case 'SCORING_IN_PROGRESS': return { successCount: 2, currentIndex: 2, errorIndex: null, errorMessage: '' };
      case 'REPORT_GENERATING': return { successCount: 3, currentIndex: 3, errorIndex: null, errorMessage: '' };
      case 'NOTIFICATION_PENDING': return { successCount: 4, currentIndex: 4, errorIndex: null, errorMessage: '' };
      case 'COMPLETED': return { successCount: 6, currentIndex: null, errorIndex: null, errorMessage: '' };
      default: return { successCount: 0, currentIndex: null, errorIndex: null, errorMessage: '' };
    }
  }

  private failureStageIndex(stage?: string): number {
    const normalized = (stage ?? '').trim().toUpperCase().replace(/[\s-]+/g, '_');
    const byStage: Record<string, number> = {
      PAYMENT: 0,
      PAYMENT_VALIDATION: 0,
      SUBMITTED: 1,
      REQUEST: 1,
      REQUEST_SUBMITTED: 1,
      SCORING: 2,
      SCORE: 2,
      SCORING_IN_PROGRESS: 2,
      REPORT: 3,
      REPORT_GENERATING: 3,
      REPORT_GENERATION: 3,
      PDF: 3,
      PDF_GENERATION: 3,
      EMAIL: 4,
      NOTIFICATION: 4,
      NOTIFICATION_PENDING: 4,
      COMPLETED: 5
    };
    return byStage[normalized] ?? this.inferFailureIndexFromContext();
  }

  private inferFailureIndexFromContext(): number {
    const reason = (this.request?.failureReason ?? '').toLowerCase();
    if (/(pdf|report|minio|object storage|almacen)/.test(reason)) return 3;
    if (/(correo|email|smtp|notification|notific)/.test(reason)) return 4;
    if (/(pago|payment|tarjeta)/.test(reason)) return 0;
    if (/(scoring|score|riesgo|credit)/.test(reason)) return 2;

    // Si el backend no pudo informar failureStage, aprovechamos el estado
    // visual inmediatamente anterior al FAILED. Así, por ejemplo, si ya
    // estaban verdes Pago/Solicitud/Scoring, el error cae en Reporte.
    const inProgressIndex = this.workflowStages.findIndex(stage => stage.status === 'IN_PROGRESS');
    if (inProgressIndex >= 0) return inProgressIndex;
    const firstIncomplete = this.workflowStages.findIndex(stage => stage.status !== 'SUCCESS');
    if (firstIncomplete >= 0) return firstIncomplete;

    // Último fallback para respuestas antiguas sin metadata de fallo.
    switch (this.request?.status) {
      case 'REPORT_GENERATING': return 3;
      case 'NOTIFICATION_PENDING': return 4;
      case 'SCORING_IN_PROGRESS': return 2;
      case 'PAYMENT_APPROVED': return 1;
      default: return 2;
    }
  }

  private async runVisualSequence(): Promise<void> {
    if (this.visualAnimationRunning) return;
    this.visualAnimationRunning = true;
    const token = this.visualSequenceToken;

    try {
      while (token === this.visualSequenceToken) {
        const firstIncomplete = this.workflowStages.findIndex(stage => stage.status !== 'SUCCESS');
        const nextIndex = firstIncomplete === -1 ? this.workflowStages.length : firstIncomplete;

        if (this.visualTargetErrorIndex !== null && nextIndex === this.visualTargetErrorIndex) {
          const failed = this.workflowStages[nextIndex];
          if (failed && failed.status !== 'ERROR') {
            failed.status = 'ERROR';
            failed.errorMessage = this.visualTargetErrorMessage;
          }
          break;
        }

        if (nextIndex < this.visualTargetSuccessCount) {
          const stage = this.workflowStages[nextIndex];
          stage.status = 'IN_PROGRESS';
          await this.visualDelay(350, token);
          if (token !== this.visualSequenceToken) break;
          stage.status = 'SUCCESS';
          await this.visualDelay(300, token);
          continue;
        }

        this.workflowStages.forEach((stage, index) => {
          if (index >= this.visualTargetSuccessCount && stage.status !== 'ERROR') stage.status = 'PENDING';
        });

        if (this.visualTargetCurrentIndex !== null && this.visualTargetCurrentIndex < this.workflowStages.length) {
          const current = this.workflowStages[this.visualTargetCurrentIndex];
          if (current.status !== 'SUCCESS' && current.status !== 'ERROR') current.status = 'IN_PROGRESS';
        }
        break;
      }
    } finally {
      this.visualAnimationRunning = false;
      const completed = this.workflowStages.filter(stage => stage.status === 'SUCCESS').length;
      if (token === this.visualSequenceToken && (
        completed < this.visualTargetSuccessCount ||
        (this.visualTargetErrorIndex !== null && this.workflowStages[this.visualTargetErrorIndex]?.status !== 'ERROR')
      )) {
        void this.runVisualSequence();
      }
    }
  }

  private visualDelay(ms: number, token: number): Promise<void> {
    return new Promise(resolve => setTimeout(() => resolve(), token === this.visualSequenceToken ? ms : 0));
  }

  private resetPaymentIdempotencyKey(): void {
    if (!this.request) return;
    const storageKey = this.idempotencyStorageKey(this.request.id);
    const generated = crypto.randomUUID();
    sessionStorage.setItem(storageKey, generated);
    this.paymentIdempotencyKey = generated;
  }

  private idempotencyStorageKey(requestId: string): string {
    return `payment-idempotency:${requestId}`;
  }

  private restoreIdempotencyKey(requestId: string, createIfMissing = false): string {
    const storageKey = this.idempotencyStorageKey(requestId);
    const existing = sessionStorage.getItem(storageKey);
    if (existing) {
      this.paymentIdempotencyKey = existing;
      return existing;
    }
    const generated = createIfMissing ? crypto.randomUUID() : '';
    if (generated) {
      sessionStorage.setItem(storageKey, generated);
      this.paymentIdempotencyKey = generated;
    }
    return generated;
  }
}
