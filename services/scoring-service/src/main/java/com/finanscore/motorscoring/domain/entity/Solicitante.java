package com.finanscore.motorscoring.domain.entity;

import com.finanscore.motorscoring.domain.enums.*;
import com.finanscore.motorscoring.domain.exception.DomainException;
import com.finanscore.motorscoring.domain.valueobject.*;
import java.time.LocalDateTime;
import java.util.Objects;

public final class Solicitante {
	private final Long id;
	
	private final NumeroDocumento documento;
	
	private final String nombresRazonSocial;
	
	private final Dinero ingresosMensuales, gastosMensuales, obligacionesFinancieras;
	
	private final int antiguedadLaboralNegocio, numeroObligacionesActivas, puntajeHistorialPagos, alertasMora;
	
	private final EstadoRegistro estado;
	
	private final LocalDateTime fechaRegistro;

	private Solicitante(Long id, NumeroDocumento doc, String nombres, Dinero ingresos, Dinero gastos, Dinero obligaciones, int antiguedad, int activas, int historial, int alertas, EstadoRegistro estado,LocalDateTime fecha) {
		
		this.id = id;
		this.documento = Objects.requireNonNull(doc);
		
		if (nombres == null || nombres.isBlank())
			throw new DomainException("Los nombres o razón social son obligatorios.");
		
		this.nombresRazonSocial = nombres.trim();
		this.ingresosMensuales = Objects.requireNonNull(ingresos);
		this.gastosMensuales = Objects.requireNonNull(gastos);
		this.obligacionesFinancieras = Objects.requireNonNull(obligaciones);
		
		if (ingresos.monto().signum() <= 0)
			throw new DomainException("Los ingresos deben ser mayores que cero.");
		
		if (ingresos.moneda() != gastos.moneda() || ingresos.moneda() != obligaciones.moneda())
			throw new DomainException("Los datos financieros deben usar la misma moneda.");
		
		if (antiguedad < 0 || activas < 0 || alertas < 0)
			throw new DomainException("Los indicadores no pueden ser negativos.");
		
		if (historial < 0 || historial > 100)
			throw new DomainException("El historial de pagos debe estar entre 0 y 100.");
		
		this.antiguedadLaboralNegocio = antiguedad;
		this.numeroObligacionesActivas = activas;
		this.puntajeHistorialPagos = historial;
		this.alertasMora = alertas;
		this.estado = Objects.requireNonNull(estado);
		this.fechaRegistro = Objects.requireNonNull(fecha);
	}

	public static Solicitante registrar(NumeroDocumento d, String n, Dinero i, Dinero g, Dinero o, int a, int ac, int h, int am, LocalDateTime f) {
		return new Solicitante(null, d, n, i, g, o, a, ac, h, am, EstadoRegistro.ACTIVO, f);
	}

	public static Solicitante reconstituir(Long id, NumeroDocumento d, String n, Dinero i, Dinero g, Dinero o, int a, int ac, int h, int am, EstadoRegistro e, LocalDateTime f) {
		return new Solicitante(id, d, n, i, g, o, a, ac, h, am, e, f);
	}

	public Solicitante actualizarPerfil(String n, Dinero i, Dinero g, Dinero o, int a, int ac, int h, int am) {
		return new Solicitante(id, documento, n, i, g, o, a, ac, h, am, estado, fechaRegistro);
	}

	public boolean tieneIngresosValidos() {
		return ingresosMensuales.monto().signum() > 0;
	}

	public boolean tieneDatosFinancierosCompletos() {
		return ingresosMensuales != null && gastosMensuales != null && obligacionesFinancieras != null;
	}

	public Long id() {
		return id;
	}

	public NumeroDocumento documento() {
		return documento;
	}

	public String nombresRazonSocial() {
		return nombresRazonSocial;
	}

	public Dinero ingresosMensuales() {
		return ingresosMensuales;
	}

	public Dinero gastosMensuales() {
		return gastosMensuales;
	}

	public Dinero obligacionesFinancieras() {
		return obligacionesFinancieras;
	}

	public int antiguedadLaboralNegocio() {
		return antiguedadLaboralNegocio;
	}

	public int numeroObligacionesActivas() {
		return numeroObligacionesActivas;
	}

	public int puntajeHistorialPagos() {
		return puntajeHistorialPagos;
	}

	public int alertasMora() {
		return alertasMora;
	}

	public EstadoRegistro estado() {
		return estado;
	}

	public LocalDateTime fechaRegistro() {
		return fechaRegistro;
	}

	public Moneda moneda() {
		return ingresosMensuales.moneda();
	}
}
