<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Email Draft Test</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,100..1000;1,9..40,100..1000&display=swap" rel="stylesheet">
    <style>
        * { font-family: 'DM Sans', sans-serif; }
        .audit-wrap  { display:flex; flex-direction:column; height:calc(100vh - 56px); }
        .audit-body  { flex:1 1 auto; }
        .hdr-bar     { background:#f8f9fa; border-bottom:1px solid #dee2e6;
                       min-height:48px; flex-shrink:0; border-radius:6px 6px 0 0; }
    </style>
</head>
<body>
<c:set var="pageTitle" value="Email Draft Test" scope="request"/>
<c:set var="pageIcon"  value="bi-envelope-paper" scope="request"/>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">

  <%-- ── Toolbar ──────────────────────────────────────────────────────────── --%>
  <div class="hdr-bar d-flex align-items-center px-3 gap-2">
    <i class="bi bi-envelope-paper fs-5 text-muted me-1"></i>
    <span class="fw-semibold">Email Draft Test</span>
    <span class="badge bg-secondary ms-1">Admin</span>
    <div class="ms-auto d-flex gap-2">
      <button type="button" class="btn btn-sm btn-outline-secondary ghost-action"
              onclick="clearForm()">
        <i class="bi bi-eraser me-1"></i>Clear
      </button>
    </div>
  </div>

  <%-- ── Scrollable body ────────────────────────────────────────────────────── --%>
  <div class="audit-body px-3 pt-3" style="overflow-y:auto;">

    <div class="row g-3">

      <%-- ── Input form ──────────────────────────────────────────────────────── --%>
      <div class="col-lg-5">
        <div class="card border-0 shadow-sm h-100">
          <div class="card-header bg-light fw-semibold py-2">
            <i class="bi bi-envelope-open me-2 text-primary"></i>Inbound Email
          </div>
          <div class="card-body">

            <div class="mb-3">
              <label class="form-label fw-semibold small">Sender Email <span class="text-danger">*</span></label>
              <input type="email" id="inboundSender" class="form-control form-control-sm"
                     placeholder="participant@example.com" />
            </div>

            <div class="mb-3">
              <label class="form-label fw-semibold small">Date Received</label>
              <input type="datetime-local" id="inboundDate" class="form-control form-control-sm" />
            </div>

            <div class="mb-3">
              <label class="form-label fw-semibold small">Subject <span class="text-danger">*</span></label>
              <input type="text" id="inboundSubject" class="form-control form-control-sm"
                     placeholder="Re: FSA claim question" />
            </div>

            <div class="mb-3">
              <label class="form-label fw-semibold small">Body <span class="text-danger">*</span></label>
              <textarea id="inboundBody" class="form-control form-control-sm" rows="9"
                        placeholder="Paste the full inbound email text here..."></textarea>
            </div>

            <div class="mb-3">
              <label class="form-label fw-semibold small">Staff Notes
                <span class="text-muted fw-normal">(optional)</span></label>
              <textarea id="userNotes" class="form-control form-control-sm" rows="3"
                        placeholder="e.g. Be brief. Remind them the deadline is March 15."></textarea>
            </div>

            <div class="mb-3">
              <label class="form-label fw-semibold small d-block">Draft Mode</label>
              <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="draftMode"
                       id="modeFresh" value="FRESH" checked />
                <label class="form-check-label small" for="modeFresh">Fresh draft</label>
              </div>
              <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="draftMode"
                       id="modeRefine" value="REFINE" />
                <label class="form-check-label small" for="modeRefine">Refine previous</label>
              </div>
            </div>

            <div id="prevDraftWrap" class="mb-3 d-none">
              <label class="form-label fw-semibold small">Previous Draft JSON</label>
              <textarea id="previousDraft" class="form-control form-control-sm font-monospace"
                        rows="4" placeholder="Paste prior response JSON here for refinement..."></textarea>
            </div>

            <div id="validationAlert" class="alert alert-warning py-2 small d-none"></div>

            <button type="button" id="generateBtn" class="btn btn-primary w-100"
                    onclick="generateDraft()">
              <i class="bi bi-stars me-1"></i>Generate Draft
            </button>

          </div><%-- /card-body --%>
        </div><%-- /card --%>
      </div><%-- /col input --%>

      <%-- ── Results ──────────────────────────────────────────────────────────── --%>
      <div class="col-lg-7">

        <%-- Spinner --%>
        <div id="spinnerWrap" class="d-none text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <div class="mt-2 text-muted small">Calling Claude…</div>
        </div>

        <%-- Error --%>
        <div id="errorWrap" class="d-none">
          <div class="alert alert-danger">
            <strong>Error</strong>
            <div id="errorMsg" class="mt-1 small"></div>
          </div>
        </div>

        <%-- Results (hidden until response) --%>
        <div id="resultsWrap" class="d-none">

          <%-- Signal badges row --%>
          <div class="d-flex flex-wrap gap-2 mb-3 align-items-center">
            <span class="fw-semibold small text-muted me-1">Signals:</span>
            <span id="badgeDraftType"  class="badge rounded-pill bg-secondary"></span>
            <span id="badgeEscalation" class="badge rounded-pill bg-secondary"></span>
            <span id="badgeEncryption" class="badge rounded-pill bg-secondary"></span>
            <span id="badgeTone"       class="badge rounded-pill bg-secondary"></span>
          </div>

          <%-- Completeness strip --%>
          <div id="completenessStrip" class="d-flex flex-wrap gap-2 mb-3">
            <span class="badge border border-secondary text-secondary small"
                  id="checkPlanNamed"></span>
            <span class="badge border border-secondary text-secondary small"
                  id="checkDateCited"></span>
            <span class="badge border border-secondary text-secondary small"
                  id="checkNextSteps"></span>
          </div>

          <%-- Escalation reason / encryption reason --%>
          <div id="escalationNote" class="alert alert-warning py-2 small d-none">
            <strong>Escalation:</strong> <span id="escalationReason"></span>
            <div id="escalationQuestion" class="d-none mt-1">
              <strong>Question for Kevin:</strong> <span id="escalationQuestionText"></span>
            </div>
          </div>
          <div id="encryptionNote" class="alert alert-info py-2 small d-none">
            <strong>Encryption (<span id="encryptionLevel"></span>):</strong>
            <span id="encryptionReason"></span>
          </div>

          <%-- Draft preview --%>
          <div class="card border-0 shadow-sm mb-3">
            <div class="card-header bg-light py-2 d-flex align-items-center gap-2">
              <i class="bi bi-envelope-paper text-primary"></i>
              <span class="fw-semibold small">Draft Preview</span>
              <span class="text-muted small ms-1" id="previewSubjectLabel"></span>
              <button type="button" class="btn btn-sm btn-outline-secondary ms-auto ghost-action"
                      onclick="copyBody()" title="Copy body HTML">
                <i class="bi bi-clipboard me-1"></i>Copy HTML
              </button>
            </div>
            <div class="card-body p-3">
              <div class="mb-2 small">
                <span class="text-muted fw-semibold me-1">Subject:</span>
                <span id="previewSubject" class="fw-semibold"></span>
              </div>
              <div id="previewBody" class="border rounded p-2 bg-white"
                   style="min-height:80px;font-size:.9rem;"></div>
            </div>
          </div>

          <%-- Metadata --%>
          <div class="card border-0 shadow-sm mb-3">
            <div class="card-header bg-light py-2">
              <span class="fw-semibold small text-muted">Metadata</span>
            </div>
            <div class="card-body p-2">
              <dl class="row mb-0 small" style="font-size:.8rem;">
                <dt class="col-sm-4 text-muted">Model</dt>
                <dd class="col-sm-8 font-monospace" id="metaModel"></dd>
                <dt class="col-sm-4 text-muted">KBs Consulted</dt>
                <dd class="col-sm-8" id="metaKbs"></dd>
                <dt class="col-sm-4 text-muted">Style/Voice Chunks</dt>
                <dd class="col-sm-8" id="metaAlways"></dd>
                <dt class="col-sm-4 text-muted">Search Chunks</dt>
                <dd class="col-sm-8" id="metaSearch"></dd>
                <dt class="col-sm-4 text-muted">AMS Context</dt>
                <dd class="col-sm-8" id="metaAms"></dd>
              </dl>
            </div>
          </div>

          <%-- Raw JSON --%>
          <details class="mb-3">
            <summary class="small text-muted" style="cursor:pointer;">
              Raw JSON response
            </summary>
            <pre id="rawJson" class="border rounded p-2 bg-light mt-2"
                 style="font-size:.75rem;max-height:400px;overflow:auto;"></pre>
          </details>

          <%-- Refine button --%>
          <button type="button" class="btn btn-sm btn-outline-secondary ghost-action"
                  onclick="useForRefine()">
            <i class="bi bi-arrow-repeat me-1"></i>Use this draft as input for Refine
          </button>

        </div><%-- /resultsWrap --%>
      </div><%-- /col results --%>

    </div><%-- /row --%>
  </div><%-- /audit-body --%>
