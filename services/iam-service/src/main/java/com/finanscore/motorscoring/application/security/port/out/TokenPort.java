package com.finanscore.motorscoring.application.security.port.out;
import com.finanscore.motorscoring.application.security.model.IssuedSession;
import java.time.Duration;
import java.util.Set;
public interface TokenPort {
    String issueTemporaryToken(Long userId, String purpose, Duration ttl);
    Long validateTemporaryToken(String token, String expectedPurpose);
    IssuedSession issueSession(Long userId, Set<String> roles, Set<String> permissions,
                               Duration accessTtl, Duration refreshTtl);
    String hashRefreshToken(String rawRefreshToken);
}
