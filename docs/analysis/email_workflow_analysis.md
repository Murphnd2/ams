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
