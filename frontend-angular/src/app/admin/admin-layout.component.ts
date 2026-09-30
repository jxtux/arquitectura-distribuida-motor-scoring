import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/services/auth.service';
@Component({standalone:true,selector:'app-admin-layout',imports:[RouterOutlet,RouterLink,RouterLinkActive],template:`
<div class="admin-shell"><aside><div class="brand">FinanScore <span>ADMIN</span></div><nav>
<a routerLink="/admin/operaciones" routerLinkActive="active">Operaciones</a>
<a routerLink="/admin/auditoria" routerLinkActive="active">Auditoría</a>
<a routerLink="/admin/errores-dlt" routerLinkActive="active">Errores / DLT</a>
</nav><button (click)="logout()">Cerrar sesión</button></aside><main><router-outlet/></main></div>`,styles:[`
.admin-shell{min-height:100vh;display:grid;grid-template-columns:230px 1fr;background:#f4f7fb;color:#14213d;font-family:Arial,sans-serif}aside{background:#17365d;color:white;padding:24px 16px;display:flex;flex-direction:column;gap:22px}.brand{font-weight:800;font-size:20px}.brand span{display:block;font-size:11px;opacity:.7}nav{display:grid;gap:8px}a{color:#dfeaff;text-decoration:none;padding:11px 12px;border-radius:8px}a.active,a:hover{background:#2b67a3;color:white}button{margin-top:auto;background:transparent;color:white;border:1px solid #8fb4da;border-radius:8px;padding:10px;cursor:pointer}main{padding:28px}@media(max-width:800px){.admin-shell{grid-template-columns:1fr}aside{position:static}nav{grid-template-columns:repeat(3,1fr)}button{margin-top:0}}
`]})
export class AdminLayoutComponent{private auth=inject(AuthService);private router=inject(Router);logout(){this.auth.logout().subscribe({next:()=>this.router.navigate(['/login']),error:()=>this.router.navigate(['/login'])});}}
