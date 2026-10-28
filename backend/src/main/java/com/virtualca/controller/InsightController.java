package com.virtualca.controller;

import com.virtualca.entity.AiFinancialInsight;
import com.virtualca.service.InsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping("/api/insights")
    public ResponseEntity<Map<String, Object>> getInsights(@RequestParam(value = "org_id", required = false) String orgIdStr) {
        UUID orgId = null;
        if (orgIdStr != null && !orgIdStr.isBlank()) {
            try {
                orgId = UUID.fromString(orgIdStr.trim());
            } catch (IllegalArgumentException ignored) {}
        }

        List<AiFinancialInsight> insights = insightService.getAiInsights(orgId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("insights", insights);
        resp.put("total", insights.size());
        return ResponseEntity.ok(resp);
    }
}

