package com.finanscore.motorscoring.application.security.model;
public record AuthStage(String token, String nextStep, boolean mfaConfigured) {}
