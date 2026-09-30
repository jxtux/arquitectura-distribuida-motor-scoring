package com.finanscore.motorscoring.application.command;

import com.finanscore.motorscoring.domain.enums.*;
import java.math.BigDecimal;


public record CrearSolicitudCreditoCommand(String identificadorExterno, TipoDocumento tipoDocumento,		
		String numeroDocumento, String nombresRazonSocial, BigDecimal ingresosMensuales, BigDecimal gastosMensuales,		
		BigDecimal obligacionesFinancieras, int antiguedadLaboralNegocio, int numeroObligacionesActivas,		
		int puntajeHistorialPagos, int alertasMora, String codigoProducto, BigDecimal montoSolicitado,		
		int plazoSolicitado, Moneda moneda, String finalidadCredito, String canalOrigen) {
}
