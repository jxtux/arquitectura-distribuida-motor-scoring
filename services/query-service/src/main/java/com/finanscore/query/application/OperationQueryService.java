package com.finanscore.query.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.finanscore.query.cache.RedisQueryCache;
import com.finanscore.query.infrastructure.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class OperationQueryService {
    private final OperationReadModelRepository operations;
    private final UserRequestReadModelRepository userRequests;
    private final RedisQueryCache cache;
    private final Timer postgresTimer;

    public OperationQueryService(OperationReadModelRepository operations,UserRequestReadModelRepository userRequests,RedisQueryCache cache,MeterRegistry meter){
        this.operations=operations;this.userRequests=userRequests;this.cache=cache;
        this.postgresTimer=Timer.builder("query_postgres_latency").description("Latency of PostgreSQL read-model queries").publishPercentileHistogram().register(meter);
    }

    @WithSpan("query-admin-operations")
    public List<QueryDtos.OperationView> adminOperations(String query,String status,int limit){
        int size=Math.max(1,Math.min(limit,500));
        return postgresTimer.record(()->operations.search(normalize(query),normalizeStatus(status),PageRequest.of(0,size)).stream().map(QueryDtos.OperationView::of).toList());
    }

    @WithSpan("query-admin-operation")
    public QueryDtos.OperationView adminOperation(UUID requestId){
        String key="operation:"+requestId;
        var cached=cache.get(key,QueryDtos.OperationView.class);if(cached.isPresent())return cached.get();
        var view=postgresTimer.record(()->QueryDtos.OperationView.of(operations.findById(requestId).orElseThrow(()->new NoSuchElementException("Operación no encontrada: "+requestId))));
        cache.put(key,view);return view;
    }

    @WithSpan("query-my-requests")
    public List<QueryDtos.UserRequestView> myRequests(Long userId){
        String key="my-requests:"+userId;
        var cached=cache.get(key,new TypeReference<List<QueryDtos.UserRequestView>>(){});if(cached.isPresent())return cached.get();
        var views=postgresTimer.record(()->userRequests.findByUserIdOrderByCreatedAtDesc(userId).stream().map(QueryDtos.UserRequestView::of).toList());
        cache.put(key,views);return views;
    }

    @WithSpan("query-my-request")
    public QueryDtos.UserRequestView myRequest(UUID requestId,Long userId){
        String key="my-request:"+userId+":"+requestId;
        var cached=cache.get(key,QueryDtos.UserRequestView.class);if(cached.isPresent())return cached.get();
        var view=postgresTimer.record(()->QueryDtos.UserRequestView.of(userRequests.findByRequestIdAndUserId(requestId,userId).orElseThrow(()->new NoSuchElementException("Solicitud no encontrada: "+requestId))));
        cache.put(key,view);return view;
    }

    public void evict(UUID requestId,Long userId){
        cache.evict("operation:"+requestId);
        if(userId!=null){cache.evict("my-requests:"+userId);cache.evict("my-request:"+userId+":"+requestId);}
    }
    private static String normalize(String x){return x==null?null:x.trim();}
    private static String normalizeStatus(String x){return x==null||x.isBlank()?null:x.trim().toUpperCase(Locale.ROOT);}
}
