package com.virtualca.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voucher_entries")
public class VoucherEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    @JsonIgnore
    private Voucher voucher;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "entry_type", nullable = false, length = 6)
    private String entryType; // DEBIT or CREDIT

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO.setScale(2);

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public VoucherEntry() {}

    public VoucherEntry(String accountName, String entryType, BigDecimal amount, String notes) {
        this.accountName = accountName;
        this.entryType = entryType;
        this.amount = amount != null ? amount.setScale(2) : BigDecimal.ZERO.setScale(2);
        this.notes = notes;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

