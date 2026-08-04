# Email Workflow Analysis

**Date:** February 18, 2026  
**Last Updated:** February 18, 2026 — SMTP standardization, Wasabi storage, branded template  
**Status:** Email system fully modernized

---

## Architecture Summary

Both email paths now use **SMTP via `EmailDAO`** as the universal sending method. Microsoft Graph API code has been removed from `SendEmail25`. Attachments are stored in **Wasabi S3-compatible cloud storage** (`ams-file-storage` bucket) and served via pre-signed URLs. All outbound emails are wrapped in a **branded HTML template** (`EmailTemplate`) with PSP-specific colors.

---

## Email Paths

### Path 1: Manual Email (Navbar)

| Servlet | URL | Purpose | Status |
|---------|-----|---------|--------|
| CreateEmail25 | `/CreateEmail25` | Opens email composer | ✅ ACTIVE |
| SaveEmailState25 | `/SaveEmailState25` | Routes all email actions | ✅ ACTIVE |
| SendEmail25 | `/SendEmail25` | Wraps body in template, persists, sends via SMTP | ✅ UPDATED |
| AddRecipient25 | `/AddRecipient25` | Add recipient | ✅ ACTIVE |
| RemoveRecipient25 | `/RemoveRecipient25` | Remove recipient | ✅ ACTIVE |
| RemoveAttachment25 | `/RemoveAttachment25` | Remove attachment | ✅ ACTIVE |
| AddAttachment25 | `/AddAttachment25` | Uploads file to Wasabi, creates WebLink | ✅ UPDATED |

**JSPs:**
- `emailMaster25.jsp` (`/WEB-INF/view/a/general/emailMaster25.jsp`) — main email page with CKEditor
- `addRecipientModal25.jsp` (`/WEB-INF/view/a/general/email/addRecipientModal25.jsp`)

**Flow:**
```
Navbar → CreateEmail25 → emailMaster25.jsp
  ↓
User composes message (CKEditor), adds recipients, attaches files
  ↓
Form submits to SaveEmailState25 → routes based on action:
  ├─ action="SE" → SendEmail25 (wraps in EmailTemplate, sends via EmailDAO SMTP)
  ├─ action="AR" → AddRecipient25 → CreateEmail25
  ├─ action="DR" → RemoveRecipient25 → CreateEmail25
  ├─ action="AA" → AddAttachment25 (uploads to Wasabi) → CreateEmail25
  └─ action="DA" → RemoveAttachment25 → CreateEmail25
```

### Path 2: Automation Email (Checklists)

| Servlet | URL | Purpose | Status |
|---------|-----|---------|--------|
| SendAuto25 | `/SendAuto25` | Loads automation, shows input form | ✅ ACTIVE |
| SendAutoFinal25 | `/SendAutoFinal25` | Processes inputs & sends via SMTP | ✅ ACTIVE |
| PreviewAutomation | `/PreviewAutomation` | Preview with dummy data | ✅ ACTIVE |

**Flow:**
```
Checklist todo button → SendAutoEmail (redirect) → SendAuto25
  → autoInputScreen25.jsp → SendAutoFinal25 → EmailDAO.sendEmail() → ViewActivity25
```

**Note:** Automation emails do NOT use the branded `EmailTemplate` wrapper — they use their own `Automation` entity templates. This is intentional; automation emails have their own formatting.

**Recipient resolution (2026-08-04).** A `Person`'s address may live on its own `assignee.email`
column **or** on a linked `Employee` (`assignee.employee_id`), and `Employee.getEmail()` itself prefers
`hr_email` over `email`. Renewal primary contacts resolved through
`AmsDataLocal.fillPrimaryContacts()` → `employer.contactList[0]` → `PersonDAO.getPersonByEmployee()`
routinely have a blank person email with the real address on the employee record.

