package com.finanscore.motorscoring.domain.repository;

import com.finanscore.motorscoring.domain.entity.*;
import java.util.Optional;


public interface ModeloScoringRepository {
	
	Optional<ModeloScoring> buscarCompletoPorId(Long idModelo);
}
