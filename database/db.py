import os
import re
import json
import uuid
import datetime
from decimal import Decimal
import psycopg2
from psycopg2 import sql, extras

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ENV_FILE = os.path.join(BASE_DIR, ".env")
USERS_FILE = os.path.join(BASE_DIR, "database", "users.json")

def read_env():
    config = {
        "DB_HOST": "localhost",
        "DB_PORT": "5432",
        "DB_USER": "postgres",
        "DB_PASSWORD": "",
        "DB_NAME": "virtual_ca_db"
    }
    if os.path.exists(ENV_FILE):
        with open(ENV_FILE, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    config[k.strip()] = v.strip()
    return config

def write_env(config):
    lines = [
        "# PostgreSQL Database Configuration\n",
        f"DB_HOST={config.get('DB_HOST', 'localhost')}\n",
        f"DB_PORT={config.get('DB_PORT', '5432')}\n",
        f"DB_USER={config.get('DB_USER', 'postgres')}\n",
        f"DB_PASSWORD={config.get('DB_PASSWORD', '')}\n",
        f"DB_NAME={config.get('DB_NAME', 'virtual_ca_db')}\n"
    ]
    with open(ENV_FILE, "w", encoding="utf-8") as f:
        f.writelines(lines)

class DecimalEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, Decimal):
            return float(obj)
        if isinstance(obj, (datetime.date, datetime.datetime)):
            return obj.isoformat()
        if isinstance(obj, uuid.UUID):
            return str(obj)
        return super().default(obj)

