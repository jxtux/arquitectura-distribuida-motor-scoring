package com.finanscore.motorscoring.infrastructure.persistence.adapter;

import com.finanscore.motorscoring.domain.entity.ProductoCrediticio;
import com.finanscore.motorscoring.domain.repository.ProductoCrediticioRepository;
import com.finanscore.motorscoring.infrastructure.persistence.entity.ProductoCrediticioJpaEntity;
import com.finanscore.motorscoring.infrastructure.persistence.springdata.ProductoCrediticioSpringDataRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Profile("postgres")
@Repository
public class ProductoCrediticioRepositoryAdapter implements ProductoCrediticioRepository {
	private final ProductoCrediticioSpringDataRepository repo;

	public ProductoCrediticioRepositoryAdapter(ProductoCrediticioSpringDataRepository r) {
		repo = r;
	}

	public Optional<ProductoCrediticio> buscarPorId(Long id) {
		return repo.findById(id).map(ProductoCrediticioJpaEntity::toDomain);
	}

	public Optional<ProductoCrediticio> buscarPorCodigo(String c) {
		return repo.findByCodigo(c).map(ProductoCrediticioJpaEntity::toDomain);
	}
}