Always use **`Person.getEffectiveEmail()`** (`employee.hr_email` → `employee.email` → `person.email`)
when selecting or displaying an email recipient — including for dedupe keys, or a contact that is both
primary and additional will double-add. Before this fix `SendAuto25` read the raw `getEmail()`, saw
nothing, and injected a spurious "To (Email Address)" prompt for a contact whose address was visible
on screen; the recipient was then dropped again at three further layers
(`AutomationHelper.getRecipientList`, `SendAutoFinal25`, `EmailDAO`). Production counts at the time:
29 Tickets, 8 Renewals, 0 Setups — **not a renewal-only bug**, despite where it surfaced.

Deliberately **not** switched to `getEffectiveEmail()`: `ModifyContact25` / `ModContact25`, which
pre-fill an *editable* field — resolving there would write the employee's address onto the person row
on save. That is a data-migration decision, not a display fix.

**Typed To/CC addresses that belong to an Employee (2026-08-04).** When a user types an address into
the injected `To:` field (`SendAutoFinal25`) or a CC list (`AutomationHelper.processLists`), resolution
runs through `EmailDAO.getPersonByEmail()`. If the address matches an `Employee` that has no `Person`
row, `AuthDAO.createPersonFromEmployee()` now creates one — carrying the employee's real name and
address — and that Person becomes the recipient. The `NEW PERSON` placeholder fallback is reserved for
addresses matching no employee at all.

This branch was **unreachable until 2026-08-04**: `PersonDAO.getPersonByEmployee()` returned an empty
placeholder rather than `null`, so `if (p == null)` never fired, and the placeholder — carrying no
address — was silently filtered out of the send by `isValidEmail`. The recipient simply never received
the mail, with no error surfaced. If you are auditing a "they never got the email" report predating
this fix, that is the mechanism.

---

## Infrastructure

### Email Sending

| Class | Purpose | Status |
|-------|---------|--------|
| `EmailDAO` | SMTP email sending (multipart/alternative: HTML + plain text) | ✅ UPDATED |
| `EmailTemplate` | Branded HTML email wrapper (header, attachments, body, signature, footer) | ✅ NEW |

**`EmailDAO` changes (Feb 18, 2026):**
- `sendEmail(Email, EntityManager)` — no longer appends attachment links (template handles them)
- Low-level `sendEmail(...)` — now sends **multipart/alternative** (plain text + HTML) for spam reduction
- Added `stripHtml()` helper for generating text alternative

**`EmailTemplate` features:**
- PSP-branded header bar (primary color) with accent divider
- Attachment pills rendered at top with pre-signed Wasabi download URLs (7-day expiry)
- Clean body area for user's CKEditor content
- Signature block (sender name, email, PSP name)
- Footer with PSP attribution
- Colors from DB constants: `EMAIL_COLOR_PRIMARY`, `EMAIL_COLOR_ACCENT`

### Attachment Storage

| Class | Purpose | Status |
|-------|---------|--------|
| `StorageDAO` | Wasabi S3 upload, pre-signed URL generation, delete | ✅ NEW |
| `AddAttachment25` | Uploads files to Wasabi with friendly download filename | ✅ UPDATED |
| `ShowFileUpload` | Redirects to pre-signed Wasabi URL (no auth required) | ✅ UPDATED |
| `WebLink` | Entity storing attachment metadata (linkPath = UUID.extension) | UNCHANGED |

**Attachment flow:**
```
Upload: AddAttachment25 → StorageDAO.uploadFile() → Wasabi (ams-file-storage/{psp-slug}/UUID.ext)
  - Content-Disposition set to friendly filename at upload time

Email link: EmailTemplate renders pre-signed Wasabi URLs directly in email body
  - 7-day expiry, no server routing needed

Fallback download: ShowFileUpload?doc=UUID.ext → StorageDAO.getDownloadUrl() → 302 redirect to Wasabi
  - Added to LoginFilter ALLOWED_ENDPOINTS (no auth required)
  - 1-hour expiry
```

