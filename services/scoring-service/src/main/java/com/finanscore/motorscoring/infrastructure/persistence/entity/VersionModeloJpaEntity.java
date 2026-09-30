package com.finanscore.motorscoring.infrastructure.persistence.entity;

import com.finanscore.motorscoring.domain.entity.VersionModelo;
import com.finanscore.motorscoring.domain.enums.EstadoVersionModelo;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;


@Entity
@Table(name = "versiones_modelo")
public class VersionModeloJpaEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_version_modelo")
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_modelo", nullable = false)
	private ModeloScoringJpaEntity modelo;
	
	@Column(name = "numero_version", nullable = false, length = 20)
	private String numeroVersion;
	
	@Column(name = "fecha_inicio_vigencia", nullable = false)
	private LocalDate fechaInicioVigencia;
	
	@Column(name = "fecha_fin_vigencia")
	private LocalDate fechaFinVigencia;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoVersionModelo estado;
	
	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;
	
	@OneToMany(mappedBy = "version", fetch = FetchType.LAZY)
	private Set<FactorScoringJpaEntity> factores = new HashSet<>();

	protected VersionModeloJpaEntity() {
	}

	public VersionModelo toDomain() {
		return new VersionModelo(id, numeroVersion, fechaInicioVigencia, fechaFinVigencia, estado, factores.stream().map(FactorScoringJpaEntity::toDomain).toList());
	}
}
