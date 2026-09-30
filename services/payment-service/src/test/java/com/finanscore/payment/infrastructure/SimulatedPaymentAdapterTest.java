package com.finanscore.payment.infrastructure;

import com.finanscore.payment.domain.PaymentProcessorPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedPaymentAdapterTest {
    private final SimulatedPaymentAdapter adapter = new SimulatedPaymentAdapter();

    @Test
    void tarjetaFicticiaValidaSeAprueba() {
        var result = adapter.process(new PaymentProcessorPort.CardData(
                "VISA", "4111111111111111", "12/30", "123"));
        assertTrue(result.approved());
        assertEquals("APPROVED", result.code());
    }

    @Test
    void tarjetaSoloCerosSeRechaza() {
        var result = adapter.process(new PaymentProcessorPort.CardData(
                "VISA", "0000000000000000", "12/30", "123"));
        assertFalse(result.approved());
        assertEquals("Pago no válido", result.message());
    }

    @Test
    void cvvInvalidoSeRechaza() {
        var result = adapter.process(new PaymentProcessorPort.CardData(
                "MASTERCARD", "5555555555554444", "12/30", "12"));
        assertFalse(result.approved());
        assertEquals("INVALID_CVV", result.code());
    }
}
