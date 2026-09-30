package com.finanscore.motorscoring.application.security.port.in;

import com.finanscore.motorscoring.application.security.command.RegisterUserCommand;
import com.finanscore.motorscoring.application.security.model.RegistrationStage;

public interface RegisterUserUseCase {
    RegistrationStage register(RegisterUserCommand command);
}
