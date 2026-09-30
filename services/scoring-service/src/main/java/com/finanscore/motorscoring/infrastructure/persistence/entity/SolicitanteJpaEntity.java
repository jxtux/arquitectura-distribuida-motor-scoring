package com.finanscore.motorscoring.infrastructure.persistence.entity;

import com.finanscore.motorscoring.domain.entity.Solicitante;
import com.finanscore.motorscoring.domain.enums.*;
import com.finanscore.motorscoring.domain.valueobject.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "solicitantes", uniqueConstraints = @UniqueConstraint(name = "uk_solicitante_documento", columnNames = {"tipo_documento", "numero_documento" }))
public class SolicitanteJpaEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_solicitante")
	private Long id;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento", nullable = false, length = 20)
	private TipoDocumento tipoDocumento;
	
	@Column(name = "numero_documento", nullable = false, length = 30)
	private String numeroDocumento;
	
	@Column(name = "nombres_razon_social", nullable = false, length = 150)
	private String nombresRazonSocial;
	
	@Column(name = "ingresos_mensuales", nullable = false, precision = 18, scale = 2)
	private BigDecimal ingresosMensuales;
	
	@Column(name = "gastos_mensuales", nullable = false, precision = 18, scale = 2)
	private BigDecimal gastosMensuales;
	
	@Column(name = "obligaciones_financieras", nullable = false, precision = 18, scale = 2)
	private BigDecimal obligacionesFinancieras;
	
	@Column(name = "antiguedad_laboral_negocio", nullable = false)
	private int antiguedadLaboralNegocio;
	
	@Column(name = "numero_obligaciones_activas", nullable = false)
	private int numeroObligacionesActivas;
	
	@Column(name = "puntaje_historial_pagos", nullable = false)
	private int puntajeHistorialPagos;
	
	@Column(name = "alertas_mora", nullable = false)
	private int alertasMora;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Moneda moneda;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoRegistro estado;
	
	@Column(name = "fecha_registro", nullable = false)
	private LocalDateTime fechaRegistro;

	protected SolicitanteJpaEntity() {
	}

	public static SolicitanteJpaEntity fromDomain(Solicitante s) {
		var e = new SolicitanteJpaEntity();
		e.id = s.id();
		e.tipoDocumento = s.documento().tipo();
		e.numeroDocumento = s.documento().numero();
		e.nombresRazonSocial = s.nombresRazonSocial();
		e.ingresosMensuales = s.ingresosMensuales().monto();
		e.gastosMensuales = s.gastosMensuales().monto();
		e.obligacionesFinancieras = s.obligacionesFinancieras().monto();
		e.antiguedadLaboralNegocio = s.antiguedadLaboralNegocio();
		e.numeroObligacionesActivas = s.numeroObligacionesActivas();
		e.puntajeHistorialPagos = s.puntajeHistorialPagos();
		e.alertasMora = s.alertasMora();
		e.moneda = s.moneda();
		e.estado = s.estado();
		e.fechaRegistro = s.fechaRegistro();
		return e;
	}

	public Solicitante toDomain() {
		return Solicitante.reconstituir(id, new NumeroDocumento(tipoDocumento, numeroDocumento), nombresRazonSocial,
				new Dinero(ingresosMensuales, moneda), new Dinero(gastosMensuales, moneda),
				new Dinero(obligacionesFinancieras, moneda), antiguedadLaboralNegocio, numeroObligacionesActivas,
				puntajeHistorialPagos, alertasMora, estado, fechaRegistro);
	}

	public Long getId() {
		return id;
	}
}
