export type AuthNextStep = 'EMAIL_VERIFY' | 'MFA_SETUP' | 'MFA_VERIFY';

export interface RegistrationStage {
  nextStep: 'EMAIL_VERIFY' | 'MFA_SETUP';
  token: string | null;
  message: string;
}

export interface AuthStage {
  token: string;
  nextStep: 'MFA_SETUP' | 'MFA_VERIFY';
  mfaConfigured: boolean;
}

export interface AccessTokenResponse {
  accessToken: string;
  expiresInSeconds: number;
}

export interface MfaSetup {
  secret: string;
  otpAuthUri: string;
}

export interface CurrentUserProfile {
  id: number;
  displayName: string;
  email: string | null;
  status: string;
}
