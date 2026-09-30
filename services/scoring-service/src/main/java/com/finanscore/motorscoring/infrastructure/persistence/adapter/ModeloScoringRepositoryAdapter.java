package com.finanscore.motorscoring.infrastructure.persistence.adapter;

import com.finanscore.motorscoring.domain.entity.ModeloScoring;
import com.finanscore.motorscoring.domain.repository.ModeloScoringRepository;
import com.finanscore.motorscoring.infrastructure.persistence.entity.ModeloScoringJpaEntity;
import com.finanscore.motorscoring.infrastructure.persistence.springdata.ModeloScoringSpringDataRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Profile("postgres")
@Repository
public class ModeloScoringRepositoryAdapter implements ModeloScoringRepository {
	private final ModeloScoringSpringDataRepository repo;

	public ModeloScoringRepositoryAdapter(ModeloScoringSpringDataRepository r) {
		repo = r;
	}

	public Optional<ModeloScoring> buscarCompletoPorId(Long id) {
		return repo.findCompletoById(id).map(ModeloScoringJpaEntity::toDomain);
	}
}
