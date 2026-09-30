package com.finanscore.motorscoring.application.service;

import com.finanscore.motorscoring.application.command.CrearSolicitudCreditoCommand;
import com.finanscore.motorscoring.application.dto.SolicitudCreditoDto;
import com.finanscore.motorscoring.application.exception.*;
import com.finanscore.motorscoring.application.usecase.CrearSolicitudCreditoUseCase;
import com.finanscore.motorscoring.domain.entity.*;
import com.finanscore.motorscoring.domain.repository.*;
import com.finanscore.motorscoring.domain.valueobject.*;
import java.time.*;


public final class CrearSolicitudCreditoService implements CrearSolicitudCreditoUseCase {
	private final SolicitanteRepository solicitantes;
	
	private final SolicitudCreditoRepository solicitudes;
	
	private final ProductoCrediticioRepository productos;
	
	private final Clock clock;

	public CrearSolicitudCreditoService(SolicitanteRepository s, SolicitudCreditoRepository q, ProductoCrediticioRepository p, Clock c) {
		solicitantes = s;
		solicitudes = q;
		productos = p;
		clock = c;
	}

	public SolicitudCreditoDto ejecutar(CrearSolicitudCreditoCommand c) {
		
		//Paso 1: construir identificador externo
		IdentificadorExterno ext = new IdentificadorExterno(c.identificadorExterno());
		
		//Paso 2: comprobar duplicado
		if (solicitudes.existePorIdentificadorExterno(ext))
			throw new SolicitudDuplicadaException("Ya existe una solicitud con identificador externo " + ext.valor());
				
		//Paso 3: buscar producto
		ProductoCrediticio producto = productos.buscarPorCodigo(c.codigoProducto()).orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + c.codigoProducto()));
		
		//Paso 4: crear Value Objects financieros
		NumeroDocumento doc = new NumeroDocumento(c.tipoDocumento(), c.numeroDocumento());		
		Dinero ingresos = new Dinero(c.ingresosMensuales(), c.moneda()), gastos = new Dinero(c.gastosMensuales(), c.moneda()), obligaciones = new Dinero(c.obligacionesFinancieras(), c.moneda());
		
		//Paso 5: obtener la fecha
		LocalDateTime ahora = LocalDateTime.now(clock);
		
		//Paso 6: buscar o crear solicitante
		//Solicitante existente   → actualizar perfil
		//Solicitante inexistente → registrar uno nuevo
		Solicitante solicitante = solicitantes.buscarPorDocumento(doc)
				.map(a -> a.actualizarPerfil(c.nombresRazonSocial(), ingresos, gastos, obligaciones,
						c.antiguedadLaboralNegocio(), c.numeroObligacionesActivas(), c.puntajeHistorialPagos(),
						c.alertasMora()))
				.orElseGet(() -> Solicitante.registrar(doc, c.nombresRazonSocial(), ingresos, gastos, obligaciones,
						c.antiguedadLaboralNegocio(), c.numeroObligacionesActivas(), c.puntajeHistorialPagos(),
						c.alertasMora(), ahora));
		
		//Paso 7: guardar solicitante
		solicitante = solicitantes.guardar(solicitante);
		
		//Paso 8: registrar solicitud
		SolicitudCredito solicitud = SolicitudCredito.registrar(solicitante.id(), producto.id(),
				Dinero.positivo(c.montoSolicitado(), c.moneda()), c.plazoSolicitado(), c.finalidadCredito(),
				c.canalOrigen(), ext, ahora);
		
		//Paso 9: guardar solicitud
		solicitud = solicitudes.guardar(solicitud);
		
		//Paso 10: construir DTO
		return new SolicitudCreditoDto(solicitud.id(), solicitante.id(), ext.valor(), producto.codigo(),
				solicitud.montoSolicitado().monto(), solicitud.plazoSolicitado(),
				solicitud.montoSolicitado().moneda().name(), solicitud.estado().name(), solicitud.fechaRegistro());
	}
}
