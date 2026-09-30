package com.finanscore.motorscoring.application.security.model;

/** Resultado del registro o reanudación de un registro incompleto. */
public record RegistrationStage(String nextStep, String token, String message) {}
