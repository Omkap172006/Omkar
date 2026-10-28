package com.virtualca.controller;

import com.virtualca.dto.DbConfigRequest;
import com.virtualca.dto.DbStatusResponse;
import com.virtualca.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/api/db-status")
    public ResponseEntity<?> getDbStatus(
            @RequestParam(value = "admin_key", required = false) String adminKey,
            @RequestParam(value = "dev_key", required = false) String devKey,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String key = (adminKey != null && !adminKey.isBlank()) ? adminKey : devKey;
        if (!adminService.isAdmin(adminHeader, authHeader, key)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Access Denied: Database connection telemetry is strictly restricted to Administrators.");
            err.put("isAdmin", false);
            err.put("connected", false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }

        DbStatusResponse status = adminService.getDbStatus();
        return ResponseEntity.ok(status);
    }

    @PostMapping("/api/db-config")
    public ResponseEntity<?> updateDbConfig(
            @RequestBody(required = false) DbConfigRequest body,
            @RequestParam(value = "admin_key", required = false) String adminKey,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String key = (body != null && body.getAdminKey() != null) ? body.getAdminKey() : adminKey;
        if (!adminService.isAdmin(adminHeader, authHeader, key)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("error", "Access Denied: Database configuration modification is restricted to Administrators.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }

        DbStatusResponse status = adminService.getDbStatus();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", status.isConnected());
        resp.put("message", status.isConnected() ? "Database connection verified." : "Unable to establish direct database connection.");
        resp.put("status", status);
        return ResponseEntity.ok(resp);
    }
}

