package com.finanscore.motorscoring.infrastructure.persistence.entity;

import com.finanscore.motorscoring.domain.entity.ReglaEvaluacion;
import com.finanscore.motorscoring.domain.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;


@Entity
@Table(name = "reglas_evaluacion")
public class ReglaEvaluacionJpaEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_regla")
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_factor", nullable = false)
	private FactorScoringJpaEntity factor;
	
	@Column(nullable = false, length = 30)
	private String codigo;
	
	@Column(nullable = false, length = 255)
	private String descripcion;
	
	@Column(name = "valor_minimo", nullable = false, precision = 18, scale = 4)
	private BigDecimal valorMinimo;
	
	@Column(name = "valor_maximo", nullable = false, precision = 18, scale = 4)
	private BigDecimal valorMaximo;
	
	@Column(nullable = false)
	private int puntaje;
	
	@Column(name = "es_excluyente", nullable = false)
	private boolean excluyente;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "resultado_excluyente", length = 30)
	private ResultadoScoring resultadoExcluyente;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoRegla estado;

	protected ReglaEvaluacionJpaEntity() {
	}

	public ReglaEvaluacion toDomain() {
		return new ReglaEvaluacion(id, codigo, descripcion, valorMinimo, valorMaximo, puntaje, excluyente, resultadoExcluyente, estado);
	}
}
