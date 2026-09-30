package com.finanscore.motorscoring.infrastructure.transaction;

import com.finanscore.motorscoring.application.command.CrearSolicitudCreditoCommand;
import com.finanscore.motorscoring.application.dto.SolicitudCreditoDto;
import com.finanscore.motorscoring.application.usecase.CrearSolicitudCreditoUseCase;
import org.springframework.transaction.support.TransactionTemplate;


public final class TransactionalCrearSolicitudCreditoUseCase implements CrearSolicitudCreditoUseCase {
	private final CrearSolicitudCreditoUseCase delegate;
	private final TransactionTemplate tx;

	public TransactionalCrearSolicitudCreditoUseCase(CrearSolicitudCreditoUseCase d, TransactionTemplate t) {
		delegate = d;
		tx = t;
	}

	public SolicitudCreditoDto ejecutar(CrearSolicitudCreditoCommand c) {
		return tx.execute(s -> delegate.ejecutar(c));
	}
}
