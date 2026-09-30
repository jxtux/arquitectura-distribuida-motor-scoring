package com.finanscore.motorscoring.infrastructure.transaction;

import com.finanscore.motorscoring.application.command.EjecutarEvaluacionScoringCommand;
import com.finanscore.motorscoring.application.dto.EvaluacionScoringDto;
import com.finanscore.motorscoring.application.usecase.EjecutarEvaluacionScoringUseCase;
import org.springframework.transaction.support.TransactionTemplate;


public final class TransactionalEjecutarEvaluacionScoringUseCase implements EjecutarEvaluacionScoringUseCase {
	
	private final EjecutarEvaluacionScoringUseCase delegate;
	private final TransactionTemplate tx;

	public TransactionalEjecutarEvaluacionScoringUseCase(EjecutarEvaluacionScoringUseCase d, TransactionTemplate t) {
		delegate = d;
		tx = t;
	}

	public EvaluacionScoringDto ejecutar(EjecutarEvaluacionScoringCommand c) {
		return tx.execute(s -> delegate.ejecutar(c));
	}
}
