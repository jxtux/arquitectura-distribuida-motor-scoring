package com.finanscore.motorscoring.infrastructure.persistence.entity;

import com.finanscore.motorscoring.domain.entity.SolicitudCredito;
import com.finanscore.motorscoring.domain.enums.*;
import com.finanscore.motorscoring.domain.valueobject.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "solicitudes_credito", uniqueConstraints = @UniqueConstraint(name = "uk_solicitud_identificador_externo", columnNames = "identificador_externo"))
public class SolicitudCreditoJpaEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_solicitud")
	private Long id;
	
	@Column(name = "id_solicitante", nullable = false)
	private Long idSolicitante;
	
	@Column(name = "id_producto", nullable = false)
	private Long idProducto;
	
	@Column(name = "monto_solicitado", nullable = false, precision = 18, scale = 2)
	private BigDecimal montoSolicitado;
	
	@Column(name = "plazo_solicitado", nullable = false)
	private int plazoSolicitado;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Moneda moneda;
	
	@Column(name = "finalidad_credito", nullable = false, length = 150)
	private String finalidadCredito;
	
	@Column(name = "canal_origen", nullable = false, length = 50)
	private String canalOrigen;
	
	@Column(name = "fecha_registro", nullable = false)
	private LocalDateTime fechaRegistro;
	
	@Column(name = "identificador_externo", nullable = false, length = 100)
	private String identificadorExterno;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoSolicitud estado;

	protected SolicitudCreditoJpaEntity() {
	}

	public static SolicitudCreditoJpaEntity fromDomain(SolicitudCredito s) {
		var e = new SolicitudCreditoJpaEntity();
		e.id = s.id();
		e.idSolicitante = s.idSolicitante();
		e.idProducto = s.idProducto();
		e.montoSolicitado = s.montoSolicitado().monto();
		e.plazoSolicitado = s.plazoSolicitado();
		e.moneda = s.montoSolicitado().moneda();
		e.finalidadCredito = s.finalidadCredito();
		e.canalOrigen = s.canalOrigen();
		e.fechaRegistro = s.fechaRegistro();
		e.identificadorExterno = s.identificadorExterno().valor();
		e.estado = s.estado();
		return e;
	}

	public SolicitudCredito toDomain() {
		return SolicitudCredito.reconstituir(id, idSolicitante, idProducto, new Dinero(montoSolicitado, moneda), plazoSolicitado, finalidadCredito, canalOrigen, fechaRegistro, new IdentificadorExterno(identificadorExterno), estado);
	}
}
