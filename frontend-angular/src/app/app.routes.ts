import { Routes } from '@angular/router';
import { authGuard, guestGuard, adminGuard } from './auth/guards/auth.guard';
import { LoginComponent } from './auth/components/login.component';
import { RegisterComponent } from './auth/components/register.component';
import { VerifyEmailComponent } from './auth/components/verify-email.component';
import { MfaSetupComponent } from './auth/components/mfa-setup.component';
import { MfaVerifyComponent } from './auth/components/mfa-verify.component';
import { SocialCallbackComponent } from './auth/components/social-callback.component';
import { ScoringComponent } from './scoring/scoring.component';
import { MyRequestsComponent } from './scoring/my-requests.component';
import { AdminLayoutComponent } from './admin/admin-layout.component';
import { AdminOperationsComponent } from './admin/admin-operations.component';
import { AdminOperationDetailComponent } from './admin/admin-operation-detail.component';
import { AdminAuditComponent } from './admin/admin-audit.component';
import { AdminDltComponent } from './admin/admin-dlt.component';

export const routes:Routes=[
  {path:'login',component:LoginComponent,canActivate:[guestGuard]},
  {path:'register',component:RegisterComponent,canActivate:[guestGuard]},
  {path:'verify-email',component:VerifyEmailComponent,canActivate:[guestGuard]},
  {path:'mfa/setup',component:MfaSetupComponent,canActivate:[guestGuard]},
  {path:'mfa/verify',component:MfaVerifyComponent,canActivate:[guestGuard]},
  {path:'auth/social-callback',component:SocialCallbackComponent,canActivate:[guestGuard]},
  {path:'evaluacion',component:ScoringComponent,canActivate:[authGuard]},
  {path:'mis-solicitudes',component:MyRequestsComponent,canActivate:[authGuard]},
  {path:'admin',component:AdminLayoutComponent,canActivate:[adminGuard],children:[
    {path:'operaciones',component:AdminOperationsComponent},
    {path:'operaciones/:requestId',component:AdminOperationDetailComponent},
    {path:'auditoria',component:AdminAuditComponent},
    {path:'errores-dlt',component:AdminDltComponent},
    {path:'',pathMatch:'full',redirectTo:'operaciones'}
  ]},
  {path:'scoring',redirectTo:'evaluacion',pathMatch:'full'},
  {path:'',pathMatch:'full',redirectTo:'login'},
  {path:'**',redirectTo:'login'}
];
