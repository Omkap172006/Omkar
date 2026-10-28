package com.virtualca.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "compliance_events")
public class ComplianceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "org_id")
    @JsonProperty("org_id")
    private UUID orgId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "compliance_category", nullable = false, length = 50)
    @JsonProperty("compliance_category")
    private String complianceCategory;

    @Column(nullable = false, length = 50)
    private String period;

    @Column(name = "due_date", nullable = false)
    @JsonProperty("due_date")
    private LocalDate dueDate;

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "penalty_per_day", precision = 10, scale = 2)
    @JsonProperty("penalty_per_day")
    private BigDecimal penaltyPerDay = BigDecimal.valueOf(50).setScale(2);

    @Column(name = "reference_link", length = 255)
    @JsonProperty("reference_link")
    private String referenceLink;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private Instant createdAt = Instant.now();

    public ComplianceEvent() {}

    public ComplianceEvent(UUID orgId, String title, String complianceCategory, String period, LocalDate dueDate, String status, BigDecimal penaltyPerDay) {
        this.orgId = orgId;
        this.title = title;
        this.complianceCategory = complianceCategory;
        this.period = period;
        this.dueDate = dueDate;
        this.status = status != null ? status : "PENDING";
        this.penaltyPerDay = penaltyPerDay != null ? penaltyPerDay : BigDecimal.valueOf(50);
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getComplianceCategory() { return complianceCategory; }
    public void setComplianceCategory(String complianceCategory) { this.complianceCategory = complianceCategory; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getPenaltyPerDay() { return penaltyPerDay; }
    public void setPenaltyPerDay(BigDecimal penaltyPerDay) { this.penaltyPerDay = penaltyPerDay; }

    public String getReferenceLink() { return referenceLink; }
    public void setReferenceLink(String referenceLink) { this.referenceLink = referenceLink; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
