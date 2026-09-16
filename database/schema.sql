-- =====================================================================
-- AI-Powered Virtual CA System: Production PostgreSQL Database Schema
-- Modules: Chart of Accounts, Double-Entry Ledger, Invoicing, Tax, Compliance
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Organizations / Businesses (Tenancy)
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    gstin VARCHAR(15) UNIQUE,
    pan VARCHAR(10) NOT NULL,
    state_code VARCHAR(2) NOT NULL, -- E.g. '27' for Maharashtra, '07' for Delhi
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    financial_year_start DATE NOT NULL DEFAULT '2026-04-01',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Chart of Accounts (COA)
-- Account Types: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(150) NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    parent_id UUID REFERENCES accounts(id),
    is_active BOOLEAN DEFAULT TRUE,
    current_balance NUMERIC(15, 2) DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(org_id, account_code)
);

-- 3. Double-Entry Vouchers (Header)
-- Voucher Types: SALES, PURCHASE, PAYMENT, RECEIPT, JOURNAL, CONTRA
CREATE TABLE IF NOT EXISTS vouchers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    voucher_number VARCHAR(100) NOT NULL,
    voucher_type VARCHAR(20) NOT NULL CHECK (voucher_type IN ('SALES', 'PURCHASE', 'PAYMENT', 'RECEIPT', 'JOURNAL', 'CONTRA')),
    voucher_date DATE NOT NULL,
    narration TEXT,
    reference_no VARCHAR(100), -- Invoice number or bank ref
    total_amount NUMERIC(15, 2) NOT NULL CHECK (total_amount >= 0),
    is_posted BOOLEAN DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(org_id, voucher_number)
);

-- 4. Voucher Entries (Debit & Credit Lines)
-- Constraint: In every voucher, SUM(debit) MUST EQUAL SUM(credit)
CREATE TABLE IF NOT EXISTS voucher_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    voucher_id UUID NOT NULL REFERENCES vouchers(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id),
    entry_type VARCHAR(6) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    notes VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. GST Compliant Invoices
CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    invoice_number VARCHAR(100) NOT NULL,
    invoice_type VARCHAR(20) NOT NULL CHECK (invoice_type IN ('TAX_INVOICE', 'BILL_OF_SUPPLY', 'CREDIT_NOTE', 'DEBIT_NOTE')),
    party_name VARCHAR(255) NOT NULL,
    party_gstin VARCHAR(15),
    party_state_code VARCHAR(2) NOT NULL,
    place_of_supply VARCHAR(2) NOT NULL,
    is_inter_state BOOLEAN NOT NULL DEFAULT FALSE,
    invoice_date DATE NOT NULL,
    due_date DATE,
    subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    cgst_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    sgst_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    igst_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    grand_total NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'UNPAID' CHECK (status IN ('DRAFT', 'ISSUED', 'PAID', 'PARTIALLY_PAID', 'CANCELLED')),
    voucher_id UUID REFERENCES vouchers(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(org_id, invoice_number)
);

-- 6. Invoice Line Items
CREATE TABLE IF NOT EXISTS invoice_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    item_description VARCHAR(255) NOT NULL,
    hsn_sac_code VARCHAR(10),
    quantity NUMERIC(12, 3) NOT NULL DEFAULT 1.0,
    unit_price NUMERIC(15, 2) NOT NULL,
    discount_amount NUMERIC(15, 2) DEFAULT 0.00,
    taxable_amount NUMERIC(15, 2) NOT NULL,
    gst_rate NUMERIC(5, 2) NOT NULL CHECK (gst_rate IN (0, 5, 12, 18, 28)),
    cgst_rate NUMERIC(5, 2) DEFAULT 0.00,
    cgst_amount NUMERIC(15, 2) DEFAULT 0.00,
    sgst_rate NUMERIC(5, 2) DEFAULT 0.00,
    sgst_amount NUMERIC(15, 2) DEFAULT 0.00,
    igst_rate NUMERIC(5, 2) DEFAULT 0.00,
    igst_amount NUMERIC(15, 2) DEFAULT 0.00,
    total_line_amount NUMERIC(15, 2) NOT NULL
);

-- 7. Statutory Compliance Calendar
CREATE TABLE IF NOT EXISTS compliance_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    compliance_category VARCHAR(50) NOT NULL, -- GST, TDS, ADVANCE_TAX, ROC, ITR
    period VARCHAR(50) NOT NULL, -- E.g. 'August 2026', 'Q1 FY 26-27'
    due_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'FILED', 'OVERDUE', 'EXEMPT')),
    penalty_per_day NUMERIC(10, 2) DEFAULT 50.00,
    reference_link VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. AI Insights & Anomaly Logs
CREATE TABLE IF NOT EXISTS ai_financial_insights (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    insight_type VARCHAR(50) NOT NULL, -- EXPENSE_ANOMALY, TAX_SAVING, CASH_FLOW_ALERT, ITC_MISMATCH
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    potential_savings NUMERIC(15, 2) DEFAULT 0.00,
    recommended_action TEXT,
    is_dismissed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_vouchers_org_date ON vouchers(org_id, voucher_date);
CREATE INDEX IF NOT EXISTS idx_voucher_entries_account ON voucher_entries(account_id);
CREATE INDEX IF NOT EXISTS idx_invoices_org_date ON invoices(org_id, invoice_date);
CREATE INDEX IF NOT EXISTS idx_compliance_due ON compliance_events(due_date, status);
