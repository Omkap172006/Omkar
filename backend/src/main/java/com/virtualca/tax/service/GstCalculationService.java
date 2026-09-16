package com.virtualca.tax.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * GST Calculation Engine according to Indian Goods and Services Tax Act.
 * Uses strict BigDecimal arithmetic with HALF_UP rounding.
 */
@Service
public class GstCalculationService {

    public record GstBreakup(
        BigDecimal taxableAmount,
        BigDecimal gstRate,
        BigDecimal cgstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstRate,
        BigDecimal sgstAmount,
        BigDecimal igstRate,
        BigDecimal igstAmount,
        BigDecimal totalTax,
        BigDecimal grandTotal,
        boolean isInterState
    ) {}

    /**
     * Computes GST breakup based on supplier state and place of supply (POS).
     * Intra-state: CGST (Rate/2) + SGST (Rate/2)
     * Inter-state: IGST (Rate)
     */
    public GstBreakup calculateTax(
            BigDecimal taxableAmount, 
            BigDecimal gstRatePercent, 
            String supplierStateCode, 
            String placeOfSupplyStateCode
    ) {
        if (taxableAmount == null || taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        boolean isInterState = !supplierStateCode.trim().equalsIgnoreCase(placeOfSupplyStateCode.trim());
        BigDecimal rateFraction = gstRatePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalTax = taxableAmount.multiply(rateFraction).setScale(2, RoundingMode.HALF_UP);

        BigDecimal cgstRate = BigDecimal.ZERO;
        BigDecimal cgstAmount = BigDecimal.ZERO;
        BigDecimal sgstRate = BigDecimal.ZERO;
        BigDecimal sgstAmount = BigDecimal.ZERO;
        BigDecimal igstRate = BigDecimal.ZERO;
        BigDecimal igstAmount = BigDecimal.ZERO;

        if (isInterState) {
            igstRate = gstRatePercent;
            igstAmount = totalTax;
        } else {
            cgstRate = gstRatePercent.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            sgstRate = cgstRate;
            cgstAmount = totalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            sgstAmount = totalTax.subtract(cgstAmount); // Prevents 1-paisa rounding skew
        }

        BigDecimal grandTotal = taxableAmount.add(totalTax);

        return new GstBreakup(
            taxableAmount.setScale(2, RoundingMode.HALF_UP),
            gstRatePercent.setScale(2, RoundingMode.HALF_UP),
            cgstRate,
            cgstAmount,
            sgstRate,
            sgstAmount,
            igstRate,
            igstAmount,
            totalTax,
            grandTotal.setScale(2, RoundingMode.HALF_UP),
            isInterState
        );
    }
}
