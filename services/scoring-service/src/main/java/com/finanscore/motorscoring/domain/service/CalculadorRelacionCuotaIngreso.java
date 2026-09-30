package com.finanscore.motorscoring.domain.service;

import com.finanscore.motorscoring.domain.entity.Solicitante;
import com.finanscore.motorscoring.domain.entity.SolicitudCredito;
import com.finanscore.motorscoring.domain.valueobject.RelacionCuotaIngreso;

public final class CalculadorRelacionCuotaIngreso {

	public RelacionCuotaIngreso calcular(SolicitudCredito solicitud, Solicitante solicitante) {

		return RelacionCuotaIngreso.calcular(
				solicitud.montoSolicitado().monto(),
				solicitud.plazoSolicitado(),
				solicitante.ingresosMensuales().monto()
		);
	}
}