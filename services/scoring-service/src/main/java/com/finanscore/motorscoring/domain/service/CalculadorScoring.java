package com.finanscore.motorscoring.domain.service;

import com.finanscore.motorscoring.domain.entity.*;
import com.finanscore.motorscoring.domain.enums.*;
import com.finanscore.motorscoring.domain.exception.SolicitudNoEvaluableException;
import com.finanscore.motorscoring.domain.valueobject.*;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;


public final class CalculadorScoring {
	
	private final CalculadorCapacidadPago capacidad;
	
	private final CalculadorRelacionDeudaIngreso relacion;
	
	private final CalculadorRelacionCuotaIngreso relacionCuotaIngreso;
	
	private final EvaluadorReglasExcluyentes excluyentes;

	public CalculadorScoring(CalculadorCapacidadPago c, CalculadorRelacionDeudaIngreso r,CalculadorRelacionCuotaIngreso rci, EvaluadorReglasExcluyentes e) {
		capacidad = c;
		relacion = r;
		relacionCuotaIngreso = rci;
		excluyentes = e;
	}

	public EvaluacionCrediticia calcular(SolicitudCredito solicitud, Solicitante solicitante, VersionModelo version, LocalDateTime fecha) {
		
		if (!solicitud.estaEvaluable())
			throw new SolicitudNoEvaluableException("La solicitud no está REGISTRADA.");
		
		version.validarPesos();
		
		CapacidadPago cp = capacidad.calcular(solicitante);		
		RelacionDeudaIngreso rdi = relacion.calcular(solicitante);
		RelacionCuotaIngreso rci = relacionCuotaIngreso.calcular(solicitud,solicitante);		
		List<ResultadoFactor> resultados = new ArrayList<>();
		int total = 0;
		
		for (FactorScoring f : version.factores()) {
			
			if (f.estado() != EstadoFactor.ACTIVO)
				continue;
			
			BigDecimal valor = valorFactor(f.codigo(), solicitud, solicitante, cp, rdi,rci);			
			ReglaEvaluacion regla = f.evaluar(valor);			
			int aporte = regla.excluyente() ? 0: BigDecimal.valueOf(regla.puntaje()).multiply(f.peso().valor()).multiply(BigDecimal.TEN).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValueExact();			
			total += aporte;
			
			resultados.add(new ResultadoFactor(f.id(), f.codigo(), valor, f.peso().valor(), regla.puntaje(), aporte, regla.codigo(), regla.descripcion(), regla.excluyente(), regla.resultadoExcluyente()));
		}
		
		total = Math.max(0, Math.min(1000, total));		
		final int score = total;
		
		ResultadoScoring resultado = excluyentes.evaluar(resultados).orElseGet(() -> clasificar(score));		
		EstadoEvaluacion estado = resultados.stream().anyMatch(ResultadoFactor::reglaExcluyente)? EstadoEvaluacion.CON_REGLA_EXCLUYENTE: EstadoEvaluacion.COMPLETADA;		
		return new EvaluacionCrediticia(null, solicitud.id(), version.id(), fecha, new PuntajeCrediticio(total), resultado, estado, resultados);
	}

	private ResultadoScoring clasificar(int p) {
		
		if (p >= 750)
			return ResultadoScoring.PREAPROBADA;
		
		if (p >= 600)
			return ResultadoScoring.REVISION_MANUAL;
		
		return ResultadoScoring.RECHAZADA;
	}

	private BigDecimal valorFactor(String codigo, SolicitudCredito sol, Solicitante s, CapacidadPago cp, RelacionDeudaIngreso rdi,
		RelacionCuotaIngreso rci) {
		
		return switch (codigo) {
		
			case "HISTORIAL_PAGOS" -> BigDecimal.valueOf(s.puntajeHistorialPagos());
			case "RELACION_DEUDA_INGRESO" -> rdi.porcentaje();
			case "CAPACIDAD_PAGO" -> porcentaje(cp.disponible().monto(), s.ingresosMensuales().monto());
			case "ESTABILIDAD_INGRESOS", "ANTIGUEDAD_LABORAL" -> BigDecimal.valueOf(s.antiguedadLaboralNegocio());
			case "OBLIGACIONES_ACTIVAS" -> BigDecimal.valueOf(s.numeroObligacionesActivas());
			case "MONTO_CAPACIDAD" -> porcentaje(sol.montoSolicitado().monto(), cp.disponible().monto().multiply(BigDecimal.valueOf(sol.plazoSolicitado())));
			case "ALERTAS_MORA" -> BigDecimal.valueOf(s.alertasMora());
			case "RELACION_CUOTA_INGRESO" -> rci.porcentaje();
			default -> throw new SolicitudNoEvaluableException("Factor no soportado: " + codigo);
		};
	}

	private BigDecimal porcentaje(BigDecimal n, BigDecimal d) {
		
		if (d.signum() <= 0)
			return new BigDecimal("9999.0000");
		
		return n.multiply(new BigDecimal("100")).divide(d, 4, RoundingMode.HALF_UP);
	}
}
