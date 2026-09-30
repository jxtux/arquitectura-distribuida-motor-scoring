package com.finanscore.payment.infrastructure;

import com.finanscore.payment.domain.ExchangeRatePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** Tasas de DEMO configurables en backend. No representan cotizaciones financieras en tiempo real. */
@Component
public class ConfiguredExchangeRateAdapter implements ExchangeRatePort {
    private final Map<String, BigDecimal> rates;

    public ConfiguredExchangeRateAdapter(
            @Value("${app.payment.rates.PEN:1.0000}") BigDecimal pen,
            @Value("${app.payment.rates.USD:0.2965}") BigDecimal usd,
            @Value("${app.payment.rates.EUR:0.2550}") BigDecimal eur,
            @Value("${app.payment.rates.CNY:2.1100}") BigDecimal cny) {
        rates = Map.of("PEN", pen, "USD", usd, "EUR", eur, "CNY", cny);
    }

    @Override
    public BigDecimal rateFromPen(String currency) {
        var rate = rates.get(currency);
        if (rate == null) throw new IllegalArgumentException("Moneda no soportada");
        return rate;
    }
}
