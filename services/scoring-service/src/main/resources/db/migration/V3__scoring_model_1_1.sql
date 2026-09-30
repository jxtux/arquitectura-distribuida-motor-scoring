-- ============================================================
-- V3__agregar_factor_relacion_cuota_ingreso.sql
--
-- Crea la versión 1.1.0 del modelo de scoring.
-- Copia los factores y reglas de la versión 1.0.0.
-- Agrega el factor RELACION_CUOTA_INGRESO.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Finalizar la versión actualmente activa
-- ------------------------------------------------------------

UPDATE versiones_modelo
SET estado = 'INACTIVA',
    fecha_fin_vigencia = DATE '2026-07-19'
WHERE id_modelo = 1
  AND estado = 'ACTIVA';


-- ------------------------------------------------------------
-- 2. Crear la nueva versión del modelo
-- ------------------------------------------------------------

INSERT INTO versiones_modelo (
    id_version_modelo,
    id_modelo,
    numero_version,
    fecha_inicio_vigencia,
    fecha_fin_vigencia,
    estado
)
VALUES (
    2,
    1,
    '1.1.0',
    DATE '2026-07-20',
    NULL,
    'ACTIVA'
);


-- ------------------------------------------------------------
-- 3. Copiar los factores existentes a la versión 1.1.0
--
-- Los pesos anteriores se reducen proporcionalmente al 90%
-- para reservar un peso de 10% al nuevo factor.
--
-- Total:
-- 22.50 + 18 + 18 + 13.50 + 9 + 4.50 + 4.50 + 0 + 10
-- = 100
-- ------------------------------------------------------------

INSERT INTO factores_scoring (
    id_factor,
    id_version_modelo,
    codigo,
    nombre,
    descripcion,
    peso,
    estado
)
VALUES
(
    9,
    2,
    'HISTORIAL_PAGOS',
    'Historial de pagos',
    'Comportamiento histórico',
    22.50,
    'ACTIVO'
),
(
    10,
    2,
    'RELACION_DEUDA_INGRESO',
    'Relación deuda-ingreso',
    'Obligaciones respecto del ingreso',
    18.00,
    'ACTIVO'
),
(
    11,
    2,
    'CAPACIDAD_PAGO',
    'Capacidad de pago',
    'Ingreso disponible',
    18.00,
    'ACTIVO'
),
(
    12,
    2,
    'ESTABILIDAD_INGRESOS',
    'Estabilidad de ingresos',
    'Continuidad de ingresos',
    13.50,
    'ACTIVO'
),
(
    13,
    2,
    'ANTIGUEDAD_LABORAL',
    'Antigüedad laboral',
    'Meses de permanencia',
    9.00,
    'ACTIVO'
),
(
    14,
    2,
    'OBLIGACIONES_ACTIVAS',
    'Obligaciones activas',
    'Cantidad de obligaciones',
    4.50,
    'ACTIVO'
),
(
    15,
    2,
    'MONTO_CAPACIDAD',
    'Monto frente a capacidad',
    'Monto solicitado respecto a capacidad',
    4.50,
    'ACTIVO'
),
(
    16,
    2,
    'ALERTAS_MORA',
    'Alertas de mora',
    'Regla excluyente',
    0.00,
    'ACTIVO'
),
(
    17,
    2,
    'RELACION_CUOTA_INGRESO',
    'Relación cuota-ingreso',
    'Porcentaje de la cuota mensual respecto de los ingresos mensuales',
    10.00,
    'ACTIVO'
);


-- ------------------------------------------------------------
-- 4. Copiar las reglas de los ocho factores anteriores
--
-- Se relacionan los factores de ambas versiones mediante
-- su código para asignar las reglas al nuevo id_factor.
-- ------------------------------------------------------------

INSERT INTO reglas_evaluacion (
    id_factor,
    codigo,
    descripcion,
    valor_minimo,
    valor_maximo,
    puntaje,
    es_excluyente,
    resultado_excluyente,
    estado
)
SELECT
    factor_nuevo.id_factor,
    regla.codigo,
    regla.descripcion,
    regla.valor_minimo,
    regla.valor_maximo,
    regla.puntaje,
    regla.es_excluyente,
    regla.resultado_excluyente,
    regla.estado
FROM reglas_evaluacion regla
INNER JOIN factores_scoring factor_anterior
    ON factor_anterior.id_factor = regla.id_factor
INNER JOIN factores_scoring factor_nuevo
    ON factor_nuevo.id_version_modelo = 2
   AND factor_nuevo.codigo = factor_anterior.codigo
WHERE factor_anterior.id_version_modelo = 1;


-- ------------------------------------------------------------
-- 5. Insertar las reglas del factor RELACION_CUOTA_INGRESO
--
-- Fórmula:
-- cuota mensual = monto solicitado / plazo solicitado
-- porcentaje = cuota mensual / ingresos mensuales * 100
-- ------------------------------------------------------------

INSERT INTO reglas_evaluacion (
    id_factor,
    codigo,
    descripcion,
    valor_minimo,
    valor_maximo,
    puntaje,
    es_excluyente,
    resultado_excluyente,
    estado
)
VALUES
(
    17,
    'RCI_BAJA',
    'Cuota menor o igual al 20% de los ingresos',
    0.0000,
    20.0000,
    100,
    FALSE,
    NULL,
    'ACTIVA'
),
(
    17,
    'RCI_MODERADA',
    'Cuota mayor al 20% y menor o igual al 30% de los ingresos',
    20.0001,
    30.0000,
    75,
    FALSE,
    NULL,
    'ACTIVA'
),
(
    17,
    'RCI_ALTA',
    'Cuota mayor al 30% y menor o igual al 40% de los ingresos',
    30.0001,
    40.0000,
    40,
    FALSE,
    NULL,
    'ACTIVA'
),
(
    17,
    'RCI_MUY_ALTA',
    'Cuota mayor al 40% de los ingresos',
    40.0001,
    9999.0000,
    0,
    FALSE,
    NULL,
    'ACTIVA'
);