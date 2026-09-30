package com.finanscore.motorscoring.application.security.port.out;
import com.finanscore.motorscoring.application.security.model.UserSession;
import java.util.Optional;
public interface SessionRepositoryPort {
    UserSession save(UserSession session);
    Optional<UserSession> findByRefreshTokenHash(String hash);
}
