package com.virtualca.ledger.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core Double-Entry Bookkeeping Engine.
 * Enforces fundamental accounting equation: Assets = Liabilities + Equity.
 * Guarantees SUM(Debit) == SUM(Credit) within atomic transactions.
 */
@Service
public class DoubleEntryEngineService {

    public enum EntryType { DEBIT, CREDIT }
    public enum VoucherType { SALES, PURCHASE, EXPENSE, PAYMENT, RECEIPT, CONTRA, JOURNAL }

    public record JournalLine(
        String accountCode,
        String accountName,
        EntryType entryType,
        BigDecimal amount,
        String notes
    ) {}

    public record JournalVoucher(
        UUID voucherId,
        String voucherNumber,
        VoucherType voucherType,
        LocalDate date,
        String narration,
        BigDecimal totalAmount,
        List<JournalLine> lines
    ) {}

    /**
     * Creates an automated balanced Double-Entry voucher for a GST Sale.
     * Debit: Debtor / Bank (Grand Total)
     * Credit: Sales Revenue (Taxable Amount)
     * Credit: Output CGST / SGST / IGST (Tax Amount)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public JournalVoucher recordGstSale(
            String invoiceNumber,
            String customerName,
            BigDecimal taxableAmount,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            boolean isInterState
    ) {
        BigDecimal totalTax = isInterState ? igstAmount : cgstAmount.add(sgstAmount);
        BigDecimal grandTotal = taxableAmount.add(totalTax);

        List<JournalLine> lines = new ArrayList<>();

        // 1. Debit Accounts Receivable / Customer
        lines.add(new JournalLine(
            "1100", 
            "Sundry Debtors - " + customerName, 
            EntryType.DEBIT, 
            grandTotal, 
            "Receivable for Inv #" + invoiceNumber
        ));

        // 2. Credit Sales Account
        lines.add(new JournalLine(
            "4000", 
            "Sales Account (Revenue)", 
            EntryType.CREDIT, 
            taxableAmount, 
            "Taxable value for Inv #" + invoiceNumber
        ));

        // 3. Credit Output Tax
        if (isInterState) {
            lines.add(new JournalLine(
                "2210", 
                "Output IGST Payable", 
                EntryType.CREDIT, 
                igstAmount, 
                "Inter-state GST liability"
            ));
        } else {
            lines.add(new JournalLine(
                "2220", 
                "Output CGST Payable", 
                EntryType.CREDIT, 
                cgstAmount, 
                "Intra-state Central GST liability"
            ));
            lines.add(new JournalLine(
                "2230", 
                "Output SGST Payable", 
                EntryType.CREDIT, 
                sgstAmount, 
                "Intra-state State GST liability"
            ));
        }

        validateBalancedBook(lines);

        return new JournalVoucher(
            UUID.randomUUID(),
            "VCH-SALE-" + System.currentTimeMillis() % 100000,
            VoucherType.SALES,
            LocalDate.now(),
            "Recorded GST Tax Invoice #" + invoiceNumber + " for " + customerName,
            grandTotal,
            lines
        );
    }

    /**
     * Creates an automated balanced Double-Entry voucher for a Business Expense.
     * Debit: Expense Head (Rent, Electricity, Software, etc.)
     * Debit: Input Tax Credit (ITC - CGST/SGST/IGST if eligible)
     * Credit: Cash / Bank / Creditor
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public JournalVoucher recordExpense(
            String expenseCategory,
            BigDecimal netExpenseAmount,
            BigDecimal itcTaxAmount,
            String paymentMode, // "BANK" or "CASH"
            String vendor
    ) {
        BigDecimal totalPaid = netExpenseAmount.add(itcTaxAmount);
        List<JournalLine> lines = new ArrayList<>();

        // Debit Expense
        lines.add(new JournalLine(
            "5100", 
            expenseCategory + " Expense", 
            EntryType.DEBIT, 
            netExpenseAmount, 
            "Paid to " + vendor
        ));

        // Debit Input Tax Credit (Asset)
        if (itcTaxAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new JournalLine(
                "1300", 
                "Input Tax Credit (ITC) Receivable", 
                EntryType.DEBIT, 
                itcTaxAmount, 
                "GST Input on " + expenseCategory
            ));
        }

        // Credit Payment source
        String creditAccountCode = "BANK".equalsIgnoreCase(paymentMode) ? "1010" : "1020";
        String creditAccountName = "BANK".equalsIgnoreCase(paymentMode) ? "HDFC Bank Current A/c" : "Cash in Hand";

        lines.add(new JournalLine(
            creditAccountCode,
            creditAccountName,
            EntryType.CREDIT,
            totalPaid,
            "Settled expense via " + paymentMode
        ));

        validateBalancedBook(lines);

        return new JournalVoucher(
            UUID.randomUUID(),
            "VCH-EXP-" + System.currentTimeMillis() % 100000,
            VoucherType.EXPENSE,
            LocalDate.now(),
            "Expense recorded: " + expenseCategory + " - " + vendor,
            totalPaid,
            lines
        );
    }

    private void validateBalancedBook(List<JournalLine> lines) {
        BigDecimal totalDebit = lines.stream()
            .filter(l -> l.entryType() == EntryType.DEBIT)
            .map(JournalLine::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = lines.stream()
            .filter(l -> l.entryType() == EntryType.CREDIT)
            .map(JournalLine::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalStateException("Double Entry Discrepancy! Total Debits (" + totalDebit + 
                ") do not equal Total Credits (" + totalCredit + ")");
        }
    }
}
