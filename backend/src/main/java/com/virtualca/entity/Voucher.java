package com.virtualca.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vouchers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"org_id", "voucher_number"})
})
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    @JsonProperty("org_id")
    private UUID orgId;

    @Column(name = "voucher_number", nullable = false, length = 100)
    @JsonProperty("voucher_number")
    private String voucherNumber;

    @Column(name = "voucher_type", nullable = false, length = 20)
    @JsonProperty("voucher_type")
    private String voucherType;

    @Column(name = "party_name", nullable = false)
    @JsonProperty("party_name")
    private String partyName;

    @Column(name = "voucher_date", nullable = false)
    @JsonProperty("voucher_date")
    private LocalDate voucherDate = LocalDate.now();

    @Column(length = 2000)
    private String narration;

    @Column(name = "taxable_amount", precision = 15, scale = 2, nullable = false)
    @JsonProperty("taxable_amount")
    private BigDecimal taxableAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "gst_rate", precision = 5, scale = 2, nullable = false)
    @JsonProperty("gst_rate")
    private BigDecimal gstRate = BigDecimal.ZERO.setScale(2);

    @Column(name = "tax_amount", precision = 15, scale = 2, nullable = false)
    @JsonProperty("tax_amount")
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    @JsonProperty("total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "is_inter_state", nullable = false)
    @JsonProperty("is_inter_state")
    private boolean isInterState = false;

    @Column(length = 20)
    private String status = "BALANCED";

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<VoucherEntry> entries = new ArrayList<>();

    public Voucher() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getVoucherNumber() { return voucherNumber; }
    public void setVoucherNumber(String voucherNumber) { this.voucherNumber = voucherNumber; }

    public String getVoucherType() { return voucherType; }
    public void setVoucherType(String voucherType) { this.voucherType = voucherType; }

    public String getPartyName() { return partyName; }
    public void setPartyName(String partyName) { this.partyName = partyName; }

    public LocalDate getVoucherDate() { return voucherDate; }
    public void setVoucherDate(LocalDate voucherDate) { this.voucherDate = voucherDate; }

    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }

    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(BigDecimal taxableAmount) { this.taxableAmount = taxableAmount; }

    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public boolean isInterState() { return isInterState; }
    public void setInterState(boolean interState) { isInterState = interState; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<VoucherEntry> getEntries() { return entries; }
    public void setEntries(List<VoucherEntry> entries) { this.entries = entries; }

    public void addEntry(VoucherEntry entry) {
        entries.add(entry);
        entry.setVoucher(this);
    }
}
