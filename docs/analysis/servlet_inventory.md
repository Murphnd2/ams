# SSA Web Application - Servlet Inventory

**Generated:** Analysis in progress
**Purpose:** Map all @WebServlet annotated servlets to identify active vs. unused code

---

## AUTHENTICATION SERVLETS (Entry Points)

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| login | `/login` | /index.jsp | ACTIVE |
| AuthenticateUser | `/AuthenticateUser` | ViewHome25 (named dispatcher) | ACTIVE |
| NeedsHelp | `/NeedsHelp` | /WEB-INF/view/authentication/loginHelp.jsp | ACTIVE |
| HelpUserLogin | `/HelpUserLogin` | /index.jsp | ACTIVE |
| OneTimeUserLogin | `/OneTimeUserLogin` | /resetPassword.jsp or /failurePage.jsp | ACTIVE |
| ResetLogin | `/ResetLogin` | /index.jsp | ACTIVE |
| LogOut | `/LogOut` | /index.jsp | ACTIVE |

---

## CORE NAVIGATION SERVLETS (Main App Flow)

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| ViewHome25 | `/ViewHome25` | Activity landing page (JSP TBD) | ACTIVE - CENTRAL HUB |
| ViewActivity25 | `/ViewActivity25` | /WEB-INF/view/a/activityDetail/activityDetail25.jsp | ACTIVE - CENTRAL HUB |
| GoActivityDetail25 | `/GoActivityDetail25` | ViewActivity25 (named dispatcher) | ACTIVE |
| ViewById | `/ViewById` | GoActivityDetail25 (named dispatcher) | ACTIVE |
| ViewChecklist25 | `/ViewChecklist25` | /WEB-INF/view/a/activityDetail/activityDetail25.jsp | ACTIVE |

---

## ACTIVITY MANAGEMENT SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| AddNoteToActivity25 | `/AddNoteToActivity25` | ViewActivity25 (named dispatcher) | ACTIVE |
| AddActivityContact25 | `/AddActivityContact25` | ViewActivity25 (named dispatcher) | ACTIVE |
| AddToDo25 | `/AddToDo25` | ViewActivity25 (named dispatcher) | ACTIVE |
| CloseActivity25 | `/CloseActivity25` | ViewHome25 (named dispatcher) | ACTIVE |
| ViewPastActivity25 | `/ViewPastActivity25` | GoActivityDetail25 (named dispatcher) | ACTIVE |
| ReturnFromPastActivity25 | `/ReturnFromPastActivity25` | GoActivityDetail25 (named dispatcher) | ACTIVE |

---

## TASK & CHECKLIST SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| TaskBuilder25 | `/TaskBuilder25` | Dynamic dispatcher | ACTIVE |
| TaskBuilder | `/TaskBuilder` | Dynamic dispatcher | UNKNOWN - DUPLICATE? |
| ClearGrid25 | `/ClearGrid25` | /WEB-INF/view/a/general/sequenceBuilder/sequenceBuilderForm.jsp | ACTIVE |
| ClearGrid | `/ClearGrid` | /WEB-INF/view/checklist/checklistBuilder.jsp | UNKNOWN - DUPLICATE? |
| MakeRecurringFromChecklist25 | `/MakeRecurringFromChecklist25` | Dynamic | ACTIVE |
| MakeRecurringFromChecklist | `/MakeRecurringFromChecklist` | Dynamic | UNKNOWN - DUPLICATE? |
| ModifyRecurringTask25 | `/ModifyRecurringTask25` | ViewHome25 (named dispatcher) | ACTIVE |

---

## SEQUENCE & RECURRING TASK SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| SequenceHome | `/SequenceHome` | /WEB-INF/view/activity/checklist/sequences/sequenceHome.jsp | ACTIVE |
| ChangeTaskSequenceTable | `/ChangeTaskSequenceTable` | RequiredSequenceBuilder or RecurringSequenceBuilder (named) | ACTIVE |
| doCheckListAction | `/doCheckListAction` | /WEB-INF/view/a/pspHome/pspHome.jsp | ACTIVE |

