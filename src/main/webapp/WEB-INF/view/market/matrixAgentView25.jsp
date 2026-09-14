<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%-- S58-P7 -- the agent's enrollment matrix, /matrix/{guid}: the PRIMARY entry surface, built for
     collecting elections from employees over a span of weeks and showing at a glance what is still
     missing. Served by EnrollmentMatrixServlet.doGetByGuid after LoginFilter's test, the
     open/not-locked preconditions and MatrixAccessResolver; every save is one participant
     (action=agentSave), re-authorised server-side.

     Field permissions (S58-P7 Part 2): payroll frequency (global list, OTHER_* included), agent
     schedule note, amount / tier / declined -- editable. Custom schedule name (the Summit-bound
     value), lock/unlock, export/push -- never on this page; agentSave ignores customScheduleName
     even if posted.

     Own chrome -- no navbar25.jsp, NO <base> tag: every URL is prefixed with the context path.
     css-js.jsp is imported because it emits only CDN URLs and a context-path-prefixed favicon. --%>
<jsp:useBean id="now" class="java.util.Date" scope="page"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="openId" value="${not empty agentSavedParticipantId ? agentSavedParticipantId : focusParticipantId}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${empty prospectName ? 'Enrollment Worksheet' : prospectName}"/> &middot; Enrollment Worksheet</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        :root {
            --amv-ink: #1f2933; --amv-muted: #6b7280; --amv-line: #e5e7eb;
            --amv-band: #0d5681; --amv-band-2: #0a4568; --amv-page: #f3f5f8; --amv-card: #fff;
            --amv-ok: #1f7a4d; --amv-ok-bg: #e6f2ec; --amv-warn: #b45309; --amv-warn-bg: #fef3c7;
            --amv-info: #1d4ed8; --amv-info-bg: #dbeafe; --amv-neutral: #4b5563; --amv-neutral-bg: #e5e7eb;
        }
        html, body { background: var(--amv-page); color: var(--amv-ink); }
        body { font-size: 1rem; line-height: 1.45; -webkit-text-size-adjust: 100%; }
        label { text-transform: none; }
        .amv-band { background: linear-gradient(135deg, var(--amv-band), var(--amv-band-2)); color: #fff; padding: 1.1rem 1rem 1rem; }
        .amv-wrap { max-width: 980px; margin: 0 auto; padding: 0 1rem; }
        .amv-kicker { font-size: 0.72rem; letter-spacing: 0.12em; text-transform: uppercase; opacity: 0.8; margin: 0 0 0.2rem; }
        .amv-employer { font-size: 1.45rem; font-weight: 700; margin: 0; line-height: 1.2; }
        .amv-chips { display: flex; flex-wrap: wrap; gap: 0.4rem 0.6rem; margin-top: 0.6rem; font-size: 0.82rem; }
        .amv-chip { background: rgba(255,255,255,0.14); border: 1px solid rgba(255,255,255,0.25); border-radius: 999px; padding: 0.15rem 0.65rem; white-space: nowrap; }
        .amv-chip i { opacity: 0.85; margin-right: 0.25rem; }

        .amv-progress { background: var(--amv-card); border: 1px solid var(--amv-line); border-radius: 10px; padding: 0.85rem 1rem; margin: 1rem 0 0.75rem;
            display: grid; grid-template-columns: 1fr auto; gap: 0.5rem 1rem; align-items: center; }
        .amv-progress-text { font-weight: 600; }
        .amv-progress-sub { color: var(--amv-muted); font-size: 0.85rem; }
        .amv-bar { grid-column: 1 / -1; height: 8px; background: var(--amv-neutral-bg); border-radius: 999px; overflow: hidden; }
        .amv-bar > span { display: block; height: 100%; background: var(--amv-ok); border-radius: 999px; }
        .amv-filter { display: flex; align-items: center; gap: 0.4rem; font-size: 0.9rem; white-space: nowrap; }
        .amv-filter input { width: 1.05rem; height: 1.05rem; }

        .amv-flash { border-radius: 8px; padding: 0.65rem 0.9rem; margin: 0.75rem 0; font-size: 0.92rem; }
        .amv-flash.ok { background: var(--amv-ok-bg); color: var(--amv-ok); border: 1px solid #b7dcc8; }
        .amv-flash.err { background: var(--amv-warn-bg); color: var(--amv-warn); border: 1px solid #f5d38a; }

        .amv-card { background: var(--amv-card); border: 1px solid var(--amv-line); border-radius: 10px; margin-bottom: 0.7rem; overflow: hidden; }
        .amv-card.just-saved { border-color: var(--amv-ok); box-shadow: 0 0 0 3px var(--amv-ok-bg); }
        .amv-head { width: 100%; text-align: left; background: none; border: none; padding: 0.75rem 1rem; cursor: pointer;
            display: grid; grid-template-columns: auto 1fr auto; gap: 0.6rem 0.8rem; align-items: center; color: var(--amv-ink); }
        .amv-head:hover { background: #f8fafc; }
        .amv-head:focus-visible { outline: 3px solid var(--amv-info-bg); outline-offset: -3px; }
        .amv-head .chev { color: var(--amv-muted); transition: transform 0.15s; }
        .amv-head[aria-expanded="true"] .chev { transform: rotate(90deg); }
        .amv-name { font-size: 1.05rem; font-weight: 700; margin: 0; }
        .amv-gapsum { grid-column: 2; color: var(--amv-muted); font-size: 0.82rem; }
        .amv-chipst { display: inline-block; font-size: 0.74rem; font-weight: 600; border-radius: 999px; padding: 0.15rem 0.6rem; white-space: nowrap; }
        .st-NOT_STARTED { background: var(--amv-neutral-bg); color: var(--amv-neutral); }
        .st-PARTIAL { background: var(--amv-warn-bg); color: var(--amv-warn); }
        .st-COMPLETE, .st-DECLINED { background: var(--amv-ok-bg); color: var(--amv-ok); }
        .st-NEEDS_PSP_CONFIRMATION, .st-SCHEDULE_NOT_APPROVED { background: var(--amv-info-bg); color: var(--amv-info); }
        .st-LOCKED { background: var(--amv-neutral-bg); color: var(--amv-neutral); }

        .amv-body { border-top: 1px solid var(--amv-line); padding: 0.9rem 1rem 1rem; }
        .amv-body[hidden], .amv-card[hidden] { display: none; }
        .amv-gaps { background: var(--amv-warn-bg); border: 1px solid #f5d38a; border-radius: 8px; padding: 0.55rem 0.8rem; margin-bottom: 0.85rem; font-size: 0.88rem; color: var(--amv-warn); }
        .amv-gaps ul { margin: 0.25rem 0 0 1.1rem; padding: 0; }
        .amv-field-label { display: block; font-size: 0.74rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--amv-muted); font-weight: 600; margin-bottom: 0.2rem; }
        .amv-note-hint { background: var(--amv-info-bg); color: var(--amv-info); border-radius: 8px; padding: 0.55rem 0.8rem; font-size: 0.86rem; margin: 0.5rem 0 0.4rem; }
        .amv-legs { border: 1px solid var(--amv-line); border-radius: 8px; margin-top: 0.85rem; }
        .amv-leg { display: grid; grid-template-columns: 1.2fr 1fr auto; gap: 0.35rem 0.9rem; align-items: end; padding: 0.7rem 0.85rem; border-bottom: 1px solid var(--amv-line); }
        .amv-leg:last-child { border-bottom: none; }
        .amv-leg.declined { background: #fafafa; }
        .amv-leg.declined .amv-input { opacity: 0.45; }
        .amv-leg-label { font-weight: 600; }
        .amv-leg-mode { display: block; color: var(--amv-muted); font-size: 0.78rem; }
        .amv-actions { display: flex; align-items: center; gap: 0.75rem; margin-top: 0.9rem; flex-wrap: wrap; }
        .amv-ro { display: grid; grid-template-columns: 1fr auto; gap: 0.2rem 1rem; padding: 0.55rem 0.85rem; border-bottom: 1px solid var(--amv-line); }
        .amv-ro:last-child { border-bottom: none; }
        .amv-ro-val { font-weight: 700; text-align: right; font-variant-numeric: tabular-nums; }
        .amv-ro-val small { font-weight: 500; color: var(--amv-muted); font-size: 0.75rem; }
        .amv-empty { background: var(--amv-card); border: 1px dashed var(--amv-line); border-radius: 10px; padding: 2rem 1rem; text-align: center; color: var(--amv-muted); }
        .amv-foot { color: var(--amv-muted); font-size: 0.78rem; margin: 1.25rem 0 2rem; }
        .amv-hidden-note { color: var(--amv-muted); font-size: 0.85rem; margin: 0.25rem 0 0.9rem; }
        @media (max-width: 640px) {
            .amv-employer { font-size: 1.25rem; }
            .amv-leg { grid-template-columns: 1fr; align-items: start; }
            .amv-progress { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>

<div class="amv-band">
    <div class="amv-wrap">
        <p class="amv-kicker">Enrollment Worksheet</p>
        <h1 class="amv-employer"><c:out value="${empty prospectName ? 'Employer' : prospectName}"/></h1>
        <div class="amv-chips">
            <span class="amv-chip"><i class="bi bi-hash"></i>Setup <c:out value="${setupId}"/></span>
            <c:if test="${not empty planYearStart or not empty planYearEnd}">
                <span class="amv-chip"><i class="bi bi-calendar3"></i>Plan year
                    <c:out value="${planYearStart}"/><c:if test="${not empty planYearStart and not empty planYearEnd}"> &ndash; </c:if><c:out value="${planYearEnd}"/></span>
            </c:if>
            <c:if test="${not empty originatingAgencyName}">
                <span class="amv-chip"><i class="bi bi-buildings"></i><c:out value="${originatingAgencyName}"/></span>
            </c:if>
        </div>
    </div>
</div>

<div class="amv-wrap">

    <c:if test="${not empty agentFlashMessage}">
        <div class="amv-flash ok" role="status"><i class="bi bi-check-circle-fill me-1"></i><c:out value="${agentFlashMessage}"/></div>
    </c:if>
    <c:if test="${not empty agentFlashError}">
        <div class="amv-flash err" role="alert"><i class="bi bi-exclamation-triangle-fill me-1"></i><c:out value="${agentFlashError}"/></div>
    </c:if>

    <c:choose>
        <c:when test="${empty roster}">
            <div class="amv-empty mt-3">No participants have been loaded for this employer yet. Your plan administrator loads the census.</div>
        </c:when>
        <c:otherwise>

            <div class="amv-progress" id="amvProgress" data-done="${assessment.doneCount}">
                <div>
                    <div class="amv-progress-text">${assessment.doneCount} of ${assessment.totalCount} complete</div>
                    <div class="amv-progress-sub">
                        ${legs.size()} plan<c:if test="${legs.size() != 1}">s</c:if> per participant
                        <c:if test="${assessment.needsPspConfirmationCount > 0}">
                            &middot; the administrator will set the payroll schedule for ${assessment.needsPspConfirmationCount}
                        </c:if>
                        <c:if test="${assessment.scheduleNotApprovedCount > 0}">
                            &middot; the administrator needs to update the payroll schedule for ${assessment.scheduleNotApprovedCount}
                        </c:if>
                    </div>
                </div>
                <label class="amv-filter">
                    <input type="checkbox" id="amvOnlyIncomplete"> Show only incomplete
                </label>
                <div class="amv-bar" aria-hidden="true">
                    <span style="width: ${assessment.totalCount == 0 ? 0 : (assessment.doneCount * 100 / assessment.totalCount)}%"></span>
                </div>
            </div>
            <div class="amv-hidden-note" id="amvHiddenNote" hidden></div>

            <c:if test="${empty legs}">
                <div class="amv-flash err"><i class="bi bi-info-circle me-1"></i>No plans are configured for this setup yet, so there is nothing to enter. Your plan administrator sets these up.</div>
            </c:if>

            <c:forEach var="p" items="${roster}" varStatus="vs">
                <c:set var="mp" value="${headersByParticipant[p.id]}"/>
                <c:set var="pa" value="${assessment.byParticipantId[p.id]}"/>
                <c:set var="isLocked" value="${not empty mp and mp.entryLocked}"/>
                <c:set var="justSaved" value="${not empty agentSavedParticipantId and agentSavedParticipantId == p.id}"/>
                <c:set var="startOpen" value="${(not empty openId and openId == p.id)}"/>
                <c:set var="selectedFreq" value="${not empty mp ? mp.payrollFrequency : ''}"/>
                <c:set var="isSentinel" value="${selectedFreq eq otherCustom or selectedFreq eq 'OTHER_NOT_IMPORTABLE'}"/>

                <div class="amv-card${justSaved ? ' just-saved' : ''}" id="card-${p.id}"
                     data-done="${pa.done}" data-keep="${justSaved}">
                    <button type="button" class="amv-head" id="head-${p.id}" aria-expanded="${startOpen}"
                            aria-controls="body-${p.id}" onclick="amvToggle(${p.id})">
                        <i class="bi bi-chevron-right chev"></i>
                        <span class="amv-name"><c:out value="${p.lastName}"/>, <c:out value="${p.firstName}"/></span>
                        <span class="amv-chipst st-${pa.statusCode}">
                            <c:if test="${pa.statusCode eq 'LOCKED'}"><i class="bi bi-lock-fill"></i> </c:if><c:out value="${pa.statusLabel}"/>
                        </span>
                        <span class="amv-gapsum">
                            <c:choose>
                                <c:when test="${justSaved}"><i class="bi bi-check-circle-fill" style="color:var(--amv-ok)"></i> Saved just now</c:when>
                                <c:when test="${pa.statusCode eq 'PARTIAL' or pa.statusCode eq 'NOT_STARTED'}">${pa.gaps.size()} field<c:if test="${pa.gaps.size() != 1}">s</c:if> still needed</c:when>
                                <c:when test="${pa.statusCode eq 'NEEDS_PSP_CONFIRMATION'}">Elections entered &middot; the administrator will set the payroll schedule</c:when>
                                <c:when test="${pa.statusCode eq 'SCHEDULE_NOT_APPROVED'}">Elections entered &middot; the administrator needs to update the payroll schedule</c:when>
                                <c:when test="${pa.statusCode eq 'LOCKED'}">Locked<c:if test="${not empty mp.lockedBy}"> by <c:out value="${mp.lockedBy}"/></c:if></c:when>
                                <c:when test="${pa.statusCode eq 'DECLINED'}">All plans declined</c:when>
                                <c:otherwise>All plans entered</c:otherwise>
                            </c:choose>
                        </span>
                    </button>

                    <div class="amv-body" id="body-${p.id}" ${startOpen ? '' : 'hidden'}>
                        <c:choose>
                            <%-- ── Locked: read-only with the reason ─────────────────────────── --%>
                            <c:when test="${isLocked}">
                                <div class="amv-gaps" style="background:var(--amv-neutral-bg); border-color:#d1d5db; color:var(--amv-neutral);">
                                    <i class="bi bi-lock-fill me-1"></i>This participant was locked
                                    <c:if test="${not empty mp.lockedBy}"> by <c:out value="${mp.lockedBy}"/></c:if>
                                    <c:if test="${not empty mp.lockedAt}"> on <c:out value="${mp.lockedAt}"/></c:if>.
                                    Their elections can't be changed here &mdash; contact your plan administrator if something is wrong.
                                </div>
                                <div class="amv-legs">
                                    <div class="amv-ro">
                                        <span>Payroll frequency</span>
                                        <span class="amv-ro-val"><c:out value="${empty selectedFreq ? 'not set' : (empty payrollFrequencyOptions[selectedFreq] ? selectedFreq : payrollFrequencyOptions[selectedFreq])}"/></span>
                                    </div>
                                    <c:forEach var="leg" items="${legs}">
                                        <c:set var="entryKey" value="${mp.id}_${leg.id}"/>
                                        <c:set var="entry" value="${entriesByParticipantAndLeg[entryKey]}"/>
                                        <div class="amv-ro">
                                            <span><c:out value="${empty leg.label ? leg.keySegment : leg.label}"/></span>
                                            <span class="amv-ro-val">
                                                <c:choose>
                                                    <c:when test="${not empty entry and entry.declined}">Declined</c:when>
                                                    <c:when test="${leg.enrollmentAmountMode eq 'TIER'}"><c:out value="${empty entry or empty entry.tierName ? '—' : (empty coverageTierOptions[entry.tierName] ? entry.tierName : coverageTierOptions[entry.tierName])}"/></c:when>
                                                    <c:when test="${not empty entry and not empty entry.amount}"><fmt:formatNumber value="${entry.amount}" type="currency" currencySymbol="$" minFractionDigits="2" maxFractionDigits="2"/>
                                                        <small>${leg.enrollmentAmountMode eq 'MONTHLY_PREMIUM' ? '/ month' : '/ year'}</small></c:when>
                                                    <c:otherwise>&mdash;</c:otherwise>
                                                </c:choose>
                                            </span>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:when>

                            <%-- ── Editable ───────────────────────────────────────────────────── --%>
                            <c:otherwise>
                                <%-- S58-P9: stored schedule is no longer one that can be chosen. Not the agent's doing --
                                     they may leave it as is (it stays selected) or pick a listed schedule if they know it. --%>
                                <c:if test="${pa.scheduleNotApproved}">
                                    <div class="amv-note-hint">
                                        <i class="bi bi-info-circle-fill me-1"></i><strong>The payroll schedule on file for this participant needs updating by your plan administrator.</strong>
                                        You can leave it as it is, or choose the correct schedule from the list if you know it.
                                    </div>
                                </c:if>
                                <c:if test="${not empty pa.gaps}">
                                    <div class="amv-gaps">
                                        <strong>Still needed:</strong>
                                        <ul>
                                            <c:forEach var="g" items="${pa.gaps}"><li><c:out value="${g.message}"/></li></c:forEach>
                                        </ul>
                                    </div>
                                </c:if>

                                <form method="post" action="${ctx}/EnrollmentMatrix" id="form-${p.id}" onsubmit="amvSubmitting(${p.id})">
                                    <input type="hidden" name="action" value="agentSave">
                                    <input type="hidden" name="setupId" value="${setupId}">
                                    <input type="hidden" name="participantId" value="${p.id}">

                                    <div class="row g-2">
                                        <div class="col-md-5">
                                            <label class="amv-field-label" for="payrollFrequency_${p.id}">Payroll frequency</label>
                                            <select class="form-select" id="payrollFrequency_${p.id}" name="payrollFrequency_${p.id}"
                                                    onchange="amvFreqChanged(${p.id})">
                                                <option value="" ${empty selectedFreq ? 'selected' : ''}>&mdash; choose &mdash;</option>
                                                <c:if test="${not empty suggestedPayrollFrequencies}">
                                                    <optgroup label="Suggested from the application">
                                                        <c:forEach var="opt" items="${suggestedPayrollFrequencies}">
                                                            <option value="${opt.key}" ${selectedFreq eq opt.key ? 'selected' : ''}><c:out value="${opt.value}"/></option>
                                                        </c:forEach>
                                                    </optgroup>
                                                    <optgroup label="All schedules">
                                                </c:if>
                                                <c:forEach var="opt" items="${payrollFrequencyOptions}">
                                                    <%-- S58-P9: the shared map carries every inactive code stored anywhere in this
                                                         matrix (the PSP page relies on that). Here an inactive code is offered only
                                                         in the select of the participant already holding it -- it stays selected
                                                         and is never rewritten on save, but nobody can pick it fresh. --%>
                                                    <c:set var="optIsSentinel" value="${opt.key eq otherCustom or opt.key eq 'OTHER_NOT_IMPORTABLE'}"/>
                                                    <c:set var="optIsInactive" value="${not optIsSentinel and not approvedPayrollCodes.contains(opt.key)}"/>
                                                    <c:if test="${not suggestedPayrollFrequencies.containsKey(opt.key) and (not optIsInactive or selectedFreq eq opt.key)}">
                                                        <option value="${opt.key}" ${selectedFreq eq opt.key ? 'selected' : ''}
                                                                ${optIsSentinel ? 'data-sentinel="true"' : ''}>
                                                            <%-- S58-P8: the shared option map labels the two sentinels in internal
                                                                 vocabulary (the PSP page needs that); agents get plain wording. --%>
                                                            <c:choose>
                                                                <c:when test="${opt.key eq otherCustom}">Other &mdash; schedule not listed (administrator will set it up)</c:when>
                                                                <c:when test="${opt.key eq 'OTHER_NOT_IMPORTABLE'}">Other &mdash; unusual schedule (administrator will handle)</c:when>
                                                                <c:when test="${optIsInactive}"><c:out value="${opt.key}"/> (administrator will update)</c:when>
                                                                <c:otherwise><c:out value="${opt.value}"/></c:otherwise>
                                                            </c:choose>
                                                        </option>
                                                    </c:if>
                                                </c:forEach>
                                                <c:if test="${not empty suggestedPayrollFrequencies}">
                                                    </optgroup>
                                                </c:if>
                                            </select>
                                            <c:if test="${not empty suggestedPayrollFrequency and empty selectedFreq}">
                                                <div class="form-text">Suggested: <c:out value="${suggestedPayrollFrequencies[suggestedPayrollFrequency]}"/></div>
                                            </c:if>
                                        </div>
                                        <div class="col-md-7" id="noteWrap_${p.id}" ${isSentinel or (not empty mp and not empty mp.agentScheduleNote) ? '' : 'hidden'}>
                                            <div class="amv-note-hint" id="noteHint_${p.id}" ${isSentinel ? '' : 'hidden'}>
                                                <i class="bi bi-info-circle-fill me-1"></i><strong>Good choice &mdash; your plan administrator will set up the actual payroll schedule.</strong>
                                                Use the note to tell them what you learned &mdash; pay day, how often, first pay date, anything unusual.
                                            </div>
                                            <label class="amv-field-label" for="agentScheduleNote_${p.id}">Note to the administrator about this payroll schedule (optional)</label>
                                            <textarea class="form-control" id="agentScheduleNote_${p.id}" name="agentScheduleNote_${p.id}"
                                                      rows="2" maxlength="500" placeholder="e.g. Paid every other Friday, first check Jan 9"><c:out value="${not empty mp ? mp.agentScheduleNote : ''}"/></textarea>
                                        </div>
                                    </div>

                                    <c:if test="${not empty legs}">
                                        <div class="amv-legs">
                                            <c:forEach var="leg" items="${legs}">
                                                <c:set var="entryKey" value="${mp.id}_${leg.id}"/>
                                                <c:set var="entry" value="${not empty mp ? entriesByParticipantAndLeg[entryKey] : null}"/>
                                                <c:set var="isDeclined" value="${not empty entry and entry.declined}"/>
                                                <div class="amv-leg${isDeclined ? ' declined' : ''}" id="leg_${p.id}_${leg.id}">
                                                    <div>
                                                        <span class="amv-leg-label"><c:out value="${empty leg.label ? leg.keySegment : leg.label}"/></span>
                                                        <span class="amv-leg-mode">
                                                            <c:choose>
                                                                <c:when test="${leg.enrollmentAmountMode eq 'MONTHLY_PREMIUM'}">Monthly premium</c:when>
                                                                <c:when test="${leg.enrollmentAmountMode eq 'ANNUAL_ELECTION'}">Annual election</c:when>
                                                                <c:when test="${leg.enrollmentAmountMode eq 'TIER'}">Coverage level</c:when>
                                                                <c:otherwise>Plan election</c:otherwise>
                                                            </c:choose>
                                                        </span>
                                                    </div>
                                                    <div class="amv-input">
                                                        <c:choose>
                                                            <c:when test="${leg.enrollmentAmountMode eq 'TIER'}">
                                                                <label class="amv-field-label" for="tier_${p.id}_${leg.id}">Coverage level</label>
                                                                <select class="form-select" id="tier_${p.id}_${leg.id}" name="tier_${p.id}_${leg.id}">
                                                                    <option value="" ${empty entry or empty entry.tierName ? 'selected' : ''}>&mdash; choose &mdash;</option>
                                                                    <c:forEach var="opt" items="${coverageTierOptions}">
                                                                        <option value="${opt.key}" ${not empty entry and entry.tierName eq opt.key ? 'selected' : ''}><c:out value="${opt.value}"/></option>
                                                                    </c:forEach>
                                                                </select>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <label class="amv-field-label" for="amount_${p.id}_${leg.id}">
                                                                    ${leg.enrollmentAmountMode eq 'MONTHLY_PREMIUM' ? 'Amount per month' : 'Amount per year'}
                                                                </label>
                                                                <div class="input-group">
                                                                    <span class="input-group-text">$</span>
                                                                    <input type="number" step="0.01" min="0" inputmode="decimal" class="form-control"
                                                                           id="amount_${p.id}_${leg.id}" name="amount_${p.id}_${leg.id}"
                                                                           value="${not empty entry and not empty entry.amount ? entry.amount : ''}">
                                                                </div>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                    <div class="form-check pb-1">
                                                        <input class="form-check-input" type="checkbox" id="declined_${p.id}_${leg.id}"
                                                               name="declined_${p.id}_${leg.id}" ${isDeclined ? 'checked' : ''}
                                                               onchange="amvDeclinedChanged(${p.id}, ${leg.id})">
                                                        <label class="form-check-label" for="declined_${p.id}_${leg.id}">Declined</label>
                                                    </div>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:if>

                                    <div class="amv-actions">
                                        <button type="submit" class="btn btn-ssa" id="save-${p.id}">
                                            <i class="bi bi-check2 me-1"></i>Save <c:out value="${p.firstName}"/>
                                        </button>
                                        <span class="text-muted" style="font-size:0.85rem;">Saves this participant only.</span>
                                    </div>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </c:forEach>
        </c:otherwise>
    </c:choose>

    <p class="amv-foot">
        <i class="bi bi-info-circle me-1"></i>Loaded <fmt:formatDate value="${now}" pattern="MMM d, yyyy h:mm a"/>.
        Each participant saves separately, so you can come back and pick up where you left off.
    </p>
</div>

<script>
    // Accordion -- plain show/hide, no focus trapping; the button keeps focus after a toggle.
    function amvToggle(pid) {
        var head = document.getElementById('head-' + pid);
        var body = document.getElementById('body-' + pid);
        if (!head || !body) return;
        var open = head.getAttribute('aria-expanded') === 'true';
        head.setAttribute('aria-expanded', open ? 'false' : 'true');
        body.hidden = open;
    }

    // OTHER_* selected -> show the PSP hint and the note. The note stays visible once it holds text.
    function amvFreqChanged(pid) {
        var sel = document.getElementById('payrollFrequency_' + pid);
        var wrap = document.getElementById('noteWrap_' + pid);
        var hint = document.getElementById('noteHint_' + pid);
        var note = document.getElementById('agentScheduleNote_' + pid);
        if (!sel || !wrap) return;
        var opt = sel.options[sel.selectedIndex];
        var sentinel = !!(opt && opt.dataset && opt.dataset.sentinel);
        if (hint) hint.hidden = !sentinel;
        wrap.hidden = !(sentinel || (note && note.value.trim().length > 0));
    }

    function amvDeclinedChanged(pid, legId) {
        var row = document.getElementById('leg_' + pid + '_' + legId);
        var box = document.getElementById('declined_' + pid + '_' + legId);
        if (row && box) row.classList.toggle('declined', box.checked);
    }

    function amvSubmitting(pid) {
        var btn = document.getElementById('save-' + pid);
        if (btn) { btn.disabled = true; btn.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Saving…'; }
        return true;
    }

    // "Show only incomplete": on by default once any participant is done; an explicit choice is
    // remembered per browser. A just-saved participant is always kept visible so the confirmation
    // is seen even when it became complete.
    (function () {
        var box = document.getElementById('amvOnlyIncomplete');
        var progress = document.getElementById('amvProgress');
        var note = document.getElementById('amvHiddenNote');
        if (!box || !progress) return;
        var key = 'amvOnlyIncomplete:' + window.location.pathname;
        var stored = null;
        try { stored = window.localStorage.getItem(key); } catch (e) { stored = null; }
        var anyDone = parseInt(progress.getAttribute('data-done') || '0', 10) > 0;
        box.checked = stored === null ? anyDone : stored === '1';

        // S58-P8 -- "keep the just-saved participant visible" applies to the render right after
        // a save only. Once the person clicks the box, honour it fully: with one saved
        // participant and the keep override live, a click hid nothing and looked broken.
        // The note line is always shown while the filter is on, even when it hides nothing, so
        // every click has a visible result.
        function apply(honourKeep) {
            var hidden = 0, doneCount = 0;
            document.querySelectorAll('.amv-card').forEach(function (card) {
                var done = card.getAttribute('data-done') === 'true';
                var keep = honourKeep && card.getAttribute('data-keep') === 'true';
                if (done) doneCount++;
                var hide = box.checked && done && !keep;
                card.hidden = hide;
                if (hide) hidden++;
            });
            if (note) {
                note.hidden = !box.checked;
                if (hidden > 0) {
                    note.textContent = hidden + ' completed participant' + (hidden === 1 ? '' : 's') + ' hidden — untick "Show only incomplete" to see them.';
                } else if (doneCount > 0) {
                    note.textContent = 'Showing everyone — the completed participant you just saved stays visible so you can see it landed.';
                } else {
                    note.textContent = 'Nothing to hide yet — no participant is complete.';
                }
            }
        }
        box.addEventListener('change', function () {
            try { window.localStorage.setItem(key, box.checked ? '1' : '0'); } catch (e) {}
            apply(false);
        });
        apply(true);

        // Nothing explicitly opened -> open the first participant still needing work.
        var anyOpen = document.querySelector('.amv-head[aria-expanded="true"]');
        if (!anyOpen) {
            var first = document.querySelector('.amv-card:not([hidden])[data-done="false"] .amv-head');
            if (first) amvToggle(first.id.replace('head-', ''));
        }
    })();
</script>
</body>
</html>
