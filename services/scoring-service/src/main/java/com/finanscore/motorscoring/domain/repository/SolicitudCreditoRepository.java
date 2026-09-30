package com.finanscore.motorscoring.domain.repository;

import com.finanscore.motorscoring.domain.entity.*;
import com.finanscore.motorscoring.domain.valueobject.*;
import java.util.Optional;


public interface SolicitudCreditoRepository {
	
	boolean existePorIdentificadorExterno(IdentificadorExterno identificador);

	Optional<SolicitudCredito> buscarPorId(Long id);

	SolicitudCredito guardar(SolicitudCredito solicitud);
}
