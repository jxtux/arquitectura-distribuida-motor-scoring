package com.finanscore.query;

import com.finanscore.contracts.EventEnvelope;
import com.finanscore.query.application.*;
import com.finanscore.query.infrastructure.*;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.extension.ExtendWith;import org.mockito.*;import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectionServiceTest {
    @Mock OperationReadModelRepository operations;@Mock UserRequestReadModelRepository users;@Mock ProcessedProjectionEventRepository processed;@Mock OperationQueryService queries;
    @InjectMocks ProjectionService service;

    @Test void createsBothReadModelsFromCreditRequestCreated(){
        UUID request=UUID.randomUUID();String eventId=UUID.randomUUID().toString();
        when(processed.existsById(anyString())).thenReturn(false);when(operations.findById(request)).thenReturn(Optional.empty());when(users.findById(request)).thenReturn(Optional.empty());
        EventEnvelope e=new EventEnvelope(eventId,"CreditRequestCreated",1,Instant.now(),UUID.randomUUID().toString(),null,null,"credit-service",request.toString(),Map.of("requestId",request.toString(),"userId",25L,"productCode","PRESTAMO_PERSONAL","amount","15000.00","currency","PEN","termMonths",24,"purpose","Estudios"));
        service.project(e);
        ArgumentCaptor<OperationReadModelEntity> op=ArgumentCaptor.forClass(OperationReadModelEntity.class);verify(operations).save(op.capture());
        assertEquals("AWAITING_PAYMENT",op.getValue().workflowStatus);assertEquals(25L,op.getValue().userId);assertEquals("PRESTAMO_PERSONAL",op.getValue().productCode);
        ArgumentCaptor<UserRequestReadModelEntity> user=ArgumentCaptor.forClass(UserRequestReadModelEntity.class);verify(users).save(user.capture());assertEquals(request,user.getValue().requestId);assertEquals("AWAITING_PAYMENT",user.getValue().workflowStatus);
        verify(queries).evict(request,25L);
    }

    @Test void duplicateProjectionEventIsIgnored(){
        EventEnvelope e=new EventEnvelope("evt-1","CreditRequestCreated",1,Instant.now(),"corr",null,null,"credit-service",UUID.randomUUID().toString(),Map.of());
        when(processed.existsById("evt-1|main")).thenReturn(true);service.project(e);verifyNoInteractions(operations,users,queries);
    }

    @Test void oldEventDoesNotDowngradeCompletedOperation(){
        UUID request=UUID.randomUUID();var existing=new OperationReadModelEntity();existing.requestId=request;existing.correlationId="corr";existing.workflowStatus="COMPLETED";existing.createdAt=Instant.now().minusSeconds(60);existing.updatedAt=Instant.now();
        when(processed.existsById(anyString())).thenReturn(false);when(operations.findById(request)).thenReturn(Optional.of(existing));
        EventEnvelope e=new EventEnvelope("evt-old","CreditRequestCreated",1,Instant.now().minusSeconds(120),"corr",null,null,"credit-service",request.toString(),Map.of("requestId",request.toString()));
        service.project(e);assertEquals("COMPLETED",existing.workflowStatus);
    }
}
