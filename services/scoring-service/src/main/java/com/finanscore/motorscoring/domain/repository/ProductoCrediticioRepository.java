package com.finanscore.motorscoring.domain.repository;

import com.finanscore.motorscoring.domain.entity.*;
import java.util.Optional;


public interface ProductoCrediticioRepository {
	
	Optional<ProductoCrediticio> buscarPorId(Long id);

	Optional<ProductoCrediticio> buscarPorCodigo(String codigo);
	
}
