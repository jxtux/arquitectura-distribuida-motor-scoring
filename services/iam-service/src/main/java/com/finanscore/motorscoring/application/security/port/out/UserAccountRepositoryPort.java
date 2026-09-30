package com.finanscore.motorscoring.application.security.port.out;
import com.finanscore.motorscoring.application.security.model.UserAccount;
import java.util.Optional;
public interface UserAccountRepositoryPort {
    Optional<UserAccount> findById(Long id);
    Optional<UserAccount> findByEmail(String email);
    UserAccount save(UserAccount user);
}
