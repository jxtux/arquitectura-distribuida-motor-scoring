package com.finanscore.query.application;

import com.finanscore.contracts.EventEnvelope;
import com.finanscore.query.infrastructure.*;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class ProjectionService {
    private final OperationReadModelRepository operations;
    private final UserRequestReadModelRepository users;
    private final ProcessedProjectionEventRepository processed;
    private final OperationQueryService queries;

    public ProjectionService(OperationReadModelRepository operations,UserRequestReadModelRepository users,ProcessedProjectionEventRepository processed,OperationQueryService queries){
        this.operations=operations;this.users=users;this.processed=processed;this.queries=queries;
    }

    @WithSpan("project-business-event")
    @Transactional
    public void project(EventEnvelope event){project(event,false,null);}

    @WithSpan("project-dlt-event")
    @Transactional
    public void projectDlt(EventEnvelope event,String topic){project(event,true,topic);}

    private void project(EventEnvelope event,boolean dlt,String topic){
        String projectionKey=event.eventId()+"|"+(dlt?"dlt:"+topic:"main");
        if(processed.existsById(projectionKey)) return;
        UUID requestId=requestId(event);
        if(requestId==null){mark(projectionKey,event.eventId());return;}

        Instant occurred=event.occurredAt()==null?Instant.now():event.occurredAt();
        OperationReadModelEntity op=operations.findById(requestId).orElseGet(()->newOperation(requestId,event,occurred));
        if(event.correlationId()!=null&&!event.correlationId().isBlank())op.correlationId=event.correlationId();
        applyBaseIdentity(op,event);

        if(dlt){
            String previousStatus=op.workflowStatus;
            if(!"COMPLETED".equals(op.workflowStatus)) op.workflowStatus="FAILED";
            op.failureStage=inferFailureStage(event.eventType(),topic,previousStatus);
            op.failureReason="DLT: "+topic;
        }else{
            applyBusinessEvent(op,event);
        }
        op.lastEventId=event.eventId();op.lastEventType=event.eventType();
        if(op.createdAt==null||occurred.isBefore(op.createdAt))op.createdAt=occurred;
        if(op.updatedAt==null||occurred.isAfter(op.updatedAt))op.updatedAt=occurred;
        operations.save(op);
        syncUserProjection(op);
        mark(projectionKey,event.eventId());
        queries.evict(op.requestId,op.userId);
    }

    private OperationReadModelEntity newOperation(UUID requestId,EventEnvelope e,Instant occurred){
        var x=new OperationReadModelEntity();x.requestId=requestId;x.correlationId=defaultCorrelation(e,requestId);x.workflowStatus="IN_PROGRESS";x.createdAt=occurred;x.updatedAt=occurred;return x;
    }

    private void applyBaseIdentity(OperationReadModelEntity x,EventEnvelope e){
        Map<String,Object> p=e.payload()==null?Map.of():e.payload();
        if(p.get("userId")!=null)x.userId=longValue(p.get("userId"));
        if(p.get("productCode")!=null)x.productCode=string(p.get("productCode"));
        if(p.get("termMonths")!=null)x.termMonths=intValue(p.get("termMonths"));
        if(p.get("purpose")!=null)x.purpose=string(p.get("purpose"));
        if(p.get("currency")!=null && Set.of("CreditRequestCreated","CreditEvaluationRequested","ScoringCalculated").contains(e.eventType()))x.currency=string(p.get("currency"));
        if(p.get("amount")!=null && Set.of("CreditRequestCreated","CreditEvaluationRequested","ScoringCalculated").contains(e.eventType()))x.amount=decimal(p.get("amount"));
    }

    private void applyBusinessEvent(OperationReadModelEntity x,EventEnvelope e){
        Map<String,Object> p=e.payload()==null?Map.of():e.payload();
        switch(e.eventType()){
            case "CreditRequestCreated" -> advance(x,"AWAITING_PAYMENT");
            case "PaymentValidated" -> {x.paymentStatus="APPROVED";x.failureReason=null;x.failureStage=null;advance(x,"PAYMENT_APPROVED");}
            case "PaymentRejected" -> {x.paymentStatus="REJECTED";x.failureStage="PAYMENT";x.failureReason=stringOrNull(p.get("reason"));advance(x,"PAYMENT_REJECTED");}
            case "CreditEvaluationRequested" -> advance(x,"SCORING_IN_PROGRESS");
            case "ScoringCalculated" -> {x.scoreValue=intValue(p.get("score"));x.recommendation=stringOrNull(p.get("recommendation"));x.modelVersion=stringOrNull(p.get("modelVersion"));advance(x,"REPORT_GENERATING");}
            case "ScoringFailed" -> fail(x,p,"SCORING");
            case "ReportGenerated" -> {x.reportId=uuidOrNull(p.get("reportId"));x.reportAvailable=x.reportId!=null;advance(x,"NOTIFICATION_PENDING");}
            case "ReportGenerationFailed" -> fail(x,p,"REPORT");
            case "NotificationSent" -> {x.notificationStatus="SENT";advance(x,"COMPLETED");}
            case "NotificationFailed" -> {x.notificationStatus="FAILED";fail(x,p,"EMAIL");}
            default -> { /* IAM y eventos no pertenecientes al workflow se ignoran */ }
        }
    }

    private void syncUserProjection(OperationReadModelEntity x){
        if(x.userId==null)return;
        var u=users.findById(x.requestId).orElseGet(UserRequestReadModelEntity::new);
        u.requestId=x.requestId;u.userId=x.userId;u.correlationId=x.correlationId;u.productCode=x.productCode;u.amount=x.amount;u.currency=x.currency;u.termMonths=x.termMonths;u.purpose=x.purpose;u.workflowStatus=x.workflowStatus;u.scoreValue=x.scoreValue;u.recommendation=x.recommendation;u.modelVersion=x.modelVersion;u.reportId=x.reportId;u.reportAvailable=x.reportAvailable;u.failureReason=x.failureReason;u.failureStage=x.failureStage;u.createdAt=x.createdAt;u.updatedAt=x.updatedAt;
        users.save(u);
    }

    private void fail(OperationReadModelEntity x,Map<String,Object> p,String stage){
        if(!"COMPLETED".equals(x.workflowStatus))x.workflowStatus="FAILED";
        x.failureStage=stage;
        Object reason=p.get("reason");if(reason==null)reason=p.get("message");if(reason!=null)x.failureReason=String.valueOf(reason);
    }
    private String inferFailureStage(String eventType,String topic,String previousStatus){
        // Para un DLT importa la etapa que estaba ejecutándose cuando agotó sus
        // retries. El nombre del evento original puede pertenecer a la etapa
        // anterior; por ejemplo report-service consume ScoringCalculated, por
        // lo que scoring.calculated.v1.DLT NO significa que haya fallado Scoring.
        // Si el read model ya estaba en REPORT_GENERATING, el fallo corresponde
        // a REPORT. Esto permite que Angular marque la fila correcta con X roja.
        String byWorkflow=switch(previousStatus==null?"":previousStatus){
            case "NOTIFICATION_PENDING" -> "EMAIL";
            case "REPORT_GENERATING" -> "REPORT";
            case "SCORING_IN_PROGRESS" -> "SCORING";
            case "PAYMENT_APPROVED" -> "SUBMITTED";
            case "AWAITING_PAYMENT","PAYMENT_REJECTED" -> "PAYMENT";
            default -> null;
        };
        if(byWorkflow!=null)return byWorkflow;

        String source=(eventType==null?"":eventType)+" "+(topic==null?"":topic);
        if(source.contains("Notification" )||source.contains("notification."))return "EMAIL";
        if(source.contains("Report" )||source.contains("report."))return "REPORT";
        if(source.contains("Scoring" )||source.contains("scoring."))return "SCORING";
        if(source.contains("CreditEvaluationRequested")||source.contains("credit.evaluation"))return "SCORING";
        if(source.contains("Payment")||source.contains("payment."))return "PAYMENT";
        return "SCORING";
    }
    private void advance(OperationReadModelEntity x,String candidate){
        if(Set.of("COMPLETED","FAILED").contains(x.workflowStatus))return;
        if(rank(candidate)>=rank(x.workflowStatus))x.workflowStatus=candidate;
    }
    private int rank(String status){return switch(status==null?"":status){case "IN_PROGRESS"->0;case "AWAITING_PAYMENT"->10;case "PAYMENT_REJECTED"->15;case "PAYMENT_APPROVED"->20;case "SCORING_IN_PROGRESS"->30;case "REPORT_GENERATING"->40;case "NOTIFICATION_PENDING"->50;case "COMPLETED"->60;case "FAILED"->100;default->0;};}
    private void mark(String projectionKey,String eventId){var p=new ProcessedProjectionEventEntity();p.projectionKey=projectionKey;p.eventId=eventId;p.processedAt=Instant.now();processed.save(p);}
    private UUID requestId(EventEnvelope e){try{Object v=e.payload()==null?null:e.payload().get("requestId");if(v==null)v=e.aggregateId();return v==null?null:UUID.fromString(String.valueOf(v));}catch(Exception ex){return null;}}
    private static String defaultCorrelation(EventEnvelope e,UUID requestId){return e.correlationId()==null||e.correlationId().isBlank()?requestId.toString():e.correlationId();}
    private static String string(Object x){return String.valueOf(x);}private static String stringOrNull(Object x){return x==null?null:String.valueOf(x);}private static Long longValue(Object x){return x==null?null:Long.valueOf(String.valueOf(x));}private static Integer intValue(Object x){return x==null?null:Integer.valueOf(String.valueOf(x));}private static BigDecimal decimal(Object x){return x==null?null:new BigDecimal(String.valueOf(x));}private static UUID uuidOrNull(Object x){try{return x==null?null:UUID.fromString(String.valueOf(x));}catch(Exception ex){return null;}}
}
