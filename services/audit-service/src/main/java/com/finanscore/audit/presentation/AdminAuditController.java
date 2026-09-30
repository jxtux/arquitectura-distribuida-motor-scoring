package com.finanscore.audit.presentation;

import com.finanscore.audit.AdminAuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {
    private final AdminAuditService service;
    public AdminAuditController(AdminAuditService service){this.service=service;}

    /** V3.3: Audit Service expone historia, no el estado operativo CQRS. */
    @GetMapping("/audit")
    public List<AdminAuditService.AuditEventView> audit(@RequestParam(required=false) String requestId,@RequestParam(required=false) String correlationId,@RequestParam(required=false) String eventType,@RequestParam(required=false) String source,@RequestParam(defaultValue="200") int limit){return service.audit(requestId,correlationId,eventType,source,limit);}

    @GetMapping("/dlt")
    public List<AdminAuditService.DltView> dlt(@RequestParam(required=false) String requestId,@RequestParam(required=false) String correlationId,@RequestParam(required=false) String sourceTopic,@RequestParam(defaultValue="200") int limit){return service.dlt(requestId,correlationId,sourceTopic,limit);}
}
