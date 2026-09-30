package com.finanscore.motorscoring.domain.valueobject;

import com.finanscore.motorscoring.domain.enums.TipoDocumento;
import com.finanscore.motorscoring.domain.exception.DomainException;
import java.util.Objects;


public record NumeroDocumento(TipoDocumento tipo, String numero) {

	public NumeroDocumento {
		Objects.requireNonNull(tipo, "El tipo de documento es obligatorio.");
	
		if (numero == null || numero.isBlank()) {
			throw new DomainException("El número de documento es obligatorio.");
		}
		
		numero = numero.trim().toUpperCase();
		
		boolean formatoValido = switch (tipo) {case DNI -> numero.matches("\\d{8}");
											   case RUC -> numero.matches("\\d{11}");
											   case CE -> numero.matches("[A-Z0-9]{9,12}");
											   case PASAPORTE -> numero.matches("[A-Z0-9]{6,12}");
											 };
											 
		if (!formatoValido) {
			throw new DomainException("Formato de documento inválido para " + tipo + ".");
		}
		
	}
}