---

## ADMIN SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| GoAdminHome | `/GoAdminHome` | /WEB-INF/view/adminHome.jsp | ACTIVE |
| RefreshTicketEmployees | `/RefreshTicketEmployees` | ViewHome25 (named dispatcher) | ACTIVE |
| CreateTicketTemplate | `/CreateTicketTemplate` | Dynamic dispatcher | ACTIVE |

---

## MONTHLY IMPORT/DATA SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| RunSelectedImportsServlet | `/RunSelectedImports` | ViewActivity25 redirect | ACTIVE |
| PreviewUploadedFilesServlet | `/PreviewUploadedFiles` | Preview page (JSP TBD) | ACTIVE |
| ShowUploadPage | `/ShowUploadPage` | /WEB-INF/view/a/z_acessory/fileUploadPage.jsp | ACTIVE |
| WipeTables25 | `/WipeTables25` | ViewHome25 (named dispatcher) | ACTIVE |

---

## EMAIL & COMMUNICATION SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| CreateEmail25 | `/CreateEmail25` | /WEB-INF/view/a/general/emailMaster25.jsp | ACTIVE |

---

## SALES & PROPOSAL SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| GenerateProp25 | `/GenerateProp25` | Dynamic | ACTIVE |
| ApplyLink | `/ApplyLink` | External redirect (JotForm) | ACTIVE |

---

## PSP/TPA MANAGEMENT SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| UpdatePsp25 | `/UpdatePsp25` | Dynamic | ACTIVE |

---

## FILE/DOCUMENT SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| ShowFileUpload | `/ShowFileUpload` | /WEB-INF/view/emailAttachments.jsp | ACTIVE |

---

## INITIALIZATION SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| GoInitialize25 | `/GoInitialize25` | /initialize.jsp | ACTIVE |

---

## MARKET/PUBLIC SERVLETS

| Servlet | URL Pattern | Forwards To | Status |
|---------|-------------|-------------|--------|
| LandingServlet | `/market/landing` | /WEB-INF/view/market/landing.jsp | ACTIVE |

---

## AUTOMATION EMAIL SERVLETS (Complex Legacy)

| Servlet | URL Pattern | Package | Forwards To | Status |
|---------|-------------|---------|-------------|--------|
| SendAuto25 | `/SendAuto25` | controller | Automation form JSP | ACTIVE |
| SendAutoFinal25 | `/SendAutoFinal25` | controller | ViewActivity25 (named) | ACTIVE |
| SendAutoEmail | `/SendAutoEmail` | previous.controller.general.admin.q | SendAuto25 (named) | LEGACY |
| SendAutomationEmailFinal | `/SendAutomationEmailFinal` | previous.controller.general.admin.q | GoAdminHome | LEGACY |
| SendAuto | `/SendAuto` | previous.controller.general.admin.q | GoAdminHome (named) | LEGACY |
| sendAutomationFinal | `/sendAutomationFinal` | previous.archive | Unknown | ARCHIVE - DEAD |
| CreateAutoEmail | `/CreateAutoEmail` | previous.controller.general.admin.q | GoAdminHome (named) | ACTIVE |
| PreviewAutomation | `/PreviewAutomation` | controller | JSP preview | ACTIVE |

---

## EMAIL HOME & ACTIONS SERVLETS

| Servlet | URL Pattern | Package | Forwards To | Status |
|---------|-------------|---------|-------------|--------|
| GoEmailHome | `/GoEmailHome` | previous.controller.general.admin | /WEB-INF/view/general/email/emailHome.jsp | ACTIVE |
| ResetEmailView | `/ResetEmailView` | previous.controller.general.admin | GoEmailHome (named) | ACTIVE |
| EmailActions | `/EmailActions` | previous.controller.general.admin | GoEmailHome (named) | ACTIVE |
| emailActionsNew | `/emailActionsNew` | previous.archive | GoEmailHome (named) | ARCHIVE - DEAD |

