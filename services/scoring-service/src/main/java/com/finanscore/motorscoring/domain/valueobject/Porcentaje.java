package com.finanscore.motorscoring.domain.valueobject;

import com.finanscore.motorscoring.domain.exception.DomainException;
import java.math.*;


public record Porcentaje(BigDecimal valor) {
	
	public Porcentaje {
		
		if (valor == null)
			throw new DomainException("El porcentaje es obligatorio.");
		
		valor = valor.setScale(4, RoundingMode.HALF_UP);
		
		if (valor.signum() < 0 || valor.compareTo(new BigDecimal("100")) > 0)
			throw new DomainException("El porcentaje debe estar entre 0 y 100.");
		
	}
}
