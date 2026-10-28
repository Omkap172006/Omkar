package com.virtualca.controller;

import com.virtualca.dto.MetricsResponse;
import com.virtualca.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/api/metrics")
    public ResponseEntity<MetricsResponse> getMetrics(@RequestParam(value = "org_id", required = false) String orgIdStr) {
        UUID orgId = null;
        if (orgIdStr != null && !orgIdStr.isBlank()) {
            try {
                orgId = UUID.fromString(orgIdStr.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(metricsService.getMetrics(orgId));
    }
}

