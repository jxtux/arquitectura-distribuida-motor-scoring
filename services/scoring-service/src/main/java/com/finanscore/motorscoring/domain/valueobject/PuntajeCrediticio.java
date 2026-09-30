package com.finanscore.motorscoring.domain.valueobject;

import com.finanscore.motorscoring.domain.exception.PuntajeInvalidoException;

public record PuntajeCrediticio(int valor) {
	
	public PuntajeCrediticio {
		
		if (valor < 0 || valor > 1000)
			throw new PuntajeInvalidoException("El puntaje debe estar entre 0 y 1000.");
	}
}
