<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Monthly Billing</title>
    <style>
        .mb-wrap { max-width: 900px; margin: 1rem auto; }
        .file-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0.55rem 0.85rem;
            border-bottom: 1px solid #f0f0f0;
            font-size: 0.85rem;
        }
        .file-row:last-child { border-bottom: none; }
        .file-row .file-name { font-weight: 600; }
        .badge-present { background: #d4edda; color: #155724; }
        .badge-missing { background: #f8d7da; color: #842029; }
        .badge-optional { background: #e2e3e5; color: #383d41; }
        .callout {
            border-radius: 8px;
            padding: 0.85rem 1rem;
            margin-bottom: 1rem;
            font-size: 0.88rem;
        }
        .callout-success { background: #d4edda; color: #155724; }
        .callout-danger { background: #f8d7da; color: #842029; }

        /* Run progress panel */
        .step-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0.5rem 0.85rem;
            border-bottom: 1px solid #f0f0f0;
            font-size: 0.85rem;
        }
        .step-row:last-child { border-bottom: none; }
        .badge-pending { background: #e2e3e5; color: #383d41; }
        .badge-running { background: #cfe2ff; color: #084298; }
        .badge-completed { background: #d4edda; color: #155724; }
        .badge-failed { background: #f8d7da; color: #842029; }
        .badge-skipped { background: #e2e3e5; color: #6c757d; }
        .spinner-sm {
            display: inline-block;
            width: 0.7rem; height: 0.7rem;
            border: 2px solid currentColor;
            border-right-color: transparent;
            border-radius: 50%;
            animation: mb-spin 0.75s linear infinite;
            margin-right: 0.35rem;
            vertical-align: -1px;
        }
        @keyframes mb-spin { to { transform: rotate(360deg); } }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid mb-wrap">

    <h4 style="color: var(--ssa);"><i class="bi bi-calculator"></i> Monthly Billing</h4>
    <p class="text-muted mb-3">Pre-flight check the monthly Summit exports, upload the HSA file, then launch the full import + billing pipeline.</p>
    <hr>

    <%-- Flash message --%>
    <c:if test="${not empty flash}">
        <div class="alert alert-warning">${flash}</div>
    </c:if>

    <%-- Error state --%>
    <c:if test="${not empty preflightError}">
        <div class="alert alert-danger">
            <i class="bi bi-exclamation-triangle me-2"></i>Could not read the upload folder: ${preflightError}
        </div>
    </c:if>

    <c:if test="${empty preflightError}">

        <%-- Pre-flight checklist --%>
        <div class="card file-card mb-3">
            <div class="hdr-bar"><i class="bi bi-list-check me-2"></i>Required Files</div>
            <div>
                <c:forEach var="f" items="${preflight.files}">
                    <div class="file-row">
                        <span class="file-name">
                            ${f.label}
                            <c:if test="${not f.required}"><span class="badge badge-optional ms-1">optional</span></c:if>
                        </span>
                        <c:choose>
                            <c:when test="${f.present}">
                                <span class="badge badge-present"><i class="bi bi-check-circle me-1"></i>${f.matchedFileName}</span>
                            </c:when>
                            <c:when test="${f.required}">
                                <span class="badge badge-missing"><i class="bi bi-x-circle me-1"></i>Missing</span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge badge-optional">Not provided</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </c:forEach>
            </div>
        </div>

        <%-- Plan Type verdict --%>
        <c:choose>
            <c:when test="${preflight.planTypeSupplied}">
                <div class="callout callout-success">
                    <i class="bi bi-check-circle me-2"></i>Plan Type file provided.
                </div>
            </c:when>
            <c:when test="${preflight.planTypeGapDetected}">
                <div class="callout callout-danger">
                    <i class="bi bi-exclamation-triangle me-2"></i>
                    New plan type(s) referenced (${preflight.newPlanTypeIds}) &mdash; run the Plan Type export in Summit and add it before launching.
                </div>
            </c:when>
            <c:otherwise>
                <div class="callout callout-success">
                    <i class="bi bi-check-circle me-2"></i>No new plan types detected &mdash; Plan Type file not needed.
                </div>
            </c:otherwise>
        </c:choose>

        <%-- Unrecognized files --%>
        <c:if test="${not empty preflight.unrecognizedFiles}">
            <div class="alert alert-secondary" style="font-size: 0.85rem;">
                <i class="bi bi-info-circle me-2"></i>Unrecognized files in the upload folder (ignored):
                <c:forEach var="uf" items="${preflight.unrecognizedFiles}" varStatus="s">${uf}<c:if test="${!s.last}">, </c:if></c:forEach>
            </div>
        </c:if>

        <%-- HSA upload control --%>
        <div class="card file-card mb-3">
            <div class="hdr-bar"><i class="bi bi-cloud-upload me-2"></i>HSA File</div>
            <div class="p-3">
                <form method="post" enctype="multipart/form-data" action="MonthlyBillingLauncher">
                    <div class="d-flex align-items-center gap-2">
                        <input type="file" class="form-control form-control-sm" name="hsaFile" accept=".csv" style="max-width: 400px;">
                        <button type="submit" class="btn btn-sm btn-outline-primary"><i class="bi bi-upload me-1"></i>Upload HSA File</button>
                    </div>
                </form>
            </div>
        </div>

        <%-- Mode selector --%>
        <div class="card file-card mb-3">
            <div class="hdr-bar"><i class="bi bi-gear me-2"></i>Run Mode</div>
            <div class="p-3">
                <div class="form-check">
                    <input class="form-check-input" type="radio" name="billingMode" id="modeFull" value="FULL" checked>
                    <label class="form-check-label" for="modeFull">Full monthly run (wipe, import, promote, clear billing, create billing)</label>
                </div>
                <div class="form-check">
                    <input class="form-check-input" type="radio" name="billingMode" id="modeBillingOnly" value="BILLING_ONLY">
                    <label class="form-check-label" for="modeBillingOnly">Billing only (re-run clear billing + create billing)</label>
                </div>
            </div>
        </div>

        <%-- Run button --%>
        <div class="card file-card mb-3">
            <div class="p-3">
                <button id="runBillingBtn" type="button" class="btn btn-warning" data-mode-source="billingMode"
                        <c:if test="${not preflight.canLaunch}">disabled</c:if>>
                    <i class="bi bi-play-fill me-1"></i>Run Monthly Billing
                </button>
                <c:if test="${not preflight.canLaunch}">
                    <div class="text-muted mt-2" style="font-size: 0.82rem;">Resolve the items above to enable.</div>
                </c:if>
                <div id="billingLauncherData"
                     data-plan-type-supplied="${preflight.planTypeSupplied}"
                     data-latest-run-id="${latestRunId}"
                     data-latest-run-status="${latestRunStatus}"></div>
                <div id="billingProgress" class="mt-3"></div>
            </div>
        </div>

    </c:if>

</div>

<script>
(function () {
  const data = document.getElementById('billingLauncherData');
  if (!data) return;

  const planTypeSupplied = data.dataset.planTypeSupplied === 'true';
  const runBtn = document.getElementById('runBillingBtn');
  const progress = document.getElementById('billingProgress');
  let pollTimer = null;

  const STEP_LABELS = {
    WIPE: 'Wipe Staging Tables',
    IMPORT: 'Import',
    PROMOTE: 'Promote',
    CLEAR_BILLING: 'Clear Billing',
    CREATE_BILLING: 'Create Billing'
  };
  const STEP_ORDER = ['WIPE', 'IMPORT', 'PROMOTE', 'CLEAR_BILLING', 'CREATE_BILLING'];
  const STATUS_CLASS = {
    PENDING: 'badge-pending',
    RUNNING: 'badge-running',
    COMPLETED: 'badge-completed',
    FAILED: 'badge-failed',
    SKIPPED: 'badge-skipped'
  };

  function esc(s) {
    if (s === null || s === undefined) return '';
    return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function launch(mode) {
    setControlsDisabled(true);
    const body = 'mode=' + encodeURIComponent(mode) +
                 '&planTypeSupplied=' + (planTypeSupplied ? 'true' : 'false');
    fetch('LaunchMonthlyBilling', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body
    }).then(async (res) => {
      if (res.status === 200) {
        const json = await res.json();
        startPolling(json.runId);
      } else if (res.status === 409 || res.status === 503) {
        const json = await res.json().catch(() => ({}));
        showMessage(json.error || 'Unable to start the run.', 'warning');
        setControlsDisabled(false);
      } else {
        showMessage('Launch failed (HTTP ' + res.status + ').', 'danger');
        setControlsDisabled(false);
      }
    }).catch(() => {
      showMessage('Network error starting the run.', 'danger');
      setControlsDisabled(false);
    });
  }

  function startPolling(runId) {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    poll(runId);
    pollTimer = setInterval(() => poll(runId), 2500);
  }

  function poll(runId) {
    fetch('BillingRunStatus?runId=' + encodeURIComponent(runId))
      .then((res) => res.ok ? res.json() : Promise.reject(res.status))
      .then((run) => {
        render(run);
        if (run.status === 'COMPLETED' || run.status === 'FAILED') {
          if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
          setControlsDisabled(false);
        }
      })
      .catch(() => { /* transient — keep polling; the run continues server-side */ });
  }

  function render(run) {
    const stepsByName = {};
    (run.steps || []).forEach(s => { stepsByName[s.stepName] = s; });

    let html = '';

    // Overall status banner
    if (run.status === 'RUNNING') {
      html += '<div class="callout" style="background:#cfe2ff;color:#084298;">' +
              '<span class="spinner-sm"></span>Running' +
              (run.currentStep ? ' &mdash; ' + esc(run.currentStep) : '') +
              '</div>';
    } else if (run.status === 'COMPLETED') {
      html += '<div class="callout callout-success"><i class="bi bi-check-circle me-2"></i>Billing run completed successfully.</div>';
    } else if (run.status === 'FAILED') {
      html += '<div class="callout callout-danger"><i class="bi bi-exclamation-triangle me-2"></i>Billing run failed' +
              (run.errorText ? ': ' + esc(run.errorText) : '.') + '</div>';
    }

    // Step list
    html += '<div class="card file-card mb-3"><div class="hdr-bar"><i class="bi bi-list-ol me-2"></i>Pipeline Steps</div><div>';
    STEP_ORDER.forEach(function (name) {
      const s = stepsByName[name];
      const status = s ? s.status : 'PENDING';
      const cls = STATUS_CLASS[status] || 'badge-pending';
      const label = STEP_LABELS[name] || name;
      const isFailed = status === 'FAILED';
      html += '<div class="step-row"' + (isFailed ? ' style="background:#fff5f5;"' : '') + '>' +
              '<span class="file-name">' + esc(label) + '</span>' +
              '<span>';
      if (s && s.detail && status === 'COMPLETED') {
        html += '<span class="text-muted me-2" style="font-size:0.78rem;">' + esc(s.detail) + '</span>';
      }
      html += '<span class="badge ' + cls + '">' +
              (status === 'RUNNING' ? '<span class="spinner-sm"></span>' : '') +
              esc(status) + '</span></span></div>';
      if (isFailed && s.errorText) {
        html += '<div class="step-row" style="background:#fff5f5;">' +
                '<span class="text-danger" style="font-size:0.78rem;">' + esc(s.errorText) + '</span></div>';
      }
    });
    html += '</div></div>';

    // Failed run — offer billing-only re-run
    if (run.status === 'FAILED') {
      html += '<button type="button" class="btn btn-sm btn-outline-warning" id="rerunBillingOnlyBtn">' +
              '<i class="bi bi-arrow-clockwise me-1"></i>Re-run billing only</button>';
    }

    progress.innerHTML = html;

    if (run.status === 'FAILED') {
      const rerunBtn = document.getElementById('rerunBillingOnlyBtn');
      if (rerunBtn) {
        rerunBtn.addEventListener('click', function () { launch('BILLING_ONLY'); });
      }
    }
  }

  function setControlsDisabled(disabled) {
    if (runBtn) runBtn.disabled = disabled;
    document.querySelectorAll('input[name="billingMode"]').forEach(function (r) { r.disabled = disabled; });
    const hsaSubmit = document.querySelector('input[name="hsaFile"]');
    if (hsaSubmit) {
      const form = hsaSubmit.closest('form');
      if (form) {
        const btn = form.querySelector('button[type="submit"]');
        if (btn) btn.disabled = disabled;
      }
    }
  }

  function showMessage(msg, level) {
    progress.innerHTML = '<div class="alert alert-' + esc(level) + '">' + esc(msg) + '</div>';
  }

  if (runBtn) {
    runBtn.addEventListener('click', function () {
      const sel = document.querySelector('input[name="billingMode"]:checked');
      launch(sel ? sel.value : 'FULL');
    });
  }

  // Resume-on-refresh: if a run is already in progress, pick up polling immediately.
  const latestId = data.dataset.latestRunId;
  const latestStatus = data.dataset.latestRunStatus;
  if (latestId && latestStatus === 'RUNNING') {
    setControlsDisabled(true);
    startPolling(latestId);
  }
})();
</script>
</body>
</html>
