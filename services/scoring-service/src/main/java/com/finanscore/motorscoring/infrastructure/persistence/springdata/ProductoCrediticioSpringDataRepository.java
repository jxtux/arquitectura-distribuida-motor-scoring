package com.finanscore.motorscoring.infrastructure.persistence.springdata;

import com.finanscore.motorscoring.infrastructure.persistence.entity.ProductoCrediticioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface ProductoCrediticioSpringDataRepository extends JpaRepository<ProductoCrediticioJpaEntity, Long> {
	Optional<ProductoCrediticioJpaEntity> findByCodigo(String codigo);
}
