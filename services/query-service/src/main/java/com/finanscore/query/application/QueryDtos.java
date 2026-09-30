package com.finanscore.query.application;

import com.finanscore.query.infrastructure.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class QueryDtos {
    private QueryDtos(){}
    public record OperationView(UUID requestId,String correlationId,Long userId,String productCode,BigDecimal amount,String currency,Integer termMonths,String purpose,String paymentStatus,String status,Integer score,String recommendation,String modelVersion,UUID reportId,boolean reportAvailable,String notificationStatus,String failureReason,String failureStage,String lastEventType,Instant createdAt,Instant updatedAt){
        public static OperationView of(OperationReadModelEntity x){return new OperationView(x.requestId,x.correlationId,x.userId,x.productCode,x.amount,x.currency,x.termMonths,x.purpose,x.paymentStatus,x.workflowStatus,x.scoreValue,x.recommendation,x.modelVersion,x.reportId,x.reportAvailable,x.notificationStatus,x.failureReason,x.failureStage,x.lastEventType,x.createdAt,x.updatedAt);}
    }
    public record UserRequestView(UUID id,String correlationId,String productCode,BigDecimal amount,String currency,Integer termMonths,String purpose,String status,Integer score,String recommendation,String modelVersion,UUID reportId,boolean reportAvailable,String failureReason,String failureStage,Instant createdAt,Instant updatedAt){
        public static UserRequestView of(UserRequestReadModelEntity x){return new UserRequestView(x.requestId,x.correlationId,x.productCode,x.amount,x.currency,x.termMonths,x.purpose,x.workflowStatus,x.scoreValue,x.recommendation,x.modelVersion,x.reportId,x.reportAvailable,x.failureReason,x.failureStage,x.createdAt,x.updatedAt);}
    }
}
