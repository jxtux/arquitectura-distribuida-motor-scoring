package com.finanscore.motorscoring.application.security.port.in;
import com.finanscore.motorscoring.application.security.model.IssuedSession;
public interface RefreshSessionUseCase {
    IssuedSession refresh(String refreshToken, String ipAddress, String userAgent);
}
