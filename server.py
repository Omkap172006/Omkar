import http.server
import socketserver
import json
import os
import sys
import urllib.parse
from decimal import Decimal

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, BASE_DIR)

from database.db import db_manager, DecimalEncoder

PORT = 8000
DEV_SECRET_KEY = "dev2026"
ADMIN_SECRET_KEY = "admin2026"

class VirtualCAHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=BASE_DIR, **kwargs)

    def _send_json(self, status_code, payload):
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Admin-Key")
        self.end_headers()
        self.wfile.write(json.dumps(payload, cls=DecimalEncoder).encode("utf-8"))

    def _is_admin(self, query_params=None, body_dict=None):
        # 1. Header checks
        admin_header = self.headers.get("X-Admin-Key", "").strip()
        if admin_header in (ADMIN_SECRET_KEY, DEV_SECRET_KEY):
            return True
        auth_header = self.headers.get("Authorization", "").strip()
        if auth_header:
            parts = auth_header.split()
            if len(parts) == 2 and parts[0].lower() == "bearer":
                if parts[1] in (ADMIN_SECRET_KEY, DEV_SECRET_KEY):
                    return True

        # 2. Query param checks
        if query_params:
            k = query_params.get("admin_key", [None])[0] or query_params.get("dev_key", [None])[0]
            if k in (ADMIN_SECRET_KEY, DEV_SECRET_KEY):
                return True
            email = query_params.get("email", [None])[0]
            if email:
                accs = db_manager.get_organizations(email=email)
                if accs and any(a.get("role", "").lower() == "admin" for a in accs):
                    return True

        # 3. Body dict checks
        if body_dict:
            k = body_dict.get("admin_key") or body_dict.get("dev_key")
            if k in (ADMIN_SECRET_KEY, DEV_SECRET_KEY):
                return True
            email = body_dict.get("email")
            if email:
                accs = db_manager.get_organizations(email=email)
                if accs and any(a.get("role", "").lower() == "admin" for a in accs):
                    return True

        return False

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)

        # Redirect root to frontend
        if parsed.path in ("/", ""):
            self.send_response(302)
            self.send_header("Location", "/frontend/index.html")
            self.end_headers()
            return

        # /api/db-status - Live PostgreSQL Connection Health & Stats (STRICT ADMIN ONLY)
        if parsed.path == "/api/db-status":
            query_params = urllib.parse.parse_qs(parsed.query)
            if not self._is_admin(query_params=query_params):
                self._send_json(403, {
                    "error": "Access Denied: Database connection telemetry is strictly restricted to Administrators.",
                    "isAdmin": False,
                    "connected": False
                })
                return
            status = db_manager.get_db_status()
            status["isAdmin"] = True
            self._send_json(200, status)
            return

        # /api/users - Organization/Tenant Directory
        if parsed.path == "/api/users":
            query_params = urllib.parse.parse_qs(parsed.query)
            email = query_params.get("email", [None])[0]
            is_admin = self._is_admin(query_params=query_params)

            if is_admin:
                orgs = db_manager.get_organizations(is_dev=True)
                self._send_json(200, {"role": "admin", "isAdmin": True, "accounts": orgs, "total": len(orgs)})
            elif email:
                orgs = db_manager.get_organizations(email=email)
                has_admin = any(a.get("role", "").lower() == "admin" for a in orgs)
                self._send_json(200, {"role": "admin" if has_admin else "user", "isAdmin": has_admin, "accounts": orgs, "total": len(orgs)})
            else:
                self._send_json(200, {"role": "guest", "isAdmin": False, "accounts": [], "total": 0, "message": "Authentication required"})
            return

        # /api/transactions - Double-Entry Ledger Vouchers
        if parsed.path == "/api/transactions":
            query_params = urllib.parse.parse_qs(parsed.query)
            org_id = query_params.get("org_id", [None])[0]
            txns = db_manager.get_transactions(org_id=org_id)
            self._send_json(200, {"transactions": txns, "total": len(txns)})
            return

        # /api/metrics - Real-Time Financial Totals
        if parsed.path == "/api/metrics":
            query_params = urllib.parse.parse_qs(parsed.query)
            org_id = query_params.get("org_id", [None])[0]
            metrics = db_manager.get_metrics(org_id=org_id)
            self._send_json(200, metrics)
            return

        # /api/compliance - Statutory Calendar Events
        if parsed.path == "/api/compliance":
            query_params = urllib.parse.parse_qs(parsed.query)
            org_id = query_params.get("org_id", [None])[0]
            events = db_manager.get_compliance_events(org_id=org_id)
            self._send_json(200, {"events": events, "total": len(events)})
            return

        # /api/insights - AI Audit & Tax Optimization Insights
        if parsed.path == "/api/insights":
            query_params = urllib.parse.parse_qs(parsed.query)
            org_id = query_params.get("org_id", [None])[0]
            insights = db_manager.get_ai_insights(org_id=org_id)
            self._send_json(200, {"insights": insights, "total": len(insights)})
            return

        super().do_GET()

    def do_POST(self):
        parsed = urllib.parse.urlparse(self.path)

        def get_body():
            content_length = int(self.headers.get("Content-Length", 0))
            raw = self.rfile.read(content_length).decode("utf-8")
            return json.loads(raw) if raw else {}

        # /api/db-config - Test & Update PostgreSQL Credentials from UI (STRICT ADMIN ONLY)
        if parsed.path == "/api/db-config":
            try:
                data = get_body()
                query_params = urllib.parse.parse_qs(parsed.query)
                if not self._is_admin(query_params=query_params, body_dict=data):
                    self._send_json(403, {
                        "success": False,
                        "error": "Access Denied: Database configuration modification is restricted to Administrators."
                    })
                    return

                new_cfg = {}
                if "host" in data: new_cfg["DB_HOST"] = data["host"].strip()
                if "port" in data: new_cfg["DB_PORT"] = str(data["port"]).strip()
                if "user" in data: new_cfg["DB_USER"] = data["user"].strip()
                if "password" in data: new_cfg["DB_PASSWORD"] = data["password"].strip()
                if "database" in data: new_cfg["DB_NAME"] = data["database"].strip()

                success, msg = db_manager.try_initialize(new_cfg)
                status = db_manager.get_db_status()
                self._send_json(200 if success else 400, {
                    "success": success,
                    "message": msg,
                    "status": status
                })
            except Exception as e:
                self._send_json(500, {"success": False, "error": str(e)})
            return

        # /api/register - Multi-Tenant Business Registration
        if parsed.path == "/api/register":
            try:
                data = get_body()
                user_record = db_manager.create_organization(data)
                user_accounts = db_manager.get_organizations(email=data.get("email"))

                self._send_json(200, {
                    "status": "success",
                    "user": user_record,
                    "userAccounts": user_accounts,
                    "userAccountCount": len(user_accounts)
                })
            except Exception as e:
                self._send_json(500, {"status": "error", "message": str(e)})
            return

        # /api/login - Authenticate and Fetch Linked Business Entities
        if parsed.path == "/api/login":
            try:
                data = get_body()
                email = data.get("email", "").strip().lower()
                admin_key = data.get("admin_key", "") or data.get("dev_key", "")

                is_admin = (admin_key in (ADMIN_SECRET_KEY, DEV_SECRET_KEY))

                if is_admin:
                    all_accs = db_manager.get_organizations(is_dev=True)
                    admin_acc = next((a for a in all_accs if a.get("role", "").lower() == "admin"), None)
                    if not admin_acc:
                        admin_acc = {
                            "name": "System Administrator",
                            "trade_name": "Virtual CA Cloud Infrastructure",
                            "company": "Virtual CA Cloud Infrastructure",
                            "email": "admin@virtualca.internal",
                            "gstin": "27ADMIN0000A1Z0",
                            "state_code": "27",
                            "stateName": "Maharashtra",
                            "role": "Admin",
                            "initials": "AD"
                        }
                    self._send_json(200, {
                        "status": "success",
                        "isAdmin": True,
                        "isDeveloper": True,
                        "adminToken": ADMIN_SECRET_KEY,
                        "activeAccount": admin_acc,
                        "user": admin_acc,
                        "userAccounts": [admin_acc] + [a for a in all_accs if a.get("gstin") != admin_acc.get("gstin")],
                        "allAccounts": all_accs
                    })
                    return

                user_accounts = db_manager.get_organizations(email=email)
                if user_accounts:
                    has_admin_role = any(a.get("role", "").lower() == "admin" for a in user_accounts)
                    self._send_json(200, {
                        "status": "success",
                        "isAdmin": has_admin_role,
                        "isDeveloper": has_admin_role,
                        "adminToken": ADMIN_SECRET_KEY if has_admin_role else None,
                        "activeAccount": user_accounts[0],
                        "userAccounts": user_accounts,
                        "totalUserAccounts": len(user_accounts)
                    })
                else:
                    self._send_json(404, {"status": "not_found", "message": "No business entity found for this email."})
            except Exception as e:
                self._send_json(500, {"status": "error", "message": str(e)})
            return

        # /api/transactions - Record New Double-Entry Voucher
        if parsed.path == "/api/transactions":
            try:
                data = get_body()
                result = db_manager.create_transaction(data)
                metrics = db_manager.get_metrics(org_id=data.get("org_id"))
                self._send_json(200, {"status": "success", "transaction": result, "metrics": metrics})
            except Exception as e:
                self._send_json(500, {"status": "error", "message": str(e)})
            return

        super().do_POST()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.end_headers()

if __name__ == "__main__":
    socketserver.ThreadingTCPServer.allow_reuse_address = True
    with socketserver.ThreadingTCPServer(("", PORT), VirtualCAHandler) as httpd:
        print(f"Virtual CA Server running on http://localhost:{PORT}")
        print("Integrated with PostgreSQL 17 database storage & Clean UI APIs.")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            pass
