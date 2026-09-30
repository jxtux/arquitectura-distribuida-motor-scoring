package com.finanscore.motorscoring.infrastructure.persistence.entity;

import com.finanscore.motorscoring.domain.entity.EvaluacionCrediticia;
import com.finanscore.motorscoring.domain.enums.*;
import com.finanscore.motorscoring.domain.valueobject.PuntajeCrediticio;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "evaluaciones_crediticias", uniqueConstraints = @UniqueConstraint(name = "uk_evaluacion_solicitud_version", columnNames = {"id_solicitud", "id_version_modelo" }))
public class EvaluacionCrediticiaJpaEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_evaluacion")
	private Long id;
	
	@Column(name = "id_solicitud", nullable = false)
	private Long idSolicitud;
	
	@Column(name = "id_version_modelo", nullable = false)
	private Long idVersionModelo;
	
	@Column(name = "fecha_evaluacion", nullable = false)
	private LocalDateTime fechaEvaluacion;
	
	@Column(name = "puntaje_total", nullable = false)
	private int puntajeTotal;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ResultadoScoring resultado;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EstadoEvaluacion estado;
	
	@OneToMany(mappedBy = "evaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ResultadoFactorJpaEntity> resultadosFactor = new ArrayList<>();
	
	@OneToOne(mappedBy = "evaluacion", cascade = CascadeType.ALL, orphanRemoval = true)
	private ResultadoScoringJpaEntity resultadoFinal;

	protected EvaluacionCrediticiaJpaEntity() {
	}

	public static EvaluacionCrediticiaJpaEntity fromDomain(EvaluacionCrediticia e) {
		var j = new EvaluacionCrediticiaJpaEntity();
		j.id = e.id();
		j.idSolicitud = e.idSolicitud();
		j.idVersionModelo = e.idVersionModelo();
		j.fechaEvaluacion = e.fechaEvaluacion();
		j.puntajeTotal = e.puntajeTotal().valor();
		j.resultado = e.resultado();
		j.estado = e.estado();
		j.resultadosFactor = new ArrayList<>(
				e.resultadosFactor().stream().map(r -> ResultadoFactorJpaEntity.fromDomain(j, r)).toList());
		j.resultadoFinal = ResultadoScoringJpaEntity.fromDomain(j, e);
		return j;
	}

	public EvaluacionCrediticia toDomain() {
		return new EvaluacionCrediticia(id, idSolicitud, idVersionModelo, fechaEvaluacion,
				new PuntajeCrediticio(puntajeTotal), resultado, estado,
				resultadosFactor.stream().map(ResultadoFactorJpaEntity::toDomain).toList());
	}
}
