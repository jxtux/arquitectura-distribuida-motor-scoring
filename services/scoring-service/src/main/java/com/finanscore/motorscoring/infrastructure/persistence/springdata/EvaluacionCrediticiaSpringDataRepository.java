package com.finanscore.motorscoring.infrastructure.persistence.springdata;

import com.finanscore.motorscoring.infrastructure.persistence.entity.EvaluacionCrediticiaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EvaluacionCrediticiaSpringDataRepository extends JpaRepository<EvaluacionCrediticiaJpaEntity, Long> {
	
	boolean existsByIdSolicitudAndIdVersionModelo(Long idSolicitud, Long idVersionModelo);
}
