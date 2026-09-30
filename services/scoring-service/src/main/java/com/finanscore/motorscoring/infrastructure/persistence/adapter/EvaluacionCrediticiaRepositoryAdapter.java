package com.finanscore.motorscoring.infrastructure.persistence.adapter;

import com.finanscore.motorscoring.domain.entity.EvaluacionCrediticia;
import com.finanscore.motorscoring.domain.repository.EvaluacionCrediticiaRepository;
import com.finanscore.motorscoring.infrastructure.persistence.entity.EvaluacionCrediticiaJpaEntity;
import com.finanscore.motorscoring.infrastructure.persistence.springdata.EvaluacionCrediticiaSpringDataRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;


@Profile("postgres")
@Repository
public class EvaluacionCrediticiaRepositoryAdapter implements EvaluacionCrediticiaRepository {
	private final EvaluacionCrediticiaSpringDataRepository repo;

	public EvaluacionCrediticiaRepositoryAdapter(EvaluacionCrediticiaSpringDataRepository r) {
		repo = r;
	}

	public boolean existePorSolicitudYVersion(Long s, Long v) {
		return repo.existsByIdSolicitudAndIdVersionModelo(s, v);
	}

	public EvaluacionCrediticia guardar(EvaluacionCrediticia e) {
		return repo.save(EvaluacionCrediticiaJpaEntity.fromDomain(e)).toDomain();
	}
}
