package com.virtualca.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualca.dto.RegisterRequest;
import com.virtualca.entity.*;
import com.virtualca.repository.*;
import com.virtualca.service.OrganizationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;
    private final VoucherRepository voucherRepository;
    private final ComplianceEventRepository complianceEventRepository;
    private final AiFinancialInsightRepository aiFinancialInsightRepository;
    private final ObjectMapper objectMapper;

    public DatabaseInitializer(
            OrganizationRepository organizationRepository,
            OrganizationService organizationService,
            VoucherRepository voucherRepository,
            ComplianceEventRepository complianceEventRepository,
            AiFinancialInsightRepository aiFinancialInsightRepository,
            ObjectMapper objectMapper
    ) {
        this.organizationRepository = organizationRepository;
        this.organizationService = organizationService;
        this.voucherRepository = voucherRepository;
        this.complianceEventRepository = complianceEventRepository;
        this.aiFinancialInsightRepository = aiFinancialInsightRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        seedOrganizations();
        seedVouchers();
        seedComplianceEvents();
        seedAiInsights();
    }

    private void seedOrganizations() {
        // Ensure Admin organization exists
        organizationService.getAdminAccount();

        // Check if other organizations exist, or import from users.json
        if (organizationRepository.count() <= 1) {
            List<File> candidates = List.of(
                new File("database/users.json"),
                new File("../database/users.json"),
                new File(System.getProperty("user.dir"), "database/users.json")
            );

            for (File file : candidates) {
                if (file.exists()) {
                    try {
                        List<Map<String, Object>> users = objectMapper.readValue(file, new TypeReference<>() {});
                        for (Map<String, Object> u : users) {
                            String email = (String) u.get("email");
                            String gstin = (String) u.get("gstin");
                            if (email != null && !organizationRepository.findByGstin(gstin).isPresent()) {
                                RegisterRequest req = new RegisterRequest();
                                req.setName((String) u.getOrDefault("name", "Business Owner"));
                                req.setEmail(email);
                                req.setCompany((String) u.getOrDefault("company", "Business Entity"));
                                req.setGstin(gstin);
                                req.setRole((String) u.getOrDefault("role", "Owner"));
                                organizationService.createOrganization(req);
                            }
                        }
                        break;
                    } catch (Exception e) {
                        System.err.println("[DatabaseInitializer] Failed to parse users.json: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void seedVouchers() {
        if (voucherRepository.count() > 0) return;

        List<Organization> orgs = organizationRepository.findAll();
        if (orgs.isEmpty()) return;

        Organization targetOrg = orgs.stream()
            .filter(o -> !"Admin".equalsIgnoreCase(o.getRole()))
            .findFirst()
            .orElse(orgs.get(0));

        UUID orgId = targetOrg.getId();

        // 1. Sale to Infosys
        Voucher v1 = new Voucher();
        v1.setOrgId(orgId);
        v1.setVoucherNumber("VCH-2026-001");
        v1.setVoucherType("SALES");
        v1.setPartyName("Infosys Technologies Pune");
        v1.setVoucherDate(LocalDate.now());
        v1.setNarration("Software architecture consulting fees");
        v1.setTaxableAmount(BigDecimal.valueOf(100000));
        v1.setGstRate(BigDecimal.valueOf(18));
        v1.setTaxAmount(BigDecimal.valueOf(18000));
        v1.setTotalAmount(BigDecimal.valueOf(118000));
        v1.setInterState(false);
        v1.setStatus("BALANCED");
        v1.addEntry(new VoucherEntry("Bank Account (HDFC)", "DEBIT", BigDecimal.valueOf(118000), "Receipt in Bank"));
        v1.addEntry(new VoucherEntry("Sales Revenue", "CREDIT", BigDecimal.valueOf(100000), "Revenue Recognized"));
        v1.addEntry(new VoucherEntry("GST Output Tax Payable", "CREDIT", BigDecimal.valueOf(18000), "Output GST Liability"));
        voucherRepository.save(v1);

        // 2. Office rent to WeWork
        Voucher v2 = new Voucher();
        v2.setOrgId(orgId);
        v2.setVoucherNumber("VCH-2026-002");
        v2.setVoucherType("EXPENSE");
        v2.setPartyName("WeWork Bandra Kurla Complex");
        v2.setVoucherDate(LocalDate.now());
        v2.setNarration("Monthly office workspace rental (TDS withheld)");
        v2.setTaxableAmount(BigDecimal.valueOf(54000));
        v2.setGstRate(BigDecimal.ZERO);
        v2.setTaxAmount(BigDecimal.ZERO);
        v2.setTotalAmount(BigDecimal.valueOf(54000));
        v2.setInterState(false);
        v2.setStatus("BALANCED");
        v2.addEntry(new VoucherEntry("Operating & Administrative Expenses", "DEBIT", BigDecimal.valueOf(54000), "Operating Outflow"));
        v2.addEntry(new VoucherEntry("Bank Account (HDFC)", "CREDIT", BigDecimal.valueOf(54000), "Direct Payment"));
        voucherRepository.save(v2);

        // 3. Inter-State Sale to Bangalore
        Voucher v3 = new Voucher();
        v3.setOrgId(orgId);
        v3.setVoucherNumber("VCH-2026-003");
        v3.setVoucherType("SALES");
        v3.setPartyName("Bangalore Cloud Systems Ltd");
        v3.setVoucherDate(LocalDate.now());
        v3.setNarration("Custom ERP development milestone 2");
        v3.setTaxableAmount(BigDecimal.valueOf(120000));
        v3.setGstRate(BigDecimal.valueOf(18));
        v3.setTaxAmount(BigDecimal.valueOf(21600));
        v3.setTotalAmount(BigDecimal.valueOf(141600));
        v3.setInterState(true);
        v3.setStatus("BALANCED");
        v3.addEntry(new VoucherEntry("Bank Account (HDFC)", "DEBIT", BigDecimal.valueOf(141600), "Receipt in Bank"));
        v3.addEntry(new VoucherEntry("Sales Revenue", "CREDIT", BigDecimal.valueOf(120000), "Revenue Recognized"));
        v3.addEntry(new VoucherEntry("GST Output Tax Payable", "CREDIT", BigDecimal.valueOf(21600), "Output GST Liability"));
        voucherRepository.save(v3);

        // 4. Server purchase from Dell
        Voucher v4 = new Voucher();
        v4.setOrgId(orgId);
        v4.setVoucherNumber("VCH-2026-004");
        v4.setVoucherType("PURCHASE");
        v4.setPartyName("Dell Technologies India");
        v4.setVoucherDate(LocalDate.now());
        v4.setNarration("High-performance development servers");
        v4.setTaxableAmount(BigDecimal.valueOf(35000));
        v4.setGstRate(BigDecimal.valueOf(18));
        v4.setTaxAmount(BigDecimal.valueOf(6300));
        v4.setTotalAmount(BigDecimal.valueOf(41300));
        v4.setInterState(false);
        v4.setStatus("BALANCED");
        v4.addEntry(new VoucherEntry("Operating & Administrative Expenses", "DEBIT", BigDecimal.valueOf(35000), "Asset Capitalization"));
        v4.addEntry(new VoucherEntry("GST Input Tax Credit (ITC)", "DEBIT", BigDecimal.valueOf(6300), "Input Tax Credit Eligible"));
        v4.addEntry(new VoucherEntry("Bank Account (HDFC)", "CREDIT", BigDecimal.valueOf(41300), "Bank Disbursal"));
        voucherRepository.save(v4);
    }

    private void seedComplianceEvents() {
        if (complianceEventRepository.count() > 0) return;

        List<ComplianceEvent> events = List.of(
            new ComplianceEvent(null, "GSTR-1 (Outward Supplies)", "GST", "August 2026", LocalDate.of(2026, 9, 11), "FILED", BigDecimal.valueOf(50)),
            new ComplianceEvent(null, "GSTR-3B (Summary & Tax Payment)", "GST", "August 2026", LocalDate.of(2026, 9, 20), "PENDING", BigDecimal.valueOf(50)),
            new ComplianceEvent(null, "TDS Challan 281 (Section 194I / 194J)", "TDS", "August 2026", LocalDate.of(2026, 10, 7), "PENDING", BigDecimal.valueOf(200)),
            new ComplianceEvent(null, "Advance Tax Q2 (45% Cumulative)", "ADVANCE_TAX", "Q2 FY 2026-27", LocalDate.of(2026, 9, 15), "PENDING", BigDecimal.valueOf(100))
        );
        complianceEventRepository.saveAll(events);
    }

    private void seedAiInsights() {
        if (aiFinancialInsightRepository.count() > 0) return;

        List<AiFinancialInsight> insights = List.of(
            new AiFinancialInsight(null, "ITC_MISMATCH", "MEDIUM", "GSTR-2B ITC Match Opportunity", "₹6,300 Input Tax Credit eligible for auto-reconciliation against Dell Technologies GSTR-1 filing.", BigDecimal.valueOf(6300), "Auto-claim in Table 4(A)(5) of GSTR-3B."),
            new AiFinancialInsight(null, "TAX_SAVING", "LOW", "Section 80JJAA Hiring Benefit", "Additional deduction of 30% on employee emoluments available for 2 new developer hires.", BigDecimal.valueOf(45000), "Ensure CA Form 10DA is uploaded prior to ITR filing."),
            new AiFinancialInsight(null, "EXPENSE_ANOMALY", "INFO", "Office Rent TDS Provision", "Rent payments to WeWork exceed ₹2.4L annual threshold; Section 194-I TDS rate applied correctly.", BigDecimal.ZERO, "TDS Challan 281 scheduled for deposit.")
        );
        aiFinancialInsightRepository.saveAll(insights);
    }
}

