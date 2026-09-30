package com.finanscore.motorscoring.domain.valueobject;

import com.finanscore.motorscoring.domain.enums.Moneda;
import com.finanscore.motorscoring.domain.enums.TipoDocumento;
import com.finanscore.motorscoring.domain.exception.DomainException;
import com.finanscore.motorscoring.domain.exception.PuntajeInvalidoException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueObjectsTest {
	
	@Test
	void debeRechazarDniConFormatoInvalido() {
		assertThrows(DomainException.class, () -> new NumeroDocumento(TipoDocumento.DNI, "123"));
	}

	@Test
	void debeRechazarDineroNegativo() {
		assertThrows(DomainException.class, () -> new Dinero(new BigDecimal("-1"), Moneda.PEN));
	}

	@Test
	void debeMantenerElScoreEntreCeroYMil() {
		assertEquals(750, new PuntajeCrediticio(750).valor());
		assertThrows(PuntajeInvalidoException.class, () -> new PuntajeCrediticio(1001));
	}
}
