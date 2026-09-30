package com.finanscore.motorscoring.domain.service;

import com.finanscore.motorscoring.domain.entity.Solicitante;
import com.finanscore.motorscoring.domain.valueobject.RelacionDeudaIngreso;


public final class CalculadorRelacionDeudaIngreso {
	
	public RelacionDeudaIngreso calcular(Solicitante s) {
		return RelacionDeudaIngreso.calcular(s.obligacionesFinancieras().monto(), s.ingresosMensuales().monto());
	}
}
