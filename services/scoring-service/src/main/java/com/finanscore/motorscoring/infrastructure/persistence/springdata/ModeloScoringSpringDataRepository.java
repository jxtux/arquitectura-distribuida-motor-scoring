package com.finanscore.motorscoring.infrastructure.persistence.springdata;

import com.finanscore.motorscoring.infrastructure.persistence.entity.ModeloScoringJpaEntity;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;


public interface ModeloScoringSpringDataRepository extends JpaRepository<ModeloScoringJpaEntity, Long> {
	
	@EntityGraph(attributePaths = { "versiones", "versiones.factores", "versiones.factores.reglas" })
	@Query("select m from ModeloScoringJpaEntity m where m.id=:id")
	Optional<ModeloScoringJpaEntity> findCompletoById(Long id);
}
