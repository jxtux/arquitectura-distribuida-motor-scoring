package com.finanscore.motorscoring.application.security.command;
public record VerifyMfaCommand(String challengeToken, String code, String ipAddress, String userAgent) {}
