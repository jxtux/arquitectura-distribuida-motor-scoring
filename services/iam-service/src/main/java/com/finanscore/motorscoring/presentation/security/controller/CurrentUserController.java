package com.finanscore.motorscoring.presentation.security.controller;

import com.finanscore.motorscoring.application.security.model.UserAccount;
import com.finanscore.motorscoring.application.security.port.in.GetCurrentUserUseCase;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("api")
@RequestMapping("/api/v1/users")
public class CurrentUserController {
    private final GetCurrentUserUseCase currentUser;

    public CurrentUserController(GetCurrentUserUseCase currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UserAccount user = currentUser.get(Long.valueOf(jwt.getSubject()));
        return new CurrentUserResponse(
            user.id(),
            user.displayName(),
            user.email(),
            user.status().name());
    }

    public record CurrentUserResponse(Long id, String displayName, String email, String status) {}
}
