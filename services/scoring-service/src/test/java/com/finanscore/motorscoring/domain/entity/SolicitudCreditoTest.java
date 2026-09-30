package com.finanscore.motorscoring.domain.entity;

import com.finanscore.motorscoring.domain.enums.EstadoSolicitud;
import com.finanscore.motorscoring.domain.enums.Moneda;
import com.finanscore.motorscoring.domain.exception.DomainException;
import com.finanscore.motorscoring.domain.valueobject.Dinero;
import com.finanscore.motorscoring.domain.valueobject.IdentificadorExterno;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitudCreditoTest {
	
	@Test
	void debeNacerRegistradaYPermitirUnaSolaEvaluacion() {
		SolicitudCredito solicitud = SolicitudCredito.registrar(1L, 1L, Dinero.positivo(new BigDecimal("10000"), Moneda.PEN), 12, "Consumo", "WEB", new IdentificadorExterno("EXT-100"), LocalDateTime.now());
		assertEquals(EstadoSolicitud.REGISTRADA, solicitud.estado());
		solicitud.marcarEvaluada();
		
		assertEquals(EstadoSolicitud.EVALUADA, solicitud.estado());
		assertThrows(DomainException.class, solicitud::marcarEvaluada);
	}
}
