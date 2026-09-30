package com.finanscore.motorscoring.domain.repository;

import com.finanscore.motorscoring.domain.entity.*;
import com.finanscore.motorscoring.domain.valueobject.*;
import java.util.Optional;


public interface SolicitanteRepository {
	
	Optional<Solicitante> buscarPorId(Long id);

	Optional<Solicitante> buscarPorDocumento(NumeroDocumento documento);

	Solicitante guardar(Solicitante solicitante);
	
}
