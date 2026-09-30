package com.finanscore.motorscoring.domain.entity;

import com.finanscore.motorscoring.domain.enums.*;
import com.finanscore.motorscoring.domain.valueobject.PuntajeCrediticio;
import java.time.LocalDateTime;
import java.util.*;

public final class EvaluacionCrediticia {

	//Contiene los objetos principales del negocio que tienen:
	//Identidad.
	//Estado.
	//Ciclo de vida.
	//Comportamiento de negocio.
	//Relaciones con otros objetos del dominio.
			
	private final Long id, idSolicitud, idVersionModelo;
	
	private final LocalDateTime fechaEvaluacion;

	private final PuntajeCrediticio puntajeTotal;

	private final ResultadoScoring resultado;

	private final EstadoEvaluacion estado;

	private final List<ResultadoFactor> resultadosFactor;

	public EvaluacionCrediticia(Long id, Long solicitud, Long version, LocalDateTime fecha, PuntajeCrediticio puntaje, ResultadoScoring resultado, EstadoEvaluacion estado, List<ResultadoFactor> factores) {
		this.id = id;
		this.idSolicitud = Objects.requireNonNull(solicitud);
		this.idVersionModelo = Objects.requireNonNull(version);
		this.fechaEvaluacion = Objects.requireNonNull(fecha);
		this.puntajeTotal = Objects.requireNonNull(puntaje);
		this.resultado = Objects.requireNonNull(resultado);
		this.estado = Objects.requireNonNull(estado);
		this.resultadosFactor = List.copyOf(factores);
	}

	public boolean esInmutable() {
		return estado == EstadoEvaluacion.COMPLETADA || estado == EstadoEvaluacion.CON_REGLA_EXCLUYENTE;
	}

	public Long id() {
		return id;
	}

	public Long idSolicitud() {
		return idSolicitud;
	}

	public Long idVersionModelo() {
		return idVersionModelo;
	}

	public LocalDateTime fechaEvaluacion() {
		return fechaEvaluacion;
	}

	public PuntajeCrediticio puntajeTotal() {
		return puntajeTotal;
	}

	public ResultadoScoring resultado() {
		return resultado;
	}

	public EstadoEvaluacion estado() {
		return estado;
	}

	public List<ResultadoFactor> resultadosFactor() {
		return resultadosFactor;
	}
}
