package com.finanscore.motorscoring.application.dto;

import java.time.LocalDateTime;
import java.util.List;


public record EvaluacionScoringDto(Long idEvaluacion, Long idSolicitud, int puntajeTotal, String resultado, String estado, String versionModelo, LocalDateTime fechaEvaluacion, List<ResultadoFactorDto> factores) {
}
