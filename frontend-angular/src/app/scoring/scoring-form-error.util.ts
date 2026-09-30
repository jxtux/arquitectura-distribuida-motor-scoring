import {
  AbstractControl,
  FormGroup
} from '@angular/forms';

export interface BackendError {
  message?: string;
  violations?: Record<string, string>;
}

/*
 * Reemplaza el antiguo:
 *   if (this.form.invalid) return;
 *
 * por un mensaje entendible para el usuario.
 */
export function buildScoringValidationMessage(
  form: FormGroup
): string {
  const invalid = Object.entries(
    form.controls as Record<string, AbstractControl>
  )
    .filter(([, control]) => control.invalid)
    .map(([name]) => humanize(name));

  return invalid.length
    ? `No se puede enviar la solicitud. Corrige: ${invalid.join(', ')}.`
    : '';
}

/*
 * Usa tanto message como violations del GlobalExceptionHandler.
 */
export function backendErrorMessage(
  error: any
): string {
  const body:
    BackendError = error?.error ?? {};

  const details = Object.entries(
    body.violations ?? {}
  ).map(
    ([field, message]) =>
      `${humanize(
        field.split('.').pop() ?? field
      )}: ${message}`
  );

  return [
    body.message ??
      'No se pudo registrar la solicitud.',
    ...details
  ].join('\n');
}

function humanize(
  value: string
): string {
  return value
    .replace(/([A-Z])/g, ' $1')
    .replace(/_/g, ' ')
    .trim()
    .replace(
      /^./,
      char => char.toUpperCase()
    );
}
