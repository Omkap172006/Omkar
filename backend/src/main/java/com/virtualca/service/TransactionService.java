package com.virtualca.service;

import com.virtualca.dto.TransactionRequest;
import com.virtualca.entity.Organization;
import com.virtualca.entity.Voucher;
import com.virtualca.entity.VoucherEntry;
import com.virtualca.ledger.service.DoubleEntryEngineService;
import com.virtualca.repository.OrganizationRepository;
import com.virtualca.repository.VoucherRepository;
import com.virtualca.tax.service.GstCalculationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TransactionService {

    private final VoucherRepository voucherRepository;
    private final OrganizationRepository organizationRepository;
    private final GstCalculationService gstCalculationService;
    private final DoubleEntryEngineService doubleEntryEngineService;

    public TransactionService(
            VoucherRepository voucherRepository,
            OrganizationRepository organizationRepository,
            GstCalculationService gstCalculationService,
            DoubleEntryEngineService doubleEntryEngineService
    ) {
        this.voucherRepository = voucherRepository;
        this.organizationRepository = organizationRepository;
        this.gstCalculationService = gstCalculationService;
        this.doubleEntryEngineService = doubleEntryEngineService;
    }

    public List<Voucher> getTransactions(UUID orgId) {
        if (orgId != null) {
            return voucherRepository.findByOrgIdOrderByCreatedAtDesc(orgId);
        }
        return voucherRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Map<String, Object> createTransaction(TransactionRequest req) {
        String rawType = req.getType() != null ? req.getType().toUpperCase().trim() : "SALES";
        String txnType;
        if (rawType.equals("SALE") || rawType.equals("SALES")) {
            txnType = "SALES";
        } else if (rawType.equals("PURCHASE") || rawType.equals("PURCHASES")) {
            txnType = "PURCHASE";
        } else if (rawType.equals("EXPENSE") || rawType.equals("EXPENSES") || rawType.equals("PAYMENT")) {
            txnType = "EXPENSE";
        } else if (rawType.equals("BANK") || rawType.equals("TREASURY")) {
            txnType = "PAYMENT";
        } else {
            txnType = rawType;
        }

        String party = (req.getParty() != null && !req.getParty().isBlank()) ? req.getParty().trim() : "Party";
        BigDecimal amt = req.getAmount() != null ? req.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        BigDecimal rate = req.getRate() != null ? req.getRate().setScale(2, RoundingMode.HALF_UP) : BigDecimal.valueOf(18).setScale(2);
        boolean interState = req.getInterState();

        UUID targetOrgId = null;
        if (req.getOrgId() != null && !req.getOrgId().isBlank()) {
            try {
                targetOrgId = UUID.fromString(req.getOrgId().trim());
            } catch (IllegalArgumentException ignored) {}
        }
        if (targetOrgId == null || !organizationRepository.existsById(targetOrgId)) {
            List<Organization> orgs = organizationRepository.findAll();
            if (orgs.isEmpty()) {
                throw new IllegalStateException("No organization found to assign transaction to.");
            }
            targetOrgId = orgs.get(0).getId();
        }

        Organization org = organizationRepository.findById(targetOrgId).orElseThrow();
        String supplierState = org.getStateCode();
        String posState = interState ? (supplierState.equals("27") ? "29" : "27") : supplierState;

        GstCalculationService.GstBreakup gst = gstCalculationService.calculateTax(amt, rate, supplierState, posState);
        BigDecimal total = gst.grandTotal();
        BigDecimal tax = gst.totalTax();

        long count = voucherRepository.countByOrgId(targetOrgId) + 1;
        String voucherNum = String.format("VCH-%s-%03d", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")), count);

        Voucher voucher = new Voucher();
        voucher.setOrgId(targetOrgId);
        voucher.setVoucherNumber(voucherNum);
        voucher.setVoucherType(txnType);
        voucher.setPartyName(party);
        voucher.setVoucherDate(LocalDate.now());
        voucher.setNarration("Transaction with " + party + " (" + txnType + ")");
        voucher.setTaxableAmount(amt);
        voucher.setGstRate(rate);
        voucher.setTaxAmount(tax);
        voucher.setTotalAmount(total);
        voucher.setInterState(interState);
        voucher.setStatus("BALANCED");

        String bankName = (req.getBank() != null && !req.getBank().isBlank()) ? req.getBank().trim() : "HDFC Bank";
        String bankAccStr = "Bank Account (" + bankName + ")";

        if (txnType.equals("SALES")) {
            voucher.addEntry(new VoucherEntry(bankAccStr, "DEBIT", total, "Bank / Sundry Debtors"));
            voucher.addEntry(new VoucherEntry("Sales Revenue", "CREDIT", amt, "Operating Revenue"));
            if (tax.compareTo(BigDecimal.ZERO) > 0) {
                voucher.addEntry(new VoucherEntry("GST Output Tax Payable", "CREDIT", tax, "Output GST Liability"));
            }
        } else if (txnType.equals("PURCHASE")) {
            voucher.addEntry(new VoucherEntry("Operating & Administrative Expenses", "DEBIT", amt, "Asset/Expense Line"));
            if (tax.compareTo(BigDecimal.ZERO) > 0) {
                voucher.addEntry(new VoucherEntry("GST Input Tax Credit (ITC)", "DEBIT", tax, "Input Tax Credit Eligible"));
            }
            voucher.addEntry(new VoucherEntry(bankAccStr, "CREDIT", total, "Bank Settlement"));
        } else {
            voucher.addEntry(new VoucherEntry("Operating & Administrative Expenses", "DEBIT", total, "Operating Expense"));
            voucher.addEntry(new VoucherEntry(bankAccStr, "CREDIT", total, "Bank Disbursal"));
        }

        Voucher saved = voucherRepository.save(voucher);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("voucher_number", saved.getVoucherNumber());
        result.put("type", saved.getVoucherType());
        result.put("party", saved.getPartyName());
        result.put("amount", saved.getTotalAmount().doubleValue());
        result.put("taxable", saved.getTaxableAmount().doubleValue());
        result.put("tax", saved.getTaxAmount().doubleValue());
        result.put("is_inter_state", saved.isInterState());
        result.put("status", "BALANCED in Spring Boot");

        return result;
    }
}

