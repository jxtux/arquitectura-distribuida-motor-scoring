package com.finanscore.motorscoring.infrastructure.persistence.adapter;

import com.finanscore.motorscoring.domain.entity.Solicitante;
import com.finanscore.motorscoring.domain.repository.SolicitanteRepository;
import com.finanscore.motorscoring.domain.valueobject.NumeroDocumento;
import com.finanscore.motorscoring.infrastructure.persistence.entity.SolicitanteJpaEntity;
import com.finanscore.motorscoring.infrastructure.persistence.springdata.SolicitanteSpringDataRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Profile("postgres")
@Repository
public class SolicitanteRepositoryAdapter implements SolicitanteRepository {
	private final SolicitanteSpringDataRepository repo;

	public SolicitanteRepositoryAdapter(SolicitanteSpringDataRepository r) {
		repo = r;
	}

	public Optional<Solicitante> buscarPorId(Long id) {
		return repo.findById(id).map(SolicitanteJpaEntity::toDomain);
	}

	public Optional<Solicitante> buscarPorDocumento(NumeroDocumento d) {
		return repo.findByTipoDocumentoAndNumeroDocumento(d.tipo(), d.numero()).map(SolicitanteJpaEntity::toDomain);
	}

	public Solicitante guardar(Solicitante s) {
		return repo.save(SolicitanteJpaEntity.fromDomain(s)).toDomain();
	}
}
