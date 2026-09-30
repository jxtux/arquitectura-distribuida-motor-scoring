package com.finanscore.motorscoring.application.security.port.in;
import com.finanscore.motorscoring.application.security.model.MfaSetup;
public interface SetupMfaUseCase { MfaSetup setup(String setupToken); }
