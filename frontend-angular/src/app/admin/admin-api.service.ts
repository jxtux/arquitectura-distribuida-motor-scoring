import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, forkJoin, map, of } from 'rxjs';
import { API_BASE_URL, GRAFANA_BASE_URL } from '../config/api.config';

export interface OperationSummary { requestId:string; correlationId:string; userId:number|null; productCode:string|null; amount:number|null; currency:string|null; termMonths?:number|null; purpose?:string|null; paymentStatus?:string|null; status:string; score:number|null; recommendation:string|null; modelVersion?:string|null; reportId?:string|null; reportAvailable?:boolean; notificationStatus?:string|null; failureReason?:string|null; lastEventType?:string|null; createdAt:string; updatedAt:string; }
export interface AuditEvent { eventId:string; eventType:string; requestId:string; correlationId:string; causationId:string|null; traceId:string|null; source:string; occurredAt:string; payload:Record<string,unknown>; }
export interface OperationDetail { summary:OperationSummary; timeline:AuditEvent[]; dlts:DltEvent[]; observability:{logs:string;traces:string;metrics:string}; }
export interface DltEvent { id:string; sourceTopic:string; sourceService:string|null; eventId:string|null; eventType:string|null; requestId:string|null; correlationId:string|null; traceId:string|null; error:string|null; retryCount:number|null; recordedAt:string; payload:string; }

@Injectable({providedIn:'root'})
export class AdminApiService {
  private readonly http=inject(HttpClient);
  private readonly queryBase=`${API_BASE_URL}/api/v1/query/admin`;
  private readonly auditBase=`${API_BASE_URL}/api/v1/admin`;

  /** CQRS read side: lista y resumen se obtienen exclusivamente del Query Service/read model. */
  operations(query='',status=''){let p=new HttpParams().set('limit',100);if(query)p=p.set('query',query);if(status)p=p.set('status',status);return this.http.get<OperationSummary[]>(`${this.queryBase}/operations`,{params:p});}

  /** Query Service aporta estado actual; Audit Service aporta solo historia y DLT. */
  operation(requestId:string){
    return forkJoin({
      summary:this.http.get<OperationSummary>(`${this.queryBase}/operations/${encodeURIComponent(requestId)}`),
      timeline:this.audit(requestId,'').pipe(catchError(() => of([] as AuditEvent[]))),
      dlts:this.dlt(requestId,'').pipe(catchError(() => of([] as DltEvent[])))
    }).pipe(map(({summary,timeline,dlts})=>({summary,timeline,dlts,observability:{logs:`${GRAFANA_BASE_URL}/explore`,traces:`${GRAFANA_BASE_URL}/explore`,metrics:`${GRAFANA_BASE_URL}/d/motor-scoring-overview/motor-scoring-overview`}})));
  }

  audit(requestId='',correlationId=''){let p=new HttpParams().set('limit',200);if(requestId)p=p.set('requestId',requestId);if(correlationId)p=p.set('correlationId',correlationId);return this.http.get<AuditEvent[]>(`${this.auditBase}/audit`,{params:p});}
  dlt(requestId='',correlationId=''){let p=new HttpParams().set('limit',200);if(requestId)p=p.set('requestId',requestId);if(correlationId)p=p.set('correlationId',correlationId);return this.http.get<DltEvent[]>(`${this.auditBase}/dlt`,{params:p});}
}
