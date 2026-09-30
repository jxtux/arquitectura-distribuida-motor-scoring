package com.finanscore.motorscoring.domain.entity;

import com.finanscore.motorscoring.domain.enums.EstadoFactor;
import com.finanscore.motorscoring.domain.exception.SolicitudNoEvaluableException;
import com.finanscore.motorscoring.domain.valueobject.Porcentaje;
import java.math.BigDecimal;
import java.util.*;

public final class FactorScoring {
	
	private final Long id;
	
	private final String codigo, nombre, descripcion;
	
	private final Porcentaje peso;
	
	private final EstadoFactor estado;
	
	private final List<ReglaEvaluacion> reglas;

	public FactorScoring(Long id, String codigo, String nombre, String descripcion, Porcentaje peso, EstadoFactor estado, List<ReglaEvaluacion> reglas) {
		this.id = Objects.requireNonNull(id);
		this.codigo = Objects.requireNonNull(codigo);
		this.nombre = Objects.requireNonNull(nombre);
		this.descripcion = descripcion == null ? "" : descripcion;
		this.peso = Objects.requireNonNull(peso);
		this.estado = Objects.requireNonNull(estado);
		this.reglas = List.copyOf(reglas).stream().sorted(Comparator.comparing(ReglaEvaluacion::valorMinimo)).toList();
	}

	public ReglaEvaluacion evaluar(BigDecimal valor) {
		
		if (estado != EstadoFactor.ACTIVO)
			throw new SolicitudNoEvaluableException("Factor inactivo: " + codigo);
		
		return reglas.stream().filter(r -> r.aplica(valor)).findFirst().orElseThrow(
				() -> new SolicitudNoEvaluableException("No existe regla para " + codigo + " y valor " + valor));
	}

	public boolean validarPeso() {
		return peso.valor().signum() >= 0;
	}

	public Long id() {
		return id;
	}

	public String codigo() {
		return codigo;
	}

	public String nombre() {
		return nombre;
	}

	public String descripcion() {
		return descripcion;
	}

	public Porcentaje peso() {
		return peso;
	}

	public EstadoFactor estado() {
		return estado;
	}

	public List<ReglaEvaluacion> reglas() {
		return reglas;
	}
}
