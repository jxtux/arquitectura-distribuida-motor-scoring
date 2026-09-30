package com.finanscore.motorscoring.domain.valueobject;

import com.finanscore.motorscoring.domain.enums.Moneda;
import java.math.BigDecimal;
import java.util.Objects;


public record CapacidadPago(Dinero disponible) {
	
	//El uso de record para implementar Value Objects se alinea perfectamente con Domain-Driven Design (DDD) 
	//porque este patrón no es solo una decisión técnica de programación; es una herramienta para proteger las 
	//reglas del negocio en el código.
	
	//RECORD
	//1. Una clase final con un atributo inmutable
	//2. El Constructor Estándar (Canonical Constructor)
	//3. Un método de lectura (Getter)
	//4. Métodos equals() y hashCode() automáticos
	//5. Método toString() automático
	
	//QUE FALTA PARA QUE CUMPLA EL PATRON VALUE OBJECT
	//1. Agregar Validación de Invariantes (Autovalidación)
	//2. Asegurar la Inmutabilidad de los Atributos Internos (Inmutabilidad Profunda)
	//3. Operaciones sin Efectos Secundarios (Side-Effect-Free Functions) Nunca deben modificar su propio estado
	//Cualquier método que transforme el valor debe retornar una nueva instancia del Value Object con el resultado calculado, 
	//manteniendo los objetos originales intactos
	
	
	// 2. CONSTRUCTOR COMPACTO PARA VALIDACIÓN:
	// Un Value Object jamás debe permitir un estado inválido (como un "disponible" nulo).
	public CapacidadPago {
		Objects.requireNonNull(disponible, "El dinero disponible no puede ser nulo");
	}
		
	public static CapacidadPago calcular(BigDecimal ingresos, BigDecimal gastos, BigDecimal obligaciones, Moneda moneda) {
		BigDecimal v = ingresos.subtract(gastos).subtract(obligaciones);
		
		if (v.signum() < 0)
			v = BigDecimal.ZERO;
		
		return new CapacidadPago(new Dinero(v, moneda));
	}
}
