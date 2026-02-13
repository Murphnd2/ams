# SSA Web Application - Complete Entry Point & Flow Analysis

**Generated:** Analysis Complete
**Total Servlets Mapped:** 80+

---

## APPLICATION ENTRY POINTS

### 1. PUBLIC ENTRY POINTS (No Authentication Required)

These URLs are accessible without login (whitelisted in LoginFilter):

| URL | Servlet | Purpose | Active |
|-----|---------|---------|--------|
| `/` | (none) | Root - likely redirects | ✓ |
| `/login` | login | Login page | ✓ |
| `/index.jsp` | (JSP direct) | Login form page | ✓ |
| `/landing-page.jsp` | (JSP direct) | Public marketing landing page | ✓ |
| `/market/landing` | LandingServlet | Market landing page | ✓ |
| `/AuthenticateUser` | AuthenticateUser | POST login credentials | ✓ |
| `/NeedsHelp` | NeedsHelp | Login help page | ✓ |
| `/HelpUserLogin` | HelpUserLogin | Request password reset email | ✓ |
| `/OneTimeUserLogin` | OneTimeUserLogin | Process GUID login links | ✓ |
| `/ResetLogin` | ResetLogin | POST new password | ✓ |
| `/initialize.jsp` | (JSP direct) | Database initialization form | ✓ |
| `/GoInitialize25` | GoInitialize25 | Initialize form page | ✓ |
| `/InitializeDataBase` | InitializeDataBase | **DANGER:** Initializes DB | ✓ |
| `/EmployerBillingDetail` | (servlet TBD) | Employer billing | ✓ |
| `/LogOut` | LogOut | Logout | ✓ |

