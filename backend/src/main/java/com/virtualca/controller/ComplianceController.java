package com.virtualca.controller;

import com.virtualca.entity.ComplianceEvent;
import com.virtualca.service.ComplianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping("/api/compliance")
    public ResponseEntity<Map<String, Object>> getCompliance(@RequestParam(value = "org_id", required = false) String orgIdStr) {
        UUID orgId = null;
        if (orgIdStr != null && !orgIdStr.isBlank()) {
            try {
                orgId = UUID.fromString(orgIdStr.trim());
            } catch (IllegalArgumentException ignored) {}
        }

        List<ComplianceEvent> events = complianceService.getComplianceEvents(orgId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("events", events);
        resp.put("total", events.size());
        return ResponseEntity.ok(resp);
    }
}

