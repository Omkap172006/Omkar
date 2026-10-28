package com.virtualca.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_financial_insights")
public class AiFinancialInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "org_id")
    @JsonProperty("org_id")
    private UUID orgId;

    @Column(name = "insight_type", nullable = false, length = 50)
    @JsonProperty("insight_type")
    private String insightType;

    @Column(nullable = false, length = 20)
    private String severity; // INFO, LOW, MEDIUM, HIGH, CRITICAL

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "potential_savings", precision = 15, scale = 2)
    @JsonProperty("potential_savings")
    private BigDecimal potentialSavings = BigDecimal.ZERO.setScale(2);

    @Column(name = "recommended_action", length = 2000)
    @JsonProperty("recommended_action")
    private String recommendedAction;

    @Column(name = "is_dismissed")
    @JsonProperty("is_dismissed")
    private boolean isDismissed = false;

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private Instant createdAt = Instant.now();

    public AiFinancialInsight() {}

    public AiFinancialInsight(UUID orgId, String insightType, String severity, String title, String description, BigDecimal potentialSavings, String recommendedAction) {
        this.orgId = orgId;
        this.insightType = insightType;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.potentialSavings = potentialSavings != null ? potentialSavings : BigDecimal.ZERO;
        this.recommendedAction = recommendedAction;
        this.isDismissed = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getInsightType() { return insightType; }
    public void setInsightType(String insightType) { this.insightType = insightType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPotentialSavings() { return potentialSavings; }
    public void setPotentialSavings(BigDecimal potentialSavings) { this.potentialSavings = potentialSavings; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }

    public boolean isDismissed() { return isDismissed; }
    public void setDismissed(boolean dismissed) { isDismissed = dismissed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
