package com.finanscore.motorscoring.domain.valueobject;

import com.finanscore.motorscoring.domain.exception.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RelacionCuotaIngreso(BigDecimal porcentaje) {

    private static final BigDecimal CIEN = new BigDecimal("100");

    public RelacionCuotaIngreso {

        if (porcentaje == null) {
            throw new DomainException("La relación cuota-ingreso es obligatoria.");
        }

        if (porcentaje.signum() < 0) {
            throw new DomainException("La relación cuota-ingreso no puede ser negativa.");
        }

        porcentaje = porcentaje.setScale( 4, RoundingMode.HALF_UP);
    }

    public static RelacionCuotaIngreso calcular(BigDecimal montoSolicitado, Integer plazoSolicitado, BigDecimal ingresosMensuales) {

        if (montoSolicitado == null || montoSolicitado.signum() <= 0) {
            throw new DomainException("El monto solicitado debe ser mayor que cero.");
        }

        if (plazoSolicitado == null || plazoSolicitado <= 0) {
            throw new DomainException("El plazo solicitado debe ser mayor que cero.");
        }

        if (ingresosMensuales == null || ingresosMensuales.signum() <= 0) {
            throw new DomainException("Los ingresos deben ser mayores que cero.");
        }

        BigDecimal cuotaEstimada = montoSolicitado.divide( BigDecimal.valueOf(plazoSolicitado), 4, RoundingMode.HALF_UP);

        BigDecimal porcentaje = cuotaEstimada.multiply(CIEN).divide(ingresosMensuales,4,RoundingMode.HALF_UP);

        return new RelacionCuotaIngreso(porcentaje);
    }
}