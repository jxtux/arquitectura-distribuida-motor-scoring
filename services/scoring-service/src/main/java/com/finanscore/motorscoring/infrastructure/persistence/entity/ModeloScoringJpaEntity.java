package com.finanscore.motorscoring.infrastructure.persistence.entity;

import com.finanscore.motorscoring.domain.entity.ModeloScoring;
import com.finanscore.motorscoring.domain.enums.EstadoModelo;
import jakarta.persistence.*;
import java.util.*;


@Entity
@Table(name = "modelos_scoring")
public class ModeloScoringJpaEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_modelo")
	private Long id;
	
	@Column(nullable = false, unique = true, length = 30)
	private String codigo;
	
	@Column(nullable = false, length = 150)
	private String nombre;
	
	@Column(length = 255)
	private String descripcion;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoModelo estado;
	
	@OneToMany(mappedBy = "modelo", fetch = FetchType.LAZY)
	private Set<VersionModeloJpaEntity> versiones = new HashSet<>();

	protected ModeloScoringJpaEntity() {
	}

	public ModeloScoring toDomain() {
		return new ModeloScoring(id, codigo, nombre, estado, versiones.stream().map(VersionModeloJpaEntity::toDomain).toList());
	}
}