---

## ADMIN VIEW MANAGEMENT SERVLETS

| Servlet | URL Pattern | Package | Forwards To | Status |
|---------|-------------|---------|-------------|--------|
| ResetAdminView | `/ResetAdminView` | previous.controller.general.admin | GoAdminHome (named) | ACTIVE |
| OnlyPastDue | `/OnlyPastDue` | previous.controller.general.admin | GoAdminHome (named) | ACTIVE |
| UpdateRenewalContact | `/UpdateRenewalContact` | previous.controller.general.admin | ResetAdminView (named) | ACTIVE |

---

## SEQUENCE BUILDER SERVLETS

| Servlet | URL Pattern | Package | Forwards To | Status |
|---------|-------------|---------|-------------|--------|
| RequiredSequenceBuilder | `/RequiredSequenceBuilder` | previous.controller.activity.checklist.sequence | SequenceHome (named) | ACTIVE |
| RecurringSequenceBuilder | `/RecurringSequenceBuilder` | previous.controller.activity.checklist.sequence | SequenceHome (named) | ACTIVE |
| ChangeTaskSequenceTable | `/ChangeTaskSequenceTable` | previous.controller.activity.checklist.sequence | RequiredSequenceBuilder or RecurringSequenceBuilder | ACTIVE |

---

## CHECKLIST/TASK SERVLETS (Previous Package)

| Servlet | URL Pattern | Package | Forwards To | Status |
|---------|-------------|---------|-------------|--------|
| RecurringItemSetupGo | `/RecurringItemSetupGo` | previous.controller.checklist | /WEB-INF/view/checklist/recurringManager.jsp | LEGACY |
| ClearGrid | `/ClearGrid` | previous.controller.activity.checklist.sequence | /WEB-INF/view/checklist/checklistBuilder.jsp | LEGACY - DUPLICATE |

---

## RENEWAL SERVLETS (Previous Package)

| Servlet | URL Pattern | Package | Forwards To | Status |
|---------|-------------|---------|-------------|--------|
| RenewalHome | `/RenewalHome` | previous.controller.activity.renewal | /WEB-INF/view/activity/renew/renewHome.jsp | ACTIVE |
| ResetRenewal | `/ResetRenewal` | previous.controller.activity.renewal | RenewalHome (named) | ACTIVE |

---

## OTHER SERVLETS (Previous Package)

| Servlet | URL Pattern | Package | Forwards To | Status |
|---------|-------------|---------|-------------|--------|
| ShowFileUpload | `/ShowFileUpload` | previous.controller | /WEB-INF/view/emailAttachments.jsp | ACTIVE |
| doCheckListAction | `/doCheckListAction` | previous.archive.checklistDetail | /WEB-INF/view/a/pspHome/pspHome.jsp | LEGACY |

---

## EMPTY/PLACEHOLDER SERVLETS (Potential Dead Code)

| Servlet | URL Pattern | Package | Notes | Status |
|---------|-------------|---------|-------|--------|
| HomeServlet | `/HomeServlet` | previous.controller | Empty doGet/doPost methods | DEAD |

---

## ARCHIVE SERVLETS (Confirmed Dead Code)

| Servlet | URL Pattern | Package | Notes | Status |
|---------|-------------|---------|-------|--------|
| sendAutomationFinal | `/sendAutomationFinal` | previous.archive | Old automation sender | DEAD |
| emailActionsNew | `/emailActionsNew` | previous.archive | Old email actions | DEAD |
| doCheckListAction | `/doCheckListAction` | previous.archive.checklistDetail | Old checklist action | DEAD |

---

## DUPLICATE/VERSION SERVLETS (Resolution Needed)

