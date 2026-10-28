package com.virtualca.service;

import com.virtualca.dto.MetricsResponse;
import com.virtualca.repository.VoucherRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class MetricsService {

    private final VoucherRepository voucherRepository;

    public MetricsService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    public MetricsResponse getMetrics(UUID orgId) {
        BigDecimal revenue = voucherRepository.sumSalesRevenue(orgId);
        BigDecimal expenses = voucherRepository.sumExpenses(orgId);
        BigDecimal outputGst = voucherRepository.sumOutputGst(orgId);
        BigDecimal inputGst = voucherRepository.sumInputGst(orgId);

        if (revenue == null) revenue = BigDecimal.ZERO;
        if (expenses == null) expenses = BigDecimal.ZERO;
        if (outputGst == null) outputGst = BigDecimal.ZERO;
        if (inputGst == null) inputGst = BigDecimal.ZERO;

        BigDecimal profit = revenue.subtract(expenses);
        BigDecimal gstPayable = outputGst.subtract(inputGst);
        if (gstPayable.compareTo(BigDecimal.ZERO) < 0) {
            gstPayable = BigDecimal.ZERO;
        }

        BigDecimal baseBank = BigDecimal.valueOf(500000.00);
        BigDecimal bank = baseBank.add(revenue).subtract(expenses);

        return new MetricsResponse(
            bank.setScale(2, RoundingMode.HALF_UP),
            revenue.setScale(2, RoundingMode.HALF_UP),
            expenses.setScale(2, RoundingMode.HALF_UP),
            profit.setScale(2, RoundingMode.HALF_UP),
            gstPayable.setScale(2, RoundingMode.HALF_UP)
        );
    }
}

