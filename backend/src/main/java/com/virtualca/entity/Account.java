package com.virtualca.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"org_id", "account_code"})
})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "account_code", nullable = false, length = 50)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType; // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE

    @Column(name = "current_balance", precision = 15, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO.setScale(2);

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Account() {}

    public Account(UUID orgId, String accountCode, String accountName, String accountType) {
        this.orgId = orgId;
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.accountType = accountType;
        this.currentBalance = BigDecimal.ZERO.setScale(2);
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

