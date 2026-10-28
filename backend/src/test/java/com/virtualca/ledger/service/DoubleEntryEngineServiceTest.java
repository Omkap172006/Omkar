package com.virtualca.ledger.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DoubleEntryEngineServiceTest {

    private DoubleEntryEngineService doubleEntryEngineService;

    @BeforeEach
    void setUp() {
        doubleEntryEngineService = new DoubleEntryEngineService();
    }

    @Test
    @DisplayName("Record GST Sale maintains Assets = Liabilities + Equity (Debits == Credits)")
    void testRecordGstSaleBalance() {
        DoubleEntryEngineService.JournalVoucher voucher = doubleEntryEngineService.recordGstSale(
            "INV-2026-001",
            "Infosys Ltd",
            new BigDecimal("100000.00"),
            new BigDecimal("9000.00"),
            new BigDecimal("9000.00"),
            BigDecimal.ZERO,
            false
        );

        assertNotNull(voucher);
        // Intra-state sale generates 4 lines: 1 Debit (Debtors), 3 Credits (Sales + CGST + SGST)
        assertEquals(4, voucher.lines().size());

        BigDecimal totalDebits = voucher.lines().stream()
            .filter(l -> l.entryType() == DoubleEntryEngineService.EntryType.DEBIT)
            .map(DoubleEntryEngineService.JournalLine::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = voucher.lines().stream()
            .filter(l -> l.entryType() == DoubleEntryEngineService.EntryType.CREDIT)
            .map(DoubleEntryEngineService.JournalLine::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("118000.00"), totalDebits);
        assertEquals(new BigDecimal("118000.00"), totalCredits);
        assertEquals(0, totalDebits.compareTo(totalCredits));
    }

    @Test
    @DisplayName("Record Expense maintains Debits == Credits")
    void testRecordExpenseBalance() {
        DoubleEntryEngineService.JournalVoucher voucher = doubleEntryEngineService.recordExpense(
            "Office Rent",
            new BigDecimal("50000.00"),
            new BigDecimal("9000.00"),
            "BANK",
            "WeWork"
        );

        assertNotNull(voucher);
        // Expense generates 3 lines: 1 Debit (Expense), 1 Debit (ITC), 1 Credit (Bank)
        assertEquals(3, voucher.lines().size());

        BigDecimal totalDebits = voucher.lines().stream()
            .filter(l -> l.entryType() == DoubleEntryEngineService.EntryType.DEBIT)
            .map(DoubleEntryEngineService.JournalLine::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = voucher.lines().stream()
            .filter(l -> l.entryType() == DoubleEntryEngineService.EntryType.CREDIT)
            .map(DoubleEntryEngineService.JournalLine::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("59000.00"), totalDebits);
        assertEquals(new BigDecimal("59000.00"), totalCredits);
        assertEquals(0, totalDebits.compareTo(totalCredits));
    }
}
