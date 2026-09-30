package com.finanscore.motorscoring.infrastructure.persistence.entity;

import com.finanscore.motorscoring.domain.entity.ResultadoFactor;
import com.finanscore.motorscoring.domain.enums.ResultadoScoring;
import jakarta.persistence.*;
import java.math.BigDecimal;


@Entity
@Table(name = "resultados_factor")
public class ResultadoFactorJpaEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_resultado_factor")
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_evaluacion", nullable = false)
	private EvaluacionCrediticiaJpaEntity evaluacion;
	
	@Column(name = "id_factor", nullable = false)
	private Long idFactor;
	
	@Column(name = "codigo_factor", nullable = false, length = 30)
	private String codigoFactor;
	
	@Column(name = "valor_evaluado", nullable = false, precision = 18, scale = 4)
	private BigDecimal valorEvaluado;
	
	@Column(name = "peso_aplicado", nullable = false, precision = 5, scale = 2)
	private BigDecimal pesoAplicado;
	
	@Column(name = "puntaje_base", nullable = false)
	private int puntajeBase;
	
	@Column(name = "puntaje_obtenido", nullable = false)
	private int puntajeObtenido;
	
	@Column(name = "regla_aplicada", nullable = false, length = 30)
	private String reglaAplicada;
	
	@Column(length = 255)
	private String observacion;
	
	@Column(name = "regla_excluyente", nullable = false)
	private boolean reglaExcluyente;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "resultado_excluyente", length = 30)
	private ResultadoScoring resultadoExcluyente;

	protected ResultadoFactorJpaEntity() {
	}

	static ResultadoFactorJpaEntity fromDomain(EvaluacionCrediticiaJpaEntity ev, ResultadoFactor r) {
		var j = new ResultadoFactorJpaEntity();
		j.evaluacion = ev;
		j.idFactor = r.idFactor();
		j.codigoFactor = r.codigoFactor();
		j.valorEvaluado = r.valorEvaluado();
		j.pesoAplicado = r.pesoAplicado();
		j.puntajeBase = r.puntajeBase();
		j.puntajeObtenido = r.puntajeObtenido();
		j.reglaAplicada = r.reglaAplicada();
		j.observacion = r.observacion();
		j.reglaExcluyente = r.reglaExcluyente();
		j.resultadoExcluyente = r.resultadoExcluyente();
		return j;
	}

	ResultadoFactor toDomain() {
		return new ResultadoFactor(idFactor, codigoFactor, valorEvaluado, pesoAplicado, puntajeBase, puntajeObtenido, reglaAplicada, observacion, reglaExcluyente, resultadoExcluyente);
	}
}
