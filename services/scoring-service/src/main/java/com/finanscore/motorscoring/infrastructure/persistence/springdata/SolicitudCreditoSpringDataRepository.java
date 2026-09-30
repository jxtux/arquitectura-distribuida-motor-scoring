package com.finanscore.motorscoring.infrastructure.persistence.springdata;

import com.finanscore.motorscoring.infrastructure.persistence.entity.SolicitudCreditoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SolicitudCreditoSpringDataRepository extends JpaRepository<SolicitudCreditoJpaEntity, Long> {
	
	boolean existsByIdentificadorExterno(String identificadorExterno);
}
