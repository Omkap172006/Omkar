# AI-Powered "Virtual CA" System — Architecture & MVP Implementation

An intelligent, autonomous bookkeeping, GST calculation, and financial compliance platform for Indian MSMEs/SMBs that automates 80–90% of routine Chartered Accountant operations while enabling seamless human CA sign-off.

---

## 📂 Project Directory Structure

```
d:/Codes/virtual-ca-system/
├── database/
│   └── schema.sql                  # Production PostgreSQL DDL (Double-Entry, Invoices, Compliance, AI Insights)
├── backend/
│   └── src/main/java/com/virtualca/
│       ├── tax/service/
│       │   └── GstCalculationService.java    # Strict BigDecimal Indian GST calculation (CGST/SGST vs IGST)
│       └── ledger/service/
│           └── DoubleEntryEngineService.java # Transactional Double-entry ledger (Debits = Credits)
└── frontend/
    └── index.html                  # Interactive P0 MVP Web Dashboard (Live Journal Engine, P&L, Calendar, Invoice)
```

---

## ⚙️ Core Architecture & Blueprint Modules

### 1. Data Flow & Processing Engine
* **Double-Entry Engine:** Enforces the fundamental accounting equation ($\text{Assets} = \text{Liabilities} + \text{Equity}$) where every transaction generates balanced debit/credit lines.
* **GST Engine:** Automatically differentiates Intra-State (Supplier State == Place of Supply $\rightarrow$ CGST + SGST) from Inter-State supplies (Supplier State $\neq$ Place of Supply $\rightarrow$ IGST).
* **Statutory Compliance Calendar:** Tracks GSTR-1, GSTR-3B, TDS Challan 281, and Advance Tax deadlines with automated status tracking and penalty risk indicators.
* **AI Anomaly & Insights Engine:** Evaluates rolling expenditure patterns to flag expense spikes and identifies tax-saving incentives (e.g. depreciation claims, Sec 80JJAA deductions).

---

## 🚀 Running the Interactive MVP

You can test the prototype directly by opening `frontend/index.html` in any web browser:
```powershell
Start-Process "d:\Codes\virtual-ca-system\frontend\index.html"
```
Or start a local static server:
```powershell
npx -y serve "d:\Codes\virtual-ca-system\frontend"
```

---

## 🗄️ Database Setup (PostgreSQL)

Execute the production schema against your local or cloud PostgreSQL database:
```bash
psql -U postgres -d virtual_ca_db -f d:/Codes/virtual-ca-system/database/schema.sql
```
This provisions:
* `organizations` (multi-tenant support)
* `accounts` (Chart of Accounts: Asset, Liability, Equity, Revenue, Expense)
* `vouchers` & `voucher_entries` (Double-entry journal lines)
* `invoices` & `invoice_items` (GST-compliant invoice structure)
* `compliance_events` (Statutory compliance calendar)
* `ai_financial_insights` (Automated anomaly tracking)

---

## ☕ Spring Boot Java Backend Implementation

Built with **Java 17/21 LTS** and **Spring Boot 3.x**:
* **Type Safety:** High-precision monetary computation via `java.math.BigDecimal` with `RoundingMode.HALF_UP` (never `float`/`double`).
* **ACID Transactions:** Enforced with `@Transactional(isolation = Isolation.SERIALIZABLE)` so that any unbalanced voucher entry triggers an immediate database rollback.
