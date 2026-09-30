package com.finanscore.motorscoring.application.security.port.in;

import com.finanscore.motorscoring.application.security.command.ConfirmMfaCommand;
import com.finanscore.motorscoring.application.security.model.IssuedSession;

public interface ConfirmMfaUseCase {
    IssuedSession confirm(ConfirmMfaCommand command);
}
