package com.finanscore.motorscoring.domain.fixture;

import com.finanscore.motorscoring.domain.entity.FactorScoring;
import com.finanscore.motorscoring.domain.entity.ReglaEvaluacion;
import com.finanscore.motorscoring.domain.entity.Solicitante;
import com.finanscore.motorscoring.domain.entity.SolicitudCredito;
import com.finanscore.motorscoring.domain.entity.VersionModelo;
import com.finanscore.motorscoring.domain.enums.EstadoFactor;
import com.finanscore.motorscoring.domain.enums.EstadoRegla;
import com.finanscore.motorscoring.domain.enums.EstadoVersionModelo;
import com.finanscore.motorscoring.domain.enums.Moneda;
import com.finanscore.motorscoring.domain.enums.ResultadoScoring;
import com.finanscore.motorscoring.domain.enums.TipoDocumento;
import com.finanscore.motorscoring.domain.valueobject.Dinero;
import com.finanscore.motorscoring.domain.valueobject.IdentificadorExterno;
import com.finanscore.motorscoring.domain.valueobject.NumeroDocumento;
import com.finanscore.motorscoring.domain.valueobject.Porcentaje;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ScoringFixture {
	private ScoringFixture() {
	}

	public static Solicitante solicitanteValido(int alertasMora) {
		return Solicitante.reconstituir(10L, new NumeroDocumento(TipoDocumento.DNI, "12345678"), "Persona de prueba",
				new Dinero(new BigDecimal("5500"), Moneda.PEN), new Dinero(new BigDecimal("1800"), Moneda.PEN),
				new Dinero(new BigDecimal("700"), Moneda.PEN), 36, 2, 85, alertasMora,
				com.finanscore.motorscoring.domain.enums.EstadoRegistro.ACTIVO, LocalDateTime.of(2026, 1, 1, 8, 0));
	}

	public static SolicitudCredito solicitudValida() {
		
		return SolicitudCredito.reconstituir(20L, 10L, 1L, Dinero.positivo(new BigDecimal("15000"), Moneda.PEN), 24,
				"Consumo", "API", LocalDateTime.of(2026, 1, 1, 8, 0), new IdentificadorExterno("EXT-TEST-001"),
				com.finanscore.motorscoring.domain.enums.EstadoSolicitud.REGISTRADA);
	}

	public static VersionModelo versionModeloValida() {
		List<FactorScoring> factores = new ArrayList<>();
		factores.add(factor(1L, "HISTORIAL_PAGOS", "25", rangosComunes(1L)));
		factores.add(factor(2L, "RELACION_DEUDA_INGRESO", "20", rangosComunes(10L)));
		factores.add(factor(3L, "CAPACIDAD_PAGO", "20", rangosComunes(20L)));
		factores.add(factor(4L, "ESTABILIDAD_INGRESOS", "15", rangosAmplios(30L)));
		factores.add(factor(5L, "ANTIGUEDAD_LABORAL", "10", rangosAmplios(40L)));
		factores.add(factor(6L, "OBLIGACIONES_ACTIVAS", "5", rangosAmplios(50L)));
		factores.add(factor(7L, "MONTO_CAPACIDAD", "5", rangosComunes(60L)));
		factores.add(new FactorScoring(8L, "ALERTAS_MORA", "Alertas de mora", "Regla excluyente",
				new Porcentaje(BigDecimal.ZERO), EstadoFactor.ACTIVO,
				List.of(new ReglaEvaluacion(70L, "SIN_ALERTA", "Sin alertas", BigDecimal.ZERO, BigDecimal.ZERO, 100,
						false, null, EstadoRegla.ACTIVA),
						new ReglaEvaluacion(71L, "CON_ALERTA", "Mora vigente", BigDecimal.ONE, new BigDecimal("9999"),
								0, true, ResultadoScoring.RECHAZADA, EstadoRegla.ACTIVA))));
		return new VersionModelo(1L, "1.0.0", LocalDate.of(2025, 1, 1), null, EstadoVersionModelo.ACTIVA, factores);
	}

	private static FactorScoring factor(Long id, String codigo, String peso, List<ReglaEvaluacion> reglas) {
		return new FactorScoring(id, codigo, codigo, codigo, new Porcentaje(new BigDecimal(peso)), EstadoFactor.ACTIVO,
				reglas);
	}

	private static List<ReglaEvaluacion> rangosComunes(long baseId) {
		return List.of(
				new ReglaEvaluacion(baseId, "BAJO", "Bajo", BigDecimal.ZERO, new BigDecimal("24.9999"), 25, false, null,
						EstadoRegla.ACTIVA),
				new ReglaEvaluacion(baseId + 1, "MEDIO", "Medio", new BigDecimal("25"), new BigDecimal("59.9999"), 60,
						false, null, EstadoRegla.ACTIVA),
				new ReglaEvaluacion(baseId + 2, "ALTO", "Alto", new BigDecimal("60"), new BigDecimal("100"), 100, false,
						null, EstadoRegla.ACTIVA),
				new ReglaEvaluacion(baseId + 3, "FUERA", "Fuera", new BigDecimal("100.0001"), new BigDecimal("9999"),
						20, false, null, EstadoRegla.ACTIVA));
	}

	private static List<ReglaEvaluacion> rangosAmplios(long baseId) {
		return List.of(
				new ReglaEvaluacion(baseId, "BAJO", "Bajo", BigDecimal.ZERO, new BigDecimal("11.9999"), 25, false, null,
						EstadoRegla.ACTIVA),
				new ReglaEvaluacion(baseId + 1, "MEDIO", "Medio", new BigDecimal("12"), new BigDecimal("35.9999"), 70,
						false, null, EstadoRegla.ACTIVA),
				new ReglaEvaluacion(baseId + 2, "ALTO", "Alto", new BigDecimal("36"), new BigDecimal("9999"), 100,
						false, null, EstadoRegla.ACTIVA));
	}
}
