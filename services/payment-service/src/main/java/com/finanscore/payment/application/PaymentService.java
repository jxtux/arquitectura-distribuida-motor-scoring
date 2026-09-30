package com.finanscore.payment.application;

import com.finanscore.payment.domain.*;
import com.finanscore.payment.infrastructure.*;
import com.finanscore.support.observability.ObservabilityContext;
import com.finanscore.support.outbox.OutboxService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PaymentService {
    private static final Logger log=LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository repo; private final PaymentProcessorPort processor; private final ExchangeRatePort rates; private final OutboxService outbox; private final BigDecimal basePricePen;
    public PaymentService(PaymentRepository r,PaymentProcessorPort p,ExchangeRatePort rates,OutboxService o,@Value("${app.payment.base-price-pen:20.00}") BigDecimal basePricePen){repo=r;processor=p;this.rates=rates;outbox=o;this.basePricePen=basePricePen;}
    public Quote quote(String service,String currency){if(!"EVALUACION_CREDITICIA".equals(service))throw new IllegalArgumentException("Servicio no soportado");var rate=rates.rateFromPen(currency);return new Quote(service,basePricePen,"PEN",currency,rate,basePricePen.multiply(rate).setScale(2,RoundingMode.HALF_UP));}

    @WithSpan("process-payment")
    @Transactional
    public Result pay(Long userId,String idem,UUID requestId,String service,String currency,String brand,String number,String expiry,String cvv,String correlation){
        try(var ignored=ObservabilityContext.bind(requestId.toString(),correlation,null)){
            if(idem==null||idem.isBlank())throw new IllegalArgumentException("Idempotency-Key es obligatorio");
            var old=repo.findByUserIdAndIdempotencyKey(userId,idem);if(old.isPresent())return Result.of(old.get());
            validateExpiry(expiry);if(!Set.of("VISA","MASTERCARD","AMEX","DINERS").contains(brand))throw new IllegalArgumentException("Marca de tarjeta no soportada");
            var q=quote(service,currency);var d=processor.process(new PaymentProcessorPort.CardData(brand,number,expiry,cvv));
            var x=new PaymentEntity();x.id=UUID.randomUUID();x.requestId=requestId;x.userId=userId;x.serviceCode=service;x.baseAmount=q.baseAmount();x.baseCurrency="PEN";x.paymentAmount=q.amount();x.paymentCurrency=currency;x.exchangeRate=q.exchangeRate();
            String digits=number.replaceAll("\\D","");x.cardBrand=brand;x.cardLast4=digits.length()>=4?digits.substring(digits.length()-4):digits;x.status=d.approved()?"APPROVED":"REJECTED";x.idempotencyKey=idem;x.createdAt=x.processedAt=Instant.now();repo.save(x);
            Map<String,Object> p=new LinkedHashMap<>();p.put("requestId",requestId.toString());p.put("userId",userId);p.put("paymentId",x.id.toString());p.put("amount",x.paymentAmount);p.put("currency",currency);
            if(d.approved())outbox.append("PaymentValidated",requestId.toString(),correlation,null,null,"payment-service",p);else{p.put("reason",d.message());outbox.append("PaymentRejected",requestId.toString(),correlation,null,null,"payment-service",p);}
            log.info("Payment processed status={} paymentId={} amount={} currency={}",x.status,x.id,x.paymentAmount,x.paymentCurrency);
            return Result.of(x);
        }
    }
    private void validateExpiry(String expiry){try{var ym=YearMonth.parse(expiry,DateTimeFormatter.ofPattern("MM/yy"));if(ym.isBefore(YearMonth.now()))throw new IllegalArgumentException("La tarjeta está expirada");}catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("Fecha MM/AA inválida");}}
    public record Quote(String serviceCode,BigDecimal baseAmount,String baseCurrency,String currency,BigDecimal exchangeRate,BigDecimal amount){}
    public record Result(UUID paymentId,String status,BigDecimal amount,String currency,String cardBrand,String last4){static Result of(PaymentEntity x){return new Result(x.id,x.status,x.paymentAmount,x.paymentCurrency,x.cardBrand,x.cardLast4);}}
}
