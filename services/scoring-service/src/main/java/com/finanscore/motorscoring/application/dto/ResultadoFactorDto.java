package com.finanscore.motorscoring.application.dto;

import java.math.BigDecimal;

public record ResultadoFactorDto(String factor, BigDecimal valorEvaluado, BigDecimal pesoAplicado, int puntajeBase, int puntajeObtenido, String reglaAplicada, String observacion, boolean excluyente) {
}
