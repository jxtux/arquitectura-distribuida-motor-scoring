package com.finanscore.query.presentation;

import com.finanscore.query.application.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/query")
public class QueryController {
    private final OperationQueryService service;
    public QueryController(OperationQueryService service){this.service=service;}

    @GetMapping("/admin/operations") @PreAuthorize("hasRole('ADMIN')")
    public List<QueryDtos.OperationView> operations(@RequestParam(required=false) String query,@RequestParam(required=false) String status,@RequestParam(defaultValue="100") int limit){return service.adminOperations(query,status,limit);}

    @GetMapping("/admin/operations/{requestId}") @PreAuthorize("hasRole('ADMIN')")
    public QueryDtos.OperationView operation(@PathVariable UUID requestId){return service.adminOperation(requestId);}

    @GetMapping("/my-requests") @PreAuthorize("hasAuthority('SCORE_READ')")
    public List<QueryDtos.UserRequestView> my(@AuthenticationPrincipal Jwt jwt){return service.myRequests(Long.valueOf(jwt.getSubject()));}

    @GetMapping("/my-requests/{requestId}") @PreAuthorize("hasAuthority('SCORE_READ')")
    public QueryDtos.UserRequestView myOne(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID requestId){return service.myRequest(requestId,Long.valueOf(jwt.getSubject()));}
}
