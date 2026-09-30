package com.finanscore.motorscoring.domain.valueobject;

import com.finanscore.motorscoring.domain.exception.DomainException;
import java.math.*;


public record RelacionDeudaIngreso(BigDecimal porcentaje) {
	
	public RelacionDeudaIngreso {

		if (porcentaje == null || porcentaje.signum() < 0)
			throw new DomainException("La relación deuda-ingreso no puede ser negativa.");
		
		porcentaje = porcentaje.setScale(4, RoundingMode.HALF_UP);
	}

	public static RelacionDeudaIngreso calcular(BigDecimal obligaciones, BigDecimal ingresos) {
		
		if (ingresos == null || ingresos.signum() <= 0)
			throw new DomainException("Los ingresos deben ser mayores que cero.");
		
		return new RelacionDeudaIngreso(obligaciones.multiply(new BigDecimal("100")).divide(ingresos, 4, RoundingMode.HALF_UP));
	}
}
