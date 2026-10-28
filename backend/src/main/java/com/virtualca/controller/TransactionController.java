package com.virtualca.controller;

import com.virtualca.dto.MetricsResponse;
import com.virtualca.dto.TransactionRequest;
import com.virtualca.entity.Voucher;
import com.virtualca.service.MetricsService;
import com.virtualca.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class TransactionController {

    private final TransactionService transactionService;
    private final MetricsService metricsService;

    public TransactionController(TransactionService transactionService, MetricsService metricsService) {
        this.transactionService = transactionService;
        this.metricsService = metricsService;
    }

    @GetMapping("/api/transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(@RequestParam(value = "org_id", required = false) String orgIdStr) {
        UUID orgId = null;
        if (orgIdStr != null && !orgIdStr.isBlank()) {
            try {
                orgId = UUID.fromString(orgIdStr.trim());
            } catch (IllegalArgumentException ignored) {}
        }

        List<Voucher> txns = transactionService.getTransactions(orgId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("transactions", txns);
        resp.put("total", txns.size());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/api/transactions")
    public ResponseEntity<Map<String, Object>> createTransaction(@RequestBody TransactionRequest req) {
        Map<String, Object> result = transactionService.createTransaction(req);

        UUID orgId = null;
        if (req.getOrgId() != null && !req.getOrgId().isBlank()) {
            try {
                orgId = UUID.fromString(req.getOrgId().trim());
            } catch (IllegalArgumentException ignored) {}
        }
        MetricsResponse metrics = metricsService.getMetrics(orgId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "success");
        resp.put("transaction", result);
        resp.put("metrics", metrics);
        return ResponseEntity.ok(resp);
    }
}

