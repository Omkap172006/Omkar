# Phase 1 Migration Audit: Python to Java Spring Boot

## 1. Project Overview & Current Architecture
The Virtual CA System currently operates as a hybrid architecture:
- **Python Runtime (`server.py`)**: Implements a threaded HTTP server listening on port `8000`, serving frontend files (`/frontend/index.html`), managing CORS, and providing REST APIs.
- **Python Database Layer (`database/db.py`)**: Interacts with PostgreSQL 17 or falls back to `database/users.json` when disconnected.
- **Java Spring Boot Services (`backend/src/...`)**: Houses `DoubleEntryEngineService.java` and `GstCalculationService.java`.
- **Relational Storage (`database/schema.sql`)**: PostgreSQL DDL for double-entry bookkeeping, organizations, accounts, vouchers, compliance events, and AI insights.

---

## 2. API Contract & Endpoint Mapping

| Python Route | HTTP Method | Python Implementation | Spring Boot Target Controller | Authentication / Scope |
| :--- | :--- | :--- | :--- | :--- |
| `/` | `GET` | Redirect 302 to `/frontend/index.html` | `WebMvcConfig` / `IndexController` | Public |
| `/frontend/**` | `GET` | `SimpleHTTPRequestHandler` directory serving | `ResourceHandlerRegistry` -> `file:frontend/` | Public |
| `/api/login` | `POST` | `server.py:do_POST` lines 199-250 | `AuthController.login()` | Public (Credentials or Admin Key) |
| `/api/register` | `POST` | `server.py:do_POST` lines 181-196 | `AuthController.register()` | Public |
| `/api/users` | `GET` | `server.py:do_GET` lines 92-107 | `AuthController.getUsers()` | Authenticated / Role-dependent |
| `/api/transactions` | `GET` | `server.py:do_GET` lines 109-115 | `TransactionController.getTransactions()` | Public / Organization-scoped |
| `/api/transactions` | `POST` | `server.py:do_POST` lines 252-262 | `TransactionController.createTransaction()` | Reuses `DoubleEntryEngineService` & `GstCalculationService` |
| `/api/metrics` | `GET` | `server.py:do_GET` lines 117-123 | `MetricsController.getMetrics()` | Organization-scoped |
| `/api/compliance` | `GET` | `server.py:do_GET` lines 125-131 | `ComplianceController.getEvents()` | Organization-scoped |
| `/api/insights` | `GET` | `server.py:do_GET` lines 133-139 | `InsightController.getInsights()` | Organization-scoped |
| `/api/db-status` | `GET` | `server.py:do_GET` lines 77-90 | `AdminController.getDbStatus()` | Admin Only (`X-Admin-Key` / Token) |
| `/api/db-config` | `POST` | `server.py:do_POST` lines 151-179 | `AdminController.updateDbConfig()` | Admin Only (`X-Admin-Key` / Token) |

---

## 3. Database Schema Entities & Relationships
1. **`Organization` (`organizations`)**: Multi-tenant business profile (UUID, GSTIN, PAN, state code, legal name, email, role).
2. **`Account` (`accounts`)**: Chart of Accounts linked to `Organization` (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE).
3. **`Voucher` (`vouchers`)**: Balanced journal voucher headers (SALES, PURCHASE, EXPENSE, PAYMENT, RECEIPT).
4. **`VoucherEntry` (`voucher_entries`)**: Individual debit/credit lines linked to `Voucher` and `Account`. Enforces $\sum \text{Debits} == \sum \text{Credits}$.
5. **`ComplianceEvent` (`compliance_events`)**: Statutory tax calendar events (GSTR-1, GSTR-3B, TDS, Advance Tax).
6. **`AiFinancialInsight` (`ai_financial_insights`)**: Automated anomaly detection, ITC reconciliation, tax-saving alerts.

---

## 4. Migration Plan Checklist
- [x] Phase 1: Audit all Python endpoints, database schemas, and Java services.
- [ ] Phase 2: Create JPA Entities and Spring Data Repositories.
- [ ] Phase 2: Create Spring Boot Services reusing `DoubleEntryEngineService` and `GstCalculationService`.
- [ ] Phase 2: Implement REST Controllers matching exact payload and response contracts.
- [ ] Phase 2: Configure WebMvc static file serving for `frontend/index.html`.
- [ ] Phase 3: Enforce security filters, CORS configuration, and tenant isolation.
- [ ] Phase 4: Compatibility testing with frontend.
- [ ] Phase 5: Verification (Compilation, Unit/Integration tests, Start Spring Boot on port 8000).
- [ ] Phase 6: Cleanup Python runtime files and finalize documentation.

