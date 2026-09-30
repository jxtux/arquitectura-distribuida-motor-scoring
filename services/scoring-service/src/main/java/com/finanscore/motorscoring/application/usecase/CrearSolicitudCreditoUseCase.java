package com.finanscore.motorscoring.application.usecase;

import com.finanscore.motorscoring.application.command.CrearSolicitudCreditoCommand;
import com.finanscore.motorscoring.application.dto.SolicitudCreditoDto;

public interface CrearSolicitudCreditoUseCase {
	
	SolicitudCreditoDto ejecutar(CrearSolicitudCreditoCommand command);
	
}
