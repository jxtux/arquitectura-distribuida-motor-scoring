package com.finanscore.motorscoring.application.security.command;

public record ConfirmMfaCommand(
    String setupToken,
    String code,
    String ipAddress,
    String userAgent
) {}
