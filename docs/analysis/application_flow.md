# SSA Web Application — Entry Point & Flow Analysis

**Last Updated:** February 21, 2026
**Total Servlets Mapped:** compute via `git grep -c "@WebServlet" src/main/java | wc -l` rather than trusting a static number — this figure changes with the code and any hand-maintained count here is guaranteed to drift.

---

## 1. PUBLIC ENTRY POINTS (No Authentication Required)

These URLs are accessible without login (whitelisted in LoginFilter):

| URL | Servlet | Purpose |
|-----|---------|---------|
| `/` | (none) | Root — redirects to login |
| `/login` | login | Login page |
| `/index.jsp` | (JSP direct) | Login form page |
| `/landing-page.jsp` | (JSP direct) | Public marketing landing page |
| `/market/landing` | LandingServlet | Market landing page |
| `/AuthenticateUser` | AuthenticateUser | POST login credentials |
| `/NeedsHelp` | NeedsHelp | Login help page |
| `/HelpUserLogin` | HelpUserLogin | Request password reset email |
| `/OneTimeUserLogin` | OneTimeUserLogin | Process GUID login links |
| `/ResetLogin` | ResetLogin | POST new password |
| `/initialize.jsp` | (JSP direct) | Database initialization form |
| `/GoInitialize25` | GoInitialize25 | Initialize form page |
| `/InitializeDataBase` | InitializeDataBase | **DANGER:** Initializes DB |
| `/EmployerBillingDetail` | (servlet) | Employer billing |
| `/LogOut` | LogOut | Logout |
| `/viewProposal/*` | ViewProposal | Public proposal landing page (GUID) |
| `/apply/*` | ApplyForProposal | Public application form (GUID) |
| `/saveApplication` | SaveApplicationProgress | AJAX auto-save field values |
| `/uploadRateSheet` | UploadRateSheet | AJAX file upload to Wasabi |
| `/ShowFileUpload` | ShowFileUpload | Pre-signed Wasabi URL redirect |
| `/AcceptInvite` | AcceptInvite | Agent invitation registration (GUID) |

