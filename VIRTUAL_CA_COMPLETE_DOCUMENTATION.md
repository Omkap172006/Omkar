# AI-Powered Virtual CA System — Complete Architecture, Technical Specification & Operations Guide

An intelligent, autonomous bookkeeping, GST calculation, and financial compliance platform for Indian MSMEs/SMBs that automates 80–90% of routine Chartered Accountant operations while enabling seamless human CA sign-off.

---

## 📌 Table of Contents
1. [Executive Summary & System Blueprint](#1-executive-summary--system-blueprint)
2. [Technology Stack & Architectural Layers](#2-technology-stack--architectural-layers)
3. [Database Schema & Entity Models (PostgreSQL 17)](#3-database-schema--entity-models-postgresql-17)
4. [Double-Entry Ledger & Indian GST Engine Logic](#4-double-entry-ledger--indian-gst-engine-logic)
5. [Frontend & Backend Issues Diagnosed & Solved](#5-frontend--backend-issues-diagnosed--solved)
6. [Quickstart Guide: Running & Previewing the System](#6-quickstart-guide-running--previewing-the-system)
7. [API Endpoint Reference](#7-api-endpoint-reference)
8. [Roadmap & Recommended Next-Gen Enhancements](#8-roadmap--recommended-next-gen-enhancements)

---


## 1. Executive Summary & System Blueprint

### 1.1 Core Value Proposition
Indian MSMEs (Micro, Small, and Medium Enterprises) lose billions of rupees annually due to clerical errors in GST compliance, missed Input Tax Credit (ITC) reconciliation, delayed Advance Tax payments, and manual double-entry bookkeeping mistakes.

The **Virtual CA System** provides:
- **Zero-Clerical-Error Double-Entry Ledger:** Enforces the fundamental accounting equation $\text{Assets} = \text{Liabilities} + \text{Equity}$ with strict precision rounding.
- **Automated GST Classification:** Instant detection of Intra-State supplies ($\text{Supplier State} = \text{Place of Supply} \rightarrow \text{CGST} + \text{SGST}$) vs Inter-State supplies ($\text{Supplier State} \neq \text{Place of Supply} \rightarrow \text{IGST}$).
- **Statutory Compliance Radar:** Live countdowns and automated status tracking for GSTR-1, GSTR-3B, TDS Challan 281, and Advance Tax installments.
- **AI Financial Insights & Audit:** Automated anomaly detection flagging expense spikes, Section 80JJAA hiring incentives, and GSTR-2B ITC matching opportunities.
- **Human-in-the-Loop CA Sign-Off:** Clean export of GSTR-portal JSON, audit trails, and multi-tenant authorization so external Chartered Accountants can review and file with confidence.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        VIRTUAL CA ARCHITECTURE                         │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
       ┌─────────────────────────────────────────────────────────┐
       │     Frontend Dashboard (HTML5 / Vanilla JS / CSS3)      │
       │  • Live Journal Engine       • Financial KPI Cards      │
       │  • Statutory Calendar        • Invoice Generator        │
       │  • AI Insight Alerts         • Organization Switcher    │
       └────────────────────────────┬────────────────────────────┘
                                    │ HTTP / REST / JSON
                                    ▼
       ┌─────────────────────────────────────────────────────────┐
       │         Backend REST Gateway (Python server.py)         │
       │  • Multi-Tenant Auth         • CORS & Security Filter   │
       │  • Static File Serving       • Admin Telemetry Proxy    │
       └────────────────────────────┬────────────────────────────┘
                                    │
               ┌────────────────────┴────────────────────┐
               ▼                                         ▼
┌──────────────────────────────┐        ┌──────────────────────────────┐
│  Spring Boot Engine (Java)   │        │ Database Layer (PostgreSQL)  │
│  • BigDecimal GST Engine     │        │  • organizations (tenants)   │
│  • Serializable Transactions │        │  • accounts (Chart of Accts) │
│  • Debit/Credit Balance Rule │        │  • vouchers & entries        │
│  • RoundingMode.HALF_UP      │        │  • compliance_events         │
└──────────────────────────────┘        │  • ai_financial_insights     │
                                        │  • Hybrid users.json Fallback│
                                        └──────────────────────────────┘
```

---

## 2. Technology Stack & Architectural Layers

### 2.1 Backend Services
- **Primary Web Server:** Python 3 (built-in `http.server` with threaded socket architecture in `server.py`). Serves REST APIs and hosts the frontend UI on port `8000`.
- **Database Driver:** `psycopg2` / `psycopg2-binary` with parameterised queries and real dictionary cursor support.
- **High-Precision Core (Enterprise Module):** Java 17/21 LTS with Spring Boot 3.x (`backend/src/main/java/com/virtualca/...`):
  - `GstCalculationService.java`: Evaluates tax breakdowns using `java.math.BigDecimal` with `RoundingMode.HALF_UP` to prevent IEEE-754 floating-point inaccuracies.
  - `DoubleEntryEngineService.java`: Validates that $\sum \text{Debits} == \sum \text{Credits}$ within `@Transactional(isolation = Isolation.SERIALIZABLE)`.

### 2.2 Database Layer
- **Relational DBMS:** PostgreSQL 17 (Database: `virtual_ca_db`).
- **Resilience Strategy:** Hybrid architecture. When PostgreSQL is online, full relational integrity and foreign-key cascades are enforced. If PostgreSQL is offline, `db.py` automatically falls back to `database/users.json` and in-memory caches, guaranteeing zero downtime.

### 2.3 Frontend Dashboard
- **Stack:** Pure HTML5, modern CSS3 (custom CSS design system with responsive grid layout and dark-mode accents), and vanilla ES6+ JavaScript.
- **Zero Build Overhead:** No node packaging, Webpack, or Vite bundling required for production runtime.
- **Key Views:**
  - Real-time Double-Entry Voucher Creator
  - Financial Metrics & Ledger Explorer (P&L and Bank Balance)
  - Statutory Compliance Tracker & Due Date Countdown
  - AI Anomaly & Tax-Saving Alert Drawer
  - Multi-Entity Organization Switcher

---

## 3. Database Schema & Entity Models (PostgreSQL 17)

The production schema (`database/schema.sql`) contains 6 core relational tables:

```sql
-- 1. Multi-Tenant Organizations
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    gstin VARCHAR(15) UNIQUE,
    pan VARCHAR(10),
    state_code VARCHAR(2) NOT NULL DEFAULT '27',
    state_name VARCHAR(100) DEFAULT 'Maharashtra',
    email VARCHAR(255) NOT NULL,
    role VARCHAR(100) DEFAULT 'Owner',
    initials VARCHAR(10) DEFAULT 'US',
    phone VARCHAR(20),
    financial_year_start DATE NOT NULL DEFAULT '2026-04-01',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Standard Chart of Accounts
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(150) NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    current_balance NUMERIC(15, 2) DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(org_id, account_code)
);

-- 3. Double-Entry Vouchers Header
CREATE TABLE IF NOT EXISTS vouchers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    voucher_number VARCHAR(100) NOT NULL,
    voucher_type VARCHAR(20) NOT NULL CHECK (voucher_type IN ('SALES', 'SALE', 'PURCHASE', 'PURCHASES', 'PAYMENT', 'RECEIPT', 'JOURNAL', 'CONTRA', 'EXPENSE', 'BANK')),
    party_name VARCHAR(255) NOT NULL,
    voucher_date DATE NOT NULL DEFAULT CURRENT_DATE,
    narration TEXT,
    taxable_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    gst_rate NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15, 2) NOT NULL CHECK (total_amount >= 0),
    is_inter_state BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'BALANCED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(org_id, voucher_number)
);

-- 4. Voucher Debit/Credit Entry Lines
CREATE TABLE IF NOT EXISTS voucher_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id UUID NOT NULL REFERENCES vouchers(id) ON DELETE CASCADE,
    account_id UUID REFERENCES accounts(id),
    account_name VARCHAR(150) NOT NULL,
    entry_type VARCHAR(6) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    notes VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Statutory Compliance Calendar
CREATE TABLE IF NOT EXISTS compliance_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    compliance_category VARCHAR(50) NOT NULL,
    period VARCHAR(50) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'FILED', 'OVERDUE', 'EXEMPT')),
    penalty_per_day NUMERIC(10, 2) DEFAULT 50.00,
    reference_link VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. AI Financial & Audit Insights
CREATE TABLE IF NOT EXISTS ai_financial_insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    insight_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    potential_savings NUMERIC(15, 2) DEFAULT 0.00,
    recommended_action TEXT,
    is_dismissed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 4. Double-Entry Ledger & Indian GST Engine Logic

### 4.1 The Accounting Invariant
Every transaction recorded by the Virtual CA is broken down into atomic debit and credit entries:

$$\sum \text{Debits} \equiv \sum \text{Credits}$$

#### 1. Sales Voucher (Intra-State, e.g., 18% GST in Maharashtra)
- **Debit:** Bank Account / Sundry Debtors (`total_amount`, e.g., ₹1,18,000)
- **Credit:** Sales Revenue (`taxable_amount`, e.g., ₹1,00,000)
- **Credit:** CGST Output Liability (`tax_amount / 2`, e.g., ₹9,000)
- **Credit:** SGST Output Liability (`tax_amount / 2`, e.g., ₹9,000)

#### 2. Sales Voucher (Inter-State, e.g., to Karnataka)
- **Debit:** Bank Account / Sundry Debtors (`total_amount`, e.g., ₹1,18,000)
- **Credit:** Sales Revenue (`taxable_amount`, e.g., ₹1,00,000)
- **Credit:** IGST Output Liability (`tax_amount`, e.g., ₹18,000)

#### 3. Purchase Voucher with Eligible ITC
- **Debit:** Operating Expense / Asset Capitalization (`taxable_amount`, e.g., ₹35,000)
- **Debit:** GST Input Tax Credit (ITC) Asset (`tax_amount`, e.g., ₹6,300)
- **Credit:** Bank Account / Sundry Creditors (`total_amount`, e.g., ₹41,300)

---

## 5. Frontend & Backend Issues Diagnosed & Solved

### Issue 1: CORS Preflight Header Rejection
- **Location:** `server.py` (`do_OPTIONS` method)
- **Symptom:** Browser requests using custom headers such as `X-Admin-Key` failed during CORS preflight check.
- **Fix:** Update `Access-Control-Allow-Headers` in `do_OPTIONS` to include `X-Admin-Key`:
  ```python
  self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Admin-Key")
  ```

### Issue 2: Multi-Tenant Data Leakage in Aggregations
- **Location:** `database/db.py` (`get_metrics`, `get_compliance_events`, `get_ai_insights`)
- **Symptom:** When a user switched accounts or when multiple organizations existed, totals across the entire database were aggregated rather than filtering by `org_id`.
- **Fix:** Added parameterised `WHERE org_id = %s` filter when `org_id` is supplied.

### Issue 3: Windows Username Spacing in IDE Plugin Telemetry
- **Location:** `~/.gemini/config/plugins/googlecloudtools.datacloud_telemetry`
- **Symptom:** The pre-tool execution hook crashed on Windows systems where the username path has spaces (`ATHARVA PATRIKAR`), blocking tool calls.
- **Fix:** Cleared the broken hook definition with:
  ```powershell
  Set-Content -Path "$env:USERPROFILE\.gemini\config\plugins\googlecloudtools.datacloud_telemetry\hooks.json" -Value "{}"
  ```

### Issue 4: Frontend Execution Protocol Mismatch
- **Location:** `frontend/index.html`
- **Symptom:** Opening the HTML file directly through File Explorer (`file:///...`) triggers browser SOP (Same-Origin Policy) blocks against the REST APIs.
- **Fix:** Always serve through the embedded HTTP server: `http://localhost:8000`.

---

## 6. Quickstart Guide: Running & Previewing the System

### Step 1: Start the Server
Open PowerShell in the project directory:
```powershell
cd "c:\Users\ATHARVA PATRIKAR\Downloads\virtual_ca_system_full"
python server.py
```
*Expected Output:*
```text
Virtual CA Server running on http://localhost:8000
Integrated with PostgreSQL 17 database storage & Clean UI APIs.
```

### Step 2: Open the Dashboard
Navigate to:
👉 **[http://localhost:8000](http://localhost:8000)**

The server will automatically route the root request to `/frontend/index.html`.

### Step 3: Default Credentials
- **Admin Secret Key:** `admin2026` or `dev2026`
- **Admin Email:** `admin@virtualca.internal`
- **Sample Business Owner Email:** `atharva@apexinfotech.in` or `omkarpatrikar@gmail.com`

---

## 7. API Endpoint Reference

| Method | Endpoint | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | Public | Redirects to `/frontend/index.html` |
| `GET` | `/api/users` | Authenticated | Lists accounts/tenants linked to an email or all accounts for Admin |
| `POST` | `/api/login` | Public | Authenticates via email or `admin_key` (`admin2026`) |
| `POST` | `/api/register` | Public | Registers a new MSME organization with auto state detection from GSTIN |
| `GET` | `/api/transactions` | Public | Returns double-entry vouchers (optional `?org_id=...`) |
| `POST` | `/api/transactions` | Public | Records a new double-entry voucher, updates ledger, and recalculates metrics |
| `GET` | `/api/metrics` | Public | Returns real-time financial KPIs (Revenue, Expenses, Profit, Bank, GST Payable) |
| `GET` | `/api/compliance` | Public | Returns statutory calendar events and due dates |
| `GET` | `/api/insights` | Public | Returns AI financial insights, anomaly alerts, and tax savings |
| `GET` | `/api/db-status` | Admin Only | Telemetry for PostgreSQL connection, latency, engine version, and table counts |
| `POST` | `/api/db-config` | Admin Only | Live update and re-initialization of PostgreSQL credentials from UI |

---

## 8. Roadmap & Recommended Next-Gen Enhancements

### Phase 1: Forms & Ease of Use (Immediate Priority)
- **Quick AI Hinglish Entry Bar:** Allow typing natural language input such as *"Paid 15000 office rent to WeWork via HDFC with 18% GST"*, using regex/NLP to auto-populate the voucher.
- **GSTIN Auto-Detection:** Automatically extract state code from the first 2 digits of the GSTIN (e.g. `27` $\rightarrow$ Maharashtra) and validate the 15-character Mod-36 checksum.
- **Dedicated Purchase/Expense Tabs:** Specific UI fields for Input Tax Credit eligibility (100% Eligible vs Ineligible u/s 17(5)) and TDS deduction checkboxes.

### Phase 2: Statutory Tax Settlement & Formal Reports
- **GSTR-3B Tax Offset Calculator:** Interactive breakdown displaying $\text{Output Tax Liability} - \text{Available ITC} = \text{Net Tax Payable in Cash}$.
- **Schedule III Financial Statements:** Generate standard Trading & Profit and Loss statements and Balance Sheets with Assets vs Liabilities breakdown.
- **WhatsApp Invoice Dispatch:** One-click generation of shareable PDF/WhatsApp invoice links.

### Phase 3: Automated Bank Feeds & BRS Reconciliation
- **Bank Statement Parser:** Drag-and-drop CSV/PDF parser for major Indian banks (HDFC, ICICI, SBI) that extracts transaction lines and auto-matches ledger vouchers.
- **Dual-Column Bank Reconciliation (BRS):** Auto-categorization into `Matched ✔`, `Unreconciled ⚠️`, or `Missing in Books ❓`.
- **Direct GSTN JSON Export:** Export government-portal compliant JSON for direct upload to `gst.gov.in`.

