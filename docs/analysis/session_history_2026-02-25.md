## February 25, 2026 — Role Cleanup + Login Modernization + PSP Branding

### UserRole Cleanup
- **Deleted roles:** 6 (Pending Agent), 7 (Anonymous), 10 (Other) — never checked in code
- **Renamed:** 102 → "BPO Admin", 103 → "BPO User" (removed Accelergent branding)
- **Updated:** `ReferenceDataSeeder.fillUserRoles()`, `DatabaseInitializer` role creation, `AmsDataGlobal.loadAssignableRoles()` exclude list (2, 3, 4, 8, 9, 102, 103), removed `getUsersByRole(em, 101)` call that caused NoResultException
- **CreateUser25 fix:** PSP Admin can now create sales-only users (no role checkboxes required) — programmatically adds role 2 (Agent) + optional 8 (Agency Admin)

### Login UI Modernization
- **index.jsp:** Self-contained SSA-branded login page — centered card with `#0d5681` header, dynamic logo from `AmsDataGlobal`, form inline (no modal), success/error alerts
- **loginHelp.jsp:** Matching card style — "Reset My Password" primary button, "Send One-Time Login Link" outlined secondary with "or" divider, URL param-driven alerts (`?status=sent`, `?status=notfound`)
- **HelpUserLogin.java:** Full rewrite — WEB_PATH + email domain fallback for links, PSP-branded emails via `EmailTemplate.wrapBodyOnly()`, 7-day GUID for password resets, 10-minute for one-time logins, proper redirects instead of silent forwards
- **OneTimeUserLogin.java:** Fixed one-time login — `authenticateAndRedirect()` now mirrors `AuthenticateUser.loadSessionData25()` exactly (creates `AmsDataLocal`, sets `authenticated=true`, calls `intializeLocalData`, assigns roles, initializes timeclock, role-based redirect)

### initialize.jsp Modernization
- Full GUI rewrite — self-contained SSA card matching login/loginHelp style, no navbar import
- Grouped form sections: Authorization, Company, Address, Primary Contact, Domain & Services, Email (SMTP)
- Post-initialization state shows "System Ready" with checkmark and login button
- Password field now uses `type="password"`

### LoginFilter Enhancement
- **Three-tier filter logic:** (1) Static resources always pass, (2) Uninitialized DB → only `/initialize.jsp`, `/GoInitialize25`, `/InitializeDataBase` allowed — everything else redirects to `/initialize.jsp`, (3) Normal auth check for initialized databases
- Prevents login page from showing before database initialization
- Removed console spam (`System.out.println("LoggedIn")` etc.)

### PSP Branding System (Logo/Favicon)
- **Database constants:** `LOGO_NAVBAR`, `LOGO_LOGIN`, `FAVICON` — seeded by `DatabaseInitializer.addPspConstants()` with generic defaults
- **AmsDataGlobal:** Three new fields loaded in `setConstants()` with fallback paths (`/images/logoA.png`, `/images/logoD.png`, `/favicon.ico`)
- **Dynamic JSP references:** `navbar25.jsp` (3 instances), `index.jsp`, `css-js.jsp` (favicon) — all use `${applicationScope.global.logoNavbar}` etc. with JSTL fallbacks for pre-init state
- **UploadPspBranding servlet:** PSP Admin-gated file upload with dimension validation (navbar: max 300×80px PNG, login: max 800×400px PNG, favicon: 32×32px ICO/PNG), saves to `/images/psp/`, updates constants, refreshes `AmsDataGlobal` immediately
- **pspBranding25.jsp:** Upload page with live file previews, dark/light preview backgrounds, specs display, SSA styling
- **Navbar link:** "Branding" added to Admin dropdown (PSP Admin only) with palette icon
- **ImageIO fix:** `setUseCache(false)` to avoid temp directory permission issues on Windows

### New Files
| File | Location |
|------|----------|
| `UploadPspBranding.java` | `controller/user/` |
| `pspBranding25.jsp` | `WEB-INF/view/user/` |

### Modified Files
| File | Changes |
|------|---------|
| `AmsDataGlobal.java` | Added `logoNavbar`, `logoLogin`, `favicon` fields + `setConstants()` loading |
| `DatabaseInitializer.java` | Seeds `LOGO_NAVBAR`, `LOGO_LOGIN`, `FAVICON` constants; role cleanup |
| `LoginFilter.java` | Three-tier logic (static → uninitialized → auth); removed console logging |
| `HelpUserLogin.java` | Full rewrite (branded emails, WEB_PATH, 7-day/10-min GUIDs, redirects) |
| `OneTimeUserLogin.java` | Fixed `authenticateAndRedirect()` to use proper `AmsDataLocal` session setup |
| `CreateUser25.java` | Sales-only user creation fix |
| `navbar25.jsp` | Dynamic logo references, Branding admin link |
| `index.jsp` | SSA card login, dynamic logo |
| `loginHelp.jsp` | SSA card style with URL param alerts |
| `initialize.jsp` | Full GUI modernization |
| `css-js.jsp` | Dynamic favicon reference |

### Deployment Backlog
- **D-31 added:** Microsoft 365 SSO (Optional Per-PSP) — LOW priority, Phase 2
