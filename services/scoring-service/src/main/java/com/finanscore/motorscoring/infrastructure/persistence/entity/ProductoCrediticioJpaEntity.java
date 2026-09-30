package com.finanscore.motorscoring.infrastructure.persistence.entity;

import com.finanscore.motorscoring.domain.entity.ProductoCrediticio;
import com.finanscore.motorscoring.domain.enums.*;
import com.finanscore.motorscoring.domain.valueobject.Dinero;
import jakarta.persistence.*;
import java.math.BigDecimal;


@Entity
@Table(name = "productos_crediticios")
public class ProductoCrediticioJpaEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_producto")
	private Long id;
	
	@Column(nullable = false, unique = true, length = 30)
	private String codigo;
	
	@Column(nullable = false, length = 150)
	private String nombre;
	
	@Column(name = "monto_minimo", nullable = false, precision = 18, scale = 2)
	private BigDecimal montoMinimo;
	
	@Column(name = "monto_maximo", nullable = false, precision = 18, scale = 2)
	private BigDecimal montoMaximo;
	
	@Column(name = "plazo_minimo", nullable = false)
	private int plazoMinimo;
	
	@Column(name = "plazo_maximo", nullable = false)
	private int plazoMaximo;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Moneda moneda;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoProducto estado;
	
	@Column(name = "id_modelo_scoring", nullable = false)
	private Long idModeloScoring;

	protected ProductoCrediticioJpaEntity() {
	}

	public ProductoCrediticio toDomain() {
		return new ProductoCrediticio(id, codigo, nombre, new Dinero(montoMinimo, moneda), new Dinero(montoMaximo, moneda), plazoMinimo, plazoMaximo, estado, idModeloScoring);
	}
}
