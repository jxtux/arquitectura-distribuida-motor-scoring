package com.finanscore.motorscoring.application.security.service;

import com.finanscore.motorscoring.application.security.exception.SecurityApplicationException;
import com.finanscore.motorscoring.application.security.model.UserAccount;
import com.finanscore.motorscoring.application.security.port.in.GetCurrentUserUseCase;
import com.finanscore.motorscoring.application.security.port.out.UserAccountRepositoryPort;

public final class GetCurrentUserService implements GetCurrentUserUseCase {
    private final UserAccountRepositoryPort users;

    public GetCurrentUserService(UserAccountRepositoryPort users) {
        this.users = users;
    }

    @Override
    public UserAccount get(Long userId) {
        if (userId == null) {
            throw new SecurityApplicationException("INVALID_SESSION", "Sesión inválida.");
        }
        return users.findById(userId)
            .orElseThrow(() -> new SecurityApplicationException("USER_NOT_FOUND", "Usuario no encontrado."));
    }
}