</div><%-- /audit-wrap --%>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
let lastRawResponse = null;

document.querySelectorAll('input[name="draftMode"]').forEach(r => {
    r.addEventListener('change', () => {
        document.getElementById('prevDraftWrap').classList
            .toggle('d-none', r.value !== 'REFINE' || !r.checked);
    });
});

async function generateDraft() {
    const sender  = document.getElementById('inboundSender').value.trim();
    const subject = document.getElementById('inboundSubject').value.trim();
    const body    = document.getElementById('inboundBody').value.trim();
    const alert   = document.getElementById('validationAlert');

    if (!sender || !subject || !body) {
        alert.textContent = 'Sender email, subject, and body are all required.';
        alert.classList.remove('d-none');
        return;
    }
    alert.classList.add('d-none');

    const mode = document.querySelector('input[name="draftMode"]:checked').value;

    const payload = {
        inboundSubject: subject,
        inboundBody:    body,
        inboundSender:  sender,
        inboundDate:    document.getElementById('inboundDate').value || null,
        userNotes:      document.getElementById('userNotes').value.trim() || null,
        draftMode:      mode
    };

    if (mode === 'REFINE') {
        const prevText = document.getElementById('previousDraft').value.trim();
        if (prevText) {
            try { payload.previousDraft = JSON.parse(prevText); }
            catch(e) {
                alert.textContent = 'Previous draft is not valid JSON — fix it or switch to Fresh mode.';
                alert.classList.remove('d-none');
                return;
            }
        }
    }

    showSpinner();
    try {
        const resp = await fetch('EmailAssistantDraft', {
            method:  'POST',
            headers: {'Content-Type': 'application/json'},
            body:    JSON.stringify(payload)
        });
        const data = await resp.json();
        if (!resp.ok) {
            renderError(data);
        } else {
            renderResult(data);
        }
    } catch(err) {
        renderError({ error: 'network', message: err.message });
    } finally {
        hideSpinner();
    }
}

