package com.virtualca.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    private String company;
    private String gstin;
    private String role;
    private String businessCategory = "Retail & E-commerce";
    private String businessSubcategory = "General retail";
    private String country = "India";
    private String language = "mr";
    private String currencyCode = "INR";
    private String currencySymbol = "₹";
    private String locale = "mr-IN";
    private String taxSystem = "Indian GST";

    public RegisterRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getBusinessCategory() { return businessCategory; }
    public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }

    public String getBusinessSubcategory() { return businessSubcategory; }
    public void setBusinessSubcategory(String businessSubcategory) { this.businessSubcategory = businessSubcategory; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getTaxSystem() { return taxSystem; }
    public void setTaxSystem(String taxSystem) { this.taxSystem = taxSystem; }
}

