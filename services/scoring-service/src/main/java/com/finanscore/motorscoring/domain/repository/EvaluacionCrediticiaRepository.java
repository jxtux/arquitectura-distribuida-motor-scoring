package com.finanscore.motorscoring.domain.repository;

import com.finanscore.motorscoring.domain.entity.*;

public interface EvaluacionCrediticiaRepository {
	
	boolean existePorSolicitudYVersion(Long idSolicitud, Long idVersionModelo);

	EvaluacionCrediticia guardar(EvaluacionCrediticia evaluacion);
}
