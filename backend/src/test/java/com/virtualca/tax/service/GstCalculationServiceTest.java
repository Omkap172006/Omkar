package com.virtualca.tax.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GstCalculationServiceTest {

    private GstCalculationService gstService;

    @BeforeEach
    void setUp() {
        gstService = new GstCalculationService();
    }

    @Test
    @DisplayName("Intra-state supply divides GST equally between CGST and SGST")
    void testIntraStateGst() {
        GstCalculationService.GstBreakup breakup = gstService.calculateTax(
            BigDecimal.valueOf(100000),
            BigDecimal.valueOf(18),
            "27", // Maharashtra
            "27"  // Maharashtra
        );

        assertFalse(breakup.isInterState());
        assertEquals(new BigDecimal("18000.00"), breakup.totalTax());
        assertEquals(new BigDecimal("9000.00"), breakup.cgstAmount());
        assertEquals(new BigDecimal("9000.00"), breakup.sgstAmount());
        assertEquals(BigDecimal.ZERO, breakup.igstAmount());
        assertEquals(new BigDecimal("118000.00"), breakup.grandTotal());
    }

    @Test
    @DisplayName("Inter-state supply allocates entire GST to IGST")
    void testInterStateGst() {
        GstCalculationService.GstBreakup breakup = gstService.calculateTax(
            BigDecimal.valueOf(100000),
            BigDecimal.valueOf(18),
            "27", // Maharashtra
            "29"  // Karnataka
        );

        assertTrue(breakup.isInterState());
        assertEquals(new BigDecimal("18000.00"), breakup.totalTax());
        assertEquals(BigDecimal.ZERO, breakup.cgstAmount());
        assertEquals(BigDecimal.ZERO, breakup.sgstAmount());
        assertEquals(new BigDecimal("18000.00"), breakup.igstAmount());
        assertEquals(new BigDecimal("118000.00"), breakup.grandTotal());
    }

    @Test
    @DisplayName("Odd amount division avoids rounding skew between CGST and SGST")
    void testOddAmountRounding() {
        GstCalculationService.GstBreakup breakup = gstService.calculateTax(
            new BigDecimal("999.99"),
            BigDecimal.valueOf(5),
            "27",
            "27"
        );

        assertEquals(breakup.totalTax(), breakup.cgstAmount().add(breakup.sgstAmount()));
        assertEquals(breakup.taxableAmount().add(breakup.totalTax()), breakup.grandTotal());
    }
}

