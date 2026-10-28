package com.virtualca.service;

import com.virtualca.dto.RegisterRequest;
import com.virtualca.entity.Account;
import com.virtualca.entity.Organization;
import com.virtualca.repository.AccountRepository;
import com.virtualca.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AccountRepository accountRepository;

    private static final Map<String, String> STATE_MAP = Map.of(
        "27", "Maharashtra", "29", "Karnataka", "07", "Delhi",
        "33", "Tamil Nadu", "24", "Gujarat", "06", "Haryana",
        "19", "West Bengal", "36", "Telangana", "09", "Uttar Pradesh"
    );

    public OrganizationService(OrganizationRepository organizationRepository, AccountRepository accountRepository) {
        this.organizationRepository = organizationRepository;
        this.accountRepository = accountRepository;
    }

    public List<Organization> getOrganizationsByEmail(String email) {
        if (email == null || email.isBlank()) return List.of();
        return organizationRepository.findByEmailIgnoreCaseOrderByCreatedAtDesc(email.trim());
    }

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Organization> getById(UUID id) {
        return organizationRepository.findById(id);
    }

    public Organization getAdminAccount() {
        List<Organization> orgs = getAllOrganizations();
        return orgs.stream()
            .filter(o -> "Admin".equalsIgnoreCase(o.getRole()))
            .findFirst()
            .orElseGet(() -> {
                Organization admin = new Organization(
                    "System Administrator",
                    "Virtual CA Cloud Infrastructure",
                    "27ADMIN0000A1Z0",
                    "ADMIN0000A",
                    "27",
                    "Maharashtra",
                    "admin@virtualca.internal",
                    "Admin",
                    "AD"
                );
                return organizationRepository.save(admin);
            });
    }

    @Transactional
    public Organization createOrganization(RegisterRequest req) {
        String name = req.getName().trim();
        String email = req.getEmail().trim().toLowerCase();
        String company = (req.getCompany() != null && !req.getCompany().isBlank()) ? req.getCompany().trim() : name;
        String gstin = (req.getGstin() != null && !req.getGstin().isBlank()) ? req.getGstin().trim().toUpperCase() : "27AABCA1234F1Z5";
        String role = (req.getRole() != null && !req.getRole().isBlank()) ? req.getRole().trim() : "Owner";

        String stateCode = "27";
        if (gstin.length() >= 2 && Character.isDigit(gstin.charAt(0)) && Character.isDigit(gstin.charAt(1))) {
            stateCode = gstin.substring(0, 2);
        }
        String stateName = STATE_MAP.getOrDefault(stateCode, "India");

        String pan = gstin.length() >= 12 ? gstin.substring(2, 12) : "ABCDE1234F";

        String[] parts = name.split("\\s+");
        String initials = "US";
        if (parts.length > 1) {
            initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        } else if (name.length() >= 2) {
            initials = name.substring(0, 2).toUpperCase();
        }

        Optional<Organization> existing = organizationRepository.findByGstin(gstin);
        Organization org;
        if (existing.isPresent()) {
            org = existing.get();
            org.setName(name);
            org.setTradeName(company);
            org.setEmail(email);
            org.setRole(role);
            org.setInitials(initials);
        } else {
            org = new Organization(name, company, gstin, pan, stateCode, stateName, email, role, initials);
        }

        Organization saved = organizationRepository.save(org);
        initializeStandardAccounts(saved.getId());
        return saved;
    }

    @Transactional
    public void initializeStandardAccounts(UUID orgId) {
        String[][] standardAccounts = {
            {"1001", "Bank Account (HDFC)", "ASSET"},
            {"1002", "Accounts Receivable / Debtors", "ASSET"},
            {"1003", "GST Input Tax Credit (ITC)", "ASSET"},
            {"2001", "Accounts Payable / Creditors", "LIABILITY"},
            {"2002", "GST Output Tax Payable", "LIABILITY"},
            {"3001", "Owner Capital / Equity", "EQUITY"},
            {"4001", "Sales Revenue", "REVENUE"},
            {"5001", "Operating & Administrative Expenses", "EXPENSE"}
        };

        for (String[] acc : standardAccounts) {
            if (accountRepository.findByOrgIdAndAccountCode(orgId, acc[0]).isEmpty()) {
                accountRepository.save(new Account(orgId, acc[0], acc[1], acc[2]));
            }
        }
    }
}