function renderResult(data) {
    lastRawResponse = data;
    hideAll();
    document.getElementById('resultsWrap').classList.remove('d-none');

    // Subject + body preview
    document.getElementById('previewSubject').textContent = data.subject || '';
    document.getElementById('previewSubjectLabel').textContent =
        data.subject ? '— ' + data.subject : '';
    document.getElementById('previewBody').innerHTML = data.body || '';

    // Draft type badge
    const dtBadge = document.getElementById('badgeDraftType');
    dtBadge.textContent = data.draftType || 'UNKNOWN';
    dtBadge.className = 'badge rounded-pill ' +
        (data.draftType === 'FULL_DRAFT' ? 'bg-success' : 'bg-info text-dark');

    // Escalation badge
    const escBadge = document.getElementById('badgeEscalation');
    const esc = data.escalation || 'NONE';
    escBadge.textContent = 'ESC: ' + esc;
    escBadge.className = 'badge rounded-pill ' + escalationClass(esc);
    // Show escalation note
    const escNote = document.getElementById('escalationNote');
    if (esc !== 'NONE') {
        document.getElementById('escalationReason').textContent = data.escalationReason || '';
        const qWrap = document.getElementById('escalationQuestion');
        if (data.escalationQuestionForKevin) {
            document.getElementById('escalationQuestionText').textContent =
                data.escalationQuestionForKevin;
            qWrap.classList.remove('d-none');
        } else {
            qWrap.classList.add('d-none');
        }
        escNote.classList.remove('d-none');
    } else {
        escNote.classList.add('d-none');
    }

    // Encryption badge
    const encBadge = document.getElementById('badgeEncryption');
    const enc = data.encryption || 'NONE';
    encBadge.textContent = 'ENC: ' + enc;
    encBadge.className = 'badge rounded-pill ' + encryptionClass(enc);
    const encNote = document.getElementById('encryptionNote');
    if (enc !== 'NONE') {
        document.getElementById('encryptionLevel').textContent   = enc;
        document.getElementById('encryptionReason').textContent  = data.encryptionReason || '';
        encNote.classList.remove('d-none');
    } else {
        encNote.classList.add('d-none');
    }

    // Tone badge
    const tone = (data.completeness && data.completeness.toneCalibrated) || 'unknown';
    const toneBadge = document.getElementById('badgeTone');
    toneBadge.textContent = 'TONE: ' + tone;
    toneBadge.className = 'badge rounded-pill ' +
        (tone === 'appropriate' ? 'bg-success' : 'bg-warning text-dark');

    // Completeness checks
    if (data.completeness) {
        const c = data.completeness;
        setCheck('checkPlanNamed',  c.planNamed,      'Plan Named');
        setCheck('checkDateCited',  c.dateCited,      'Date Cited');
        setCheck('checkNextSteps',  c.nextStepsStated, 'Next Steps');
    }

    // Metadata
    if (data.metadata) {
        const m = data.metadata;
        document.getElementById('metaModel').textContent  = m.modelUsed || '—';
        document.getElementById('metaKbs').textContent    =
            (m.kbsConsulted || []).join(', ');
        document.getElementById('metaAlways').textContent = m.alwaysLoadChunkCount;
        document.getElementById('metaSearch').textContent = m.searchChunkCount;
        document.getElementById('metaAms').textContent    =
            m.amsContextResolved ? '✅ Resolved' : '⚠️ Not found';
    }

    // Raw JSON
    document.getElementById('rawJson').textContent = JSON.stringify(data, null, 2);
}

