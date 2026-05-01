# AMS Proxy / Reverse-Proxy Readiness Audit

**Date:** 2026-05-01  
**Branch:** `refactor/modernize-architecture`  
**Phase:** 1 — Read-Only Discovery  
**Scope:** Cloudflare proxy enablement for SSA instances; forward-compatibility for TPA deployments

---

## Section 1 — Client IP Address Handling

### `getRemoteAddr()` occurrences

| File | Line(s) | Context | Purpose | Forwarded-header aware? |
|------|---------|---------|---------|------------------------|
| `ServeVideo.java` | 243 | `getClientIp()` helper — used as fallback | Records first-view IP into `VideoToken.ipAddress` (`video_token.ip_address`) | **Yes** — checks `X-Forwarded-For` first; `getRemoteAddr()` is fallback only |
| `NdtTestRunServlet.java` | 90 | `doGet` — view action | Logs to `NdtAccessLog.ipAddress` (`ndt_access_log.ip_address`), `ACTION_VIEW` | **No** |
| `NdtTestRunServlet.java` | 185 | `handleCreate` | Logs to `NdtAccessLog.ipAddress`, `ACTION_CREATE` | **No** |
| `NdtTestRunServlet.java` | 275, 331 | `handleUpload`, `handleDeleteUpload` | Logs to `NdtAccessLog.ipAddress`, `ACTION_UPLOAD` / `ACTION_DELETE_UPLOAD` | **No** |

### Forwarded-header reads

`ServeVideo.java:239` (`getClientIp()` method):

```java
String xForwarded = request.getHeader("X-Forwarded-For");
if (xForwarded != null && !xForwarded.isEmpty()) {
    return xForwarded.split(",")[0].trim();
}
return request.getRemoteAddr();
```

This reads the **leftmost** entry from `X-Forwarded-For`. This is the correct value for a trusted-proxy architecture but is **vulnerable to IP spoofing** if the header is not stripped or overwritten at the proxy edge. A malicious client can inject any IP by sending `X-Forwarded-For: 1.2.3.4` before the proxy appends the real address. Without `RemoteIpValve` enforcing a trusted-proxy range, there is no validation.

No code reads `X-Real-IP`, `CF-Connecting-IP`, or `True-Client-IP`.  
No code calls `getRemoteHost()`.  
No code calls `getRemoteUser()` (auth method, not IP — verified absent).

---

## Section 2 — Absolute URL & Hostname Generation

### Pattern A — Raw request attributes (no override; **currently broken behind nginx**)

These servlets build URLs directly from `request.getScheme()` + `request.getServerName()` + `request.getServerPort()`. Because nginx terminates SSL and forwards HTTP to Tomcat on port 8080 — and `RemoteIpValve` is absent — Tomcat observes scheme=`http`, port=`8080`. The Host header (`proxy_set_header Host $host`) gives the correct hostname, but the scheme and port are wrong. The port-exclusion guard `if (port != 80 && port != 443)` does **not** exclude 8080, so generated URLs have the form `http://superiorstate.biz:8080/...`.

**These links are already broken in production since nginx was put in front (March 2026).**

| File | Lines | Link built | Sent externally? |
|------|-------|-----------|-----------------|
| `ProposalDetail.java` | 40–44, 78–82 | Proposal share link (`/proposal/{guid}`) | **Yes** — stored as request attribute, then used in outbound email via POST handler |
| `SendProposal.java` | 56–60 | Proposal share link | **Yes** — directly embedded in email body sent to prospects |
| `SendInvitation.java` | 144–149 | Agency invitation link (`/AcceptInvite?guid=...`) | **Yes** — emailed to invited agents |
| `EmailBillingToEmployer.java` | 48–50 | Employer billing detail link | **Yes** — stored in session as `bcLink`, displayed in `sendBillingForm.jsp`, included in emails to employers |
| `125eligibility.jsp` | 34–43 | Return URL for NDT eligibility questionnaire callback (`/Eligibility125Complete`) | Depends on usage — passed to the NDT questionnaire form |

