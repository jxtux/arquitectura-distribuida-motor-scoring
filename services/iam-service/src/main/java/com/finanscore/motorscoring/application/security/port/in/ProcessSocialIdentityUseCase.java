package com.finanscore.motorscoring.application.security.port.in;
import com.finanscore.motorscoring.application.security.command.ProcessSocialIdentityCommand;
import com.finanscore.motorscoring.application.security.model.AuthStage;
public interface ProcessSocialIdentityUseCase { AuthStage process(ProcessSocialIdentityCommand command); }
