# Email Workflow Analysis

**Date:** February 17, 2026
**Branch:** refactor/modernize-architecture

---

## Email Paths Confirmed

The app has **two ways to send email**, both confirmed by the user:

1. **Navbar "Email" button** → `CreateEmail25` (modern)
2. **Task automation in checklists** → `SendAuto25` → `SendAutoFinal25` (modern)

---

## ACTIVE — Modern Email System (`controller.email` package)

### Path 1: Manual Email (Navbar)

| Servlet | URL | Purpose | Status |
|---------|-----|---------|--------|
| CreateEmail25 | `/CreateEmail25` | Opens email composer | ✅ ACTIVE |
| SaveEmailState25 | `/SaveEmailState25` | Routes all email actions | ✅ ACTIVE |
| SendEmail25 | `/SendEmail25` | Sends the email | ✅ ACTIVE |
| AddRecipient25 | `/AddRecipient25` | Add recipient | ✅ ACTIVE |
| RemoveRecipient25 | `/RemoveRecipient25` | Remove recipient | ✅ ACTIVE |
| RemoveAttachment25 | `/RemoveAttachment25` | Remove attachment | ✅ ACTIVE |

**JSPs:**
- `emailMaster25.jsp` (`/WEB-INF/view/a/general/emailMaster25.jsp`) — main email page
- `addRecipientModal25.jsp` (`/WEB-INF/view/a/general/email/addRecipientModal25.jsp`)

**Flow:** Navbar → `CreateEmail25` → `emailMaster25.jsp` → form submits to `SaveEmailState25` → routes to appropriate action servlet → returns to `CreateEmail25`

### Path 2: Automation Email (Checklists)

| Servlet | URL | Purpose | Status |
|---------|-----|---------|--------|
| SendAuto25 | `/SendAuto25` | Loads automation, shows input form | ✅ ACTIVE |
| SendAutoFinal25 | `/SendAutoFinal25` | Processes inputs & sends email | ✅ ACTIVE |
| PreviewAutomation | `/PreviewAutomation` | Preview with dummy data | ✅ ACTIVE |

**JSPs:**
- `autoInputScreen25.jsp` (`/WEB-INF/view/a/taskManager/autoInputScreen25.jsp`)
- `autoConfirmSend25.jsp` (`/WEB-INF/view/a/taskManager/autoConfirmSend25.jsp`)

**Flow:** Checklist todo button → `SendAutoEmail` (redirect) → `SendAuto25` → `autoInputScreen25.jsp` → form submits to `SendAutoFinal25` → sends email → forwards to `ViewActivity25`

### Infrastructure (Shared)

| Servlet/Class | Purpose | Status |
|---------------|---------|--------|
| ShowFileUpload | Serves file downloads for attachment links in sent emails | ✅ ACTIVE — DO NOT DELETE |
| dbEmail.java | Email sending utility (SMTP) | ✅ ACTIVE |
| WebLink.java | Builds attachment URLs using `ShowFileUpload` | ✅ ACTIVE |

---

## KEEP — Redirect Wrapper (Cannot Delete Yet)

| Servlet | URL | Package | Why Keep |
|---------|-----|---------|----------|
| SendAutoEmail | `/SendAutoEmail` | previous.controller.general.admin.q | DB `task.servletName` records contain `"SendAutoEmail?aeId=123"`. Just forwards to `SendAuto25`. Safe but needed. |

**Note:** `UpdateTask25.java` also writes `"SendAutoEmail?aeId=..."` into new task records. A future cleanup could change this to `"SendAuto25?aeId=..."` and update existing DB records, then delete `SendAutoEmail`.

---

## LEGACY — Deletion Candidates

### Legacy Email Home Ecosystem

None of these are reachable from the modern UI (navbar25, activityDetail25, pspHome25).

| File | Type | Package/Path | Why Legacy |
|------|------|-------------|------------|
| GoEmailHome.java | Servlet | previous.controller.general.admin | Forwards to legacy `emailHome.jsp`. Not linked from modern UI. |
| EmailActions.java | Servlet | previous.controller.general.admin | Action processor for legacy email page. Uses `sessionScope.adminView`. |
| ResetEmailView.java | Servlet | previous.controller.general.admin | Just forwards to `GoEmailHome`. |
| EscapeEmail.java | Servlet | previous.controller.general.admin | Cancel button on legacy email page. Forwards to `GoEmailHome`. |
| AddEmail.java | Servlet | previous.controller.general.admin | Legacy inline email sender. Used by `addEmailForm.jsp` (old activity detail). |
| emailActionsNew.java | Servlet | previous.archive | Archive version of `EmailActions`. Already confirmed dead. |
| emailHome.jsp | JSP | /WEB-INF/view/general/email/ | Legacy email composer page. Uses old `navbar.jsp` and `sessionScope.currentEmailSubject`. |
| addEmailForm.jsp | JSP | /WEB-INF/view/activity/note/ | Legacy inline email form. Uses `sessionScope.adminView`, submits to `AddEmail`. |
| addEmailModal.jsp | JSP | /WEB-INF/view/activity/note/ | Modal wrapper for `addEmailForm.jsp`. Not imported by any modern (`/view/a/`) page. |
| emailView.jsp | JSP | /WEB-INF/view/general/email/ | Legacy email viewer. Uses old `navbar.jsp`. |
| addRecipientForm.jsp | JSP | /WEB-INF/view/general/email/forms/ | Legacy add-recipient form for `emailHome.jsp`. |
| toWhoList.jsp | JSP | /WEB-INF/view/general/email/lists/ | Legacy recipient list for `emailHome.jsp`. |
| attachmentList.jsp | JSP | /WEB-INF/view/general/email/lists/ | Legacy attachment list for `emailHome.jsp`. |
| toWhoList2.jsp | JSP | /WEB-INF/view/general/email/lists/ | Used only by `emailView.jsp` (legacy). |
| attachmentList2.jsp | JSP | /WEB-INF/view/general/email/lists/ | Used only by `emailView.jsp` (legacy). |

