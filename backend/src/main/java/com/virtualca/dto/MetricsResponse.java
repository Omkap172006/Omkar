package com.virtualca.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MetricsResponse {
    private BigDecimal bank;
    private BigDecimal revenue;
    private BigDecimal expenses;
    private BigDecimal profit;
    private BigDecimal gstPayable;

    public MetricsResponse() {}

    public MetricsResponse(BigDecimal bank, BigDecimal revenue, BigDecimal expenses, BigDecimal profit, BigDecimal gstPayable) {
        this.bank = bank != null ? bank.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        this.revenue = revenue != null ? revenue.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        this.expenses = expenses != null ? expenses.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        this.profit = profit != null ? profit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        this.gstPayable = gstPayable != null ? gstPayable.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }

    public BigDecimal getBank() { return bank; }
    public void setBank(BigDecimal bank) { this.bank = bank; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public BigDecimal getExpenses() { return expenses; }
    public void setExpenses(BigDecimal expenses) { this.expenses = expenses; }

    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }

    public BigDecimal getGstPayable() { return gstPayable; }
    public void setGstPayable(BigDecimal gstPayable) { this.gstPayable = gstPayable; }
}

