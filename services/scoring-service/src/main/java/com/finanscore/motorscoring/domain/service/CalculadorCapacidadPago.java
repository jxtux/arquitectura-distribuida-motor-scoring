package com.finanscore.motorscoring.domain.service;

import com.finanscore.motorscoring.domain.entity.Solicitante;
import com.finanscore.motorscoring.domain.valueobject.CapacidadPago;


public final class CalculadorCapacidadPago {
	
	public CapacidadPago calcular(Solicitante s) {
		
		return CapacidadPago.calcular(s.ingresosMensuales().monto(), s.gastosMensuales().monto(),s.obligacionesFinancieras().monto(), s.moneda());
	}
}