**Static Resources (No Auth):**
- /images/*, /css/*, /js/*, /fonts/*, /webfonts/*, /bootstrap-icons/*
- /logo* (any logo file)
- *.png, *.jpg, *.jpeg, *.gif, *.svg, *.ico

---

## 2. AUTHENTICATION FLOW

```
┌─────────────────────────────────────────────────────────────┐
│                     AUTHENTICATION CHAIN                     │
└─────────────────────────────────────────────────────────────┘

START: User visits any protected URL
   ↓
LoginFilter (/*) checks authentication
   ↓
NOT AUTHENTICATED → redirect to /login
   ↓
/login → login servlet → /index.jsp (login form)
   ↓
User submits form → /AuthenticateUser (POST)
   ↓
AuthenticateUser validates credentials (dbAuth.validateLogin)
   ↓
SUCCESS:
  - Creates AmsDataLocal in session
  - Sets local.authenticated = true
  - Loads user data, PSP, roles
  - Forward to ViewHome25 → Dashboard
   ↓
FAILURE:
  - displayLoginFailure() called (FIXME: not implemented!)
  - User stuck on form?

ALTERNATIVE FLOWS:
  Forgot Password → /NeedsHelp → /HelpUserLogin → Email with GUID
                                                      ↓
  User clicks link → /OneTimeUserLogin?guid=xxx
                                 ↓
                    /WEB-INF/view/authentication/resetPassword.jsp
                                 ↓
  Submit new password → /ResetLogin → /index.jsp
```

---

## 3. MAIN APPLICATION HUB SERVLETS

After authentication, all traffic flows through these central hubs:

### ViewHome25 (Main Dashboard)
**URL:** `/ViewHome25`
**Purpose:** Activity listing/dashboard - the home base
**Forwards to:** Activity landing page (JSP)
**Called by:** 30+ servlets after completing actions

**Servlets that return to ViewHome25:**
- CloseActivity25
- ModifyRecurringTask25
- WipeTables25
- RefreshTicketEmployees
- CreateChecklist25
- All "complete action, return home" servlets

### ViewActivity25 (Activity Detail View)
**URL:** `/ViewActivity25`
**Purpose:** Display single activity with all details
**Forwards to:** /WEB-INF/view/a/activityDetail/activityDetail25.jsp
**Called by:** 20+ servlets after activity-related actions

**Servlets that return to ViewActivity25:**
- AddNoteToActivity25
- AddActivityContact25
- AddToDo25
- SendEmail25
- SendAutoFinal25
- GoActivityDetail25 (prepares data, then forwards here)

### GoAdminHome (Admin Dashboard)
**URL:** `/GoAdminHome`
**Purpose:** Admin control panel
**Forwards to:** /WEB-INF/view/adminHome.jsp
**Called by:** Many admin servlets

---

## 4. MAJOR FEATURE FLOWS

### A. EMAIL SENDING WORKFLOW

```
CreateEmail25 → /WEB-INF/view/a/general/emailMaster25.jsp
   ↓
User fills form, clicks action button
   ↓
SaveEmailState25 (routes based on action parameter):
   ├─ action="SE" → SendEmail25 → Microsoft Graph API → ViewActivity25
   ├─ action="DR" → RemoveRecipient25 → CreateEmail25
   ├─ action="DA" → RemoveAttachment25 → CreateEmail25
   └─ action="AR" → AddRecipient25 (TBD) → CreateEmail25
```

**Technology:** Microsoft Graph API with Azure AD OAuth2

### B. AUTOMATION EMAIL WORKFLOW

```
SendAuto25 → Displays automation form with inputs
   ↓
User fills inputs
   ↓
SendAutoFinal25 (with CSRF protection)
   ↓
- Processes automation template
- Replaces placeholders <[{0}]> to <[{19}]>
- Creates Email entity
- Sends via dbEmail.sendEmail()
- Appends email to Activity
   ↓
ViewActivity25
```

### C. ACTIVITY LIFECYCLE

```
CREATE:
  TaskBuilder25 → Creates activity setup
     ↓
  ViewActivity25 (new activity detail)

VIEW:
  ViewById?id=123 → GoActivityDetail25 → ViewActivity25

UPDATE:
  AddNoteToActivity25 → ViewActivity25
  AddActivityContact25 → ViewActivity25
  AddToDo25 → ViewActivity25

CLOSE:
  CloseActivity25 → ViewHome25
```

### D. CHECKLIST MANAGEMENT

```
CURRENT (25 versions):
  CreateChecklist25 → ViewHome25
  ClearGrid25 → sequence builder form
  MakeRecurringFromChecklist25 → converts to recurring

LEGACY (previous package):
  ChecklistManagerGo → checklist home
  TaskDetailView → ChecklistManagerGo
  ApplySequenceFilter → ChecklistManagerGo
```

### E. SEQUENCE BUILDER FLOW

```
SequenceHome → Main sequence management page
   ↓
ChangeTaskSequenceTable → switches between:
   ├─ RequiredSequenceBuilder → SequenceHome
   └─ RecurringSequenceBuilder → SequenceHome
```

### F. MONTHLY DATA IMPORT WORKFLOW

```
ShowUploadPage → File upload form
   ↓
User uploads CSV/Excel files
   ↓
PreviewUploadedFiles → Shows matched tables
   ↓
RunSelectedImportsServlet → Processes imports
   ↓
- Converts to UTF-8
- Maps to table structures
- Imports via Importer.java
- Moves to processed directory
   ↓
ViewActivity25
```

---

## 5. FILTER CHAIN

### LoginFilter (ACTIVE)
- **Pattern:** `/*` (ALL requests)
- **Purpose:** Authentication guard
- **Behavior:** 
  - Checks `AmsDataLocal.isAuthenticated()` in session
  - Allows public endpoints (see list above)
  - Allows static resources
  - Redirects to /login if not authenticated

### CsrfFilter (NOT ACTIVE - SECURITY ISSUE)
- **Pattern:** None (no @WebFilter annotation!)
- **Purpose:** CSRF protection for automation emails
- **Behavior:** 
  - Generates CSRF token in session
  - Checks token on requests with `sendAutoEmail` parameter
- **ISSUE:** Filter class exists but is NOT REGISTERED as @WebFilter
  - May not be active in production!
  - CSRF protection may not be working

---

## 6. PACKAGE STRUCTURE & MIGRATION STATUS

```
┌────────────────────────────────────────────────────────┐
│                    PACKAGE HIERARCHY                    │
└────────────────────────────────────────────────────────┘

controller (net.superiorstate.ams.controller)
├─ Status: CURRENT/ACTIVE
├─ Servlets: ~40
├─ Examples: ViewHome25, ViewActivity25, SendEmail25
└─ Pattern: Servlet names end in "25"

previous.controller (net.superiorstate.ams.previous.controller)
├─ Status: LEGACY - Still in use, migration in progress
├─ Servlets: ~25
├─ Examples: AuthenticateUser, GoAdminHome, RenewalHome
└─ Sub-packages:
    ├─ authentication (login system)
    ├─ general.admin (admin features)
    ├─ activity.* (activity management)
    └─ checklist.* (checklist features)

previous.archive (net.superiorstate.ams.previous.archive)
├─ Status: DEAD CODE - Safe to delete
├─ Servlets: ~10
├─ Examples: sendAutomationFinal, emailActionsNew
└─ All confirmed unused

previous.filter
├─ CsrfFilter (not registered!)
└─ Status: Unclear if active
```

---

## 7. CRITICAL FINDINGS

### Security Issues
1. **CsrfFilter not registered** - CSRF protection may not be working
2. **InitializeDataBase accessible** - Dangerous admin function is whitelisted
3. **AuthenticateUser.displayLoginFailure()** - Not implemented (FIXME)
4. **No rate limiting** - Login attempts not throttled

### Dead Code Confirmed (Safe to Delete)
**Archive Package:**
- sendAutomationFinal
- emailActionsNew  
- doCheckListAction
- goCheckListDetail
- createSimpleChecklist

**Other:**
- HomeServlet (empty implementation)

### Duplicate Resolution
**Use these versions:**
- TaskBuilder25 (not TaskBuilder)
- ClearGrid25 (not ClearGrid)
- MakeRecurringFromChecklist25 (not MakeRecurringFromChecklist)
- SendAuto25/SendAutoFinal25 (not SendAutoEmail, SendAutomationEmailFinal)

### Migration Targets (Still in previous.controller)
**High Priority (used on every request):**
- Authentication servlets (AuthenticateUser, LogOut, etc.)
- GoAdminHome
- ChecklistManagerGo

**Medium Priority:**
- RenewalHome
- SequenceHome
- Email servlets (GoEmailHome, EmailActions)

---

## 8. DEPENDENCY GRAPH

```
Entry → LoginFilter → AuthenticateUser → ViewHome25
                                            ↓
                        ┌───────────────────┼───────────────────┐
                        ↓                   ↓                   ↓
                  ViewActivity25      GoAdminHome       ChecklistManagerGo
                        ↓                   ↓                   ↓
              [Activity Features]   [Admin Features]   [Checklist Features]
                        ↓                   ↓                   ↓
                  AddNote, ToDo,      RefreshData,      TaskDetailView,
                  SendEmail,          UpdateSettings,   ApplyFilter,
                  CloseActivity       SendAuto          SequenceBuilder
                        ↓                   ↓                   ↓
                  ViewActivity25      GoAdminHome       ChecklistManagerGo
                        ↓                   ↓                   ↓
                    [Return to home] ← ← ← ← ← ← ← ← ← ← ← ← ←
                        ↓
                   ViewHome25
```

---

## 9. TECHNOLOGY STACK

**Backend:**
- Jakarta EE 10 (Servlets, JPA)
- Tomcat 10
- MySQL database (beta_ssa schema)
- EntityManager (JPA) for database access

**Authentication:**
- Session-based (AmsDataLocal in session)
- SHA-512 password hashing with salt
- Azure AD OAuth2 (for email sending)

**Email:**
- Microsoft Graph API
- Azure AD authentication
- Service account sending

**Frontend:**
- JSP with JSTL
- Bootstrap 5
- Bootstrap Icons

**Deployment:**
- WAR file to Tomcat
- Domain: superiorstate.biz

---

## 10. NEXT STEPS FOR CLEANUP

### Immediate Actions
1. ✅ Delete archive package servlets (confirmed dead)
2. ✅ Delete HomeServlet (empty implementation)
3. ⚠️ Register CsrfFilter with @WebFilter or remove it
4. ⚠️ Implement AuthenticateUser.displayLoginFailure()
5. ⚠️ Remove InitializeDataBase from LoginFilter whitelist (or protect better)

### Short Term
1. Map all JSP files to identify orphaned pages
2. Migrate authentication servlets from previous.controller to controller
3. Add rate limiting to AuthenticateUser
4. Add logging to LoginFilter

### Medium Term
1. Migrate remaining previous.controller servlets to controller
2. Delete previous package entirely
3. Consolidate duplicate JSP files
4. Add comprehensive error handling

### Long Term
1. Replace session-based auth with JWT
2. Add API endpoints (REST)
3. Migrate from JSP to modern frontend framework
4. Add comprehensive testing
