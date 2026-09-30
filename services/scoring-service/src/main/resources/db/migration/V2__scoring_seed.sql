INSERT INTO modelos_scoring(id_modelo,codigo,nombre,descripcion,estado) VALUES(1,'MODELO_PERSONAL','Modelo de préstamo personal','Modelo inicial RF04-RF06','ACTIVO');
INSERT INTO versiones_modelo(id_version_modelo,id_modelo,numero_version,fecha_inicio_vigencia,fecha_fin_vigencia,estado) VALUES(1,1,'1.0.0',DATE '2025-01-01',NULL,'ACTIVA');
INSERT INTO factores_scoring(id_factor,id_version_modelo,codigo,nombre,descripcion,peso,estado) VALUES
(1,1,'HISTORIAL_PAGOS','Historial de pagos','Comportamiento histórico',25,'ACTIVO'),
(2,1,'RELACION_DEUDA_INGRESO','Relación deuda-ingreso','Obligaciones respecto del ingreso',20,'ACTIVO'),
(3,1,'CAPACIDAD_PAGO','Capacidad de pago','Ingreso disponible',20,'ACTIVO'),
(4,1,'ESTABILIDAD_INGRESOS','Estabilidad de ingresos','Continuidad de ingresos',15,'ACTIVO'),
(5,1,'ANTIGUEDAD_LABORAL','Antigüedad laboral','Meses de permanencia',10,'ACTIVO'),
(6,1,'OBLIGACIONES_ACTIVAS','Obligaciones activas','Cantidad de obligaciones',5,'ACTIVO'),
(7,1,'MONTO_CAPACIDAD','Monto frente a capacidad','Monto solicitado respecto a capacidad',5,'ACTIVO'),
(8,1,'ALERTAS_MORA','Alertas de mora','Regla excluyente',0,'ACTIVO');
INSERT INTO reglas_evaluacion(id_factor,codigo,descripcion,valor_minimo,valor_maximo,puntaje,es_excluyente,resultado_excluyente,estado) VALUES
(1,'HP_BAJO','Historial deficiente',0,39.9999,20,FALSE,NULL,'ACTIVA'),(1,'HP_REGULAR','Historial regular',40,59.9999,50,FALSE,NULL,'ACTIVA'),(1,'HP_BUENO','Historial bueno',60,79.9999,75,FALSE,NULL,'ACTIVA'),(1,'HP_EXCELENTE','Historial excelente',80,100,100,FALSE,NULL,'ACTIVA'),
(2,'RDI_BAJA','Endeudamiento bajo',0,20,100,FALSE,NULL,'ACTIVA'),(2,'RDI_MEDIA','Endeudamiento moderado',20.0001,35,80,FALSE,NULL,'ACTIVA'),(2,'RDI_ALTA','Endeudamiento elevado',35.0001,50,50,FALSE,NULL,'ACTIVA'),(2,'RDI_MUY_ALTA','Endeudamiento muy elevado',50.0001,9999,20,FALSE,NULL,'ACTIVA'),
(3,'CP_CRITICA','Capacidad crítica',0,9.9999,20,FALSE,NULL,'ACTIVA'),(3,'CP_BAJA','Capacidad baja',10,24.9999,60,FALSE,NULL,'ACTIVA'),(3,'CP_MEDIA','Capacidad media',25,39.9999,80,FALSE,NULL,'ACTIVA'),(3,'CP_ALTA','Capacidad alta',40,100,100,FALSE,NULL,'ACTIVA'),
(4,'EI_BAJA','Ingresos poco estables',0,5,20,FALSE,NULL,'ACTIVA'),(4,'EI_MEDIA','Estabilidad inicial',6,11,40,FALSE,NULL,'ACTIVA'),(4,'EI_BUENA','Ingresos estables',12,23,70,FALSE,NULL,'ACTIVA'),(4,'EI_ALTA','Ingresos muy estables',24,9999,100,FALSE,NULL,'ACTIVA'),
(5,'AL_BAJA','Antigüedad menor a un año',0,11,25,FALSE,NULL,'ACTIVA'),(5,'AL_MEDIA','Antigüedad entre uno y tres años',12,35,60,FALSE,NULL,'ACTIVA'),(5,'AL_ALTA','Antigüedad mayor a tres años',36,9999,100,FALSE,NULL,'ACTIVA'),
(6,'OA_BAJA','Pocas obligaciones',0,1,100,FALSE,NULL,'ACTIVA'),(6,'OA_MEDIA','Obligaciones moderadas',2,3,75,FALSE,NULL,'ACTIVA'),(6,'OA_ALTA','Varias obligaciones',4,5,50,FALSE,NULL,'ACTIVA'),(6,'OA_MUY_ALTA','Demasiadas obligaciones',6,9999,20,FALSE,NULL,'ACTIVA'),
(7,'MC_BAJA','Monto conservador',0,30,100,FALSE,NULL,'ACTIVA'),(7,'MC_MEDIA','Monto moderado',30.0001,60,75,FALSE,NULL,'ACTIVA'),(7,'MC_ALTA','Monto elevado',60.0001,100,50,FALSE,NULL,'ACTIVA'),(7,'MC_MUY_ALTA','Monto superior a capacidad',100.0001,9999,10,FALSE,NULL,'ACTIVA'),
(8,'AM_SIN_ALERTAS','Sin alertas',0,0,100,FALSE,NULL,'ACTIVA'),(8,'AM_CON_ALERTAS','Mora vigente',1,9999,0,TRUE,'RECHAZADA','ACTIVA');
INSERT INTO productos_crediticios(id_producto,codigo,nombre,monto_minimo,monto_maximo,plazo_minimo,plazo_maximo,moneda,estado,id_modelo_scoring) VALUES(1,'PRESTAMO_PERSONAL','Préstamo personal',1000,50000,6,48,'PEN','ACTIVO',1);
