PATCH V3.6.2 / V3.6.3 - ADMIN AUDIT / DLT

Causa:
- payload_json y headers_json son columnas PostgreSQL TEXT.
- Las entidades tenían @Lob, por lo que Hibernate/JDBC intentaba leerlas como PostgreSQL Large Object (OID).
- Resultado: HTTP 500 "Large Objects may not be used in auto-commit mode".

Cambios:
1) Elimina @Lob de AuditEventEntity.payloadJson.
2) Elimina @Lob de DltEventEntity.payloadJson y headersJson.
3) AdminAuditService usa @Transactional(readOnly=true).
4) AdminApiService hace que Ver detalle siga mostrando el resumen aunque Audit/DLT temporalmente falle.

No requiere migración SQL: las columnas ya son TEXT.

Después de copiar los archivos sobre el proyecto:
  docker compose --env-file .env --env-file secrets/bootstrap.env up -d --build audit-service frontend-angular

Luego verificar:
  docker compose --env-file .env --env-file secrets/bootstrap.env logs --tail=100 audit-service

Y probar:
  http://localhost:4200/admin/auditoria
  http://localhost:4200/admin/dlt
  http://localhost:4200/admin/operaciones/<requestId>
