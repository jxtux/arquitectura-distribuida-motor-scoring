package com.finanscore.motorscoring.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public record SolicitudCreditoDto(Long idSolicitud, Long idSolicitante, String identificadorExterno, String codigoProducto, BigDecimal montoSolicitado, int plazoSolicitado, String moneda, String estado, LocalDateTime fechaRegistro) {
}
