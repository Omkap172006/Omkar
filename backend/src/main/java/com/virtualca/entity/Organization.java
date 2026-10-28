package com.virtualca.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "trade_name")
    @JsonProperty("trade_name")
    private String tradeName;

    @Column(unique = true, length = 15)
    private String gstin;

    @Column(length = 10)
    private String pan;

    @Column(name = "state_code", nullable = false, length = 2)
    @JsonProperty("state_code")
    private String stateCode = "27";

    @Column(name = "state_name", length = 100)
    @JsonProperty("state_name")
    private String stateName = "Maharashtra";

    @Column(nullable = false)
    private String email;

    @Column(length = 100)
    private String role = "Owner";

    @Column(length = 10)
    private String initials = "US";

    @Column(length = 20)
    private String phone;

    @Column(name = "country", length = 50)
    @JsonProperty("country")
    private String country = "India";

    @Column(name = "business_category", length = 100)
    @JsonProperty("business_category")
    private String businessCategory = "Retail & E-commerce";

    @Column(name = "business_subcategory", length = 100)
    @JsonProperty("business_subcategory")
    private String businessSubcategory = "General retail";

    @Column(name = "currency_code", length = 10)
    @JsonProperty("currency_code")
    private String currencyCode = "INR";

    @Column(name = "currency_symbol", length = 5)
    @JsonProperty("currency_symbol")
    private String currencySymbol = "₹";

    @Column(name = "locale", length = 20)
    @JsonProperty("locale")
    private String locale = "mr-IN";

    @Column(name = "language", length = 10)
    @JsonProperty("language")
    private String language = "mr";

    @Column(name = "tax_system", length = 50)
    @JsonProperty("tax_system")
    private String taxSystem = "Indian GST";

    @Column(name = "financial_year_start", nullable = false)
    @JsonProperty("financial_year_start")
    private LocalDate financialYearStart = LocalDate.of(2026, 4, 1);

    @Column(name = "created_at")
    @JsonProperty("created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private Instant updatedAt = Instant.now();

    public Organization() {}

    public Organization(String name, String tradeName, String gstin, String pan, String stateCode, String stateName, String email, String role, String initials) {
        this.name = name;
        this.tradeName = tradeName;
        this.gstin = gstin;
        this.pan = pan;
        this.stateCode = (stateCode != null && !stateCode.isBlank()) ? stateCode : "27";
        this.stateName = (stateName != null && !stateName.isBlank()) ? stateName : "Maharashtra";
        this.email = email;
        this.role = (role != null && !role.isBlank()) ? role : "Owner";
        this.initials = (initials != null && !initials.isBlank()) ? initials : "US";
        this.country = "India";
        this.businessCategory = "Retail & E-commerce";
        this.businessSubcategory = "General retail";
        this.currencyCode = "INR";
        this.currencySymbol = "₹";
        this.locale = "mr-IN";
        this.language = "mr";
        this.taxSystem = "Indian GST";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }

    // Frontend alias for company
    @JsonProperty("company")
    public String getCompany() { return tradeName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    // Frontend alias for stateCode
    @JsonProperty("stateCode")
    public String getStateCodeCamel() { return stateCode; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    // Frontend alias for stateName
    @JsonProperty("stateName")
    public String getStateNameCamel() { return stateName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getBusinessCategory() { return businessCategory; }
    public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }

    @JsonProperty("businessCategory")
    public String getBusinessCategoryCamel() { return businessCategory; }

    public String getBusinessSubcategory() { return businessSubcategory; }
    public void setBusinessSubcategory(String businessSubcategory) { this.businessSubcategory = businessSubcategory; }

    @JsonProperty("businessSubcategory")
    public String getBusinessSubcategoryCamel() { return businessSubcategory; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    @JsonProperty("currencyCode")
    public String getCurrencyCodeCamel() { return currencyCode; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }

    @JsonProperty("currencySymbol")
    public String getCurrencySymbolCamel() { return currencySymbol; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTaxSystem() { return taxSystem; }
    public void setTaxSystem(String taxSystem) { this.taxSystem = taxSystem; }

    @JsonProperty("taxSystem")
    public String getTaxSystemCamel() { return taxSystem; }

    public LocalDate getFinancialYearStart() { return financialYearStart; }
    public void setFinancialYearStart(LocalDate financialYearStart) { this.financialYearStart = financialYearStart; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