### Pattern B — `SYSTEM_URL` override in `ssa.properties` (correct for configured instances)

Three servlets check `AppConfig.get("SYSTEM_URL")` first and fall back to scheme+host+port construction only if `SYSTEM_URL` is blank. If `SYSTEM_URL` is set correctly in `ssa.properties` (per the runbook), these work.

| File | Lines | Use |
|------|-------|-----|
| `BpoPspClients.java` | 103–113 | BPO URL sent in partnership approval callback to PSP |
| `VendorManager25.java` | 150–158 | PSP URL sent in vendor registry registration request |
| `ManageVideos.java` | 66–73 | Base URL for training video token links (shown to admin) |
| `SuperDashboard.java` | 186–188 | Master URL for managed-installation registration; falls back to `global.getWebPath()` |

### Pattern C — `WEB_PATH` DB constant (correct; proxy-agnostic)

| File | Lines | Use |
|------|-------|-----|
| `HelpUserLogin.java` | 125–131 | Password reset and one-time login links; reads `WEB_PATH` constant, falls back to user's email domain |
| `CreateUser25.java` | 308–314 | New user setup link; same fallback chain |

### `WEB_PATH` constant

- Seeded at initialization from the domain entered in the setup form.
- Read at runtime via `AppConstantDAO.getConstantValue(em, "WEB_PATH")`.
- Also loaded into `AmsDataGlobal.webPath` at startup.
- `AmsDataGlobal.java:395`: fallback hard-codes `"https://superiorstate.biz/"` if the DB read fails.

### Hardcoded SSA domain references in Java code

| File | Content | Concern |
|------|---------|---------|
| `AmsDataGlobal.java:395` | `"https://superiorstate.biz/"` — fallback for `WEB_PATH` | SSA-specific; TPA would get wrong fallback if DB constant missing |
| `DatabaseInitializer.java:878` | `"https://superiorstate.net"` — seeds `MASTER_REGISTRY_URL` constant | All deployments default to SSA's master registry |
| `DocumentConstants.java:24` | `DOC_PATH = "https://superiorstate.net/tpo/docs/"` | TPAs would link to SSA's doc server |
| `DocumentConstants.java:15–29` | Multiple SharePoint URLs under `superiorstate-my.sharepoint.com` | SSA tenant only; TPA deployments would reference wrong storage |
| `CreateUser25.java:330` | `EmailDAO.sendEmail("noreply@superiorstate.net", ...)` | Hardcoded FROM address on new-user welcome email |
| `HelpUserLogin.java:89` | `EmailDAO.sendEmail("noreply@superiorstate.net", ...)` | Hardcoded FROM address on password reset email |
| `PersonDAO.java:250` | `"superiorstate.net"` in email-domain merge-suppression ignore list | Internal SSA email domain bypasses person-merge logic; harmless for TPA |
| `DemoDataSeeder.java:823` | `"https://bpo.superiorstate.biz"` — demo BPO URL in seeder | Demo data only; no runtime impact |

---

## Section 3 — Scheme / SSL Handling

**`isSecure()` calls:** None found in the codebase.

**HTTP→HTTPS redirect:** Handled entirely by nginx (`return 301 https://$host$request_uri;`). No redirect logic in application code or Tomcat config.

**Session cookie configuration:** `web.xml` contains no `<session-config>` element. Tomcat 10 defaults apply:
- `HttpOnly=true` — set by default
- `Secure` flag — **NOT set**. Tomcat sets the `Secure` flag only when `request.isSecure()` returns true. Without `RemoteIpValve`, Tomcat sees plain HTTP from nginx on port 8080 and considers the connection insecure. Session cookies are therefore sent without the `Secure` flag even though the browser-to-nginx leg is HTTPS.
- `SameSite` attribute — not configured anywhere in `web.xml` or servlet code.

**`getScheme()` called for decisions (not just URL building):** None found. All `getScheme()` usages are URL construction only; no security decisions gate on it.

