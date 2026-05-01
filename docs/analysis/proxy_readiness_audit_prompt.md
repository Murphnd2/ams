# AMS Proxy / Reverse-Proxy Readiness Audit — Read-Only Investigation

## Purpose

This is **Phase 1** of enabling Cloudflare proxy (orange cloud) for SSA's deployment,
and documenting reverse-proxy compatibility for future TPA deployments where SSA
will not control DNS or proxy configuration.

This is a **READ-ONLY discovery phase**. Do not modify any code, configuration,
or deployment scripts. Output is a single analysis document.

## Context

AMS is currently exposed directly to the internet on each VPS:
- `master.superiorstate.biz`, `demo.superiorstate.biz`, `bpo.superiorstate.biz`
- Tomcat 10 with Let's Encrypt SSL (HTTP-01 challenge today)
- DNS now lives at Cloudflare with proxy DISABLED (gray cloud / DNS only)

The goal is to enable Cloudflare proxy on SSA's installation for DDoS protection
and origin IP hiding, while preserving the option for other TPAs to deploy AMS
without Cloudflare (direct-to-origin, or behind their own reverse proxy, CDN,
load balancer, etc.).

The right architecture is **proxy-agnostic**: Tomcat's `RemoteIpValve` handles
forwarded headers transparently when configured, and the app code should not
care whether a proxy is in front of it. Before changing anything, we need to
know whether the existing AMS code violates that assumption anywhere.

## Output

Produce `docs/analysis/proxy_readiness_audit.md` with the structure described
in "Deliverable Format" below. After completing the investigation, **stop**.
Do not proceed to code changes, configuration changes, or migration scripts.

## Investigation Sections

### 1. Client IP Address Handling

Search the codebase for every occurrence of:
- `request.getRemoteAddr()`
- `request.getRemoteHost()`
- `getRemoteUser()` (note this is auth, not IP — flag for completeness)
- Header reads for: `X-Forwarded-For`, `X-Real-IP`, `CF-Connecting-IP`, `True-Client-IP`
- Any other custom header reads pulling client identity

For each occurrence document:
- Full file path + line number
- The surrounding method/context
- The purpose of the IP read (audit logging, allowlist, throttling, display, session affinity)
- Whether it has any forwarded-header awareness today

### 2. Absolute URL & Hostname Generation

Search for:
- `request.getRequestURL()`
- `request.getRequestURI()` paired with manual scheme/host construction
- `request.getServerName()`
- `request.getServerPort()`
- `request.getScheme()`
- String concatenation building URLs ("https://" + ...)
- Hardcoded references to `superiorstate.biz`, `superiorstate.net`, or any
  specific hostname in code (not config/docs)
- Email link generation (password resets, notifications, proposal share links,
  application invitation links, agency portal access)
- Any centralized "base URL" constant or configuration

For each, document where it's used and whether it would produce the wrong
output behind a reverse proxy that terminates SSL.

### 3. Scheme / SSL Handling

- Where is `request.isSecure()` called?
- Any HTTP→HTTPS redirect logic? Where?
- Session cookie configuration — is `Secure` flag set? Where?
- `SameSite` attribute on cookies?
- Any code that checks `getScheme()` to make decisions?

### 4. Tomcat Configuration

Inventory all relevant config files:
- `server.xml` — every `<Connector>` block (ports, secure/scheme attributes,
  proxy attributes), any `<Valve>` (Engine, Host, or Context level)
- `context.xml` — any Valve, Manager, or Resource referencing client IP/host
- `web.xml` (both Tomcat-default and AMS-specific) — filters, security constraints
- Any `application.properties` or system properties consumed at runtime

Specifically: is `org.apache.catalina.valves.RemoteIpValve` configured anywhere?

### 5. Custom Servlet Filters

List every class implementing `jakarta.servlet.Filter` (or `javax.servlet.Filter`):
- File path, @WebFilter URL pattern, ordering
- Whether it touches client IP, scheme, host, or forwarded headers
- `LoginFilter` is known — document what else exists and what each does

### 6. SSL / Cert Setup (deployment, not code)

Check `docs/deployment_strategy.md`, `docs/deployment_backlog.md`, and any
scripts in `docs/` or repo root for:
- How SSL is terminated (Tomcat directly? nginx in front? IIS for legacy?)
- ACME challenge type currently used (HTTP-01 implies port 80 must hit origin —
  this BREAKS behind Cloudflare proxy unless using DNS-01 or Cloudflare Origin CA)
- Cert renewal automation — script paths, cron entries
- Port 80 → 443 redirect logic location

### 7. Multi-Tenant Hostname & Deployment Configuration

Critical for TPA deployments:
- How does an AMS instance determine its own public hostname?
  Hardcoded? Property file? Database? `AmsDataGlobal` constant? Per-PSP setting?
- Where would a new TPA configure their hostname when deploying?
- Does any code path assume a specific domain (`*.superiorstate.biz`,
  `*.superiorstate.net`)?
- Are there any DNS-related instructions documented for non-SSA deployments?
- Is `BPO_HOST` / `MASTER_HOST` / `DEMO_HOST` (or similar) handled anywhere?
- How are subdomain-based tenants distinguished, if at all?

### 8. Audit Logging & Persisted IPs

Search for code that writes IPs to the database:
- Activity logs, login attempts, session creation, security events
- Document affected entity classes, table names, column names
- Note whether the column would be polluted with proxy IPs (e.g., 172.x Cloudflare
  ranges) instead of real client IPs if proxy is enabled without `RemoteIpValve`

### 9. WebSocket / Streaming Endpoints

Check whether AMS has any:
- WebSocket endpoints (`@ServerEndpoint` or similar)
- Long-polling endpoints
- Server-sent events (SSE)
- File upload/download endpoints with non-standard streaming

These have specific Cloudflare-proxy implications (WebSocket support, response
buffering, request size limits) worth flagging.

### 10. Hardcoded URLs in Email/Notification Templates

Check JSP templates, mail templates, AI prompt templates for:
- Hardcoded protocol/host references
- Reliance on environment-specific URLs
- Any code generating links sent externally (to agencies, employers, agents)

## Deliverable Format

`docs/analysis/proxy_readiness_audit.md` with these top-level sections matching
the investigation outline above (sections 1-10), plus:

### 11. Risk Summary

A consolidated table classifying each significant finding as:
- **Trivial** — works as-is with `RemoteIpValve`, no app code change needed
- **Cosmetic** — works behind proxy but stores/displays Cloudflare IP unless valve configured
- **Functional** — will break or misbehave behind proxy without code change
- **Architectural** — design decision needed (e.g., per-tenant proxy strategy, TPA deployment guide)

### 12. TPA Deployment Implications

A short section addressing:
- What instructions would a TPA need to deploy AMS at their own domain?
- What DNS records would they need to create (A record? CNAME? both?)
- What happens if they deploy AMS without any reverse proxy?
- What happens if they deploy behind their own CDN (CloudFront, Fastly, etc.)?
- Are there any code assumptions that would prevent non-Cloudflare deployments?

### 13. Recommended Next Phase

A short list of the concrete code/config changes that would need to happen,
ordered by priority. **Do not implement them.** Just enumerate.

## Constraints

- READ-ONLY: do not edit any source files, configs, or scripts
- Do not run the application
- Do not run tests
- Do not create migration scripts (no schema changes are part of this phase)
- Do not propose code yet — just document findings

## Anti-Goals

- No scope creep into unrelated infrastructure
- No premature design decisions — just discovery
- No "while I'm in here" cleanup of unrelated code
- No suggestions to upgrade Tomcat / Java / dependencies
