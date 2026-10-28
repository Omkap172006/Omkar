package com.virtualca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class TransactionRequest {
    private String type;
    private String party;
    private BigDecimal amount;
    private BigDecimal rate;

    @JsonProperty("inter_state")
    private Boolean interState;

    @JsonProperty("org_id")
    private String orgId;

    private String bank;

    public TransactionRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getParty() { return party; }
    public void setParty(String party) { this.party = party; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public Boolean getInterState() { return interState != null && interState; }
    public void setInterState(Boolean interState) { this.interState = interState; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }
}