function renderError(data) {
    hideAll();
    document.getElementById('errorWrap').classList.remove('d-none');
    document.getElementById('errorMsg').textContent =
        (data.error || '') + ': ' + (data.message || JSON.stringify(data));
}

function setCheck(id, ok, label) {
    const el = document.getElementById(id);
    el.textContent = (ok ? '✓ ' : '✗ ') + label;
    el.className = 'badge border small ' +
        (ok ? 'border-success text-success' : 'border-danger text-danger');
}

function escalationClass(esc) {
    if (esc === 'NONE')      return 'bg-success';
    if (esc === 'SOFT_JUDG') return 'bg-warning text-dark';
    if (esc === 'SOFT_CONF') return 'bg-warning text-dark';
    if (esc === 'HARD')      return 'bg-danger';
    return 'bg-secondary';
}

function encryptionClass(enc) {
    if (enc === 'NONE')        return 'bg-success';
    if (enc === 'RECOMMENDED') return 'bg-warning text-dark';
    if (enc === 'REQUIRED')    return 'bg-danger';
    return 'bg-secondary';
}

function showSpinner() {
    hideAll();
    document.getElementById('generateBtn').disabled = true;
    document.getElementById('spinnerWrap').classList.remove('d-none');
}
function hideSpinner() {
    document.getElementById('generateBtn').disabled = false;
    document.getElementById('spinnerWrap').classList.add('d-none');
}
function hideAll() {
    ['spinnerWrap','errorWrap','resultsWrap'].forEach(id =>
        document.getElementById(id).classList.add('d-none'));
}

function clearForm() {
    ['inboundSender','inboundDate','inboundSubject','inboundBody',
     'userNotes','previousDraft'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    document.getElementById('modeFresh').checked = true;
    document.getElementById('prevDraftWrap').classList.add('d-none');
    document.getElementById('validationAlert').classList.add('d-none');
    hideAll();
}

function copyBody() {
    const html = document.getElementById('previewBody').innerHTML;
    navigator.clipboard.writeText(html).then(
        () => { const btn = event.currentTarget;
                const orig = btn.innerHTML;
                btn.innerHTML = '<i class="bi bi-check me-1"></i>Copied!';
                setTimeout(() => btn.innerHTML = orig, 1500); },
        () => alert('Clipboard write failed.')
    );
}

function useForRefine() {
    if (!lastRawResponse) return;
    document.getElementById('previousDraft').value =
        JSON.stringify(lastRawResponse, null, 2);
    document.getElementById('modeRefine').checked = true;
    document.getElementById('prevDraftWrap').classList.remove('d-none');
    window.scrollTo({ top: 0, behavior: 'smooth' });
}
</script>
</body>
</html>