### Legacy Automation Servlets

| File | Type | Package | Why Legacy |
|------|------|---------|------------|
| SendAuto.java | Servlet | previous.controller.general.admin.q | Forwards to `GoAdminHome` (deleted). Dead. |
| SendAutomationEmailFinal.java | Servlet | previous.controller.general.admin.q | Forwards to `GoAdminHome` (deleted). Dead. |
| sendAutomationFinal.java | Servlet | previous.archive | Archive version. Already confirmed dead. |
| UpdateAutomation.java | Servlet | previous.controller.activity.checklist.task | Forwards to `GoAdminHome` (deleted). Replaced by `UpdateTask25`. |
| updateTaskInfo.java | Servlet | previous.archive | Archive version of `UpdateTask25`. Dead. |
| CreateAutoEmail.java | Servlet | previous.controller.general.admin.q | Forwards to `GoAdminHome` (deleted). Dead. |

---

## Deletion Checklist — COMPLETED ✅

**Build verified:** Maven clean + package both exit code 0.

**Legacy Email Home (6 Java + 10 JSP = 16 files deleted):**
- [x] `GoEmailHome.java`
- [x] `EmailActions.java`
- [x] `ResetEmailView.java`
- [x] `EscapeEmail.java`
- [x] `AddEmail.java`
- [x] `emailActionsNew.java` (archive)
- [x] `emailHome.jsp`
- [x] `addEmailForm.jsp`
- [x] `addEmailModal.jsp`
- [x] `addRecipientForm.jsp`
- [x] `addRecipientModal.jsp` (found during cleanup)
- [x] `addAttachmentModal.jsp` (found during cleanup)
- [x] `emailMaster.jsp` (legacy version — found during cleanup)
- [x] `toWhoList.jsp` (in `/general/email/lists/`)
- [x] `attachmentList.jsp` (in `/general/email/lists/`)

**Legacy Automation (3 Java files deleted, 3 already gone from GoAdminHome cleanup):**
- [x] `sendAutomationFinal.java` (archive)
- [x] `updateTaskInfo.java` (archive)
- [x] `UpdateAutomation.java`
- [x] `SendAuto.java` — already deleted in GoAdminHome cleanup
- [x] `SendAutomationEmailFinal.java` — already deleted in GoAdminHome cleanup
- [x] `CreateAutoEmail.java` — already deleted in GoAdminHome cleanup

**Bonus files found during cleanup (not in original list):**
- [x] `emailMaster.jsp` (legacy version of emailMaster25.jsp)
- [x] `addRecipientModal.jsp` (wrapper for addRecipientForm.jsp)
- [x] `addAttachmentModal.jsp` (imported by legacy emailMaster.jsp)

### DO NOT Delete
- `ShowFileUpload.java` — serves attachment downloads for all sent emails
- `SendAutoEmail.java` — DB task records reference it as URL; just redirects to `SendAuto25`
- `ViewEmail.java` — ✅ VERIFIED ACTIVE. Called from `historyDetail25.jsp` and `emailList.jsp` (opens in new tab). Forwards to legacy `emailView.jsp` (uses old `navbar.jsp`). Functional but visually inconsistent — future migration candidate for `ViewEmail25`.
- `emailView.jsp` — used by `ViewEmail.java` (active)
- `toWhoList2.jsp` — used by `emailView.jsp` (active)
- `attachmentList2.jsp` — used by `emailView.jsp` (active)
- `dbEmail.java` — core email sending utility used by modern servlets

### Future Cleanup (Not Now)
- Change `UpdateTask25.addAutomationToTask()` to write `"SendAuto25?aeId=..."` instead of `"SendAutoEmail?aeId=..."`
- Update existing DB `task.servletName` records from `SendAutoEmail` to `SendAuto25`
- Then delete `SendAutoEmail.java`

---

## Summary

| Category | Count |
|----------|-------|
| Modern ACTIVE servlets | 9 (CreateEmail25, SaveEmailState25, SendEmail25, AddRecipient25, RemoveRecipient25, RemoveAttachment25, SendAuto25, SendAutoFinal25, PreviewAutomation) |
| Infrastructure (keep) | 2 (ShowFileUpload, SendAutoEmail redirect) |
| Legacy deletion candidates | 19 files deleted (16 email home + 3 automation, plus 3 already gone from prior cleanup) |
