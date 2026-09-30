package com.finanscore.motorscoring.application.security.port.out;
import java.util.Set;
public interface AuthorizationRepositoryPort {
    Set<String> findRoles(Long userId);
    Set<String> findPermissions(Long userId);
    void assignRole(Long userId, String roleName);
}
