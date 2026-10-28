# AI-Powered Virtual CA System — Enterprise Java Spring Boot Platform

An intelligent, autonomous bookkeeping, GST calculation, and financial compliance platform for Indian MSMEs/SMBs that automates 80–90% of routine Chartered Accountant operations while enabling seamless human CA sign-off.

> **Migration Status:** The system runtime has been completely migrated to **Pure Java 17 + Spring Boot 3.3.4**. Python (`server.py`, `database/db.py`) has been fully decommissioned from the application runtime.

---

## 📂 Project Directory Structure

```
virtual_ca_system_full/
├── backend/
│   ├── pom.xml                                   # Enterprise Maven build specification
│   └── src/
│       ├── main/
│       │   ├── java/com/virtualca/
│       │   │   ├── VirtualCaApplication.java     # Spring Boot Entry Point
│       │   │   ├── config/
│       │   │   │   ├── WebMvcConfig.java         # Static file routing & CORS configuration
│       │   │   │   ├── DatabaseConfig.java       # Resilient PostgreSQL & persistent fallback
│       │   │   │   └── DatabaseInitializer.java  # Auto-seeding from users.json & ledger init
│       │   │   ├── controller/
│       │   │   │   ├── AuthController.java       # /api/login, /api/register, /api/users
│       │   │   │   ├── TransactionController.java# /api/transactions (GET & POST)
│       │   │   │   ├── MetricsController.java    # /api/metrics (P&L & Bank Balance)
│       │   │   │   ├── ComplianceController.java # /api/compliance
│       │   │   │   ├── InsightController.java    # /api/insights
│       │   │   │   ├── AdminController.java      # /api/db-status, /api/db-config
│       │   │   │   ├── IndexController.java      # Redirects / to /frontend/index.html
│       │   │   │   └── GlobalExceptionHandler.java # Centralized error handling
│       │   │   ├── dto/                          # Strong typed request & response contracts
│       │   │   ├── entity/                       # JPA entities matching PostgreSQL schema
│       │   │   ├── repository/                   # Spring Data JPA repositories
│       │   │   ├── service/                      # Business & tenant isolation logic
│       │   │   ├── tax/service/
│       │   │   │   └── GstCalculationService.java    # Strict BigDecimal Indian GST (CGST/SGST vs IGST)
│       │   │   └── ledger/service/
│       │   │       └── DoubleEntryEngineService.java # Transactional Double-entry ledger (Debits = Credits)
│       │   └── resources/
│       │       └── application.properties        # Server port, database, and security settings
│       └── test/
│           ├── java/com/virtualca/
│           │   ├── VirtualCaApplicationTests.java   # End-to-end MockMvc controller tests (12 tests)
│           │   ├── tax/service/GstCalculationServiceTest.java # Indian GST breakup tests (3 tests)
│           │   └── ledger/service/DoubleEntryEngineServiceTest.java # Double-entry ledger tests (2 tests)
│           └── resources/
│               └── application.properties        # Fast in-memory test configuration
├── database/
│   ├── schema.sql                                # Production PostgreSQL 17 DDL
│   └── users.json                                # Seed business owner accounts
├── frontend/
│   └── index.html                                # Interactive Web Dashboard served on port 8000
├── run.ps1                                       # 1-click startup script (PowerShell)
├── run.bat                                       # 1-click startup script (Command Prompt)
└── MIGRATION_AUDIT_REPORT.md                     # Migration audit and API verification record
```

---

## 🛠️ Prerequisites & Technology Stack

1. **Java Runtime:** Java 17 LTS or higher (e.g., Eclipse Adoptium Temurin 17 / OpenJDK 17).
2. **Build Tool:** Apache Maven 3.9+ (or use the installed Maven at `c:\tools\apache-maven-3.9.9\bin\mvn.cmd`).
3. **Database (Optional/Recommended):** PostgreSQL 17.
   - *Resilience Guarantee:* If PostgreSQL is online on port 5432, the application automatically uses it. If PostgreSQL is offline, `DatabaseConfig` automatically activates a persistent file-backed database in `database/` so that no financial data is ever lost.
4. **Python Requirement:** **NONE.** No Python runtime, script, or environment is required to run the application.

---

## 🚀 How to Run the Application (Without Python)

### Option 1: Quick 1-Click Launch (Recommended)
In PowerShell:
```powershell
.\run.ps1
```
Or in Command Prompt:
```cmd
run.bat
```

### Option 2: Run with Java directly
```powershell
java -jar backend/target/virtual-ca-backend-1.0.0.jar
```

### Option 3: Run with Maven from source
```powershell
cd backend
mvn spring-boot:run
```

Once started, open your web browser at:
👉 **[http://localhost:8000](http://localhost:8000)**

*(The root path `/` automatically serves the full interactive dashboard at `/frontend/index.html`.)*

---

## 🔑 Default Credentials & Keys

- **Master Admin Secret Key:** `admin2026` or `dev2026`
- **System Administrator Email:** `admin@virtualca.internal`
- **Default Owner Email:** `atharva@apexinfotech.in` or `omkarpatrikar@gmail.com`

---

## 🧪 Running Automated Tests

Run the full suite of 17 automated unit and integration tests:

```powershell
cd backend
mvn test
```

### Test Coverage Highlights:
- **`GstCalculationServiceTest` (3 tests):** Validates Intra-State 50/50 split (CGST + SGST), Inter-State allocation (IGST), and 1-paisa rounding skew prevention.
- **`DoubleEntryEngineServiceTest` (2 tests):** Enforces $\sum \text{Debits} == \sum \text{Credits}$ across both sales and operating expense vouchers.
- **`VirtualCaApplicationTests` (12 tests):** Tests full Spring Boot MVC context, admin/user login, entity registration, tenant directory listing, double-entry voucher creation, real-time KPI metrics calculation, statutory calendar queries, AI financial insights, and 403 Forbidden security enforcement on telemetry endpoints.

---

## 📡 REST API Reference

| Endpoint | Method | Security Level | Purpose |
| :--- | :--- | :--- | :--- |
| `/` | `GET` | Public | Direct delivery of dashboard UI |
| `/frontend/index.html` | `GET` | Public | Frontend SPA and interactive ledger |
| `/api/login` | `POST` | Public | Authenticates via email or `admin_key` (`admin2026`) |
| `/api/register` | `POST` | Public | Registers a new MSME entity (state auto-detected from GSTIN) |
| `/api/users` | `GET` | Protected | Multi-tenant organization directory |
| `/api/transactions` | `GET` | Public | Retrieves double-entry vouchers (optional `?org_id=...`) |
| `/api/transactions` | `POST` | Public | Records balanced voucher, updates ledger and P&L metrics |
| `/api/metrics` | `GET` | Public | Real-time financial KPIs (Revenue, Expenses, Profit, Bank, GST) |
| `/api/compliance` | `GET` | Public | Statutory compliance due dates (GSTR-1, 3B, TDS, Advance Tax) |
| `/api/insights` | `GET` | Public | AI financial anomaly and tax-saving recommendations |
| `/api/db-status` | `GET` | Admin Only | Live DB engine health, latency, active connections & table counts |
| `/api/db-config` | `POST` | Admin Only | Live database connection verification and settings |