---

## Section 4 — Tomcat Configuration

**No Tomcat configuration files are stored in this repository.** Configuration is managed on the VPS filesystem at `/var/lib/tomcat10/conf/`. The documentation in `docs/tomcat_ssl_setup.md` describes the intended connector:

```xml
<Connector address="127.0.0.1" port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000" />
```

**`RemoteIpValve`: NOT configured anywhere.** Neither the documented connector nor any deployment documentation mentions `RemoteIpValve`, `proxyName`, `proxyPort`, `scheme="https"`, or `secure="true"`.

nginx (`docs/tomcat_ssl_setup.md`) does forward the necessary headers:

```nginx
proxy_set_header Host              $host;
proxy_set_header X-Real-IP         $remote_addr;
proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto https;
```

But Tomcat does not consume these headers without the valve. The result:

| Property | What Tomcat returns | What it should return |
|----------|--------------------|-----------------------|
| `getScheme()` | `"http"` | `"https"` |
| `getServerPort()` | `8080` | `443` |
| `getRemoteAddr()` | `127.0.0.1` | real client IP |
| `isSecure()` | `false` | `true` |
| Session `Secure` flag | not set | set |

`getServerName()` is correctly returned from the `Host` header (`proxy_set_header Host $host`) even without the valve.

**No `context.xml` in the repository.** No Valve, Manager, or Resource definitions are tracked in source control.

**`web.xml` (AMS-specific):** Contains only a JSTL jar-skip entry. No security constraints, no filter declarations (filters use annotations), no session config.

---

## Section 5 — Custom Servlet Filters

Two filter classes found:

| Class | Annotation | URL Pattern | Touches proxy headers? | Purpose |
|-------|-----------|------------|----------------------|---------|
| `LoginFilter` | `@WebFilter("/*")` | All URLs | **No** | Session authentication gate; checks session attribute `local.isAuthenticated()` |
| `ApiTokenFilter` | `@WebFilter("/api/*")` | `/api/*` | **No** | Bearer token auth for REST API endpoints; validates against DB constants and BpoRegistration/PspClient entities |

`LoginFilter` passes through: `/api/*`, static resources (images/css/js/fonts), specific public paths (`/login`, `/RequestQuote`, `/AcceptInvite`, `/video`, `/outlook/*`, etc.). No filter inspects or validates forwarded headers.

No filter enforces HTTPS, checks the `Secure` cookie flag, or redirects HTTP to HTTPS (nginx handles this).

---

## Section 6 — SSL / Cert Setup

**SSL termination:** nginx on port 443 → Tomcat on port 8080 (localhost only). Tomcat does not handle HTTPS directly.

**Certificate authority:** Let's Encrypt.

**ACME challenge type: HTTP-01 via the certbot nginx authenticator.**

```bash
sudo certbot certonly --nginx -d yourdomain.com -d www.yourdomain.com
```

This challenge type **is incompatible with Cloudflare proxy (orange cloud)**:
- HTTP-01 requires that `http://<domain>/.well-known/acme-challenge/<token>` be served by the origin.
- With Cloudflare proxy enabled, Cloudflare terminates the HTTP connection and may serve a cached or transformed response rather than routing the request to the origin's certbot listener.
- Even if Cloudflare passes the challenge through, the certificate would be issued for the origin and Cloudflare would still present its own edge certificate to clients — the Let's Encrypt cert becomes an "origin certificate" in the chain.
- **Auto-renewal would break** once orange cloud is enabled unless the challenge method is changed.

**Alternatives for Cloudflare deployment:**
1. **DNS-01 challenge** — certbot requests a `_acme-challenge` TXT record via the Cloudflare DNS API; works regardless of proxy status.
2. **Cloudflare Origin CA** — Cloudflare-issued cert (15-year validity) for the origin; only trusted by Cloudflare, not by direct-to-origin clients. Valid for SSA deployments where all traffic goes through Cloudflare.

**Port 80 → 443 redirect:** nginx (`return 301 https://$host$request_uri;`). Correct, no app-code change needed.

