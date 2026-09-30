package com.finanscore.motorscoring.presentation.security.dto;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}
    public record RegisterRequest(@NotBlank String displayName, @Email @NotBlank String email,
                                  @Size(min=12,max=128) String password) {}
    public record VerifyEmailRequest(@Email @NotBlank String email, @Pattern(regexp="\\d{6}") String code) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record SetupMfaRequest(@NotBlank String setupToken) {}
    public record ConfirmMfaRequest(@NotBlank String setupToken, @Pattern(regexp="\\d{6}") String code) {}
    public record VerifyMfaRequest(@NotBlank String challengeToken, @Pattern(regexp="\\d{6}") String code) {}
    public record MessageResponse(String message) {}
    public record AccessTokenResponse(String accessToken, long expiresInSeconds) {}
}
