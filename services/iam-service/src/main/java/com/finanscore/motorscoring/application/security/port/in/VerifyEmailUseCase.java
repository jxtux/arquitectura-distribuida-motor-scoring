package com.finanscore.motorscoring.application.security.port.in;
import com.finanscore.motorscoring.application.security.command.VerifyEmailCommand;
public interface VerifyEmailUseCase { String verify(VerifyEmailCommand command); }