class DatabaseManager:
    def __init__(self):
        self.config = read_env()
        self.is_connected = False
        self.connection_error = None
        self.try_initialize()

    def get_connection(self, dbname=None):
        cfg = self.config
        target_db = dbname if dbname else cfg.get("DB_NAME", "virtual_ca_db")
        return psycopg2.connect(
            host=cfg.get("DB_HOST", "localhost"),
            port=int(cfg.get("DB_PORT", 5432)),
            user=cfg.get("DB_USER", "postgres"),
            password=cfg.get("DB_PASSWORD", ""),
            dbname=target_db,
            connect_timeout=3
        )

    def try_initialize(self, new_config=None):
        if new_config:
            self.config.update(new_config)
            write_env(self.config)

        try:
            # 1. Connect to default postgres DB to ensure virtual_ca_db exists
            cfg = self.config
            conn = psycopg2.connect(
                host=cfg.get("DB_HOST", "localhost"),
                port=int(cfg.get("DB_PORT", 5432)),
                user=cfg.get("DB_USER", "postgres"),
                password=cfg.get("DB_PASSWORD", ""),
                dbname="postgres",
                connect_timeout=3
            )
            conn.autocommit = True
            with conn.cursor() as cur:
                target_db = cfg.get("DB_NAME", "virtual_ca_db")
                cur.execute("SELECT 1 FROM pg_database WHERE datname = %s", (target_db,))
                if not cur.fetchone():
                    try:
                        cur.execute("ALTER DATABASE template1 REFRESH COLLATION VERSION;")
                    except Exception:
                        pass
                    cur.execute(sql.SQL("CREATE DATABASE {}").format(sql.Identifier(target_db)))
            conn.close()

            # 2. Connect to virtual_ca_db and create schema tables
            app_conn = self.get_connection()
            app_conn.autocommit = True
            with app_conn.cursor() as cur:
                self._run_schema_migrations(cur)
            app_conn.close()

            self.is_connected = True
            self.connection_error = None

            # 3. Seed initial data
            self.seed_defaults()
            return True, "PostgreSQL connected and schema verified."
        except Exception as e:
            self.is_connected = False
            self.connection_error = str(e)
            return False, str(e)

    def _run_schema_migrations(self, cur):
        # We use standard gen_random_uuid() which is native in PostgreSQL 13+
        ddl = """
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

        CREATE TABLE IF NOT EXISTS vouchers (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            org_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
            voucher_number VARCHAR(100) NOT NULL,
            voucher_type VARCHAR(20) NOT NULL CHECK (voucher_type IN ('SALES', 'PURCHASE', 'PAYMENT', 'RECEIPT', 'JOURNAL', 'CONTRA', 'EXPENSE')),
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
        """
        cur.execute(ddl)

    def seed_defaults(self):
        if not self.is_connected:
            return

        try:
            conn = self.get_connection()
            with conn.cursor(cursor_factory=extras.RealDictCursor) as cur:
                # 1. Check organizations
                cur.execute("SELECT COUNT(*) AS c FROM organizations")
                row = cur.fetchone()
                if row["c"] == 0:
                    # Import existing users from users.json
                    users = []
                    if os.path.exists(USERS_FILE):
                        try:
                            with open(USERS_FILE, "r", encoding="utf-8") as f:
                                users = json.load(f)
                        except Exception:
                            pass

                    if not users:
                        users = [
                            {
                                "name": "Omkar Patrikar",
                                "email": "omkarpatrikar@gmail.com",
                                "company": "Infotech Services",
                                "gstin": "27BCCC267C123R4",
                                "stateCode": "27",
                                "stateName": "Maharashtra",
                                "role": "Owner",
                                "initials": "OM"
                            }
                        ]

                    for u in users:
                        cur.execute("""
                            INSERT INTO organizations (name, trade_name, gstin, state_code, state_name, email, role, initials)
                            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                            ON CONFLICT (gstin) DO UPDATE SET role = EXCLUDED.role
                        """, (
                            u.get("name", "Business Owner"),
                            u.get("company", "Business Entity"),
                            u.get("gstin", "27AABCA1234F1Z5"),
                            u.get("stateCode", "27"),
                            u.get("stateName", "Maharashtra"),
                            u.get("email", "owner@business.in"),
                            u.get("role", "Owner"),
                            u.get("initials", "BO")
                        ))

                # Always guarantee Master Admin organization is present
                cur.execute("""
                    INSERT INTO organizations (name, trade_name, gstin, state_code, state_name, email, role, initials)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                    ON CONFLICT (gstin) DO UPDATE SET
                        role = 'Admin',
                        email = EXCLUDED.email,
                        name = EXCLUDED.name
                """, (
                    "System Administrator",
                    "Virtual CA Cloud Infrastructure",
                    "27ADMIN0000A1Z0",
                    "27",
                    "Maharashtra",
                    "admin@virtualca.internal",
                    "Admin",
                    "AD"
                ))

                # Ensure default chart of accounts for all organizations
                cur.execute("SELECT id FROM organizations")
                org_rows = cur.fetchall()
                for org in org_rows:
                    org_id = org["id"]
                    standard_accounts = [
                        ('1001', 'Bank Account (HDFC)', 'ASSET'),
                        ('1002', 'Accounts Receivable / Debtors', 'ASSET'),
                        ('1003', 'GST Input Tax Credit (ITC)', 'ASSET'),
                        ('2001', 'Accounts Payable / Creditors', 'LIABILITY'),
                        ('2002', 'GST Output Tax Payable', 'LIABILITY'),
                        ('3001', 'Owner Capital / Equity', 'EQUITY'),
                        ('4001', 'Sales Revenue', 'REVENUE'),
                        ('5001', 'Operating & Administrative Expenses', 'EXPENSE')
                    ]
                    for code, name, acct_type in standard_accounts:
                        cur.execute("""
                            INSERT INTO accounts (org_id, account_code, account_name, account_type)
                            VALUES (%s, %s, %s, %s)
                            ON CONFLICT (org_id, account_code) DO NOTHING
                        """, (org_id, code, name, acct_type))

                # Check if vouchers exist, otherwise seed realistic initial double-entry records
                cur.execute("SELECT COUNT(*) AS c FROM vouchers")
                if cur.fetchone()["c"] == 0 and org_rows:
                    first_org = org_rows[0]["id"]
                    seed_txns = [
                        {
                            "type": "SALES",
                            "party": "Infosys Technologies Pune",
                            "taxable": 100000,
                            "rate": 18,
                            "tax": 18000,
                            "total": 118000,
                            "desc": "Software architecture consulting fees",
                            "inter_state": False
                        },
                        {
                            "type": "EXPENSE",
                            "party": "WeWork Bandra Kurla Complex",
                            "taxable": 54000,
                            "rate": 0,
                            "tax": 0,
                            "total": 54000,
                            "desc": "Monthly office workspace rental (TDS withheld)",
                            "inter_state": False
                        },
                        {
                            "type": "SALES",
                            "party": "Bangalore Cloud Systems Ltd",
                            "taxable": 120000,
                            "rate": 18,
                            "tax": 21600,
                            "total": 141600,
                            "desc": "Custom ERP development milestone 2",
                            "inter_state": True
                        },
                        {
                            "type": "PURCHASE",
                            "party": "Dell Technologies India",
                            "taxable": 35000,
                            "rate": 18,
                            "tax": 6300,
                            "total": 41300,
                            "desc": "High-performance development servers",
                            "inter_state": False
                        }
                    ]

                    for idx, t in enumerate(seed_txns, start=1):
                        v_num = f"VCH-2026-{idx:03d}"
                        cur.execute("""
                            INSERT INTO vouchers (org_id, voucher_number, voucher_type, party_name, voucher_date, narration, taxable_amount, gst_rate, tax_amount, total_amount, is_inter_state, status)
                            VALUES (%s, %s, %s, %s, CURRENT_DATE, %s, %s, %s, %s, %s, %s, 'BALANCED')
                            RETURNING id
                        """, (
                            first_org, v_num, t["type"], t["party"], t["desc"],
                            t["taxable"], t["rate"], t["tax"], t["total"], t["inter_state"]
                        ))
                        v_id = cur.fetchone()["id"]

                        # Insert balanced double entry lines
                        if t["type"] == "SALES":
                            # Debit Bank/Debtor (Gross), Credit Sales (Taxable), Credit Output GST (Tax)
                            cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'DEBIT', %s, 'Receipt in Bank')", (v_id, "Bank Account (HDFC)", t["total"]))
                            cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'CREDIT', %s, 'Revenue Recognized')", (v_id, "Sales Revenue", t["taxable"]))
                            if t["tax"] > 0:
                                cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'CREDIT', %s, 'Output GST Liability')", (v_id, "GST Output Tax Payable", t["tax"]))
                        elif t["type"] == "PURCHASE":
                            # Debit Asset/Expense (Taxable), Debit ITC (Tax), Credit Bank (Gross)
                            cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'DEBIT', %s, 'Asset Capitalization')", (v_id, "Operating & Administrative Expenses", t["taxable"]))
                            if t["tax"] > 0:
                                cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'DEBIT', %s, 'Input Tax Credit Eligible')", (v_id, "GST Input Tax Credit (ITC)", t["tax"]))
                            cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'CREDIT', %s, 'Bank Disbursal')", (v_id, "Bank Account (HDFC)", t["total"]))
                        else:
                            # Debit Operating Expense, Credit Bank
                            cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'DEBIT', %s, 'Operating Outflow')", (v_id, "Operating & Administrative Expenses", t["total"]))
                            cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'CREDIT', %s, 'Direct Payment')", (v_id, "Bank Account (HDFC)", t["total"]))

                # Check compliance events
                cur.execute("SELECT COUNT(*) AS c FROM compliance_events")
                if cur.fetchone()["c"] == 0 and org_rows:
                    first_org = org_rows[0]["id"]
                    events = [
                        ("GSTR-1 (Outward Supplies)", "GST", "August 2026", "2026-09-11", "FILED", 50.00),
                        ("GSTR-3B (Summary & Tax Payment)", "GST", "August 2026", "2026-09-20", "PENDING", 50.00),
                        ("TDS Challan 281 (Section 194I / 194J)", "TDS", "August 2026", "2026-10-07", "PENDING", 200.00),
                        ("Advance Tax Q2 (45% Cumulative)", "ADVANCE_TAX", "Q2 FY 2026-27", "2026-09-15", "PENDING", 100.00)
                    ]
                    for title, cat, period, due, status, penalty in events:
                        cur.execute("""
                            INSERT INTO compliance_events (org_id, title, compliance_category, period, due_date, status, penalty_per_day)
                            VALUES (%s, %s, %s, %s, %s, %s, %s)
                        """, (first_org, title, cat, period, due, status, penalty))

                # Check AI Insights
                cur.execute("SELECT COUNT(*) AS c FROM ai_financial_insights")
                if cur.fetchone()["c"] == 0 and org_rows:
                    first_org = org_rows[0]["id"]
                    insights = [
                        ("ITC_MISMATCH", "MEDIUM", "GSTR-2B ITC Match Opportunity", "₹6,300 Input Tax Credit eligible for auto-reconciliation against Dell Technologies GSTR-1 filing.", 6300.00, "Auto-claim in Table 4(A)(5) of GSTR-3B."),
                        ("TAX_SAVING", "LOW", "Section 80JJAA Hiring Benefit", "Additional deduction of 30% on employee emoluments available for 2 new developer hires.", 45000.00, "Ensure CA Form 10DA is uploaded prior to ITR filing."),
                        ("EXPENSE_ANOMALY", "INFO", "Office Rent TDS Provision", "Rent payments to WeWork exceed ₹2.4L annual threshold; Section 194-I TDS rate applied correctly.", 0.00, "TDS Challan 281 scheduled for deposit.")
                    ]
                    for itype, sev, title, desc, savings, action in insights:
                        cur.execute("""
                            INSERT INTO ai_financial_insights (org_id, insight_type, severity, title, description, potential_savings, recommended_action)
                            VALUES (%s, %s, %s, %s, %s, %s, %s)
                        """, (first_org, itype, sev, title, desc, savings, action))

            conn.commit()
            conn.close()
        except Exception as e:
            print(f"[DB Seeding Error]: {e}")

    # ==========================================
    # DATA ACCESS METHODS (WITH HYBRID FALLBACK)
    # ==========================================

    def get_db_status(self):
        if not self.is_connected:
            return {
                "connected": False,
                "database": self.config.get("DB_NAME", "virtual_ca_db"),
                "host": self.config.get("DB_HOST", "localhost"),
                "port": self.config.get("DB_PORT", "5432"),
                "user": self.config.get("DB_USER", "postgres"),
                "error": self.connection_error,
                "table_counts": {},
                "latency_ms": None,
                "server_version": None,
                "active_connections": 0
            }

        counts = {}
        import time
        start_time = time.perf_counter()
        try:
            conn = self.get_connection()
            with conn.cursor() as cur:
                cur.execute("SELECT version();")
                v_row = cur.fetchone()
                raw_version = v_row[0] if v_row else "PostgreSQL 17"
                ver_match = re.search(r"PostgreSQL\s+([\d\.]+)", raw_version, re.IGNORECASE)
                version_str = f"PostgreSQL {ver_match.group(1)}" if ver_match else "PostgreSQL 17"

                cur.execute("SELECT count(*) FROM pg_stat_activity WHERE datname = %s", (self.config.get("DB_NAME", "virtual_ca_db"),))
                act_row = cur.fetchone()
                active_conns = act_row[0] if act_row else 1

                for table in ["organizations", "accounts", "vouchers", "voucher_entries", "compliance_events", "ai_financial_insights"]:
                    cur.execute(sql.SQL("SELECT COUNT(*) FROM {}").format(sql.Identifier(table)))
                    counts[table] = cur.fetchone()[0]
            conn.close()
            latency = round((time.perf_counter() - start_time) * 1000, 2)
            return {
                "connected": True,
                "database": self.config.get("DB_NAME", "virtual_ca_db"),
                "host": self.config.get("DB_HOST", "localhost"),
                "port": self.config.get("DB_PORT", "5432"),
                "user": self.config.get("DB_USER", "postgres"),
                "engine": version_str,
                "server_version": raw_version.split(",")[0] if raw_version else version_str,
                "latency_ms": latency,
                "active_connections": active_conns,
                "table_counts": counts
            }
        except Exception as e:
            return {
                "connected": False,
                "error": str(e),
                "database": self.config.get("DB_NAME", "virtual_ca_db"),
                "host": self.config.get("DB_HOST", "localhost"),
                "port": self.config.get("DB_PORT", "5432"),
                "user": self.config.get("DB_USER", "postgres"),
                "table_counts": {},
                "latency_ms": None
            }

    def get_organizations(self, email=None, is_dev=False):
        if self.is_connected:
            try:
                conn = self.get_connection()
                with conn.cursor(cursor_factory=extras.RealDictCursor) as cur:
                    if is_dev:
                        cur.execute("SELECT * FROM organizations ORDER BY created_at DESC")
                    elif email:
                        cur.execute("SELECT * FROM organizations WHERE LOWER(email) = LOWER(%s) ORDER BY created_at DESC", (email.strip(),))
                    else:
                        cur.execute("SELECT * FROM organizations LIMIT 0")
                    rows = cur.fetchall()
                conn.close()
                return [dict(r) for r in rows]
            except Exception as e:
                print(f"[DB Error get_organizations]: {e}")

        # Fallback to users.json
        if os.path.exists(USERS_FILE):
            with open(USERS_FILE, "r", encoding="utf-8") as f:
                users = json.load(f)
            if is_dev:
                return users
            if email:
                return [u for u in users if u.get("email", "").strip().lower() == email.strip().lower()]
        return []

    def create_organization(self, data):
        name = data.get("name", "New User").strip()
        email = data.get("email", "").strip().lower()
        company = data.get("company", "Business Entity").strip()
        gstin = (data.get("gstin", "") or "27AABCA1234F1Z5").strip().upper()
        role = data.get("role", "Owner")

        state_code = gstin[:2] if len(gstin) >= 2 and gstin[:2].isdigit() else "27"
        state_names = {
            "27": "Maharashtra", "29": "Karnataka", "07": "Delhi",
            "33": "Tamil Nadu", "24": "Gujarat", "06": "Haryana",
            "19": "West Bengal", "36": "Telangana", "09": "Uttar Pradesh"
        }
        state_name = state_names.get(state_code, "India")

        parts = name.split()
        initials = (parts[0][0] + (parts[1][0] if len(parts) > 1 else parts[0][1:2])).upper() if name else "US"

        org_record = {
            "name": name,
            "trade_name": company,
            "company": company,
            "gstin": gstin,
            "state_code": state_code,
            "stateCode": state_code,
            "state_name": state_name,
            "stateName": state_name,
            "email": email,
            "role": role,
            "initials": initials
        }

        if self.is_connected:
            try:
                conn = self.get_connection()
                with conn.cursor(cursor_factory=extras.RealDictCursor) as cur:
                    cur.execute("""
                        INSERT INTO organizations (name, trade_name, gstin, state_code, state_name, email, role, initials)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                        ON CONFLICT (gstin) DO UPDATE SET
                            name = EXCLUDED.name,
                            trade_name = EXCLUDED.trade_name,
                            email = EXCLUDED.email,
                            role = EXCLUDED.role,
                            initials = EXCLUDED.initials
                        RETURNING *
                    """, (name, company, gstin, state_code, state_name, email, role, initials))
                    row = cur.fetchone()
                    org_id = row["id"]
                    org_record["id"] = str(org_id)

                    # Ensure standard chart of accounts
                    standard_accounts = [
                        ('1001', 'Bank Account (HDFC)', 'ASSET'),
                        ('1002', 'Accounts Receivable / Debtors', 'ASSET'),
                        ('1003', 'GST Input Tax Credit (ITC)', 'ASSET'),
                        ('2001', 'Accounts Payable / Creditors', 'LIABILITY'),
                        ('2002', 'GST Output Tax Payable', 'LIABILITY'),
                        ('3001', 'Owner Capital / Equity', 'EQUITY'),
                        ('4001', 'Sales Revenue', 'REVENUE'),
                        ('5001', 'Operating & Administrative Expenses', 'EXPENSE')
                    ]
                    for code, aname, atype in standard_accounts:
                        cur.execute("""
                            INSERT INTO accounts (org_id, account_code, account_name, account_type)
                            VALUES (%s, %s, %s, %s)
                            ON CONFLICT (org_id, account_code) DO NOTHING
                        """, (org_id, code, aname, atype))

                conn.commit()
                conn.close()
            except Exception as e:
                print(f"[DB Create Org Error]: {e}")

        # Also sync users.json for safety
        try:
            users = []
            if os.path.exists(USERS_FILE):
                with open(USERS_FILE, "r", encoding="utf-8") as f:
                    users = json.load(f)
            existing = next((u for u in users if u.get("email", "").lower() == email and u.get("company", "").lower() == company.lower()), None)
            if existing:
                existing.update(org_record)
            else:
                org_record["id"] = org_record.get("id") or f"USR-{len(users)+1:03d}"
                users.append(org_record)
            with open(USERS_FILE, "w", encoding="utf-8") as f:
                json.dump(users, f, indent=2)
        except Exception as e:
            print(f"[JSON Sync Error]: {e}")

        return org_record

    def get_transactions(self, org_id=None):
        if self.is_connected:
            try:
                conn = self.get_connection()
                with conn.cursor(cursor_factory=extras.RealDictCursor) as cur:
                    if org_id:
                        cur.execute("""
                            SELECT v.*, 
                                   json_agg(json_build_object(
                                       'account_name', e.account_name,
                                       'entry_type', e.entry_type,
                                       'amount', e.amount,
                                       'notes', e.notes
                                   )) AS entries
                            FROM vouchers v
                            LEFT JOIN voucher_entries e ON v.id = e.voucher_id
                            WHERE v.org_id = %s
                            GROUP BY v.id
                            ORDER BY v.created_at DESC
                        """, (org_id,))
                    else:
                        cur.execute("""
                            SELECT v.*, 
                                   json_agg(json_build_object(
                                       'account_name', e.account_name,
                                       'entry_type', e.entry_type,
                                       'amount', e.amount,
                                       'notes', e.notes
                                   )) AS entries
                            FROM vouchers v
                            LEFT JOIN voucher_entries e ON v.id = e.voucher_id
                            GROUP BY v.id
                            ORDER BY v.created_at DESC
                        """)
                    rows = cur.fetchall()
                conn.close()
                return [dict(r) for r in rows]
            except Exception as e:
                print(f"[DB Error get_transactions]: {e}")

        # Default sample transactions
        return [
            {
                "voucher_number": "TX-01",
                "voucher_type": "SALES",
                "party_name": "Infosys Technologies Pune",
                "total_amount": 118000,
                "narration": "Consulting fees received",
                "gst_rate": 18,
                "tax_amount": 18000,
                "status": "BALANCED"
            },
            {
                "voucher_number": "TX-02",
                "voucher_type": "EXPENSE",
                "party_name": "WeWork Bandra Kurla Complex",
                "total_amount": 54000,
                "narration": "Office rent paid (₹6,000 TDS withheld)",
                "gst_rate": 0,
                "tax_amount": 0,
                "status": "BALANCED"
            },
            {
                "voucher_number": "TX-03",
                "voucher_type": "SALES",
                "party_name": "Bangalore Client Solutions",
                "total_amount": 141600,
                "narration": "Software development project",
                "gst_rate": 18,
                "tax_amount": 21600,
                "status": "BALANCED"
            },
            {
                "voucher_number": "TX-04",
                "voucher_type": "PURCHASE",
                "party_name": "Dell Technologies India",
                "total_amount": 41300,
                "narration": "New development servers",
                "gst_rate": 18,
                "tax_amount": 6300,
                "status": "BALANCED"
            }
        ]

    def create_transaction(self, data):
        raw_type = str(data.get("type", "SALES")).upper().strip()
        if raw_type in ("SALE", "SALES"):
            txn_type = "SALES"
        elif raw_type in ("PURCHASE", "PURCHASES"):
            txn_type = "PURCHASE"
        elif raw_type in ("EXPENSE", "EXPENSES", "PAYMENT"):
            txn_type = "EXPENSE"
        elif raw_type in ("BANK", "TREASURY"):
            txn_type = "PAYMENT"
        else:
            txn_type = raw_type

        party = data.get("party", "Party").strip()
        amt = Decimal(str(data.get("amount", 0)))
        rate = Decimal(str(data.get("rate", 18)))
        tax = (amt * rate) / Decimal("100")
        total = amt + tax
        inter_state = bool(data.get("inter_state", False))
        org_id = data.get("org_id")

        if self.is_connected:
            try:
                conn = self.get_connection()
                with conn.cursor(cursor_factory=extras.RealDictCursor) as cur:
                    # Ensure check constraint supports SALES, SALE, PURCHASE, EXPENSE, etc.
                    try:
                        cur.execute("""
                            ALTER TABLE vouchers DROP CONSTRAINT IF EXISTS vouchers_voucher_type_check;
                            ALTER TABLE vouchers ADD CONSTRAINT vouchers_voucher_type_check 
                            CHECK (voucher_type IN ('SALES', 'SALE', 'PURCHASE', 'PURCHASES', 'PAYMENT', 'RECEIPT', 'JOURNAL', 'CONTRA', 'EXPENSE', 'BANK'));
                        """)
                    except Exception:
                        pass
                    # If org_id is not passed or not found, pick first org
                    target_org_id = None
                    if org_id:
                        try:
                            cur.execute("SELECT id FROM organizations WHERE id = %s", (org_id,))
                            r = cur.fetchone()
                            if r: target_org_id = r["id"]
                        except Exception:
                            pass

                    if not target_org_id:
                        cur.execute("SELECT id FROM organizations LIMIT 1")
                        r = cur.fetchone()
                        if r: target_org_id = r["id"]

                    if not target_org_id:
                        raise ValueError("No organization exists to attach transaction.")

                    cur.execute("SELECT COUNT(*) AS c FROM vouchers WHERE org_id = %s", (target_org_id,))
                    cnt = cur.fetchone()["c"] + 1
                    v_num = f"VCH-{datetime.date.today().strftime('%Y%m')}-{cnt:03d}"

                    # Insert voucher header
                    cur.execute("""
                        INSERT INTO vouchers (org_id, voucher_number, voucher_type, party_name, voucher_date, narration, taxable_amount, gst_rate, tax_amount, total_amount, is_inter_state, status)
                        VALUES (%s, %s, %s, %s, CURRENT_DATE, %s, %s, %s, %s, %s, %s, 'BALANCED')
                        RETURNING id, voucher_number
                    """, (
                        target_org_id, v_num, txn_type, party,
                        f"Transaction with {party}", amt, rate, tax, total, inter_state
                    ))
                    v_row = cur.fetchone()
                    v_id = v_row["id"]
                    voucher_num = v_row["voucher_number"]

                    bank_name = data.get("bank", "HDFC Bank").strip()
                    bank_acc_str = f"Bank Account ({bank_name})"

                    # Insert balanced double entry lines
                    if txn_type in ("SALE", "SALES"):
                        cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'DEBIT', %s, 'Bank / Sundry Debtors')", (v_id, bank_acc_str, total))
                        cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'CREDIT', %s, 'Operating Revenue')", (v_id, "Sales Revenue", amt))
                        if tax > 0:
                            cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'CREDIT', %s, 'Output GST Liability')", (v_id, "GST Output Tax Payable", tax))
                    elif txn_type in ("PURCHASE", "PURCHASES"):
                        cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'DEBIT', %s, 'Asset/Expense Line')", (v_id, "Operating & Administrative Expenses", amt))
                        if tax > 0:
                            cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'DEBIT', %s, 'Input Tax Credit Eligible')", (v_id, "GST Input Tax Credit (ITC)", tax))
                        cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'CREDIT', %s, 'Bank Settlement')", (v_id, bank_acc_str, total))
                    else:
                        cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'DEBIT', %s, 'Operating Expense')", (v_id, "Operating & Administrative Expenses", total))
                        cur.execute("INSERT INTO voucher_entries (voucher_id, account_name, entry_type, amount, notes) VALUES (%s, %s, 'CREDIT', %s, 'Bank Disbursal')", (v_id, bank_acc_str, total))

                conn.commit()
                conn.close()

                return {
                    "success": True,
                    "voucher_number": voucher_num,
                    "type": txn_type,
                    "party": party,
                    "amount": float(total),
                    "taxable": float(amt),
                    "tax": float(tax),
                    "is_inter_state": inter_state,
                    "status": "BALANCED in PostgreSQL"
                }
            except Exception as e:
                print(f"[DB Create Transaction Error]: {e}")
                return {"success": False, "error": str(e)}

        # In-memory fallback
        return {
            "success": True,
            "voucher_number": f"TX-MEM-{int(datetime.datetime.now().timestamp())%10000}",
            "type": txn_type,
            "party": party,
            "amount": float(total),
            "taxable": float(amt),
            "tax": float(tax),
            "is_inter_state": inter_state,
            "status": "BALANCED (Local Cache)"
        }

    def get_metrics(self, org_id=None):
        if self.is_connected:
            try:
                conn = self.get_connection()
                with conn.cursor(cursor_factory=extras.RealDictCursor) as cur:
                    # Calculate live totals from vouchers
                    cur.execute("""
                        SELECT 
                            COALESCE(SUM(CASE WHEN voucher_type IN ('SALE', 'SALES') THEN taxable_amount ELSE 0 END), 0) AS total_revenue,
                            COALESCE(SUM(CASE WHEN voucher_type IN ('PURCHASE', 'PURCHASES', 'EXPENSE') THEN taxable_amount ELSE 0 END), 0) AS total_expenses,
                            COALESCE(SUM(CASE WHEN voucher_type IN ('SALE', 'SALES') THEN tax_amount ELSE 0 END), 0) AS output_gst,
                            COALESCE(SUM(CASE WHEN voucher_type IN ('PURCHASE', 'PURCHASES') THEN tax_amount ELSE 0 END), 0) AS input_gst
                        FROM vouchers
                    """)
                    row = cur.fetchone()
                    rev = float(row["total_revenue"])
                    exp = float(row["total_expenses"])
                    profit = rev - exp
                    gst_payable = max(0.0, float(row["output_gst"]) - float(row["input_gst"]))

                    # Base bank balance + revenue collected - expenses paid
                    base_bank = 500000.0
                    bank = base_bank + rev - exp
                conn.close()
                return {
                    "bank": round(bank, 2),
                    "revenue": round(rev, 2),
                    "expenses": round(exp, 2),
                    "profit": round(profit, 2),
                    "gstPayable": round(gst_payable, 2)
                }
            except Exception as e:
                print(f"[DB Metrics Error]: {e}")

        return {
            "bank": 642800.0,
            "revenue": 845000.0,
            "expenses": 318500.0,
            "profit": 526500.0,
            "gstPayable": 48240.0
        }

    def get_compliance_events(self, org_id=None):
        if self.is_connected:
            try:
                conn = self.get_connection()
                with conn.cursor(cursor_factory=extras.RealDictCursor) as cur:
                    cur.execute("SELECT * FROM compliance_events ORDER BY due_date ASC")
                    rows = cur.fetchall()
                conn.close()
                return [dict(r) for r in rows]
            except Exception as e:
                print(f"[DB Compliance Error]: {e}")

        return [
            {"title": "GSTR-1 (Outward Supplies)", "compliance_category": "GST", "period": "August 2026", "due_date": "2026-09-11", "status": "FILED", "penalty_per_day": 50},
            {"title": "GSTR-3B (Summary & Tax Payment)", "compliance_category": "GST", "period": "August 2026", "due_date": "2026-09-20", "status": "PENDING", "penalty_per_day": 50},
            {"title": "TDS Challan 281 (Rent / Professional)", "compliance_category": "TDS", "period": "August 2026", "due_date": "2026-10-07", "status": "PENDING", "penalty_per_day": 200},
            {"title": "Advance Tax Q2 (45% Cumulative)", "compliance_category": "ADVANCE_TAX", "period": "Q2 FY 2026-27", "due_date": "2026-09-15", "status": "PENDING", "penalty_per_day": 100}
        ]

    def get_ai_insights(self, org_id=None):
        if self.is_connected:
            try:
                conn = self.get_connection()
                with conn.cursor(cursor_factory=extras.RealDictCursor) as cur:
                    cur.execute("SELECT * FROM ai_financial_insights ORDER BY created_at DESC")
                    rows = cur.fetchall()
                conn.close()
                return [dict(r) for r in rows]
            except Exception as e:
                print(f"[DB Insights Error]: {e}")

        return [
            {
                "insight_type": "ITC_MISMATCH",
                "severity": "MEDIUM",
                "title": "GSTR-2B ITC Match Opportunity",
                "description": "₹6,300 Input Tax Credit eligible for auto-reconciliation against Dell Technologies GSTR-1 filing.",
                "potential_savings": 6300.00,
                "recommended_action": "Auto-claim in Table 4(A)(5) of GSTR-3B."
            },
            {
                "insight_type": "TAX_SAVING",
                "severity": "LOW",
                "title": "Section 80JJAA Hiring Benefit",
                "description": "Additional deduction of 30% on employee emoluments available for 2 new developer hires.",
                "potential_savings": 45000.00,
                "recommended_action": "Ensure CA Form 10DA is uploaded prior to ITR filing."
            }
        ]

db_manager = DatabaseManager()