**Cert renewal automation:** certbot systemd timer (installed by `python3-certbot-nginx`). Runs twice daily. Configured to use the nginx authenticator (`authenticator = nginx` in `/etc/letsencrypt/renewal/*.conf`).

**Cert expiry (production):** 2026-06-18 (per `docs/tomcat_ssl_setup.md`). Auto-renewal targets 30 days before expiry — next renewal attempt would be around 2026-05-19.

---

## Section 7 — Multi-Tenant Hostname & Deployment Configuration

### How an instance determines its public URL

Two separate mechanisms exist and are **not unified**:

1. **`SYSTEM_URL` in `ssa.properties`** (infrastructure config)  
   - Read via `AppConfig.get("SYSTEM_URL")`.
   - Used by: `BpoPspClients`, `VendorManager25`, `ManageVideos`, `SuperDashboard`.
   - Set during provisioning by the operator (per deployment runbook §Phase 2).
   - Not set on the master image (`SYSTEM_URL=` is blank in the template).

2. **`WEB_PATH` DB constant** (application config)  
   - Seeded from the domain entered in `/initialize.jsp` at first-run initialization.
   - Read via `AppConstantDAO.getConstantValue(em, "WEB_PATH")`.
   - Used by: `HelpUserLogin`, `CreateUser25`, `AmsDataGlobal`.
   - Fallback if DB read fails: `"https://superiorstate.biz/"` (hard-coded in `AmsDataGlobal.java:395`).

These two mechanisms can diverge (e.g., if `SYSTEM_URL` is updated in `ssa.properties` but `WEB_PATH` is not updated in the DB, or vice versa).

### Where a TPA configures their hostname

- **Provisioning time:** Set `SYSTEM_URL=https://their.domain.com` in `ssa.properties`.
- **Initialization form:** Enter their domain in the domain field → seeds `WEB_PATH` DB constant.
- After initialization, `WEB_PATH` can be updated via direct DB edit; no admin UI exists for it.

### SSA-specific domain assumptions

| Location | Hardcoded value | Impact on TPA |
|----------|----------------|---------------|
| `AmsDataGlobal.java:395` | `"https://superiorstate.biz/"` | TPA gets SSA domain if `WEB_PATH` DB constant is missing |
| `DatabaseInitializer.java:878` | `MASTER_REGISTRY_URL = "https://superiorstate.net"` | TPA's master registry URL defaults to SSA's master node |
| `DocumentConstants.java:24` | `"https://superiorstate.net/tpo/docs/"` | Help documents link to SSA's server |
| `DocumentConstants.java:15–29` | SharePoint at `superiorstate-my.sharepoint.com` | Resource links point to SSA's SharePoint |
| `CreateUser25.java:330` | `"noreply@superiorstate.net"` | TPA welcome emails come from SSA's email address |
| `HelpUserLogin.java:89` | `"noreply@superiorstate.net"` | TPA password reset emails come from SSA's email address |

### DNS documentation for non-SSA deployments

`deployment_strategy.md §8.2` documents: "The PSP creates an A record pointing their chosen domain to the VM IP address." No CNAME guidance, no CDN guidance, no documentation for deployments behind a load balancer with an intermediate IP, or for wildcard-DNS scenarios.

### BPO / multi-instance URL handling

- `BpoRegistration.bpoUrl` — stored in the DB; set to the BPO's own URL during partnership setup.
- `ManagedInstallation.installationUrl` — the target installation's URL for master-to-PSP API calls.
- No `BPO_HOST` / `MASTER_HOST` / `DEMO_HOST` environment constants. Hostnames are stored in DB entities, not in `ssa.properties`.

### Subdomain tenancy

None. Each deployment is a fully independent VM. No subdomain routing within a single instance. Subdomains are distinguished entirely by DNS → separate VPS IP.

---

## Section 8 — Audit Logging & Persisted IPs

### `NdtAccessLog` (`ndt_access_log.ip_address VARCHAR(45)`)

