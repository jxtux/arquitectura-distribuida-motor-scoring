package com.finanscore.motorscoring.application.security.port.in;
import com.finanscore.motorscoring.application.security.command.LoginLocalCommand;
import com.finanscore.motorscoring.application.security.model.AuthStage;
public interface LoginLocalUseCase { AuthStage login(LoginLocalCommand command); }
