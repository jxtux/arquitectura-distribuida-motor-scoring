package com.finanscore.motorscoring.application.usecase;

import com.finanscore.motorscoring.application.command.EjecutarEvaluacionScoringCommand;
import com.finanscore.motorscoring.application.dto.EvaluacionScoringDto;

public interface EjecutarEvaluacionScoringUseCase {
	
	EvaluacionScoringDto ejecutar(EjecutarEvaluacionScoringCommand command);
	
}