- **Entity:** `net.superiorstate.ams.model.activity.ndt.NdtAccessLog`
- **Table column:** `ip_address VARCHAR(45)`
- **Call sites:** `NdtTestRunServlet.java` lines 90, 185, 275, 331 — all pass `request.getRemoteAddr()` directly.
- **Current behavior behind nginx (no RemoteIpValve):** `getRemoteAddr()` returns `127.0.0.1` (nginx-to-Tomcat loopback). Every NDT access log entry already stores `127.0.0.1`, not the real client IP.
- **Behavior with Cloudflare (no RemoteIpValve):** Would store Cloudflare edge IPs (ranges `103.x`, `104.x`, `172.x`, `198.x`) instead of real client IPs.
- **Severity:** The audit log data is already incorrect. This is a current production data quality issue, not just a future concern.

### `VideoToken` (`video_token.ip_address VARCHAR(45)`)

- **Entity:** `net.superiorstate.ams.model.general.VideoToken`
- **Table column:** `ip_address VARCHAR(45)`
- **Call site:** `ServeVideo.java:238–243` via `getClientIp()`.
- **Current behavior:** Reads `X-Forwarded-For` first — returns real client IP if nginx is trusted. Nginx appends the real client IP to `X-Forwarded-For` (`$proxy_add_x_forwarded_for`), so the leftmost entry is the client IP.
- **Spoofing risk:** Without `RemoteIpValve` enforcing a trusted-proxy allowlist, a client can send `X-Forwarded-For: 1.2.3.4` and `getClientIp()` will return `1.2.3.4`. The real IP is appended by nginx but the code takes only the leftmost entry.
- **Behavior with Cloudflare:** Cloudflare sets `CF-Connecting-IP` to the real client IP and includes the real IP in `X-Forwarded-For`. However, the leftmost-entry extraction logic remains spoofable unless `RemoteIpValve` is configured.

---

## Section 9 — WebSocket / Streaming Endpoints

**WebSocket endpoints (`@ServerEndpoint`):** None found.

**Server-sent events (SSE, `text/event-stream`):** None found.

**Long-polling:** None found.

### Non-standard streaming patterns

| Servlet | Mechanism | Cloudflare concern |
|---------|-----------|-------------------|
| `ServeVideo.java` | HTTP byte-range MP4 streaming. Reads file with `RandomAccessFile`; responds with `Content-Range`, partial content (206). | Cloudflare supports byte-range streaming. No special configuration needed. File size limit: Cloudflare free/pro plans buffer responses up to 10 GB. With Cloudflare set to "bypass cache" for `/video` URLs, no issue. |
| `CreateBillingDiag.java` | Chunked HTML response written progressively via `PrintWriter`. Sets `X-Accel-Buffering: no` (nginx directive). | Cloudflare ignores `X-Accel-Buffering`. Cloudflare may fully buffer the response before delivering to client, eliminating the "real-time progress" effect. This page is admin-only (`/CreateBillingDiag`) — low user impact. |

---

## Section 10 — Hardcoded URLs in Email/Notification Templates

### Outbound emails containing generated links

| Servlet | Link type | URL source | Behind nginx: correct? |
|---------|-----------|-----------|----------------------|
| `HelpUserLogin.java` | Password reset | `WEB_PATH` DB constant | ✓ Yes |
| `CreateUser25.java` | New user setup | `WEB_PATH` DB constant | ✓ Yes |
| `ProposalDetail.java` (POST) | Proposal share link | Raw `getScheme()`+`getServerName()`+`getServerPort()` | ✗ Produces `http://host:8080/...` |
| `SendProposal.java` (GET + POST) | Proposal share link | Raw `getScheme()`+`getServerName()`+`getServerPort()` | ✗ Same |
| `SendInvitation.java` | Agency invitation | Raw `getScheme()`+`getServerName()`+`getServerPort()` | ✗ Same |
| `EmailBillingToEmployer.java` | Billing detail | Raw `getScheme()`+`getServerName()`+`getServerPort()` | ✗ Same |