**Static Resources (No Auth):**
- /images/*, /css/*, /js/*, /fonts/*, /webfonts/*, /bootstrap-icons/*
- /logo* (any logo file)
- *.png, *.jpg, *.jpeg, *.gif, *.svg, *.ico

---

## 2. AUTHENTICATION FLOW

```
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
AuthenticateUser validates credentials (AuthDAO.validateLogin)
   ↓
SUCCESS:
  - Creates AmsDataLocal in session
  - Sets local.authenticated = true
  - Loads user data, PSP, roles
  - Role-based redirect:
      PSP User/Admin → ViewHome25
      Agent/Agency Manager → AgentHome
   ↓
FAILURE:
  - displayLoginFailure() called (FIXME: not implemented)

ALTERNATIVE FLOWS:
  Forgot Password → /NeedsHelp → /HelpUserLogin → Email with GUID
                                                      ↓
  User clicks link → /OneTimeUserLogin?guid=xxx
                                 ↓
                    /WEB-INF/view/authentication/resetPassword.jsp
                                 ↓
  Submit new password → /ResetLogin → /index.jsp

INVITATION FLOW:
  PSP sends invite → email with /AcceptInvite?guid=xxx
                                 ↓
                    acceptInvite.jsp (registration form)
                                 ↓
  Submit → AcceptInvite POST → creates User → /index.jsp (login)
```

---

## 3. MAIN APPLICATION HUB SERVLETS

After authentication, traffic flows through these central hubs based on role:

### ViewHome25 (PSP Dashboard)
**URL:** `/ViewHome25`
**Purpose:** Activity listing/dashboard — the home base for PSP users
**Forwards to:** Activity landing page (JSP)
**Called by:** 30+ servlets after completing actions

### AgentHome (Agent/Agency Manager Dashboard)
**URL:** `/AgentHome`
**Purpose:** Pipeline view with opportunity management for agents
**Forwards to:** `/WEB-INF/view/sales/agentHome25.jsp`
**Features:** Stage-grouped opportunity list, detail panel, new opportunity modal, quick stats

### ViewActivity25 (Activity Detail View)
**URL:** `/ViewActivity25`
**Purpose:** Display single activity with all details (notes, checklist, contacts, email)
**Forwards to:** `/WEB-INF/view/a/activityDetail/activityDetail25.jsp`
**Supports:** Ticket, Renewal, Setup, Opportunity activity types
**Called by:** 20+ servlets after activity-related actions

---

## 4. MAJOR FEATURE FLOWS

### A. EMAIL SENDING WORKFLOW

```
CreateEmail25 → /WEB-INF/view/a/general/emailMaster25.jsp
   ↓
User fills form (CKEditor), adds recipients, attaches files
   ↓
SaveEmailState25 (routes based on action parameter):
   ├─ action="SE" → SendEmail25 → EmailTemplate wrap → SMTP via EmailDAO → ViewActivity25
   ├─ action="AR" → AddRecipient25 → CreateEmail25
   ├─ action="DR" → RemoveRecipient25 → CreateEmail25
   ├─ action="AA" → AddAttachment25 (uploads to Wasabi) → CreateEmail25
   └─ action="DA" → RemoveAttachment25 → CreateEmail25
```

**Technology:** SMTP via EmailDAO (branded HTML template via EmailTemplate class). Attachments stored in Wasabi S3.

### B. AUTOMATION EMAIL WORKFLOW

```
SendAuto25 → Displays automation form with inputs
   ↓
User fills inputs
   ↓
SendAutoFinal25
   ↓
- Processes automation template
- Replaces placeholders <[{0}]> to <[{19}]>
- Creates Email entity
- Sends via EmailDAO.sendEmail() (SMTP)
- Appends email to Activity
   ↓
ViewActivity25
```

### C. ACTIVITY LIFECYCLE

```
CREATE:
  Ticket/Renewal/Setup:
    TaskBuilder25 → Creates activity + checklist → ViewActivity25
  Opportunity:
    CreateOpportunity → Creates opportunity + checklist → redirect AgentHome

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
CreateChecklist25 → ViewHome25
ClearGrid25 → sequence builder form
MakeRecurringFromChecklist25 → converts to recurring
```

### E. SEQUENCE BUILDER FLOW

```
SequenceBuilder25 → /WEB-INF/view/a/general/sequenceBuilder/sequenceManager25.jsp
   ↓
Two-panel layout: left = sequence list, right = task builder
   ↓
SequenceAction25 (handles SAVE, CREATE, DELETE)
   ↓
→ redirect SequenceBuilder25
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

### G. SALES PIPELINE FLOW

```
PROPOSAL:
  ProposalBuilder (GET=form, POST=create) → ProposalDetail
    ↓
  SendProposal → email with GUID link
    ↓
  ViewProposal (public, GUID) → prospect views pricing/features
    ↓
  ApplyForProposal (public, GUID) → prospect fills application
    ↓
  SaveApplicationProgress (AJAX auto-save)
    ↓
  ApplyForProposal POST → submits application
    ↓
  ReviewApplications → list of submitted applications
    ↓
  ReviewApplication → approve/deny/more-info → Setup created on approval

OPPORTUNITY:
  AgentHome → CreateOpportunity → Opportunity created with checklist
    ↓
  ViewById → GoActivityDetail25 → ViewActivity25 (Opportunity detail)
    ↓
  UpdateOpportunityStage (AJAX stage dropdown)
```

### H. INVITATION FLOW

```
PSP Admin:
  PspAgencyHome → Invite Modal → SendInvitation
    ↓
  Creates Agency (if new), Person, Invitation → sends email
    ↓
Agent:
  Email link → AcceptInvite GET → acceptInvite.jsp (registration form)
    ↓
  AcceptInvite POST → creates User, grants role → redirect to login
```

---

## 5. FILTER CHAIN

### LoginFilter (ACTIVE)
- **Pattern:** `/*` (ALL requests)
- **Purpose:** Authentication guard
- **Behavior:**
  - Checks `AmsDataLocal.isAuthenticated()` in session
  - Allows public endpoints (see §1)
  - Allows static resources
  - Redirects to /login if not authenticated

### CsrfFilter (NOT ACTIVE — SECURITY ISSUE)
- **Pattern:** None (no @WebFilter annotation)
- **Purpose:** CSRF protection for automation emails
- **ISSUE:** Filter class exists but is NOT REGISTERED as @WebFilter. CSRF protection may not be working.

---

## 6. PACKAGE STRUCTURE

```
src/main/java/net/superiorstate/ams/
├── controller/
│   ├── activity/           ← Activity CRUD, AddFileToTask, ShowFileUpload, StdAuto
│   │   ├── contact/        ← AddContactToActivity, AddActivityContact25, ModifyContact25
│   │   ├── renewal/        ← Renewal servlets
│   │   ├── setup/          ← GenerateProp25, ProposalBuilder, ReviewApplication,
│   │   │                     AgentHome, CreateOpportunity, SendInvitation, AcceptInvite,
│   │   │                     PspAdminHome, PspAgencyHome, ServiceManagerHome, LibraryHome
│   │   └── ticket/         ← CreateTicket25
│   ├── admin/              ← Admin tooling
│   ├── api/                ← M2M API + Outlook integration
│   ├── assistant/          ← ChatAssistant (AI chatbot)
│   ├── authentication/     ← AuthenticateUser, login
│   ├── checklist/          ← Checklist management, AddRecurringSequence
│   ├── data/               ← Import/export servlets
│   ├── email/              ← Email workflow, ViewEmail
│   ├── home/               ← Home dashboards (likely per role)
│   ├── market/             ← Marketing/library
│   ├── monthly/            ← Billing servlets
│   ├── sequence/           ← SequenceBuilder25, SequenceAction25
│   └── user/               ← User management
├── data/
│   ├── dao/                ← All database query classes (15 DAOs)
│   ├── resolver/           ← Entity lookups, person resolution
│   ├── service/            ← Business logic (billing, imports, sync, Claude API, knowledge search)
│   └── util/               ← Validators, helpers, constants, EmailTemplate
│   ├── AmsDataGlobal.java  ← Application-scoped state
│   ├── AmsDataLocal.java   ← Session-scoped state
│   └── ActivityFilter.java
├── filter/                 ← LoginFilter
└── model/
    ├── activity/           ← Activity, Opportunity, CheckList, Renewal, Ticket, Setup
    │   ├── checklist/      ← CheckList, ToDo, Task, sequences
    │   ├── note/           ← Note, Email entity
    │   ├── renewal/
    │   └── ticket/
    ├── billing/            ← Billing entities
    ├── general/            ← Person, User, PSP, Address
    ├── sales/              ← Agency, Prospect, Proposal, Application, Invitation
    │   └── offering/       ← LOS, Enhancement, ServiceModule, Rate, Feature,
    │                         ResourceCategory, MarketingMaterial
    ├── summit/             ← Employee, Employer, Benefit (archive, imports, temp)
    └── (root)              ← Activity25, Constant, view-backed DTOs
```

---

## 7. CRITICAL FINDINGS

### Security Issues
1. **CsrfFilter not registered** — CSRF protection may not be working
2. **InitializeDataBase accessible** — Dangerous admin function is whitelisted
3. **AuthenticateUser.displayLoginFailure()** — Not implemented (FIXME)
4. **No rate limiting** — Login attempts not throttled

### Completed Cleanup
- **238 dead files deleted** across 5 sessions
- **32 cryptic data classes renamed**
- **`previous/` package entirely eliminated**
- All code now in clean `controller/`, `data/`, `model/` packages

---

## 8. DEPENDENCY GRAPH

```
Entry → LoginFilter → AuthenticateUser
                            ↓
              ┌─────────────┼──────────────┐
              ↓             ↓              ↓
         ViewHome25    AgentHome    (admin pages)
         (PSP users)   (Agents)
              ↓             ↓              ↓
        ViewActivity25 ← ← ┘     ServiceManagerHome
              ↓                   PspAdminHome
        [Activity Features]       PspAgencyHome
        AddNote, ToDo,            LibraryHome
        SendEmail,
        CloseActivity
              ↓
        ViewHome25 / AgentHome
```

---

## 9. TECHNOLOGY STACK

**Backend:**
- Jakarta EE 10 (Servlets, JPA via EclipseLink)
- Tomcat 10
- Java 17, Maven WAR packaging
- MySQL database (beta_ssa schema)

**Authentication:**
- Session-based (AmsDataLocal in session)
- SHA-512 password hashing with salt
- Role-based routing (PSP → ViewHome25, Agent → AgentHome)

**Email:**
- SMTP via EmailDAO (provider-agnostic — SMTP2GO, Gmail, any SMTP service)
- Branded HTML template via EmailTemplate class
- PSP-specific colors from DB constants

**Storage:**
- Wasabi S3-compatible cloud storage (ams-file-storage bucket)
- Pre-signed URLs for downloads (1-hour or 7-day expiry)
- StorageDAO handles upload, download URL generation, delete

**AI:**
- Anthropic Claude API (Haiku 4.5) for employee knowledge chatbot
- RAG with pre-indexed JSON knowledge bases + live ticket resolution queries

**Frontend:**
- JSP with JSTL
- Bootstrap 5
- Bootstrap Icons
- CKEditor (email composer)
- SortableJS (drag-and-drop in sequence builder, service manager)

**Deployment:**
- WAR file to Tomcat
- Domain: superiorstate.biz
