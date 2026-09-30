package com.finanscore.motorscoring.application.security.port.out;
import com.finanscore.motorscoring.application.security.model.LocalCredential;
import java.util.Optional;
public interface LocalCredentialRepositoryPort {
    Optional<LocalCredential> findByUserId(Long userId);
    LocalCredential save(LocalCredential credential);
}
