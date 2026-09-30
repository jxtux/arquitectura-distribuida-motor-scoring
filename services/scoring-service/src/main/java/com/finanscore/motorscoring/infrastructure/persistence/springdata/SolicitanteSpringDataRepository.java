package com.finanscore.motorscoring.infrastructure.persistence.springdata;

import com.finanscore.motorscoring.domain.enums.TipoDocumento;
import com.finanscore.motorscoring.infrastructure.persistence.entity.SolicitanteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface SolicitanteSpringDataRepository extends JpaRepository<SolicitanteJpaEntity, Long> {
	
	Optional<SolicitanteJpaEntity> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
	
}
