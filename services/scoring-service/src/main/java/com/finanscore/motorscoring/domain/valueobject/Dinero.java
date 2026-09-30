package com.finanscore.motorscoring.domain.valueobject;

import com.finanscore.motorscoring.domain.enums.Moneda;
import com.finanscore.motorscoring.domain.exception.DomainException;
import java.math.*;
import java.util.Objects;


public record Dinero(BigDecimal monto, Moneda moneda) {

	public Dinero {
		Objects.requireNonNull(monto, "El monto es obligatorio.");
		Objects.requireNonNull(moneda, "La moneda es obligatoria.");
		monto = monto.setScale(2, RoundingMode.HALF_UP);
		
		if (monto.signum() < 0)
			throw new DomainException("El monto no puede ser negativo.");
	}

	public static Dinero positivo(BigDecimal monto, Moneda moneda) {
		Dinero d = new Dinero(monto, moneda);
		
		if (d.monto.signum() <= 0)
			throw new DomainException("El monto debe ser mayor que cero.");
		
		return d;
	}
}
