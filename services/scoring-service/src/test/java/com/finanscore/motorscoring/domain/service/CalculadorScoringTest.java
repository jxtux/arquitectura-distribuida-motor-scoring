package com.finanscore.motorscoring.domain.service;

import com.finanscore.motorscoring.domain.entity.EvaluacionCrediticia;
import com.finanscore.motorscoring.domain.enums.EstadoEvaluacion;
import com.finanscore.motorscoring.domain.enums.ResultadoScoring;
import com.finanscore.motorscoring.domain.fixture.ScoringFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadorScoringTest {
	private CalculadorScoring calculador;

	@BeforeEach
	void setUp() {
		calculador = new CalculadorScoring(new CalculadorCapacidadPago(), new CalculadorRelacionDeudaIngreso(),
		 		new CalculadorRelacionCuotaIngreso(),
				new EvaluadorReglasExcluyentes());
	}

	@Test
	void debeCalcularPuntajeConTodosLosFactores() {
		EvaluacionCrediticia evaluacion = calculador.calcular(ScoringFixture.solicitudValida(),
				ScoringFixture.solicitanteValido(0), ScoringFixture.versionModeloValida(),
				LocalDateTime.of(2026, 1, 2, 10, 0));
		assertTrue(evaluacion.puntajeTotal().valor() >= 0);
		assertTrue(evaluacion.puntajeTotal().valor() <= 1000);
		assertEquals(8, evaluacion.resultadosFactor().size());
		assertEquals(EstadoEvaluacion.COMPLETADA, evaluacion.estado());
	}

	@Test
	void unaAlertaDeMoraDebeForzarRechazo() {
		EvaluacionCrediticia evaluacion = calculador.calcular(ScoringFixture.solicitudValida(),
				ScoringFixture.solicitanteValido(1), ScoringFixture.versionModeloValida(),
				LocalDateTime.of(2026, 1, 2, 10, 0));
		assertEquals(ResultadoScoring.RECHAZADA, evaluacion.resultado());
		assertEquals(EstadoEvaluacion.CON_REGLA_EXCLUYENTE, evaluacion.estado());
	}
}
