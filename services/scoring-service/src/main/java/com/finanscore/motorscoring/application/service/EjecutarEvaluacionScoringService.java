package com.finanscore.motorscoring.application.service;

import com.finanscore.motorscoring.application.command.EjecutarEvaluacionScoringCommand;
import com.finanscore.motorscoring.application.dto.*;
import com.finanscore.motorscoring.application.exception.*;
import com.finanscore.motorscoring.application.usecase.EjecutarEvaluacionScoringUseCase;
import com.finanscore.motorscoring.domain.entity.*;
import com.finanscore.motorscoring.domain.exception.SolicitudNoEvaluableException;
import com.finanscore.motorscoring.domain.repository.*;
import com.finanscore.motorscoring.domain.service.CalculadorScoring;
import java.time.*;


public final class EjecutarEvaluacionScoringService implements EjecutarEvaluacionScoringUseCase {
	private final SolicitudCreditoRepository solicitudes;
	private final SolicitanteRepository solicitantes;
	private final ProductoCrediticioRepository productos;
	private final ModeloScoringRepository modelos;
	private final EvaluacionCrediticiaRepository evaluaciones;
	private final CalculadorScoring calculador;
	private final Clock clock;

	public EjecutarEvaluacionScoringService(SolicitudCreditoRepository q, SolicitanteRepository s, 	ProductoCrediticioRepository p, ModeloScoringRepository m, EvaluacionCrediticiaRepository e, CalculadorScoring c, Clock clock) {
		solicitudes = q;
		solicitantes = s;
		productos = p;
		modelos = m;
		evaluaciones = e;
		calculador = c;
		this.clock = clock;
	}

	public EvaluacionScoringDto ejecutar(EjecutarEvaluacionScoringCommand cmd) {
		
		//1. Buscar solicitud
		SolicitudCredito solicitud = solicitudes.buscarPorId(cmd.idSolicitud()).orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada: " + cmd.idSolicitud()));
		
		//2. Validar estado solicitud
		if (!solicitud.estaRegistrada())
			throw new SolicitudNoEvaluableException("La solicitud ya fue evaluada o no está registrada.");
		
		//3. Recuperar solicitante
		Solicitante solicitante = solicitantes.buscarPorId(solicitud.idSolicitante()).orElseThrow(() -> new RecursoNoEncontradoException("Solicitante no encontrado."));
		
		//4. Verificar información financiera
		if (!solicitante.tieneDatosFinancierosCompletos() || !solicitante.tieneIngresosValidos())
			throw new SolicitudNoEvaluableException("Información financiera incompleta o inválida.");
		
		//5. Recuperar producto
		ProductoCrediticio producto = productos.buscarPorId(solicitud.idProducto()).orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado."));
		
		//6. Validar condiciones del producto
		producto.validarSolicitud(solicitud.montoSolicitado(), solicitud.plazoSolicitado());
		
		LocalDateTime ahora = LocalDateTime.now(clock);
		
		//3. Recuperar modelo
		//modelo de evaluación crediticia completo.
		//	Modelo: SCORING_PERSONAS
		//	Estado: ACTIVO
		//	Descripción: Modelo para préstamos personales
		//	Versiones:
		//		  ├── 1.0.0, vigente durante 2026
		//		  └── 2.0.0, vigente desde 2027
		ModeloScoring modelo = modelos.buscarCompletoPorId(producto.idModeloScoring()).orElseThrow(() -> new RecursoNoEncontradoException("Modelo no encontrado."));
		
		//8. Seleccionar versión activa
		VersionModelo version = modelo.versionActiva(ahora.toLocalDate());
		
		//9. Validar pesos
		version.validarPesos();
		
		//10. Verificar duplicidad de evaluación
		if (evaluaciones.existePorSolicitudYVersion(solicitud.id(), version.id()))
			throw new SolicitudDuplicadaException("La solicitud ya fue evaluada con la versión activa.");		
		
		//11. calcular scoring
		//Calcula la capacidad de pago.
		//Calcula la relación deuda-ingreso.
		//Calcula la relación cuota-ingreso.
		//Recorre los factores activos.
		//Busca la regla aplicable para cada factor.
		//Calcula el puntaje ponderado.
		//Detecta reglas excluyentes.
		//Suma el score.
		//Determina si el resultado es:
		//PREAPROBADA
		//REVISION_MANUAL
		//RECHAZADA
		//Construye una EvaluacionCrediticia.
		EvaluacionCrediticia evaluacion = evaluaciones.guardar(calculador.calcular(solicitud, solicitante, version, ahora));
		
		//12. Cambiar estado de solicitud
		solicitud.marcarEvaluada();		
		solicitudes.guardar(solicitud);
		
		//13 Mapear resultados
		var factores = evaluacion.resultadosFactor().stream().map(r -> new ResultadoFactorDto(r.codigoFactor(), r.valorEvaluado(), r.pesoAplicado(), r.puntajeBase(),r.puntajeObtenido(), r.reglaAplicada(), r.observacion(), r.reglaExcluyente())).toList();
		
		return new EvaluacionScoringDto(evaluacion.id(), evaluacion.idSolicitud(), evaluacion.puntajeTotal().valor(), evaluacion.resultado().name(), evaluacion.estado().name(), version.numeroVersion(),evaluacion.fechaEvaluacion(), factores);
	}
}