**Old flow (removed):**
- ~~Local disk storage via `AmsDataGlobal.getSavePath()`~~
- ~~`emailAttachments.jsp` with broken `getInternalAnchorTag()`~~
- ~~Microsoft Graph API in `SendEmail25`~~

### Redirect Wrapper (Still Needed)

| Servlet | URL | Package | Why Keep |
|---------|-----|---------|----------|
| SendAutoEmail | `/SendAutoEmail` | previous.controller | DB `task.servletName` records contain `"SendAutoEmail?aeId=123"`. Just forwards to `SendAuto25`. |

**Future cleanup:** Change `UpdateTask25.addAutomationToTask()` to write `"SendAuto25?aeId=..."`, update existing DB records, then delete `SendAutoEmail`.

---

## DB Constants Used by Email System

| Constant | Purpose | Example Value |
|----------|---------|---------------|
| `SMTP_SERVER` | SMTP host | `mail.smtp2go.com` |
| `SMTP_PORT` | SMTP port | `2525` |
| `SMTP_USER` | SMTP username | (per PSP) |
| `SMTP_PASSWORD` | SMTP password | (per PSP) |
| `SMTP_FROM` | Verified sender address (optional) | (per PSP) |
| `SMTP_DEBUG` | Enable SMTP debug logging (optional) | `TRUE` / `FALSE` |
| `S3_ENDPOINT` | Wasabi endpoint | `https://s3.us-east-1.wasabisys.com` |
| `S3_BUCKET` | Storage bucket name | `ams-file-storage` |
| `S3_ACCESS_KEY` | Wasabi access key | (per PSP) |
| `S3_SECRET_KEY` | Wasabi secret key | (per PSP) |
| `EMAIL_COLOR_PRIMARY` | Email template header/accent color | `#2B5F8A` |
| `EMAIL_COLOR_ACCENT` | Email template highlight color | `#7AB648` |

**PSP deployment note:** `S3_ENDPOINT` and `S3_BUCKET` are shared across PSPs. `S3_ACCESS_KEY` and `S3_SECRET_KEY` should be blank in seed data — each PSP configures their own during `initialize.jsp` setup.

---

## Spam Reduction Measures

1. **Multipart/alternative** — every email includes both HTML and plain text parts
2. **Table-based HTML layout** — maximum email client compatibility, no div/CSS tricks
3. **No JavaScript, forms, or hidden text** in email body
4. **Clean text-to-HTML ratio** — plain text alternative ensures reasonable ratio
5. **Proper charset declaration** and viewport meta tag
6. **`role="presentation"`** on all layout tables
7. **Pre-signed URLs** for attachments — no suspicious redirect chains

---

## Files Modified (Feb 18, 2026)

| File | Change |
|------|--------|
| `controller/email/SendEmail25.java` | Removed Graph API, uses SMTP via EmailDAO, wraps body in EmailTemplate |
| `controller/email/AddAttachment25.java` | Uploads to Wasabi via StorageDAO instead of local disk |
| `controller/activity/ShowFileUpload.java` | Redirects to pre-signed Wasabi URL instead of broken JSP |
| `data/dao/EmailDAO.java` | Multipart/alternative sending, removed duplicate attachment rendering |
| `data/dao/StorageDAO.java` | NEW — Wasabi S3 upload, pre-signed URLs, delete |
| `data/util/EmailTemplate.java` | NEW — Branded HTML email template with attachment rendering |
| `LoginFilter.java` | Added `/ShowFileUpload` to ALLOWED_ENDPOINTS |

---

## Obsolete Files (Can Be Deleted)

| File | Reason |
|------|--------|
| `emailAttachments.jsp` | Replaced by ShowFileUpload redirect to Wasabi |

**Note:** `WebLink.getInternalAnchorTag()` is referenced only by `emailAttachments.jsp` and was never implemented. Both can be cleaned up together.
