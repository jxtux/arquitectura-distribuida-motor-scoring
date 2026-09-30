package com.finanscore.motorscoring.application.security.port.in;
import com.finanscore.motorscoring.application.security.command.VerifyMfaCommand;
import com.finanscore.motorscoring.application.security.model.IssuedSession;
public interface VerifyMfaUseCase { IssuedSession verify(VerifyMfaCommand command); }
