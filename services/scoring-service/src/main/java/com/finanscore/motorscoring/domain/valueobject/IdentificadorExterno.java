package com.finanscore.motorscoring.domain.valueobject;

import com.finanscore.motorscoring.domain.exception.DomainException;

public record IdentificadorExterno(String valor) {
	
	public IdentificadorExterno {
		
		if (valor == null || valor.isBlank())
			throw new DomainException("El identificador externo es obligatorio.");
		
		valor = valor.trim();
		
		if (valor.length() > 100)
			throw new DomainException("El identificador externo no puede superar 100 caracteres.");
	}
}
