package com.virtualca.controller;

import com.virtualca.dto.LoginRequest;
import com.virtualca.dto.RegisterRequest;
import com.virtualca.entity.Organization;
import com.virtualca.service.AdminService;
import com.virtualca.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AuthController {

    private final OrganizationService organizationService;
    private final AdminService adminService;

    public AuthController(OrganizationService organizationService, AdminService adminService) {
        this.organizationService = organizationService;
        this.adminService = adminService;
    }

    @PostMapping("/api/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody(required = false) LoginRequest body,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String key = (body != null) ? body.getEffectiveKey() : null;
        String email = (body != null && body.getEmail() != null) ? body.getEmail().trim().toLowerCase() : "";

        boolean isAdmin = adminService.isAdmin(adminHeader, authHeader, key);

        Map<String, Object> resp = new LinkedHashMap<>();

        if (isAdmin) {
            List<Organization> allAccs = organizationService.getAllOrganizations();
            Organization adminAcc = organizationService.getAdminAccount();

            List<Organization> userAccs = new ArrayList<>();
            userAccs.add(adminAcc);
            for (Organization a : allAccs) {
                if (!a.getGstin().equalsIgnoreCase(adminAcc.getGstin())) {
                    userAccs.add(a);
                }
            }

            resp.put("status", "success");
            resp.put("isAdmin", true);
            resp.put("isDeveloper", true);
            resp.put("adminToken", "admin2026");
            resp.put("activeAccount", adminAcc);
            resp.put("user", adminAcc);
            resp.put("userAccounts", userAccs);
            resp.put("allAccounts", allAccs);
            return ResponseEntity.ok(resp);
        }

        if (!email.isBlank()) {
            List<Organization> userAccounts = organizationService.getOrganizationsByEmail(email);
            if (!userAccounts.isEmpty()) {
                boolean hasAdminRole = userAccounts.stream().anyMatch(a -> "admin".equalsIgnoreCase(a.getRole()));
                resp.put("status", "success");
                resp.put("isAdmin", hasAdminRole);
                resp.put("isDeveloper", hasAdminRole);
                resp.put("adminToken", hasAdminRole ? "admin2026" : null);
                resp.put("activeAccount", userAccounts.get(0));
                resp.put("user", userAccounts.get(0));
                resp.put("userAccounts", userAccounts);
                resp.put("totalUserAccounts", userAccounts.size());
                return ResponseEntity.ok(resp);
            }
        }

        resp.put("status", "not_found");
        resp.put("message", "No business entity found for this email.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
    }

    @PostMapping("/api/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        Organization userRecord = organizationService.createOrganization(req);
        List<Organization> userAccounts = organizationService.getOrganizationsByEmail(req.getEmail());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "success");
        resp.put("user", userRecord);
        resp.put("userAccounts", userAccounts);
        resp.put("userAccountCount", userAccounts.size());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/users")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "admin_key", required = false) String adminKey,
            @RequestParam(value = "dev_key", required = false) String devKey,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String paramKey = (adminKey != null && !adminKey.isBlank()) ? adminKey : devKey;
        boolean isAdmin = adminService.isAdmin(adminHeader, authHeader, paramKey);

        Map<String, Object> resp = new LinkedHashMap<>();
        if (isAdmin) {
            List<Organization> orgs = organizationService.getAllOrganizations();
            resp.put("role", "admin");
            resp.put("isAdmin", true);
            resp.put("accounts", orgs);
            resp.put("total", orgs.size());
            return ResponseEntity.ok(resp);
        } else if (email != null && !email.isBlank()) {
            List<Organization> orgs = organizationService.getOrganizationsByEmail(email);
            boolean hasAdmin = orgs.stream().anyMatch(a -> "admin".equalsIgnoreCase(a.getRole()));
            resp.put("role", hasAdmin ? "admin" : "user");
            resp.put("isAdmin", hasAdmin);
            resp.put("accounts", orgs);
            resp.put("total", orgs.size());
            return ResponseEntity.ok(resp);
        } else {
            resp.put("role", "guest");
            resp.put("isAdmin", false);
            resp.put("accounts", List.of());
            resp.put("total", 0);
            resp.put("message", "Authentication required");
            return ResponseEntity.ok(resp);
        }
    }
}

