package com.finanscore.motorscoring.infrastructure.persistence.entity;

import com.finanscore.motorscoring.domain.entity.EvaluacionCrediticia;
import com.finanscore.motorscoring.domain.enums.ResultadoScoring;
import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "resultados_scoring")
public class ResultadoScoringJpaEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_resultado_scoring")
	private Long id;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_evaluacion", nullable = false, unique = true)
	private EvaluacionCrediticiaJpaEntity evaluacion;
	
	@Column(name = "puntaje_total", nullable = false)
	private int puntajeTotal;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ResultadoScoring resultado;
	
	@Column(name = "fecha_resultado", nullable = false)
	private LocalDateTime fechaResultado;

	protected ResultadoScoringJpaEntity() {
	}

	static ResultadoScoringJpaEntity fromDomain(EvaluacionCrediticiaJpaEntity ev, EvaluacionCrediticia d) {
		var j = new ResultadoScoringJpaEntity();
		j.evaluacion = ev;
		j.puntajeTotal = d.puntajeTotal().valor();
		j.resultado = d.resultado();
		j.fechaResultado = d.fechaEvaluacion();
		
		return j;
	}
}
