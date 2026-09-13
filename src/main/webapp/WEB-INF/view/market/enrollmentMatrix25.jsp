<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Enrollment Matrix</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .audit-wrap {
            display: flex; flex-direction: column;
            height: calc(100vh - 64px);
        }
        .toolbar {
            padding: 0.65rem 1rem;
            background: #fff; border-bottom: 1px solid #dee2e6;
            display: flex; align-items: center; gap: 0.75rem;
        }
        .toolbar .t-title { font-weight: 700; color: var(--ssa, #0d5681); font-size: 0.95rem; margin: 0; }
        .rc-body { flex: 1; overflow-y: auto; padding: 0.75rem 1rem; background: #eef1f5; }
        .status-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-bottom: 0.9rem; font-size: 0.85rem;
        }
        .status-card.pushed { background: #fff3cd; border-color: #ffe69c; }
        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
        .matrix-layout { display: flex; gap: 0.9rem; align-items: flex-start; }
        .matrix-sidebar {
            flex: 0 0 240px; background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            max-height: calc(100vh - 220px); overflow-y: auto;
        }
        .matrix-sidebar button.participant-item {
            display: block; width: 100%; text-align: left; border: none; background: none;
            padding: 0.5rem 0.85rem; font-size: 0.85rem; border-bottom: 1px solid #f1f3f5;
            cursor: pointer;
        }
        .matrix-sidebar button.participant-item:hover { background: #f8f9fa; }
        .matrix-sidebar button.participant-item.active { background: var(--ssa, #0d5681); color: #fff; font-weight: 600; }
        .matrix-sidebar .locked-badge { font-size: 0.68rem; color: #b45309; margin-left: 0.3rem; }
        .matrix-detail-wrap { flex: 1; }
        .matrix-detail-panel {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px; padding: 0.9rem 1.1rem;
        }
        .detail-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem;
            font-weight: 700; color: var(--ssa, #0d5681); font-size: 0.95rem; }
        .lock-banner { font-size: 0.8rem; color: #b45309; background: #fff3cd; border: 1px solid #ffe69c;
            border-radius: 4px; padding: 0.4rem 0.6rem; margin-bottom: 0.75rem; }
        .nav-tabs.leg-tabs { border-bottom: 1px solid #dee2e6; }
        .nav-tabs.leg-tabs .nav-link { font-size: 0.82rem; }
        .tab-content { border: 1px solid #dee2e6; border-top: none; padding: 0.85rem; border-radius: 0 0 6px 6px; }
        .form-check label { text-transform: none; }
        label.field-label { font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em;
            color: #6c757d; font-weight: 600; margin-bottom: 0.15rem; display: block; }
        .empty-state { text-align: center; padding: 2.5rem 1rem; color: #6c757d; font-size: 0.88rem;
            background: #fff; border: 1px dashed #dee2e6; border-radius: 6px; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0">
            <i class="bi bi-grid-3x3-gap me-1"></i>Enrollment Matrix
            <c:if test="${not empty prospectName}"> &mdash; <c:out value="${prospectName}"/></c:if>
        </h1>
    </div>

    <div class="rc-body">

        <c:if test="${not empty sessionScope.enrollmentMatrixMessage}">
            <div class="alert alert-success alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-check-circle me-1"></i><c:out value="${sessionScope.enrollmentMatrixMessage}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="enrollmentMatrixMessage" scope="session"/>
        </c:if>
        <c:if test="${not empty sessionScope.enrollmentMatrixError}">
            <div class="alert alert-danger alert-dismissible fade show py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${sessionScope.enrollmentMatrixError}"/>
                <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert"></button>
            </div>
            <c:remove var="enrollmentMatrixError" scope="session"/>
        </c:if>

        <%-- s52m -- one of doGet's post-gate guard clauses refused (bad/missing setupId, no PSP
             on the session, no such setup, or a setup with no linked application) and forwarded
             straight here instead of redirecting, so a PSP admin who is allowed on this page can
             see why it refused. The pre-gate PSP-admin check is unaffected -- it still redirects,
             so a non-PSP-admin never learns this page exists. --%>
        <c:if test="${not empty guardMessage}">
            <div class="alert alert-danger py-2" style="font-size:0.85rem;" role="alert">
                <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${guardMessage}"/>
            </div>
        </c:if>

        <c:if test="${matrix.pushed}">
            <div class="status-card pushed">
                <i class="bi bi-lock-fill me-1"></i>This matrix was pushed
                <c:if test="${not empty matrix.pushedBy}"> by <c:out value="${matrix.pushedBy}"/></c:if>
                <c:if test="${not empty matrix.pushedAt}"> at <c:out value="${matrix.pushedAt}"/></c:if>.
                The whole table is read-only.
            </div>
        </c:if>

        <c:choose>
            <c:when test="${empty roster}">
                <div class="empty-state">No participants loaded for this employer yet.</div>
            </c:when>
            <c:otherwise>

                <%-- Hidden unlock form, outside the main save form -- HTML forbids nesting forms,
                     and unlocking is a separate, smaller POST with its own confirm. Each row's
                     Unlock button is type=button and fills this form via JS before submitting it. --%>
                <form id="unlockForm" method="post" action="${pageContext.request.contextPath}/EnrollmentMatrix" style="display:none;">
                    <input type="hidden" name="action" value="unlock">
                    <input type="hidden" name="setupId" value="${setupId}">
                    <input type="hidden" name="matrixParticipantId" id="unlockMatrixParticipantId">
                </form>

                <%-- Carries the prefill value as a properly HTML-attribute-escaped hidden field
                     rather than embedding it in a JS string literal -- fn:escapeXml escapes for
                     HTML, not JS, so a name containing a quote would otherwise break the script
                     or leave literal "&quot;" in the value. JS reads this element's .value. --%>
                <input type="hidden" id="mostRecentCustomScheduleName" value="<c:out value="${mostRecentCustomScheduleName}"/>">
                <input type="hidden" id="otherCustomValue" value="<c:out value="${otherCustom}"/>">

                <form id="matrixForm" method="post" action="${pageContext.request.contextPath}/EnrollmentMatrix">
                    <input type="hidden" name="action" value="save">
                    <input type="hidden" name="setupId" value="${setupId}">
                    <input type="hidden" name="participantIds" value="<c:forEach var="p" items="${roster}" varStatus="vs">${p.id}<c:if test="${!vs.last}">,</c:if></c:forEach>">

                    <div class="matrix-layout">
                        <div class="matrix-sidebar" id="participantList">
                            <c:forEach var="p" items="${roster}" varStatus="vs">
                                <c:set var="header" value="${headersByParticipant[p.id]}"/>
                                <button type="button" class="participant-item${vs.first ? ' active' : ''}"
                                        data-target="detail-${p.id}" onclick="ammSelect(${p.id})">
                                    <c:out value="${p.lastName}"/>, <c:out value="${p.firstName}"/>
                                    <c:if test="${not empty header and header.entryLocked}">
                                        <span class="locked-badge"><i class="bi bi-lock-fill"></i></span>
                                    </c:if>
                                </button>
                            </c:forEach>
                        </div>

                        <div class="matrix-detail-wrap">
                            <c:forEach var="p" items="${roster}" varStatus="vs">
                                <c:set var="header" value="${headersByParticipant[p.id]}"/>
                                <c:set var="isLocked" value="${not empty header and header.entryLocked}"/>
                                <c:set var="disableInputs" value="${isLocked or matrix.pushed}"/>

                                <div class="matrix-detail-panel" id="detail-${p.id}" ${vs.first ? '' : 'hidden'}>
                                    <div class="detail-header">
                                        <c:out value="${p.lastName}"/>, <c:out value="${p.firstName}"/>
                                    </div>

                                    <c:if test="${isLocked}">
                                        <div class="lock-banner">
                                            <i class="bi bi-lock-fill me-1"></i>Locked
                                            <c:if test="${not empty header.lockedBy}"> by <c:out value="${header.lockedBy}"/></c:if>
                                            <c:if test="${not empty header.lockedAt}"> at <c:out value="${header.lockedAt}"/></c:if>.
                                            <button type="button" class="btn btn-sm btn-outline-secondary py-0 px-2 ms-2"
                                                    onclick="ammUnlock(${header.id})" ${matrix.pushed ? 'disabled' : ''}>Unlock</button>
                                        </div>
                                    </c:if>

                                    <div class="row g-2 mb-3">
                                        <div class="col-md-3">
                                            <label class="field-label" for="payrollFrequency_${p.id}">Payroll frequency</label>
                                            <select class="form-select form-select-sm" id="payrollFrequency_${p.id}"
                                                    name="payrollFrequency_${p.id}" onchange="ammFrequencyChanged(${p.id})"
                                                    ${disableInputs ? 'disabled' : ''}>
                                                <option value="" ${empty header.payrollFrequency ? 'selected' : ''}>&mdash; choose &mdash;</option>
                                                <c:forEach var="opt" items="${payrollFrequencyOptions}">
                                                    <option value="${opt}" ${not empty header and header.payrollFrequency eq opt ? 'selected' : ''}>
                                                        <c:out value="${opt}"/>
                                                    </option>
                                                </c:forEach>
                                            </select>
                                            <%-- ⚠️ Only OTHER_CUSTOM and OTHER_NOT_IMPORTABLE exist. The curated
                                                 enrollment-approved global list is not yet designated -- this
                                                 select is deliberately not built out further. --%>
                                        </div>
                                        <div class="col-md-4" id="customScheduleWrap_${p.id}"
                                             ${not empty header and header.payrollFrequency eq otherCustom ? '' : 'style="display:none;"'}>
                                            <label class="field-label" for="customScheduleName_${p.id}">Custom schedule name</label>
                                            <input type="text" class="form-control form-control-sm"
                                                   id="customScheduleName_${p.id}" name="customScheduleName_${p.id}"
                                                   value="<c:out value="${header.customScheduleName}"/>"
                                                   ${disableInputs ? 'disabled' : ''}>
                                        </div>
                                        <c:if test="${not disableInputs}">
                                            <div class="col-md-3 d-flex align-items-end">
                                                <div class="form-check mb-1">
                                                    <input class="form-check-input" type="checkbox" id="lock_${p.id}" name="lock_${p.id}">
                                                    <label class="form-check-label" for="lock_${p.id}">Lock this row on save</label>
                                                </div>
                                            </div>
                                        </c:if>
                                    </div>

                                    <c:choose>
                                        <c:when test="${empty legs}">
                                            <div class="text-muted" style="font-size:0.85rem;">
                                                No enrollment legs are configured for this setup's sale yet
                                                (no <span class="mono">summit_plan_template_map</span> row with a
                                                non-<span class="mono">NONE</span> <span class="mono">enrollment_amount_mode</span>
                                                for an elected service item).
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <ul class="nav nav-tabs leg-tabs" role="tablist">
                                                <c:forEach var="leg" items="${legs}" varStatus="lvs">
                                                    <li class="nav-item" role="presentation">
                                                        <button class="nav-link${lvs.first ? ' active' : ''}"
                                                                id="legTab-${p.id}-${leg.id}" data-bs-toggle="tab"
                                                                data-bs-target="#legPanel-${p.id}-${leg.id}"
                                                                type="button" role="tab">
                                                            <c:out value="${empty leg.label ? leg.keySegment : leg.label}"/>
                                                        </button>
                                                    </li>
                                                </c:forEach>
                                            </ul>
                                            <div class="tab-content">
                                                <c:forEach var="leg" items="${legs}" varStatus="lvs">
                                                    <c:set var="entryKey" value="${header.id}_${leg.id}"/>
                                                    <c:set var="entry" value="${not empty header ? entriesByParticipantAndLeg[entryKey] : null}"/>
                                                    <div class="tab-pane fade${lvs.first ? ' show active' : ''}"
                                                         id="legPanel-${p.id}-${leg.id}" role="tabpanel">
                                                        <div class="row g-2 align-items-end">
                                                            <c:if test="${leg.enrollmentAmountMode eq 'MONTHLY_PREMIUM' or leg.enrollmentAmountMode eq 'ANNUAL_ELECTION'}">
                                                                <div class="col-md-3">
                                                                    <label class="field-label" for="amount_${p.id}_${leg.id}">
                                                                        <c:choose>
                                                                            <c:when test="${leg.enrollmentAmountMode eq 'MONTHLY_PREMIUM'}">Monthly premium</c:when>
                                                                            <c:otherwise>Annual election</c:otherwise>
                                                                        </c:choose>
                                                                    </label>
                                                                    <input type="number" step="0.01" class="form-control form-control-sm mono"
                                                                           id="amount_${p.id}_${leg.id}" name="amount_${p.id}_${leg.id}"
                                                                           value="${entry.amount}" ${disableInputs ? 'disabled' : ''}>
                                                                </div>
                                                            </c:if>
                                                            <c:if test="${leg.enrollmentAmountMode eq 'TIER'}">
                                                                <div class="col-md-4">
                                                                    <label class="field-label" for="tier_${p.id}_${leg.id}">Tier</label>
                                                                    <input type="text" class="form-control form-control-sm"
                                                                           id="tier_${p.id}_${leg.id}" name="tier_${p.id}_${leg.id}"
                                                                           value="<c:out value="${entry.tierName}"/>" ${disableInputs ? 'disabled' : ''}>
                                                                    <%-- No amount box under TIER -- the HRA setup already carries the
                                                                         amount against the tier; the enrollment file resolves it by
                                                                         tier-name match. --%>
                                                                </div>
                                                            </c:if>
                                                            <div class="col-md-3 d-flex align-items-end">
                                                                <div class="form-check mb-1">
                                                                    <input class="form-check-input" type="checkbox"
                                                                           id="declined_${p.id}_${leg.id}" name="declined_${p.id}_${leg.id}"
                                                                           ${not empty entry and entry.declined ? 'checked' : ''}
                                                                           ${disableInputs ? 'disabled' : ''}>
                                                                    <label class="form-check-label" for="declined_${p.id}_${leg.id}">Declined</label>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </c:forEach>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </c:forEach>
                        </div>
                    </div>

                    <c:if test="${not matrix.pushed}">
                        <div class="mt-3">
                            <button type="submit" class="btn btn-sm btn-ssa">Save Matrix</button>
                        </div>
                    </c:if>
                </form>

            </c:otherwise>
        </c:choose>

    </div>
</div>

<script>
    // Master-detail: one participant's panel visible at a time. No JS framework -- plain show/hide.
    function ammSelect(participantId) {
        document.querySelectorAll('.matrix-detail-panel').forEach(function (el) { el.hidden = true; });
        document.querySelectorAll('.participant-item').forEach(function (el) { el.classList.remove('active'); });
        var panel = document.getElementById('detail-' + participantId);
        if (panel) panel.hidden = false;
        var item = document.querySelector('.participant-item[data-target="detail-' + participantId + '"]');
        if (item) item.classList.add('active');
    }

    // OTHER_CUSTOM prefill: only when the field is currently empty, never overwriting what is
    // already typed, and never storing a matrix-level default -- read from the hidden fields
    // above (HTML-attribute-escaped, not JS-string-escaped, so a name with a quote in it can't
    // break this script).
    function ammFrequencyChanged(participantId) {
        var select = document.getElementById('payrollFrequency_' + participantId);
        var wrap = document.getElementById('customScheduleWrap_' + participantId);
        var input = document.getElementById('customScheduleName_' + participantId);
        var otherCustomValue = document.getElementById('otherCustomValue').value;
        var mostRecent = document.getElementById('mostRecentCustomScheduleName').value;
        if (!select || !wrap) return;
        if (select.value === otherCustomValue) {
            wrap.style.display = '';
            if (input && !input.value && mostRecent) {
                input.value = mostRecent;
            }
        } else {
            wrap.style.display = 'none';
        }
    }

    function ammUnlock(matrixParticipantId) {
        if (!confirm('Unlock this participant row? Its entries can be edited again until it is re-locked.')) return;
        document.getElementById('unlockMatrixParticipantId').value = matrixParticipantId;
        document.getElementById('unlockForm').submit();
    }
</script>
</body>
</html>