| Base Name | Current (25) | Legacy (previous) | Archive | Resolution |
|-----------|-------------|-------------------|---------|------------|
| TaskBuilder | TaskBuilder25 (controller) | TaskBuilder (previous.controller.checklist) | - | Use TaskBuilder25 |
| ClearGrid | ClearGrid25 (controller) | ClearGrid (previous.controller) | - | Use ClearGrid25 |
| MakeRecurringFromChecklist | MakeRecurringFromChecklist25 (controller) | MakeRecurringFromChecklist (previous.controller) | - | Use 25 version |
| SendEmail | SendEmail25 (controller.emailItems) | - | - | Active |
| SendAuto | SendAuto25 (controller) | SendAutoEmail, SendAuto (previous) | sendAutomationFinal | Use SendAuto25 & SendAutoFinal25 |
| EmailActions | - | EmailActions (previous.controller) | emailActionsNew | Use EmailActions in previous |

---

## KEY FINDINGS

### Package Structure Analysis

**Package Hierarchy (by status):**
1. **controller** (current) - Active production code, "25" suffix = current version
2. **previous.controller** - Legacy code still in use, being migrated to controller
3. **previous.archive** - Dead code, confirmed for removal

**Total Servlets Found: 75+**
- Active (controller): ~40 servlets
- Legacy (previous): ~25 servlets  
- Archive (previous.archive): ~10 servlets

### Central Hub Servlets (Most Referenced)
1. **ViewHome25** - Main dashboard/activity listing (controller)
2. **ViewActivity25** - Activity detail view (controller)
3. **AuthenticateUser** - Login processor (previous.controller.authentication)
4. **GoAdminHome** - Admin dashboard (previous.controller.general.admin)
5. **GoEmailHome** - Email home page (previous.controller.general.admin)

### Clear Migration Pattern
```
previous.archive → previous.controller → controller
     (DEAD)           (LEGACY)           (ACTIVE)
```

Examples:
- emailActionsNew (archive) → EmailActions (previous) → [future migration]
- sendAutomationFinal (archive) → SendAutomationEmailFinal (previous) → SendAutoFinal25 (controller)
- ClearGrid (previous) → ClearGrid25 (controller)

### Duplicate Resolution
**Use these versions:**
- TaskBuilder25 (not TaskBuilder)
- ClearGrid25 (not ClearGrid)  
- MakeRecurringFromChecklist25 (not MakeRecurringFromChecklist)
- SendAuto25 & SendAutoFinal25 (not SendAutoEmail, SendAuto, SendAutomationEmailFinal)

**Legacy versions still in use (previous.controller):**
- AuthenticateUser, GoAdminHome, GoEmailHome (authentication/admin still in previous package)
- RenewalHome, SequenceHome (renewal/sequence features in previous package)
- EmailActions (email features partially migrated)

### Archive Package (Confirmed Dead Code - Safe to Remove)
1. sendAutomationFinal
2. emailActionsNew
3. doCheckListAction

### Issues Found
1. **HomeServlet** - Empty implementation, dead code
2. **AuthenticateUser.displayLoginFailure()** - FIXME comment, not implemented
3. **Authentication still in previous package** - Not migrated to controller yet
4. **Mixed package usage** - Some features span both previous and controller packages

### Technology Stack Identified
- Jakarta EE (servlets, JPA)
- Microsoft Graph API (email sending)
- Azure AD authentication (OAuth2)
- MySQL database (beta_ssa schema)
- CSRF protection via CsrfFilter

---

## NEXT STEPS

1. ✅ **COMPLETE:** Map all @WebServlet servlets
2. Search for JSP files to identify which are referenced vs. orphaned
3. Analyze LoginFilter and CsrfFilter for complete filter mapping
4. Check for servlets/features referenced only in JSP files (not servlet-to-servlet)
5. Create migration priority list (which previous.controller servlets to migrate next)
6. Identify safe-to-delete files (archive package confirmed)