### Hardcoded FROM address

`CreateUser25.java:330` and `HelpUserLogin.java:89` both hard-code `"noreply@superiorstate.net"` as the sender. This is not configurable; TPA deployments send system emails with an SSA return address.

### Document / resource URLs in application constants

`DocumentConstants.java` contains constants used in the UI (not in emailed links) that reference SSA's infrastructure:
- `DOC_PATH = "https://superiorstate.net/tpo/docs/"` — document download base URL
- Multiple SharePoint document links under `superiorstate-my.sharepoint.com`

These render in-browser as clickable links. TPA users following them reach SSA's documents. Not a proxy-readiness issue but an architectural TPA concern.

---

## Section 11 — Risk Summary

| Finding | Location | Risk Class | Notes |
|---------|----------|-----------|-------|
| URL generation via raw `getScheme()`+`getServerName()`+`getServerPort()` produces `http://host:8080/...` | `ProposalDetail`, `SendProposal`, `SendInvitation`, `EmailBillingToEmployer`, `125eligibility.jsp` | **Functional** | **Already broken in production since nginx was added March 2026.** Proposal links, invitation emails, and billing links sent to external users contain wrong scheme and port. |
| `NdtTestRunServlet` logs `getRemoteAddr()` = `127.0.0.1` to `ndt_access_log.ip_address` | `NdtTestRunServlet.java:90,185,275,331` | **Functional** | All NDT audit log entries already store `127.0.0.1`, not client IPs. Data quality is compromised now; Cloudflare makes it worse (edge IP instead of loopback). |
| Session cookies lack `Secure` flag | Tomcat defaults; `RemoteIpValve` absent | **Functional** | Tomcat thinks connection is HTTP, so session cookie `Secure` flag is not set. Session tokens are technically transferable over plain HTTP if a client bypasses the nginx redirect. |
| ACME HTTP-01 renewal breaks under Cloudflare orange cloud | certbot nginx authenticator | **Functional** | Enabling proxy (orange cloud) on SSA's Cloudflare records will break certificate auto-renewal at next renewal window (~2026-05-19 for production). Must switch to DNS-01 or Cloudflare Origin CA before enabling proxy. |
| `X-Forwarded-For` read in `ServeVideo.getClientIp()` without trusted-proxy validation | `ServeVideo.java:239–243` | **Functional** | Client can inject arbitrary source IP via `X-Forwarded-For` header. Without `RemoteIpValve` defining trusted proxy ranges, the app accepts client-supplied IPs. |
| `RemoteIpValve` absent from Tomcat configuration | Documented `server.xml` config | **Functional** | Root cause of scheme/port/IP issues above. One valve declaration fixes all of them. |
| `SYSTEM_URL` (ssa.properties) and `WEB_PATH` (DB constant) coexist as duplicate URL config mechanisms, applied inconsistently | Multiple servlets | **Architectural** | 5 servlets use raw `getScheme()` entirely bypassing both mechanisms. 4 servlets use `SYSTEM_URL`. 2 servlets use `WEB_PATH`. No single authoritative source of truth for the instance's base URL. |
| Hardcoded `"noreply@superiorstate.net"` FROM address in new-user and password-reset emails | `CreateUser25.java:330`, `HelpUserLogin.java:89` | **Architectural** | TPA deployments cannot brand outbound system emails without code change. |
| `MASTER_REGISTRY_URL` defaults to `"https://superiorstate.net"` | `DatabaseInitializer.java:878` | **Architectural** | TPA PSP instances register with SSA's master registry by default. TPA would need to update this constant to point to their own master or leave it unreachable. |
| `DocumentConstants.java` hardcodes SSA SharePoint and `superiorstate.net/tpo/docs/` | `DocumentConstants.java` | **Architectural** | TPA users see SSA-specific help documents. Not configurable. |
| `AmsDataGlobal.java:395` fallback hard-codes `"https://superiorstate.biz/"` | `AmsDataGlobal.java` | **Cosmetic** | If DB constant `WEB_PATH` is missing, fallback produces an SSA URL. Would only affect a misconfigured instance. |
| `VideoToken.ipAddress` — `X-Forwarded-For` extraction stores real client IP today (with nginx) | `ServeVideo.java:239–243` | **Cosmetic** | Currently correct behind nginx. Would continue to work with `RemoteIpValve` enforcing trusted proxies. Spoofing risk noted above. |
| `CreateBillingDiag.java` streaming response buffered by Cloudflare | `CreateBillingDiag.java` | **Cosmetic** | Admin-only diagnostic page loses "real-time" output appearance. Functional output is still delivered. |
| No WebSockets, SSE, or long-polling | — | **Trivial** | No special Cloudflare WebSocket proxy configuration needed. |
| `LoginFilter` and `ApiTokenFilter` are proxy-agnostic | Both filter classes | **Trivial** | Neither filter touches IP, scheme, or forwarded headers. No changes needed. |

