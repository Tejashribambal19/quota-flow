# Quota Flow — Hackathon Demo Checklist

## Before the demo

- [ ] Start Docker Desktop.
- [ ] Run `docker compose up -d` and confirm PostgreSQL/Redis are healthy.
- [ ] Run backend tests and confirm zero failures.
- [ ] Start the backend and JavaFX client.
- [ ] Confirm both demo accounts can log in.
- [ ] Keep `simulate-usage.ps1` ready in a PowerShell window.
- [ ] Do not display JWT tokens or passwords on the projector.

## Five-minute demo

### 1. Problem — 30 seconds

Shared SaaS infrastructure needs accurate per-customer usage tracking. Without quota enforcement, one customer can overload the service and billing cannot be reconciled reliably.

### 2. Platform Admin — 60 seconds

1. Log in as Platform Admin.
2. Show total plans, tenants and active tenants.
3. Show the valid tamper-evident audit chain.
4. Create a plan or tenant using the new admin form.

### 3. Tenant Admin — 90 seconds

1. Log out and sign in as ABC Logistics Admin.
2. Show the resource chart and quota cards.
3. Explain 80% warning, 90% critical and 100% block behavior.
4. Generate a small usage event from the dashboard.
5. Refresh and show the updated Redis-backed counter.

### 4. Reliability — 60 seconds

1. Explain that the Redis Lua script makes check-and-increment atomic.
2. Mention the passing concurrency test.
3. Explain monthly Redis keys and automatic new-month reset.
4. Explain that PostgreSQL permanently stores every usage decision.

### 5. Billing and security — 60 seconds

1. Show cost allocation and the monthly total.
2. Download and open the PDF invoice.
3. Explain JWT role routing and backend tenant isolation.
4. Explain the cryptographic audit chain.

## Final one-line pitch

“Quota Flow gives SaaS platforms a reliable Java-based control plane for real-time metering, fair quota enforcement, tenant isolation and billing-ready usage data.”

## Recovery commands

```powershell
docker compose up -d
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
cd backend
.\mvnw.cmd spring-boot:run
```
