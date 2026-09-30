package com.finanscore.motorscoring.infrastructure.persistence.adapter;

import com.finanscore.motorscoring.domain.entity.SolicitudCredito;
import com.finanscore.motorscoring.domain.repository.SolicitudCreditoRepository;
import com.finanscore.motorscoring.domain.valueobject.IdentificadorExterno;
import com.finanscore.motorscoring.infrastructure.persistence.entity.SolicitudCreditoJpaEntity;
import com.finanscore.motorscoring.infrastructure.persistence.springdata.SolicitudCreditoSpringDataRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Profile("postgres")
@Repository
public class SolicitudCreditoRepositoryAdapter implements SolicitudCreditoRepository {
	private final SolicitudCreditoSpringDataRepository repo;

	public SolicitudCreditoRepositoryAdapter(SolicitudCreditoSpringDataRepository r) {
		repo = r;
	}

	public boolean existePorIdentificadorExterno(IdentificadorExterno i) {
		return repo.existsByIdentificadorExterno(i.valor());
	}

	public Optional<SolicitudCredito> buscarPorId(Long id) {
		return repo.findById(id).map(SolicitudCreditoJpaEntity::toDomain);
	}

	public SolicitudCredito guardar(SolicitudCredito s) {
		return repo.save(SolicitudCreditoJpaEntity.fromDomain(s)).toDomain();
	}
}