---

## Section 12 — TPA Deployment Implications

### Deploying AMS at a TPA's own domain

1. **DNS:** TPA creates an A record pointing their chosen domain (e.g., `ams.theirtpa.com`) to the VPS IP. The current runbook documents A record only. No guidance exists for CNAMEs, wildcard records, or CDN-fronted deployments.

2. **ssa.properties:** TPA (or the operator) sets `SYSTEM_URL=https://ams.theirtpa.com`. This must match the nginx `server_name` and the cert domain.

3. **Initialization form:** TPA enters `https://ams.theirtpa.com` as the domain → seeds `WEB_PATH` DB constant.

4. **SSL:** Cert is provisioned via `certbot --nginx` after DNS propagates. Works correctly for direct-to-origin deployments.

### Deploying without any reverse proxy

If Tomcat serves HTTPS directly (connector `port="443"`, `scheme="https"`, `secure="true"`), then `getScheme()`, `getServerPort()`, and `getRemoteAddr()` are all correct and the URL-generation bugs disappear. This is not the current deployment model but is technically viable. The `Secure` session cookie flag would be set correctly. No `RemoteIpValve` is needed in this case.

### Deploying behind their own CDN (CloudFront, Fastly, etc.)

- Same `RemoteIpValve` gap applies — scheme and port would be wrong without the valve.
- The HTTP-01 cert renewal issue applies to any CDN that proxies port 80, not just Cloudflare. DNS-01 is the CDN-agnostic solution.
- `X-Forwarded-For` spoofing risk applies unless `RemoteIpValve` is configured with the CDN's IP ranges as trusted proxies.
- CDN-specific headers (`CF-Connecting-IP`, CloudFront's `CloudFront-Viewer-Address`) are not read by any AMS code. The app relies on `X-Forwarded-For` only.

### Can AMS be deployed without Cloudflare today?

Yes — the current production architecture (direct nginx) is not Cloudflare-specific. Nothing in the code requires Cloudflare. However:
- The URL-generation bugs are already present and would exist behind any reverse proxy without `RemoteIpValve`.
- The email FROM address `noreply@superiorstate.net` is SSA-specific and cannot be changed without a code change.
- `MASTER_REGISTRY_URL` defaults to SSA's master node — TPA would need to update or disable this.
- `DocumentConstants` links are SSA-only.

### Code assumptions that prevent clean non-SSA deployments

1. Hardcoded FROM email addresses
2. `MASTER_REGISTRY_URL` default
3. `AmsDataGlobal.java` fallback URL
4. `DocumentConstants.java` resource links (SharePoint + docs server)

These are straightforward to address with DB constants or `ssa.properties` additions, but currently require code changes.

---

## Section 13 — Recommended Next Phase

Ordered by priority:

### P0 — Immediate (blocks Cloudflare proxy enablement and fixes live production bugs)

1. **Configure `RemoteIpValve` in Tomcat `server.xml`** on all production VMs:
   ```xml
   <Valve className="org.apache.catalina.valves.RemoteIpValve"
          remoteIpHeader="X-Forwarded-For"
          protocolHeader="X-Forwarded-Proto"
          trustedProxies="127\.0\.0\.1"
          internalProxies="127\.0\.0\.1" />
   ```
   This fixes: `getScheme()` returning `"http"`, `getServerPort()` returning `8080`, `getRemoteAddr()` returning `127.0.0.1`, and the session cookie `Secure` flag. Add this to the master image before provisioning new PSPs. **This single change unblocks most of the issues below.**

2. **Switch cert renewal to DNS-01 before enabling Cloudflare proxy** (or use Cloudflare Origin CA). The production cert renews around 2026-05-19. If Cloudflare proxy is enabled before renewal, `certbot renew` will fail silently until the certificate expires.

3. **Migrate URL-generation in email-sending servlets to `WEB_PATH` DB constant** (`ProposalDetail`, `SendProposal`, `SendInvitation`, `EmailBillingToEmployer`, `125eligibility.jsp`). These links are already broken in production behind nginx. The `WEB_PATH` pattern used by `HelpUserLogin` and `CreateUser25` is the correct model.

4. **Fix `NdtTestRunServlet` to read `X-Forwarded-For` / use `RemoteIpValve`-aware IP** for `NdtAccessLog`. Once `RemoteIpValve` is configured, `request.getRemoteAddr()` will return the correct client IP automatically — no code change needed beyond the valve.

### P1 — Before TPA deployments

5. **Unify `SYSTEM_URL` and `WEB_PATH` into a single base-URL mechanism.** `WEB_PATH` is more portable (stored in DB, survives `ssa.properties` changes, accessible in all contexts). `SYSTEM_URL` in `ssa.properties` is needed for startup-time use before DB is initialized. Consider reading `SYSTEM_URL` from `ssa.properties` at init time and seeding `WEB_PATH` from it if the DB constant is empty, then using only `WEB_PATH` everywhere in the app.

6. **Externalize the email FROM address.** Add a `NOREPLY_EMAIL` DB constant (or `ssa.properties` key) seeded from the initialization form. `CreateUser25` and `HelpUserLogin` currently hard-code `"noreply@superiorstate.net"`.

7. **Externalize `MASTER_REGISTRY_URL` default.** The `DatabaseInitializer` seeds this with `"https://superiorstate.net"`. A TPA that doesn't use SSA's master registry gets a broken/unauthorized registry endpoint by default. Seed from `ssa.properties` `MASTER_URL` key or leave blank until the operator sets it.

8. **Add `RemoteIpValve` configuration to the master VM image** and update `docs/tomcat_ssl_setup.md` with the required `server.xml` snippet.

9. **Document DNS-01 renewal procedure** in `docs/tomcat_ssl_setup.md` as the recommended path for all new deployments (direct-to-origin or CDN-proxied).

### P2 — Hardening

10. **Fix `ServeVideo.getClientIp()` to use `RemoteIpValve`-aware `getRemoteAddr()`** (once the valve is in place, drop the manual `X-Forwarded-For` header read; the valve handles it). If the manual read is kept as a fallback, add trusted-proxy range validation to prevent IP spoofing.

11. **Add `SameSite=Lax` to session cookie config** in `web.xml`:
    ```xml
    <session-config>
      <cookie-config>
        <http-only>true</http-only>
        <secure>true</secure>
      </cookie-config>
    </session-config>
    ```
    (The `Secure` attribute here will work correctly once `RemoteIpValve` is in place and `isSecure()` returns true.)

12. **Cloudflare page rules** — configure bypass-cache rules for `/video/*` and `/api/*` paths to prevent response buffering and ensure API tokens are not cached. Also configure Cloudflare to strip `CF-Connecting-IP` from non-Cloudflare origins if deploying to both proxied and direct instances.

13. **Externalize `DocumentConstants`** — convert hard-coded SSA SharePoint and docs links to DB constants so TPA deployments can point to their own resources.
