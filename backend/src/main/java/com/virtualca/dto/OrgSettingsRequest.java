package com.virtualca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class OrgSettingsRequest {

    @JsonProperty("org_id")
    private UUID orgId;

    @JsonProperty("business_category")
    private String businessCategory;

    @JsonProperty("business_subcategory")
    private String businessSubcategory;

    @JsonProperty("country")
    private String country;

    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("currency_symbol")
    private String currencySymbol;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("language")
    private String language;

    @JsonProperty("tax_system")
    private String taxSystem;

    public OrgSettingsRequest() {}

    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }

    public String getBusinessCategory() { return businessCategory; }
    public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }

    public String getBusinessSubcategory() { return businessSubcategory; }
    public void setBusinessSubcategory(String businessSubcategory) { this.businessSubcategory = businessSubcategory; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTaxSystem() { return taxSystem; }
    public void setTaxSystem(String taxSystem) { this.taxSystem = taxSystem; }
}
