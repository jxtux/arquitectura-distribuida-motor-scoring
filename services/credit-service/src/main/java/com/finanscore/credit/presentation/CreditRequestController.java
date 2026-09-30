package com.finanscore.credit.presentation;

import com.finanscore.credit.application.CreditRequestService;
import com.finanscore.credit.infrastructure.CreditRequestEntity;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;import java.util.UUID;

/**
 * CQRS command side. Desde V3.3 las consultas de solicitudes no se exponen
 * desde Credit Service: Admin y usuarios consultan exclusivamente Query Service.
 */
@RestController @RequestMapping("/api/v1/credit-requests")
public class CreditRequestController {
    private final CreditRequestService service;
    public CreditRequestController(CreditRequestService service){this.service=service;}

    @PostMapping @PreAuthorize("hasAuthority('SCORE_CREATE')")
    public View create(@AuthenticationPrincipal Jwt jwt,@RequestHeader(value="X-Correlation-Id",required=false) String correlationId,@Valid @RequestBody CreateRequest r){
        return View.of(service.create(Long.valueOf(jwt.getSubject()),correlationId,r.productCode(),r.amount(),r.termMonths(),r.purpose(),r.consent()));
    }

    public record CreateRequest(@NotBlank String productCode,@NotNull @DecimalMin("0.01") BigDecimal amount,@Min(1) int termMonths,@NotBlank @Size(max=150) String purpose,@AssertTrue boolean consent){}
    public record View(UUID id,String correlationId,String productCode,BigDecimal amount,String currency,int termMonths,String purpose,String status,Integer score,String recommendation,String modelVersion,UUID reportId,java.time.Instant createdAt){
        static View of(CreditRequestEntity x){return new View(x.id,x.correlationId,x.productCode,x.amount,x.currency,x.termMonths,x.purpose,x.status.name(),x.scoreValue,x.recommendation,x.modelVersion,x.reportId,x.createdAt);}
    }
}
