package com.finanscore.motorscoring.infrastructure.persistence.entity;

import com.finanscore.motorscoring.domain.entity.FactorScoring;
import com.finanscore.motorscoring.domain.enums.EstadoFactor;
import com.finanscore.motorscoring.domain.valueobject.Porcentaje;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;


@Entity
@Table(name = "factores_scoring")
public class FactorScoringJpaEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_factor")
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_version_modelo", nullable = false)
	private VersionModeloJpaEntity version;
	
	@Column(nullable = false, length = 30)
	private String codigo;
	
	@Column(nullable = false, length = 150)
	private String nombre;
	
	@Column(length = 255)
	private String descripcion;
	
	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal peso;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoFactor estado;
	
	@OneToMany(mappedBy = "factor", fetch = FetchType.LAZY)
	private Set<ReglaEvaluacionJpaEntity> reglas = new HashSet<>();

	protected FactorScoringJpaEntity() {
	}

	public FactorScoring toDomain() {
		return new FactorScoring(id, codigo, nombre, descripcion, new Porcentaje(peso), estado, reglas.stream().map(ReglaEvaluacionJpaEntity::toDomain).toList());
	}
}
