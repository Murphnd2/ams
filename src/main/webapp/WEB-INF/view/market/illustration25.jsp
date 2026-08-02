<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>ICHRA Illustration</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        /* T78: `height: calc(100vh - 64px)` fought mobile browser chrome — 100vh on a
           phone counts the address bar that then retracts, so the inner scroll region was
           always slightly wrong and the page scrolled inside a box inside a box.
           min-height on the desktop breakpoint keeps the toolbar-plus-scroll-body layout
           the design relies on; on narrow screens the page simply scrolls normally, which
           is what a phone expects. */
        .illustration-wrap {
            display: flex; flex-direction: column;
        }
        @media (min-width: 768px) {
            .illustration-wrap { height: calc(100vh - 64px); }
        }
        /* Wide result tables must be reachable on a narrow screen. Nothing in the ICHRA
           path had an overflow wrapper, so a five-column table simply ran off the side. */
        .table-responsive { overflow-x: auto; -webkit-overflow-scrolling: touch; }
        /* T78: the form's fixed pixel widths. Kept as a comfortable minimum rather than a
           fixed size, so a phone can shrink them and a desktop still gets the same look. */
        .illustration-wrap .form-control, .illustration-wrap .form-select { min-width: 0; }
        .toolbar {
            padding: 0.65rem 1rem;
            background: #fff; border-bottom: 1px solid #dee2e6;
            display: flex; align-items: center; gap: 0.75rem;
        }
        .toolbar .t-title {
            font-weight: 700; color: var(--ssa, #0d5681); font-size: 0.95rem; margin: 0;
        }
        .illustration-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .status-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem;
        }
        .empty-state {
            text-align: center; padding: 2.5rem 1rem; color: #6c757d; font-size: 0.88rem;
            background: #fff; border: 1px dashed #dee2e6; border-radius: 6px;
        }
        .disclaimer {
            background: #fff3cd; border: 1px solid #ffe69c; border-radius: 6px;
            padding: 0.75rem 1rem; margin-bottom: 0.9rem; font-size: 0.85rem; color: #664d03;
        }
        .results-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; background: #fff; }
        .results-table th {
            background: #f8f9fa; text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .results-table td { padding: 0.55rem 0.75rem; border-bottom: 1px solid #eee; }
        .results-table tr.headline td { font-weight: 700; color: #0d5681; font-size: 0.95rem; }
        .footnote { font-size: 0.75rem; color: #6c757d; margin-top: 0.35rem; }
        .quiet-note { font-size: 0.78rem; color: #6c757d; margin-top: 0.5rem; }
        .meta-line { font-size: 0.8rem; color: #495057; margin-top: 0.75rem; }

        /* W11: the disabled "Use This in a Proposal" control still answered hover, which
           is the universal signal for "this works" — an agent clicks it, nothing happens,
           and they conclude the page is broken rather than the feature gated. Kill every
           hover affordance and say "not available" with the cursor instead. */
        .ssa-action:disabled,
        .ssa-action[disabled] {
            cursor: not-allowed;
            opacity: 0.55;
        }
        /* ── W10: make the slider read as a control ──────────────────────────────
           The agent who watched this get built looked straight past it: "I didn't get
           the impression that the page was anything other than a static result." The
           cause is visible in the markup — it used `.status-card`, byte-identical to
           the read-only result panels above and below it. It looked like output.

           So it stops looking like output: its own tint and accent edge, a filled
           track, a handle big enough to read as a grab target, and ticks marking each
           band's flip contribution.

           ⚠️ Every colour here is neutral or the existing SSA blue. Nothing marks a
           region as good, better or recommended — no green below a threshold, no red
           above, no highlighted "optimal" band. The ticks are computed thresholds
           stated as facts, and the no-steering boundary is the reason they look
           identical to each other. */
        .contrib-card {
            background: #f7fbfd;
            border: 1px solid #bcd9e8;
            border-left: 3px solid var(--ssa, #0d5681);
        }
        .contrib-slider-wrap { position: relative; padding-bottom: 1.1rem; }
        .contrib-slider-wrap input[type=range] {
            -webkit-appearance: none; appearance: none;
            width: 100%; height: 1.4rem; background: transparent; cursor: grab; margin: 0;
        }
        .contrib-slider-wrap input[type=range]:active { cursor: grabbing; }
        /* Filled portion behind the handle: position indicator, not a preference. */
        .contrib-slider-wrap input[type=range]::-webkit-slider-runnable-track {
            height: 8px; border-radius: 4px; border: 1px solid #b7c7d1;
            background: linear-gradient(to right,
                var(--ssa, #0d5681) 0%, var(--ssa, #0d5681) var(--fill, 0%),
                #e7edf1 var(--fill, 0%), #e7edf1 100%);
        }
        .contrib-slider-wrap input[type=range]::-moz-range-track {
            height: 8px; border-radius: 4px; border: 1px solid #b7c7d1; background: #e7edf1;
        }
        .contrib-slider-wrap input[type=range]::-moz-range-progress {
            height: 8px; border-radius: 4px; background: var(--ssa, #0d5681);
        }
        /* 22px handle — a deliberate touch target, and the one thing that makes it read
           as draggable at a glance. Sized for a finger; the rest of T78 is not this run. */
        .contrib-slider-wrap input[type=range]::-webkit-slider-thumb {
            -webkit-appearance: none; appearance: none;
            width: 22px; height: 22px; margin-top: -8px;
            border-radius: 50%; background: #fff;
            border: 3px solid var(--ssa, #0d5681);
            box-shadow: 0 1px 3px rgba(0,0,0,0.28);
        }
        .contrib-slider-wrap input[type=range]::-moz-range-thumb {
            width: 22px; height: 22px; border-radius: 50%; background: #fff;
            border: 3px solid var(--ssa, #0d5681); box-shadow: 0 1px 3px rgba(0,0,0,0.28);
        }
        .contrib-slider-wrap input[type=range]:focus { outline: none; }
        .contrib-slider-wrap input[type=range]:focus::-webkit-slider-thumb,
        .contrib-slider-wrap input[type=range]:hover::-webkit-slider-thumb {
            box-shadow: 0 0 0 5px rgba(13,86,129,0.18), 0 1px 3px rgba(0,0,0,0.28);
        }
        .contrib-slider-wrap input[type=range]:focus::-moz-range-thumb,
        .contrib-slider-wrap input[type=range]:hover::-moz-range-thumb {
            box-shadow: 0 0 0 5px rgba(13,86,129,0.18), 0 1px 3px rgba(0,0,0,0.28);
        }
        /* Flip-point ticks. Grey, identical, unlabelled as good or bad. */
        .contrib-ticks { position: absolute; left: 0; right: 0; top: 1.35rem; height: 1rem; }
        .contrib-tick { position: absolute; transform: translateX(-50%); text-align: center; }
        .contrib-tick i {
            display: block; width: 1px; height: 6px; background: #8a99a4; margin: 0 auto;
        }
        .contrib-tick span { font-size: 0.62rem; color: #6c757d; white-space: nowrap; }
        /* T78: on a narrow track the "age N" labels collide into an unreadable smear.
           Degrade the TICKS, never the handle — the marks stay (they are what makes the
           flip points visible before anything is dragged), the labels drop, and the
           affordability table below still carries every exact figure. */
        @media (max-width: 575.98px) {
            .contrib-tick span { display: none; }
            .contrib-slider-wrap { padding-bottom: 0.6rem; }
        }
        /* M2: full-width track on a phone, value underneath. The slider is the demo
           control and it should get the whole width of the device it is demoed on. */
        @media (max-width: 767.98px) {
            .contrib-controls { flex-direction: column; align-items: stretch !important; gap: 0.4rem !important; }
            .contrib-controls > div[id="contribReadout"] { text-align: left !important; min-width: 0 !important; }
        }
        /* M4: the input and result cards sat inset while the hub's cards ran edge to edge.
           Reclaim the horizontal space on a phone — it is the axis in shortest supply. */
        @media (max-width: 767.98px) {
            .illustration-body { padding-left: 0.5rem; padding-right: 0.5rem; }
            .status-card { padding-left: 0.7rem; padding-right: 0.7rem; }
        }

        .ssa-action:disabled:hover,
        .ssa-action[disabled]:hover {
            cursor: not-allowed;
            opacity: 0.55;
            box-shadow: none !important;
            transform: none !important;
            filter: none !important;
            text-decoration: none !important;
        }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="illustration-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0"><i class="bi bi-calculator me-1"></i>ICHRA Illustration</h1>
        <%-- ⚠️ The Range / Age Band toggle stood here and is gone.

             It presented two modes as a choice between tools. They were never two tools:
             both are this page, and the only difference is whether the agent has age bands
             to enter. Kevin, 2026-08-01: "steps 1, 2 and 3 are really 3 versions of the
             same thing — the only difference is level of detail available." Pre-sale this
             is done ONCE, at whatever detail is to hand, so the form now grows instead of
             switching.

             It also carried a live bug the moment it existed (W13-R): it dropped the ZIP,
             and after prompt J's edits it dropped the county too. With no toggle there is
             no toggle to lose state across — the fix is structural rather than another
             parameter added to a link.

             G5's carry logic died with it. That is the point: there is nothing to carry
             between, because there is nowhere else to be.

             ⚠️ `mode=RANGE` and `mode=AGE_BAND` remain fully supported URL parameters and
             are honoured verbatim — the hub cards and every existing link still land
             exactly where they did. See IllustrationServlet's mode block. --%>
    </div>

    <div class="illustration-body">

        <c:choose>
            <c:when test="${empty configuredPlanYears}">
                <div class="empty-state">
                    <i class="bi bi-graph-up"></i>
                    <div style="font-size:0.85rem; margin-top:0.5rem;">Rate cache is not configured on this installation.</div>
                </div>
            </c:when>
            <c:otherwise>

                <c:if test="${not empty inputError}">
                    <div class="alert alert-danger py-2" style="font-size:0.85rem;" role="alert">
                        <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${inputError}"/>
                    </div>
                </c:if>

                <%-- W14 — collapse the inputs once they have done their job.

                     Observed: "top section is still taking up a lot of space displaying
                     the entry data." Fifteen age fields plus three banners pushed the
                     slider and every figure below the fold — and this page is read on a
                     phone, on a desk, with an employer watching. A correct answer nobody
                     scrolls to is a correct answer nobody reads. Same species as W10.

                     ⚠️ The three banners are NOT part of this. They are compliance text:
                     not moved, not restyled, not consolidated, not shortened, not hidden,
                     and the staging banner still renders above the results without
                     scrolling. Recovering the form's height is the entire win here.

                     Collapse is presentation only. The form stays in the DOM with every
                     value intact, so expanding re-shows it exactly as it was — no
                     re-submit, no reload, nothing cleared. With JavaScript off the summary
                     never renders and the form is simply always open, which is today's
                     behaviour. --%>
                <%-- S7-D / T103 second cut: `illustrationLanding` is the flag that tells a
                     landing (no ageN supplied, nothing asked) apart from a result that came
                     back empty (ages supplied, cache had nothing) — both reach here with an
                     empty row set and no other distinguishing attribute. Set by the servlet
                     only on that specific path; absent (falsy) everywhere else. --%>
                <c:set var="hasResult" value="${not empty selectedCounty and empty inputError and not illustrationLanding}"/>
                <div class="status-card" id="inputSummary" ${hasResult ? '' : 'style="display:none;"'}>
                    <div class="d-flex align-items-center flex-wrap gap-2">
                        <span><i class="bi bi-sliders2 me-1"></i><strong>Inputs</strong></span>
                        <span class="text-muted" id="inputSummaryText"></span>
                        <button type="button" class="btn btn-sm btn-outline-secondary ms-auto" id="inputSummaryEdit">Edit</button>
                    </div>
                </div>

                <div class="status-card" id="inputCard" ${hasResult ? 'style="display:none;"' : ''}>
                    <form method="get" action="Illustration" class="row gy-2 gx-3 align-items-end">
                        <%-- ⚠️ The hidden `mode` field is deliberately GONE. Sending it would
                             pin the form to whichever tier it opened in, so adding the first
                             age band would submit and come back as a range. The servlet
                             derives mode from whether any ageN is non-blank when no explicit
                             mode is present — which is exactly this form — and honours the
                             parameter verbatim when a link supplies one. --%>
                        <%-- Item 13: carry the opportunity attribution across this form's own
                             re-submissions. Already resolved and scope-checked server-side;
                             absent entirely when there is none. Not a picker — no UI. --%>
                        <c:if test="${not empty opportunityId}">
                            <input type="hidden" name="opportunityId" value="${opportunityId}">
                        </c:if>
                        <%-- T74 ZIP intake. The agent has the employer's ZIP, not its county FIPS.

                             ⚠️ The note that stood here was wrong and caused R1. It read "the
                             servlet consults this ONLY when countyFips is absent, so the county
                             selector below still wins" — which also meant a stale selection beat
                             a freshly typed ZIP, and produced another county's rates with no
                             warning. The rule now: a blank ZIP leaves the county contract
                             untouched, and a present ZIP is always resolved, with a county it
                             contradicts never computed from. See IllustrationServlet.

                             The county selector deliberately STAYS. The crosswalk is
                             ZCTA-derived and Texas-only, so some valid ZIPs do not resolve
                             and the agent needs a way through; it is also what every
                             existing link uses; and keeping it makes this whole change
                             reversible by deleting the ZIP block. --%>
                        <div class="col-auto">
                            <label class="form-label mb-1" for="zip">ZIP</label>
                            <%-- W3: the placeholder was "75482", a real Hopkins ZIP, which read
                                 as a value already entered. Five hashes cannot be mistaken for
                                 one. W4: the resolved-county confirmation that used to sit here
                                 is gone — it repeated what the dropdown two inches to the right
                                 already said, and adding a line inside an `align-items: end` row
                                 knocked the row out of alignment every time a ZIP resolved. --%>
                            <input type="text" class="form-control form-control-sm" id="zip" name="zip"
                                   inputmode="numeric" pattern="[0-9]{5}" maxlength="5" placeholder="#####"
                                   value="${submittedZip}" style="max-width:110px;">
                        </div>

                        <div class="col-auto">
                            <label class="form-label mb-1" for="countyFips">County</label>
                            <select class="form-select form-select-sm" id="countyFips" name="countyFips" ${empty availableCounties ? 'disabled' : ''}>
                                <option value="">-- Select a county --</option>
                                <c:forEach var="county" items="${availableCounties}">
                                    <option value="${county.countyFips}" ${county.countyFips == submittedCountyFips ? 'selected' : ''}>
                                        <c:out value="${county.countyName}"/>, <c:out value="${county.state}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>

                        <c:choose>
                            <c:when test="${fn:length(configuredPlanYears) > 1}">
                                <div class="col-auto">
                                    <label class="form-label mb-1" for="planYear">Plan Year</label>
                                    <select class="form-select form-select-sm" id="planYear" name="planYear">
                                        <c:forEach var="y" items="${configuredPlanYears}">
                                            <option value="${y}" ${y == selectedPlanYear ? 'selected' : ''}>${y}</option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <input type="hidden" name="planYear" value="${selectedPlanYear}">
                            </c:otherwise>
                        </c:choose>

                        <%-- ── One progressive form ─────────────────────────────────────
                             Everything below is rendered together. What the agent supplies
                             decides the output; nothing here is a mode.

                               headcount only          -> premium range
                               + age bands             -> per-employee net cost by band
                               + contribution          -> slider, group net, employer outlay
                               + affordability basis   -> flip points and verdicts

                             ⚠️ Still a GET form with a submit. "Progressive" means which
                             inputs show and which output sections render — NOT live
                             recomputation. The slider remains the only live control. --%>

                        <%-- Eligible Employees is the tier-1 input, and it stops being the
                             input the moment an age band exists — the bands carry their own
                             counts and their sum is the headcount. Hidden rather than
                             removed so nothing is lost switching back, and the servlet
                             ignores it in AGE_BAND regardless. --%>
                        <div class="col-auto" id="headcountField" ${mode == 'AGE_BAND' ? 'style="display:none;"' : ''}>
                            <label class="form-label mb-1" for="headcount">Eligible Employees</label>
                            <input type="number" class="form-control form-control-sm" id="headcount" name="headcount"
                                   min="1" max="10000" value="${submittedHeadcount}" style="max-width:150px;">
                        </div>

                                <%-- W7 — the age-band repeater. Was five fixed triplets edge to edge:
                                     "somewhat hard to read and blended together left to right", and
                                     fifteen inputs for a case that usually needs three.

                                     ⚠️ THE QUERY-PARAMETER CONTRACT IS UNCHANGED. Rows are still
                                     age1..ageN / count1..countN / income1..incomeN, contiguous from
                                     1. The rows are rendered rather than hardcoded and the script
                                     renumbers after a removal, precisely so the names stay what
                                     they were — this servlet is GET-only, its URLs carry state, and
                                     the mode toggle, the hub cards, the proposal hand-off and every
                                     verified link depend on those names.

                                     ⚠️ The cap stays at ${ageBandMaxRows} and stays server-side. It is NOT an
                                     arbitrary markup limit: proposalBuilder.jsp echoes exactly six
                                     age/count pairs into the proposal POST, and that file is out of
                                     this run's fence. Raising the limit here without raising it
                                     there would silently drop rows 7+ from every proposal snapshot.
                                     Logged rather than risked.

                                     W8 — income is rendered only when the selected basis consumes
                                     it. Verified in source: IllustrationServlet:306 parses income
                                     only when the basis is INCOME, and :475 uses the configured
                                     FPL_ANNUAL_<year> constant for FPL. So FPL and None never read
                                     it, and FPL is the basis an agent uses when he has ages and no
                                     wages — the common tier-1/tier-2 case. Driven off the basis
                                     rather than a bare toggle, so the relationship is visible. --%>
                                <div class="col-12">
                                    <label class="form-label mb-1 d-block">Ages and Headcounts
                                        <span class="text-muted fw-normal" style="font-size:0.78rem;">&mdash; optional; adding one replaces Eligible Employees</span>
                                    </label>
                                    <%-- The form can now open with ZERO bands, so "+ Add age band"
                                         has nothing to clone from. This is that source. It carries
                                         `age-band-template`, NOT `age-band-row`, so neither the
                                         repeater's row list nor the collapsed-summary counter sees
                                         it; the class is swapped on clone. Its inputs are unnamed
                                         so it can never submit anything.

                                         ⚠️ S7-A — no `d-flex` here. Bootstrap's `.d-flex` is
                                         `!important` and beat this element's inline
                                         `display:none`, so the "hidden" template rendered
                                         visible on every load: a permanent blank row with no
                                         name attribute on its inputs (view-source only ever
                                         showed `age1` inside this comment). `clone.className`
                                         fully replaces this list on use (search this file for
                                         it), so `d-flex` was never doing anything for a live
                                         row either -- it is safe to leave out. --%>
                                    <div class="age-band-template align-items-end gap-2 mb-2" id="ageBandTemplate" style="display:none;" aria-hidden="true">
                                        <div>
                                            <label class="form-label mb-1" style="font-size:0.7rem;">Age</label>
                                            <input type="number" class="form-control form-control-sm age-band-age" min="21" max="64">
                                        </div>
                                        <div>
                                            <label class="form-label mb-1" style="font-size:0.7rem;">Count</label>
                                            <input type="number" class="form-control form-control-sm age-band-count" min="1" value="1">
                                        </div>
                                        <div class="age-band-income" style="display:none;">
                                            <label class="form-label mb-1" style="font-size:0.7rem;">Income</label>
                                            <input type="number" step="1" class="form-control form-control-sm" min="1" placeholder="$/yr">
                                        </div>
                                        <button type="button" class="btn btn-sm btn-outline-secondary age-band-remove"
                                                aria-label="Remove this age band">&times;</button>
                                    </div>

                                    <div id="ageBandRows">
                                        <c:forEach begin="1" end="${ageBandMaxRows}" var="i">
                                            <%-- ⚠️ K3-b — THE DUPLICATED BAND, and it was this line.
                                                 It read `i == 1 and mode == 'AGE_BAND'`, which
                                                 force-rendered a blank row 1 whenever the mode was
                                                 AGE_BAND — and since prompt K made mode DERIVED
                                                 from "any age band present", that fired on every
                                                 submit that had a band anywhere. Leave row 1 blank,
                                                 put 40 in row 2, submit: the server rendered the
                                                 forced blank row 1 AND the real row 2. Two bands on
                                                 screen where the agent entered one, and a headcount
                                                 nothing on screen explained.

                                                 The servlet has published `modeExplicit` since
                                                 prompt K for exactly this; the JSP simply never
                                                 used it. A row now renders when it carries data —
                                                 full stop — plus row 1 when the caller asked for
                                                 AGE_BAND *in the URL*, which is the hub card and
                                                 the JavaScript-off path, and is never true of a
                                                 form submit.

                                                 Consequence, and it is the correct one: a blank row
                                                 does not survive a submit. A blank band is not a
                                                 band, and renumber() closes the gap on the next
                                                 add. K-a falls out of the same change — tier 1 now
                                                 opens with zero rows, matching the label above. --%>
                                            <c:if test="${not empty submittedAges[i-1] or (i == 1 and modeExplicit and mode == 'AGE_BAND')}">
                                                <div class="age-band-row d-flex align-items-end gap-2 mb-2" data-row>
                                                    <div>
                                                        <label class="form-label mb-1" style="font-size:0.7rem;" for="age${i}">Age</label>
                                                        <input type="number" class="form-control form-control-sm age-band-age" id="age${i}" name="age${i}"
                                                               min="21" max="64" value="${submittedAges[i-1]}">
                                                    </div>
                                                    <div>
                                                        <label class="form-label mb-1" style="font-size:0.7rem;" for="count${i}">Count</label>
                                                        <%-- ⚠️ W15 — Count is the worst case of the
                                                             placeholder rule and needs more than a
                                                             placeholder. It showed a grey "1", which
                                                             reads as an entered value ("placeholder
                                                             was showing a 1, I thought it was an
                                                             entry"), and a blank field silently
                                                             computed as 1 anyway — the walk's URL
                                                             carried `count1=` empty while the result
                                                             assumed one life. Entered and assumed
                                                             were indistinguishable.

                                                             So it carries a REAL default of 1. The
                                                             value in the box is now the value that
                                                             counts, and an agent never has to wonder
                                                             whether a figure counted 1 employee or 8.
                                                             The servlet's blank-defaults-to-1 parse
                                                             is untouched and still covers a
                                                             hand-edited URL. --%>
                                                        <input type="number" class="form-control form-control-sm age-band-count" id="count${i}" name="count${i}"
                                                               min="1" value="${empty submittedCounts[i-1] ? 1 : submittedCounts[i-1]}">
                                                    </div>
                                                    <div class="age-band-income" ${affordabilityBasis == 'INCOME' ? '' : 'style="display:none;"'}>
                                                        <label class="form-label mb-1" style="font-size:0.7rem;" for="income${i}">Income</label>
                                                        <%-- W15: "Annual" was a label wearing a
                                                             placeholder's clothes. The label already
                                                             says Income; this says the shape. --%>
                                                        <input type="number" step="1" class="form-control form-control-sm" id="income${i}" name="income${i}"
                                                               min="1" placeholder="$/yr" value="${submittedIncomes[i-1]}">
                                                    </div>
                                                    <button type="button" class="btn btn-sm btn-outline-secondary age-band-remove"
                                                            aria-label="Remove this age band">&times;</button>
                                                </div>
                                            </c:if>
                                        </c:forEach>
                                    </div>
                                    <%-- ⚠️ This is the tier-1 → tier-2 affordance and it must be
                                         obvious with ZERO bands present. If an agent cannot see
                                         that more detail is available, the tiering is invisible
                                         and this whole run achieved nothing — the same failure
                                         as the slider nobody noticed (W10). Hence the accent
                                         outline rather than the muted secondary it started as.

                                         It is not a recommendation to use more detail: the note
                                         beside it states what it does, not what to do. --%>
                                    <button type="button" class="btn btn-sm btn-outline-primary fw-semibold" id="ageBandAdd"
                                            data-max="${ageBandMaxRows}"><i class="bi bi-plus-lg me-1"></i>Add age band</button>
                                    <span class="quiet-note ms-2" id="ageBandMaxNote" style="display:none;">
                                        Maximum ${ageBandMaxRows} age bands.
                                    </span>
                                </div>
                                <%-- W9 — the two inputs an agent has to explain out loud to an
                                     employer while looking at them.

                                     ⚠️ Factual only: what the input is and what it changes. NO
                                     guidance on what to choose — not a typical value, not a
                                     starting point, not a range, not a "most employers". The
                                     no-steering boundary applies to inputs exactly as it does to
                                     outputs, and a suggested contribution is a recommended
                                     contribution however it is phrased.

                                     Triggered by click/focus rather than hover: a hover-only
                                     tooltip does not exist on a phone, which is the device this
                                     surface is actually used on. --%>
                                <%-- Tier 3 and 4. Both only mean anything once there is a band to
                                     apply them to, so they appear with the first one and hide with
                                     the last. Hidden, never removed — values survive. --%>
                                <div class="col-auto age-band-dependent" id="contributionField" ${mode == 'AGE_BAND' ? '' : 'style="display:none;"'}>
                                    <label class="form-label mb-1" for="contribution">Employer Monthly Contribution
                                        <button type="button" class="btn btn-link p-0 ms-1 align-baseline ichra-info"
                                                style="font-size:0.75rem; text-decoration:none;"
                                                data-bs-toggle="popover" data-bs-trigger="focus" data-bs-placement="top"
                                                data-bs-title="Employer Monthly Contribution"
                                                data-bs-content="The amount the employer puts toward each employee's individual premium every month. It lowers what the employee pays and raises the employer's total outlay. It is also what the affordability threshold is measured against."
                                                aria-label="About Employer Monthly Contribution"><i class="bi bi-info-circle"></i></button>
                                    </label>
                                    <input type="number" step="0.01" class="form-control form-control-sm" id="contribution" name="contribution"
                                           min="0" value="${submittedContribution}" style="max-width:170px;">
                                </div>
                                <div class="col-auto age-band-dependent" id="basisField" ${mode == 'AGE_BAND' ? '' : 'style="display:none;"'}>
                                    <label class="form-label mb-1" for="affordabilityBasis">Affordability Basis
                                        <button type="button" class="btn btn-link p-0 ms-1 align-baseline ichra-info"
                                                style="font-size:0.75rem; text-decoration:none;"
                                                data-bs-toggle="popover" data-bs-trigger="focus" data-bs-placement="top"
                                                data-bs-title="Affordability Basis"
                                                data-bs-content="Which income figure the affordability threshold is calculated from. FPL Safe Harbor uses the federal poverty guideline, so no employee income is needed. Entered Income uses an income you type for each age band. None hides the affordability section entirely."
                                                aria-label="About Affordability Basis"><i class="bi bi-info-circle"></i></button>
                                    </label>
                                    <select class="form-select form-select-sm" id="affordabilityBasis" name="affordabilityBasis" style="max-width:190px;">
                                        <option value="" ${empty affordabilityBasis ? 'selected' : ''}>None</option>
                                        <option value="FPL" ${affordabilityBasis == 'FPL' ? 'selected' : ''}>FPL Safe Harbor</option>
                                        <option value="INCOME" ${affordabilityBasis == 'INCOME' ? 'selected' : ''}>Entered Income</option>
                                    </select>
                                </div>

                        <div class="col-auto">
                            <button type="submit" class="ssa-action save" ${empty availableCounties ? 'disabled' : ''}>
                                <i class="bi bi-calculator me-1"></i>Illustrate
                            </button>
                        </div>
                    </form>

                    <c:if test="${missingReferenceCount > 0}">
                        <div class="quiet-note">
                            <c:out value="${missingReferenceCount}"/> cached county reference${missingReferenceCount == 1 ? '' : 'es'} not shown above — no matching county_reference row.
                        </div>
                    </c:if>
                </div>

                <%-- T74: crossing ZIP. 34% of Texas ZIPs touch more than one county, so this
                     is a normal step, not an error — hence a status-card and neutral wording
                     rather than an alert.

                     Nothing is pre-selected and nothing is marked likely. The list arrives
                     ordered by land-area share purely so it is stable and the bigger slice
                     is not buried, and that ordering must NOT read as a recommendation —
                     the no-steering boundary applies to counties exactly as it does to
                     plans. Every entry is rendered identically.

                     Each choice is a link to the ordinary ?countyFips= URL, so the result
                     the agent lands on is linkable and shareable like any other. --%>
                <%-- R2/R4: rendered always, hidden when unused, so the blur lookup reuses
                     THIS markup and THIS wording rather than carrying a second copy in
                     JavaScript. One source of truth for the copy; the script only toggles
                     visibility and swaps the ZIP and the list items. --%>
                <div class="status-card" id="zipChooserPanel" ${empty zipCandidates ? 'style="display:none;"' : ''}>
                    <strong><i class="bi bi-signpost-2 me-1"></i>ZIP <span id="zipChooserZip"><c:out value="${submittedZip}"/></span> is in more than one county</strong>
                    <div class="footnote" style="margin-bottom:0.6rem;">
                        Rates differ by county, so pick the one this employer is in.
                    </div>
                    <ul id="zipChooserList" style="list-style:none; padding-left:0; margin-bottom:0;">
                        <c:forEach var="cand" items="${zipCandidates}">
                            <c:url value="Illustration" var="candUrl">
                                <c:param name="mode" value="${mode}"/>
                                <c:param name="countyFips" value="${cand.countyFips}"/>
                                <c:param name="planYear" value="${selectedPlanYear}"/>
                                <%-- W5: carry the ZIP the agent typed. Losing it made the field
                                     go blank the moment they picked a county, so the page forgot
                                     the thing they had just entered. Safe under the R1
                                     precedence rule: zip and countyFips arrive together,
                                     containsCounty() finds they agree, and the explicit county
                                     is kept. --%>
                                <c:if test="${not empty submittedZip}">
                                    <c:param name="zip" value="${submittedZip}"/>
                                </c:if>
                                <c:if test="${not empty opportunityId}">
                                    <c:param name="opportunityId" value="${opportunityId}"/>
                                </c:if>
                            </c:url>
                            <%-- The crosswalk knows all 254 Texas counties; the illustration
                                 prices only the warmed ones. A candidate we cannot price
                                 still appears — the agent needs to know that is where the
                                 employer sits — but it says so, before the click rather
                                 than after.

                                 ⚠️ Descriptive, not evaluative. "No rates cached yet" is a
                                 fact about our data. It is NOT a reason to prefer the other
                                 county, and an unpriced entry is not demoted, greyed into
                                 uselessness, or disabled: same link, same weight, same
                                 land-area order. Steering counties is steering. --%>
                            <li style="padding:0.25rem 0;">
                                <a href="${candUrl}"><c:out value="${cand.countyName}"/>, <c:out value="${cand.state}"/></a>
                                <%-- `empty` first so a missing set shows the caveat rather than
                                     throwing — same cautious direction the endpoint takes. --%>
                                <c:if test="${empty pricedCountyFips or not pricedCountyFips.contains(cand.countyFips)}">
                                    <span class="text-muted" style="font-size:0.75rem;"> — no rates cached yet</span>
                                </c:if>
                            </li>
                        </c:forEach>
                    </ul>
                </div>

                <%-- T74: the ZIP is not in the crosswalk.

                     ⚠️ Wording is load-bearing. This is almost certainly a real ZIP — our
                     data is ZCTA-derived (so PO-box-only ZIPs are absent entirely) and
                     Texas-only. The gap is ours. An agent who thinks he mistyped will
                     retype it three times; an agent told the data is missing uses the
                     county selector and moves on. Never "invalid ZIP".

                     Kept distinct from the unwarmed-county case, which the servlet reports
                     separately through inputError and which is T76's to fix. --%>
                <%-- W4's other half. Deleting the under-field confirmation removed the only
                     signal for a ZIP that resolves to exactly ONE county the illustration
                     cannot price — the dropdown cannot show that county, because the dropdown
                     is built from the counties that have rates. So the message moves here,
                     out of the form row (fixing the alignment shift) and using the SAME
                     wording the servlet produces after a click, shown before it instead. --%>
                <div class="status-card" id="zipUnpricedPanel" style="display:none;">
                    <strong><i class="bi bi-info-circle me-1"></i>We don't have rates for <span id="zipUnpricedCounty"></span> yet</strong>
                    <div class="footnote" style="margin-top:0.4rem;">
                        The county list holds only the counties we have rates for, so it will not contain this one.
                    </div>
                </div>

                <%-- Same always-render-hidden treatment as the chooser above, for the same
                     reason: the blur lookup must not carry a second copy of this wording. --%>
                <div class="status-card" id="zipNoMatchPanel" ${zipNoMatch ? '' : 'style="display:none;"'}>
                    <strong><i class="bi bi-info-circle me-1"></i>We don't have ZIP <span id="zipNoMatchZip"><c:out value="${submittedZip}"/></span> in our county lookup</strong>
                    <div class="footnote" style="margin-top:0.4rem;">
                        ZIP coverage is incomplete — the lookup is built from Census tabulation areas,
                        which omit some valid ZIPs, and currently covers Texas only. This is a gap in
                        our data, not a problem with the ZIP.
                        <strong>Select the county above instead</strong> — everything else works the same.
                    </div>
                </div>

                <c:if test="${empty availableCounties}">
                    <div class="empty-state">
                        <i class="bi bi-map"></i>
                        <div style="font-size:0.85rem; margin-top:0.5rem;">No counties have cached rate data yet. Rates are loaded by the nightly rate-cache warm job.</div>
                    </div>
                </c:if>

                <%-- W1: everything below is the RESULT of the last computation, and it is
                     wrapped so the ZIP field can hide all of it at once.

                     Observed on production: a full Hopkins rate table rendering BELOW a
                     Collin/Denton chooser, with only the small footer line naming Hopkins.
                     R1's twin — R1 *computed* from a contradicted county, this *displayed*
                     one. A stale result under a fresh chooser is a wrong number on screen,
                     and the fact that it was correct for a county the agent has moved on
                     from is exactly what makes it dangerous. --%>
                <div id="illustrationResults">

                <%-- R3: `empty inputError` guards the whole result panel. A validation
                     failure returns from the mode handler BEFORE hasRates is set, so
                     `not hasRates` was true and this branch printed "No cached rate
                     data..." for a county that demonstrably has rates — collapsing the
                     unwarmed-county state (T76's) into a plain validation error, which is
                     precisely the pair prompt F required kept apart. On a validation
                     error, render no result panel at all.

                     S7-D / T103 second cut: `illustrationLanding` guards it the same way,
                     for the same reason — a landing also returns before hasRates is set,
                     and without this it fell into the exact `not hasRates` branch R3 was
                     written to keep separate, printing "No cached rate data" for a county
                     nobody asked about anything yet. --%>
                <c:if test="${not empty selectedCounty and mode == 'AGE_BAND' and empty inputError and not illustrationLanding}">
                    <c:choose>
                        <c:when test="${not hasRates}">
                            <div class="empty-state">
                                <i class="bi bi-exclamation-circle"></i>
                                <div style="font-size:0.85rem; margin-top:0.5rem;">
                                    No cached rate data for age(s)
                                    <c:forEach var="a" items="${missingAges}" varStatus="as">${a}<c:if test="${!as.last}">, </c:if></c:forEach>
                                    in this county — cache-completeness gap, not computed.
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>

                            <c:if test="${not empty sourceEnv and sourceEnv != 'PRODUCTION'}">
                                <div class="disclaimer" style="background:#f8d7da; border-color:#f5c2c7; color:#842029;">
                                    <i class="bi bi-exclamation-triangle-fill me-1"></i>
                                    <strong>Test-environment rates.</strong> These figures came from the
                                    <c:out value="${sourceEnv}"/> environment, not production market data. Do not present this to a client.
                                </div>
                            </c:if>

                            <div class="disclaimer">
                                <i class="bi bi-info-circle me-1"></i>
                                This is an illustration based on cached market rates, not a quote and not a compliance determination.
                                Actual premiums depend on individual enrollee details, and ICHRA affordability must be determined separately.
                            </div>

                            <div class="disclaimer">
                                <i class="bi bi-info-circle me-1"></i>
                                <strong>Off-exchange plans only.</strong> These figures cover the off-exchange individual market.
                                On-exchange plans are not included in the plan counts or the premium figures shown.
                            </div>

                            <%-- G9 / build-plan §1 step 5. The slider recomputes CLIENT-SIDE from
                                 figures already on the page — no POST per tick, no AJAX, no new
                                 endpoint, no second servlet. /Illustration is GET-only by design
                                 (IllustrationServlet:58-60), and that stays true: the slider does
                                 arithmetic on rendered data and never talks to the server.

                                 It never recomputes the flip point. flip = onexLCSP − pct × (income
                                 ÷ 12) does not depend on the contribution, so the server's figure is
                                 already final and is simply read back out of the row. That is
                                 deliberate: the regulated computation stays in
                                 AffordabilityCalculator, in one place, and there is no second
                                 implementation in JavaScript that could drift from it. The two
                                 constants are consequently NOT needed client-side.

                                 Nothing here is persisted. Dragging the slider writes no row, no
                                 log line, no column — the illustration_log row for this run was
                                 already written server-side, and it carries no contribution figure
                                 at all (IllustrationServlet:608-618). --%>
                            <%-- K3-c: the slider moves a contribution. With none supplied there is
                                 nothing for it to move, so the whole card is absent rather than
                                 rendered at zero — a slider parked at $0 invites the reading that
                                 the employer contributes nothing, which is a claim, not a blank. --%>
                            <c:if test="${contributionSupplied}">
                            <div class="status-card contrib-card mt-3" id="contribSliderCard">
                                <strong><i class="bi bi-sliders me-1"></i>Employer Monthly Contribution</strong>
                                <span class="text-muted" style="font-size:0.8rem;">&mdash; drag to see the effect; nothing is saved</span>
                                <%-- M2: on a phone the track rendered at roughly 60% width with the
                                     value pinned far right, so the ticks compressed into a small
                                     span and the usable drag was shorter than the finger travel
                                     available. Below 768px the value drops beneath a full-width
                                     track; above it the inline layout is unchanged. --%>
                                <div class="contrib-controls d-flex align-items-center gap-3 mt-2">
                                    <%-- data-submitted carries the figure the server actually
                                         computed with. It is read from here rather than from the
                                         input's own value, because a range input snaps its value to
                                         the step and would misreport what was submitted. --%>
                                    <div class="contrib-slider-wrap flex-grow-1">
                                        <input type="range" id="contribSlider"
                                               min="0" step="5" value="${submittedContribution}"
                                               data-submitted="${submittedContribution}"
                                               aria-label="Employer monthly contribution">
                                        <%-- Ticks are a visual echo of the affordability table's
                                             Flip Contribution column, which screen readers already
                                             read properly — so they are hidden from the a11y tree
                                             rather than duplicated into it. --%>
                                        <div class="contrib-ticks" id="contribTicks" aria-hidden="true"></div>
                                    </div>
                                    <div style="font-size:1.05rem; font-weight:700; color:#0d5681; min-width:7rem; text-align:right;"
                                         id="contribReadout"></div>
                                </div>
                                <%-- T105: the slider sits above the results table (L2 requires the
                                     staging banner and slider visible without scrolling, so it
                                     cannot move below it), and dragging it rewrote numbers an agent
                                     demoing it could not see without scrolling -- a control that
                                     visibly moves and nothing else, exactly what the progressive
                                     rework exists to defeat. These two figures are the effect of the
                                     drag, shown where the drag is. render() below writes them from
                                     the same groupNet / c*lives values in the same pass that rewrites
                                     the table, not a second computation -- so they cannot disagree
                                     with it for the same contribution. --%>
                                <div class="contrib-live-figures d-flex flex-wrap gap-3 mt-2" style="font-size:0.82rem; color:#495057;">
                                    <span>Group Monthly Net Cost <strong id="contribGroupNetOut" style="color:#0d5681;"></strong></span>
                                    <span>Maximum Employer Monthly Commitment <strong id="contribMaxCommitOut" style="color:#0d5681;"></strong></span>
                                </div>
                                <%-- Rendered only when there are flip points to mark. Factual:
                                     it says where a verdict changes, not where to aim. --%>
                                <div class="footnote" id="contribTickLegend" style="display:none;">
                                    Marks on the track show where each age band's affordability verdict changes.
                                </div>
                                <div class="footnote" id="contribRevertNote" style="display:none;">
                                    Showing <span id="contribShown"></span>; the figures were calculated at
                                    <span id="contribSubmitted"></span>.
                                    <a href="#" id="contribReset">Reset</a>
                                </div>
                                <c:if test="${empty affordabilityBasis}">
                                    <%-- Without a basis there is no flip point to show, so say why
                                         rather than leaving the agent to discover the selector. Not
                                         a recommendation to turn it on and not a default. --%>
                                    <div class="footnote">
                                        Net cost only. Choose an <strong>Affordability Basis</strong> above and re-run to see
                                        the contribution at which each employee crosses the affordability threshold.
                                    </div>
                                </c:if>
                            </div>
                            </c:if><%-- /contributionSupplied — slider card (K3-c) --%>

                            <div class="table-responsive">
                            <table class="results-table">
                                <thead>
                                <tr>
                                    <th>Age</th>
                                    <th>Count</th>
                                    <th>Lowest Bronze <span class="text-muted fw-normal">(per employee)</span></th>
                                    <%-- K3-c: net cost exists only against a contribution. Without
                                         one the table is a per-band premium table and these two
                                         columns are absent rather than blank — an empty currency
                                         cell reads as zero. --%>
                                    <c:if test="${contributionSupplied}">
                                        <th>Net / Employee <span class="text-muted fw-normal">(after contribution)</span></th>
                                        <th>Band Net Total</th>
                                    </c:if>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="row" items="${ageBandResultRows}">
                                    <tr class="net-row" data-count="${row.count}" data-floor="${row.floorPremium}">
                                        <td>${row.age}</td>
                                        <td>${row.count}</td>
                                        <td><fmt:formatNumber value="${row.floorPremium}" type="currency"/></td>
                                        <c:if test="${contributionSupplied}">
                                            <td class="net-per-emp"><fmt:formatNumber value="${row.netPerEmployee}" type="currency"/></td>
                                            <td class="net-band"><fmt:formatNumber value="${row.bandNet}" type="currency"/></td>
                                        </c:if>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                            </div>

                            <c:if test="${contributionSupplied}">
                                <div class="status-card mt-3">
                                    <strong>Group Monthly Net Cost</strong>
                                    <div style="font-size:1.05rem; font-weight:700; color:#0d5681; margin-top:0.35rem;" id="groupNetTotalOut">
                                        <fmt:formatNumber value="${groupNetTotal}" type="currency"/>
                                    </div>
                                    <div class="footnote">For ${submittedTotalLives} eligible ${submittedTotalLives == 1 ? 'employee' : 'employees'}, after employer contribution. Sum of the Band Net Total column.</div>
                                </div>

                                <%-- T106: this is a ceiling, not a spend, and the label and caption
                                     must say so. Walked on production: 55x2 at $735.59 and 28x3 at
                                     $358.56, contribution $400 -- the age-28 row shows Net/Employee
                                     $0.00 (premium below the allowance) while this card asserted
                                     "$2,000.00" as an outlay, a spend the page's own numbers
                                     contradict. An ICHRA reimburses actual premium up to the
                                     allowance; unused allowance is forfeited, not paid. Option A
                                     (decided): keep the figure -- it is the correct maximum
                                     regardless of which plans employees choose, which is exactly
                                     why it belongs on screen -- and fix the words instead. Option B
                                     (compute the bronze-floor actual-spend figure) was rejected: it
                                     assumes every employee buys the cheapest bronze plan and
                                     understates real cost. A range (option C) is out of scope. --%>
                                <div class="status-card mt-3">
                                    <strong>Maximum Employer Monthly Commitment</strong>
                                    <div style="font-size:1.05rem; font-weight:700; color:#0d5681; margin-top:0.35rem;" id="employerOutlayOut">
                                        <fmt:formatNumber value="${employerOutlay}" type="currency"/>
                                    </div>
                                    <div class="footnote">The ceiling, not a spend. An ICHRA reimburses each employee's actual premium up to this allowance, and unused allowance is forfeited, not paid — actual employer cost will be lower for any employee whose premium falls below the contribution (see Net / Employee above).</div>
                                </div>
                            </c:if>
                            <%-- Tier 2 with no contribution: say what the table IS, so the absent
                                 columns read as a boundary rather than a missing figure. --%>
                            <c:if test="${not contributionSupplied}">
                                <div class="footnote">
                                    Premium at the bronze floor, per employee, for ${submittedTotalLives} eligible ${submittedTotalLives == 1 ? 'employee' : 'employees'}.
                                    Enter an <strong>Employer Monthly Contribution</strong> above to see net cost and the
                                    contribution slider.
                                </div>
                            </c:if>

                            <c:if test="${not empty affordabilityBasis}">
                                <div class="status-card mt-3">
                                    <strong><i class="bi bi-shield-check me-1"></i>Affordability Threshold</strong>
                                    <span class="text-muted">
                                        &mdash; <c:choose><c:when test="${affordabilityBasis == 'FPL'}">FPL safe harbor</c:when><c:otherwise>entered income</c:otherwise></c:choose> basis
                                    </span>

                                    <c:choose>
                                        <c:when test="${not empty affordabilityUnavailableReason}">
                                            <div class="alert alert-warning py-2 mt-2" style="font-size:0.85rem;">
                                                <i class="bi bi-exclamation-triangle me-1"></i><c:out value="${affordabilityUnavailableReason}"/>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <p class="text-muted mt-2 mb-2" style="font-size:0.78rem;">
                                                This is an analysis for the employer, not a determination, and not advice to any employee.
                                                <c:if test="${affordabilityBasis == 'INCOME'}"> Entered-income results rest on an assumed income the employer cannot verify.</c:if>
                                                <c:if test="${affordabilityBasis == 'FPL'}"> FPL safe-harbor results depend on the employer electing that safe harbor.</c:if>
                                                Threshold figures are derived from cached rates and can differ from a live quote by a cent or two &mdash; treat as an estimate, not an exact figure.
                                            </p>

                                            <div class="table-responsive">
                                            <table class="results-table">
                                                <thead>
                                                <tr>
                                                    <th>Age</th>
                                                    <th>Count</th>
                                                    <th>On-Exchange LCSP</th>
                                                    <th>Flip Contribution</th>
                                                    <th>At Entered Contribution</th>
                                                </tr>
                                                </thead>
                                                <tbody>
                                                <c:forEach var="row" items="${affordabilityRows}">
                                                    <%-- data-flip is the server's own figure, read back
                                                         unchanged. The slider compares against it; it
                                                         never recomputes it. --%>
                                                    <tr class="afford-row" data-flip="${row.available ? row.flipContribution : ''}">
                                                        <td>${row.age}</td>
                                                        <td>${row.count}</td>
                                                        <c:choose>
                                                            <c:when test="${row.available}">
                                                                <td><fmt:formatNumber value="${row.onexLcspPremium}" type="currency"/></td>
                                                                <td><fmt:formatNumber value="${row.flipContribution}" type="currency"/></td>
                                                                <td class="afford-verdict">
                                                                    <%-- K3-c: a verdict needs a contribution to
                                                                         compare against. Without one the
                                                                         threshold still stands on its own and is
                                                                         shown; this cell says what is missing
                                                                         rather than picking a side. --%>
                                                                    <c:choose>
                                                                        <c:when test="${empty row.affordable}"><span class="text-muted">Enter a contribution to see the verdict</span></c:when>
                                                                        <c:when test="${row.affordable}">Affordable &mdash; employee loses PTC eligibility</c:when>
                                                                        <c:otherwise>Unaffordable &mdash; employee keeps PTC eligibility</c:otherwise>
                                                                    </c:choose>
                                                                </td>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <td colspan="3" class="text-muted"><c:out value="${row.unavailableReason}"/></td>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </tr>
                                                </c:forEach>
                                                </tbody>
                                            </table>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </c:if>

                            <div class="meta-line">
                                <c:out value="${selectedCounty.countyName}"/>, <c:out value="${selectedCounty.state}"/> &middot;
                                Plan Year ${selectedPlanYear} &middot;
                                <c:choose>
                                    <c:when test="${not empty fetchedAtDisplay}">
                                        Rates as of <c:out value="${fetchedAtDisplay}"/>
                                    </c:when>
                                    <c:otherwise>Cache freshness unavailable</c:otherwise>
                                </c:choose>
                                <c:choose>
                                    <c:when test="${sourceEnv == 'PRODUCTION'}"> &middot; Source: production</c:when>
                                    <c:when test="${empty sourceEnv}"> &middot; Source: not recorded</c:when>
                                </c:choose>
                            </div>

                            <%-- Build-plan item 7 hand-off. URL contract is provisional — item 6 decides
                                 whether ProposalBuilder re-derives the illustration from these inputs or
                                 reads a snapshot row; if a snapshot is needed, this becomes one param
                                 (a snapshot id) instead of the set below. Carries INPUTS only (county,
                                 plan year, mode, ages/counts, contribution) — never computed outputs,
                                 never affordability/income (those are scenario inputs on an assumed
                                 income and have no business in a proposal URL), never a prospect id (no
                                 drop-in prospect picker exists on this page — ProposalBuilder prompts for
                                 the prospect as it always does). --%>
                            <c:url value="ProposalBuilder" var="proposalHandoffUrl">
                                <c:param name="mode" value="AGE_BAND"/>
                                <c:param name="countyFips" value="${submittedCountyFips}"/>
                                <c:param name="planYear" value="${selectedPlanYear}"/>
                                <c:param name="contribution" value="${submittedContribution}"/>
                                <c:forEach begin="1" end="6" var="i">
                                    <c:if test="${not empty submittedAges[i-1]}">
                                        <c:param name="age${i}" value="${submittedAges[i-1]}"/>
                                        <c:param name="count${i}" value="${submittedCounts[i-1]}"/>
                                    </c:if>
                                </c:forEach>
                            </c:url>
                            <c:choose>
                                <c:when test="${sourceEnv == 'PRODUCTION'}">
                                    <a href="${proposalHandoffUrl}" class="ssa-action save" id="ichraProposalLink">
                                        <i class="bi bi-file-earmark-plus me-1"></i>Use This in a Proposal
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <button type="button" class="ssa-action save" disabled>
                                        <i class="bi bi-file-earmark-plus me-1"></i>Use This in a Proposal
                                    </button>
                                    <div class="quiet-note">Available once production rates are configured.</div>
                                </c:otherwise>
                            </c:choose>

                            <%-- G9 slider behaviour. Pure display arithmetic over data already in the
                                 DOM; no fetch, no form submit, no storage of any kind. Deliberately
                                 NOT here: any marking of a contribution as recommended, optimal or
                                 best, any default the slider snaps to, and any ranking — the control
                                 reports the flip point as a fact and leaves the choice with the
                                 agent. Deleting this script block restores the previous page
                                 exactly; every figure it touches is already rendered correctly by
                                 the server for the submitted contribution. --%>
                            <script>
                            (function () {
                                var slider = document.getElementById('contribSlider');
                                if (!slider) return;

                                var submitted = parseFloat(slider.getAttribute('data-submitted'));
                                if (isNaN(submitted)) submitted = 0;
                                // Set on first drag. The "you have moved it" note keys off this
                                // rather than off a value comparison, so a submitted figure that
                                // is not on a step boundary cannot make the note appear on load.
                                var userMoved = false;

                                var money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
                                var netRows = Array.prototype.slice.call(document.querySelectorAll('tr.net-row'));
                                var affordRows = Array.prototype.slice.call(document.querySelectorAll('tr.afford-row'));

                                // Headroom to the highest premium on the page, so the agent can always
                                // drag past the point where net cost reaches zero. Rounded up to a
                                // sane step; never below a floor, so a cheap county still gets range.
                                var highestFloor = 0;
                                netRows.forEach(function (tr) {
                                    var f = parseFloat(tr.getAttribute('data-floor'));
                                    if (!isNaN(f) && f > highestFloor) highestFloor = f;
                                });
                                var max = Math.max(1000, Math.ceil((highestFloor * 1.1) / 50) * 50);
                                if (submitted > max) max = Math.ceil(submitted / 50) * 50;
                                slider.max = max;
                                slider.value = submitted;

                                // W10 — flip-point ticks on the track. The money moment becomes
                                // visible BEFORE anything is dragged, which is the whole point:
                                // an agent who never notices the control never drags it.
                                //
                                // Values come from the affordability rows the server already
                                // computed — this reads data-flip, it never recomputes a
                                // threshold. Ages are deduplicated per distinct flip so two bands
                                // sharing one point produce one tick.
                                //
                                // ⚠️ Every tick is identical: same colour, same size, no ordering,
                                // no region shaded good or bad, no "recommended" anything. A flip
                                // point is a fact about where a verdict changes. Making one look
                                // preferable to another would be steering.
                                var ticksBox = document.getElementById('contribTicks');
                                var tickLegend = document.getElementById('contribTickLegend');

                                function renderTicks(max) {
                                    if (!ticksBox || max <= 0) return;
                                    var byFlip = {};
                                    affordRows.forEach(function (tr) {
                                        var flip = parseFloat(tr.getAttribute('data-flip'));
                                        if (isNaN(flip) || flip < 0 || flip > max) return;
                                        var ageCell = tr.querySelector('td');
                                        var age = ageCell ? ageCell.textContent.trim() : '';
                                        var key = flip.toFixed(2);
                                        if (!byFlip[key]) byFlip[key] = { flip: flip, ages: [] };
                                        if (age && byFlip[key].ages.indexOf(age) === -1) byFlip[key].ages.push(age);
                                    });

                                    var keys = Object.keys(byFlip);
                                    if (!keys.length) return;

                                    ticksBox.textContent = '';
                                    keys.forEach(function (key) {
                                        var entry = byFlip[key];
                                        var mark = document.createElement('div');
                                        mark.className = 'contrib-tick';
                                        mark.style.left = ((entry.flip / max) * 100) + '%';

                                        var bar = document.createElement('i');
                                        mark.appendChild(bar);

                                        var label = document.createElement('span');
                                        label.textContent = 'age ' + entry.ages.join('/');
                                        mark.appendChild(label);

                                        ticksBox.appendChild(mark);
                                    });
                                    if (tickLegend) tickLegend.style.display = '';
                                }

                                var readout = document.getElementById('contribReadout');
                                var revertNote = document.getElementById('contribRevertNote');
                                var shownEl = document.getElementById('contribShown');
                                var submittedEl = document.getElementById('contribSubmitted');
                                var formInput = document.getElementById('contribution');
                                var proposalLink = document.getElementById('ichraProposalLink');
                                var groupOut = document.getElementById('groupNetTotalOut');
                                var outlayOut = document.getElementById('employerOutlayOut');
                                // T105: the slider card's own copies of the same two figures,
                                // written in the same render() pass from the same groupNet / c*lives
                                // values as groupOut/outlayOut below -- one computation, two places,
                                // so they cannot drift apart.
                                var contribGroupNetOut = document.getElementById('contribGroupNetOut');
                                var contribMaxCommitOut = document.getElementById('contribMaxCommitOut');

                                function setContributionParam(href, value) {
                                    // Rewrites only the contribution parameter so the proposal
                                    // snapshot cannot disagree with the figure on screen. Without
                                    // this, dragging to 350 and clicking through would snapshot the
                                    // originally submitted 400 -- silently.
                                    if (!href) return href;
                                    var parts = href.split('?');
                                    if (parts.length < 2) return href;
                                    var pairs = parts[1].split('&').filter(function (p) {
                                        return p.indexOf('contribution=') !== 0;
                                    });
                                    pairs.push('contribution=' + encodeURIComponent(value));
                                    return parts[0] + '?' + pairs.join('&');
                                }

                                var baseHref = proposalLink ? proposalLink.getAttribute('href') : null;

                                function render() {
                                    var c = parseFloat(slider.value);
                                    if (isNaN(c) || c < 0) c = 0;

                                    readout.textContent = money.format(c);

                                    // Track fill follows the handle. A position indicator, not a
                                    // judgement about the region it covers.
                                    slider.style.setProperty('--fill', (max > 0 ? (c / max) * 100 : 0) + '%');

                                    var groupNet = 0;
                                    var lives = 0;
                                    netRows.forEach(function (tr) {
                                        var floor = parseFloat(tr.getAttribute('data-floor'));
                                        var count = parseInt(tr.getAttribute('data-count'), 10);
                                        if (isNaN(floor) || isNaN(count)) return;
                                        var net = Math.max(0, floor - c);
                                        var band = net * count;
                                        groupNet += band;
                                        lives += count;
                                        tr.querySelector('.net-per-emp').textContent = money.format(net);
                                        tr.querySelector('.net-band').textContent = money.format(band);
                                    });

                                    if (groupOut) groupOut.textContent = money.format(groupNet);
                                    if (outlayOut) outlayOut.textContent = money.format(c * lives);
                                    if (contribGroupNetOut) contribGroupNetOut.textContent = money.format(groupNet);
                                    if (contribMaxCommitOut) contribMaxCommitOut.textContent = money.format(c * lives);

                                    // The flip point does not move with the contribution -- it is the
                                    // server's figure. Only which side of it we are on changes, and
                                    // the two strings are the ones already on the page.
                                    affordRows.forEach(function (tr) {
                                        var cell = tr.querySelector('.afford-verdict');
                                        if (!cell) return;
                                        var flip = parseFloat(tr.getAttribute('data-flip'));
                                        if (isNaN(flip)) return;
                                        // Dash built from its code point so this block stays pure
                                        // ASCII and cannot be mangled by an encoding step between
                                        // here and the browser. The resulting wording is identical
                                        // to what the server renders above: the slider flips
                                        // between two already-approved strings and introduces no
                                        // new phrasing about any employee (boundary 1).
                                        var DASH = String.fromCharCode(0x2014);
                                        cell.textContent = (c >= flip)
                                            ? 'Affordable ' + DASH + ' employee loses PTC eligibility'
                                            : 'Unaffordable ' + DASH + ' employee keeps PTC eligibility';
                                    });

                                    if (formInput) formInput.value = c;
                                    if (proposalLink && baseHref) {
                                        proposalLink.setAttribute('href', setContributionParam(baseHref, c));
                                    }

                                    revertNote.style.display = userMoved ? '' : 'none';
                                    if (userMoved) {
                                        shownEl.textContent = money.format(c);
                                        submittedEl.textContent = money.format(submitted);
                                    }
                                }

                                renderTicks(max);

                                slider.addEventListener('input', function () {
                                    userMoved = true;
                                    render();
                                });
                                document.getElementById('contribReset').addEventListener('click', function (e) {
                                    e.preventDefault();
                                    slider.value = submitted;
                                    userMoved = false;
                                    render();
                                });
                                render();
                            })();
                            </script>

                        </c:otherwise>
                    </c:choose>
                </c:if>

                <%-- R3, RANGE side. Same reasoning as the AGE_BAND guard above: this is
                     the branch actually observed printing "No rate data for this county
                     yet." for Hopkins while the real failure was a blank headcount. --%>
                <c:if test="${not empty selectedCounty and mode != 'AGE_BAND' and empty inputError}">
                    <c:choose>
                        <c:when test="${not hasRates}">
                            <div class="empty-state">
                                <i class="bi bi-exclamation-circle"></i>
                                <div style="font-size:0.85rem; margin-top:0.5rem;">No rate data for this county yet.</div>
                            </div>
                        </c:when>
                        <c:otherwise>

                            <c:if test="${not empty sourceEnv and sourceEnv != 'PRODUCTION'}">
                                <div class="disclaimer" style="background:#f8d7da; border-color:#f5c2c7; color:#842029;">
                                    <i class="bi bi-exclamation-triangle-fill me-1"></i>
                                    <strong>Test-environment rates.</strong> These figures came from the
                                    <c:out value="${sourceEnv}"/> environment, not production market data. Do not present this to a client.
                                </div>
                            </c:if>

                            <div class="disclaimer">
                                <i class="bi bi-info-circle me-1"></i>
                                This is an illustration based on cached market rates, not a quote and not a compliance determination.
                                Actual premiums depend on individual enrollee details, and ICHRA affordability must be determined separately.
                            </div>

                            <div class="disclaimer">
                                <i class="bi bi-info-circle me-1"></i>
                                <strong>Off-exchange plans only.</strong> These figures cover the off-exchange individual market.
                                On-exchange plans are not included in the plan counts or the premium figures shown.
                            </div>

                            <div class="table-responsive">
                            <table class="results-table">
                                <thead>
                                <tr>
                                    <th></th>
                                    <th>Age 21</th>
                                    <th>Age 40</th>
                                    <th>Age 64</th>
                                </tr>
                                </thead>
                                <tbody>
                                <tr class="headline">
                                    <td>Lowest bronze <span class="text-muted fw-normal">(practical floor)</span></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age21Row.lowestBronzePremium}"><fmt:formatNumber value="${age21Row.lowestBronzePremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age40Row.lowestBronzePremium}"><fmt:formatNumber value="${age40Row.lowestBronzePremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age64Row.lowestBronzePremium}"><fmt:formatNumber value="${age64Row.lowestBronzePremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Second-lowest silver <span class="text-muted fw-normal">(off-exchange)</span></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age21Row.benchmarkSilverPremium}"><fmt:formatNumber value="${age21Row.benchmarkSilverPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age40Row.benchmarkSilverPremium}"><fmt:formatNumber value="${age40Row.benchmarkSilverPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age64Row.benchmarkSilverPremium}"><fmt:formatNumber value="${age64Row.benchmarkSilverPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                <tr>
                                    <td>Market low</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age21Row.marketLowPremium}"><fmt:formatNumber value="${age21Row.marketLowPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age40Row.marketLowPremium}"><fmt:formatNumber value="${age40Row.marketLowPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty age64Row.marketLowPremium}"><fmt:formatNumber value="${age64Row.marketLowPremium}" type="currency"/></c:when>
                                            <c:otherwise>&mdash;</c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                            </div>
                            <div class="footnote">
                                <i class="bi bi-exclamation-triangle me-1"></i>
                                Market low at Age 21 may reflect a catastrophic plan, available only to enrollees under 30 — not available at ages 30 and up.
                                Lowest bronze is the practical floor for this illustration.
                            </div>

                            <div class="status-card mt-3">
                                <strong>Estimated monthly group premium at the bronze floor</strong>
                                <div style="font-size:1.05rem; font-weight:700; color:#0d5681; margin-top:0.35rem;">
                                    <c:choose>
                                        <c:when test="${not empty groupMonthlyLow and not empty groupMonthlyHigh}">
                                            <fmt:formatNumber value="${groupMonthlyLow}" type="currency"/> &ndash; <fmt:formatNumber value="${groupMonthlyHigh}" type="currency"/>
                                        </c:when>
                                        <c:when test="${not empty groupMonthlyLow}">
                                            From <fmt:formatNumber value="${groupMonthlyLow}" type="currency"/>
                                        </c:when>
                                        <c:when test="${not empty groupMonthlyHigh}">
                                            Up to <fmt:formatNumber value="${groupMonthlyHigh}" type="currency"/>
                                        </c:when>
                                        <c:otherwise>&mdash;</c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="footnote">For ${submittedHeadcount} eligible ${submittedHeadcount == 1 ? 'employee' : 'employees'}. This spread reflects age mix across the group, not plan choice.</div>
                            </div>

                            <div class="meta-line">
                                <c:out value="${selectedCounty.countyName}"/>, <c:out value="${selectedCounty.state}"/> &middot;
                                Plan Year ${selectedPlanYear} &middot;
                                <c:if test="${not empty carrierCount}">${carrierCount} carriers (age ${countRowAge}) &middot; </c:if>
                                <c:if test="${not empty planCount}">${planCount} plans (age ${countRowAge}) &middot; </c:if>
                                <c:choose>
                                    <c:when test="${not empty fetchedAtDisplay}">
                                        Rates as of <c:out value="${fetchedAtDisplay}"/>
                                    </c:when>
                                    <c:otherwise>Cache freshness unavailable</c:otherwise>
                                </c:choose>
                                <c:choose>
                                    <c:when test="${sourceEnv == 'PRODUCTION'}"> &middot; Source: production</c:when>
                                    <c:when test="${empty sourceEnv}"> &middot; Source: not recorded</c:when>
                                </c:choose>
                            </div>

                            <%-- Build-plan item 7 hand-off. URL contract is provisional — item 6 decides
                                 whether ProposalBuilder re-derives the illustration from these inputs or
                                 reads a snapshot row; if a snapshot is needed, this becomes one param
                                 (a snapshot id) instead of the set below. Carries INPUTS only (county,
                                 plan year, mode, headcount) — never computed outputs, never a prospect id
                                 (no drop-in prospect picker exists on this page — ProposalBuilder prompts
                                 for the prospect as it always does). --%>
                            <c:url value="ProposalBuilder" var="proposalHandoffUrl">
                                <c:param name="mode" value="RANGE"/>
                                <c:param name="countyFips" value="${submittedCountyFips}"/>
                                <c:param name="planYear" value="${selectedPlanYear}"/>
                                <c:param name="headcount" value="${submittedHeadcount}"/>
                            </c:url>
                            <c:choose>
                                <c:when test="${sourceEnv == 'PRODUCTION'}">
                                    <a href="${proposalHandoffUrl}" class="ssa-action save">
                                        <i class="bi bi-file-earmark-plus me-1"></i>Use This in a Proposal
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <button type="button" class="ssa-action save" disabled>
                                        <i class="bi bi-file-earmark-plus me-1"></i>Use This in a Proposal
                                    </button>
                                    <div class="quiet-note">Available once production rates are configured.</div>
                                </c:otherwise>
                            </c:choose>

                        </c:otherwise>
                    </c:choose>
                </c:if>

                </div><%-- /#illustrationResults (W1) --%>

            </c:otherwise>
        </c:choose>

    </div>
</div>
<%-- R2/R4: resolve the ZIP on blur, without submitting anything.

     Before this, the only trigger was Enter -- which submits the form, so typing a
     ZIP with the headcount still empty answered with "Enter a valid number of
     eligible employees". Resolving a ZIP had become entangled with computing an
     illustration; this separates them.

     Deliberately NOT here: any copy of its own. The chooser and no-match panels are
     server-rendered above and merely hidden, and this script toggles them and swaps
     their ZIP and list items -- so the wording has one source and cannot drift.

     R1's other half: changing the ZIP clears the county selection immediately, before
     any lookup returns. A stale selection must not survive a new ZIP. The server
     enforces the same precedence independently, because this script may not run.

     Progressive enhancement throughout -- with JavaScript off, the field still posts
     as ?zip= and the servlet resolves it exactly as it does today. Nothing here is the
     only path to anything. --%>
<script>
(function () {
    var zipInput = document.getElementById('zip');
    if (!zipInput) return;

    var countySelect  = document.getElementById('countyFips');
    var chooser       = document.getElementById('zipChooserPanel');
    var chooserZip    = document.getElementById('zipChooserZip');
    var chooserList   = document.getElementById('zipChooserList');
    var noMatch       = document.getElementById('zipNoMatchPanel');
    var noMatchZip    = document.getElementById('zipNoMatchZip');
    var unpriced      = document.getElementById('zipUnpricedPanel');
    var unpricedName  = document.getElementById('zipUnpricedCounty');
    var results       = document.getElementById('illustrationResults');
    var form          = zipInput.form;

    // What the server already rendered for. Re-looking-up the same value on every
    // blur would flicker the panels the server just drew.
    var lastLookedUp = (zipInput.value || '').trim();

    function hidePanels() {
        if (chooser) chooser.style.display = 'none';
        if (noMatch) noMatch.style.display = 'none';
        if (unpriced) unpriced.style.display = 'none';
    }

    // R1 + R4 + W1. Any edit to the ZIP invalidates the county selection, whatever panel
    // is on screen, AND the results below -- immediately, not when the lookup returns.
    //
    // W1 is the one that bit: a full Hopkins table was left rendering underneath a
    // Collin/Denton chooser, with only the footer line naming the county it belonged to.
    // Results are the answer to a question the agent has just changed, so they go with it.
    function invalidate() {
        hidePanels();
        if (countySelect) countySelect.value = '';
        if (results) results.style.display = 'none';
    }

    function currentParam(name, fallback) {
        if (!form) return fallback;
        var el = form.elements[name];
        return (el && el.value) ? el.value : fallback;
    }

    function selectCounty(county) {
        if (!countySelect) return;
        var found = false;
        for (var i = 0; i < countySelect.options.length; i++) {
            if (countySelect.options[i].value === county.fips) {
                countySelect.selectedIndex = i;
                found = true;
                break;
            }
        }
        // W4: on success, say nothing. The dropdown two inches to the right now shows the
        // county, and repeating it under the field was redundant and broke the row's
        // alignment. Only the case the dropdown CANNOT express gets a message -- a county
        // with no cached rates is not in the dropdown at all.
        if (!found && unpriced && unpricedName) {
            unpricedName.textContent = county.name + ', ' + county.state;
            unpriced.style.display = '';
        }
    }

    function renderChooser(zip, counties) {
        if (!chooser || !chooserList) return;
        if (chooserZip) chooserZip.textContent = zip;

        var mode = currentParam('mode', 'RANGE');
        var planYear = currentParam('planYear', '');

        // Rebuilt with createElement/textContent, never innerHTML: every value here
        // came off an HTTP response, and a response is data, not markup.
        chooserList.textContent = '';
        counties.forEach(function (county) {
            // W5: carry the ZIP through, matching the server-rendered chooser's links.
            var href = 'Illustration?mode=' + encodeURIComponent(mode)
                     + '&countyFips=' + encodeURIComponent(county.fips)
                     + (planYear ? '&planYear=' + encodeURIComponent(planYear) : '')
                     + '&zip=' + encodeURIComponent(zip);

            var a = document.createElement('a');
            a.setAttribute('href', href);
            a.textContent = county.name + ', ' + county.state;

            var li = document.createElement('li');
            li.style.padding = '0.25rem 0';
            li.appendChild(a);

            // Same caveat the server-rendered chooser carries, and for the same reason:
            // the crosswalk knows every county, the illustration prices only the warmed
            // ones. Descriptive only -- the entry keeps its link, its weight and its
            // place in the order.
            if (county.priced === false) {
                var note = document.createElement('span');
                note.className = 'text-muted';
                note.style.fontSize = '0.75rem';
                note.textContent = ' ' + String.fromCharCode(0x2014) + ' no rates cached yet';
                li.appendChild(note);
            }

            chooserList.appendChild(li);
        });

        chooser.style.display = '';
    }

    function lookup() {
        var raw = (zipInput.value || '').trim();
        if (raw === lastLookedUp) return;
        lastLookedUp = raw;

        invalidate();

        // Not five digits yet: say nothing at all. Half-typed input is not an error,
        // and calling it one is what makes an agent retype a ZIP three times.
        if (!/^[0-9]{5}$/.test(raw)) return;

        // planYear is sent so the endpoint can report which candidates have cached rates.
        // Omitted or unparseable, everything reports unpriced -- the cautious direction.
        var lookupPlanYear = currentParam('planYear', '');
        fetch('IchraZipLookup?zip=' + encodeURIComponent(raw)
                + (lookupPlanYear ? '&planYear=' + encodeURIComponent(lookupPlanYear) : ''), {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (res) { return res.ok ? res.json() : { counties: [] }; })
            .then(function (data) {
                // The field moved on while this was in flight -- drop the answer.
                if ((zipInput.value || '').trim() !== raw) return;

                var counties = (data && Array.isArray(data.counties)) ? data.counties : [];
                if (counties.length === 0) {
                    if (noMatchZip) noMatchZip.textContent = raw;
                    if (noMatch) noMatch.style.display = '';
                } else if (counties.length === 1) {
                    selectCounty(counties[0]);
                } else {
                    // Nothing auto-selects. The agent picks, exactly as on the server path.
                    renderChooser(raw, counties);
                }
            })
            .catch(function () {
                // Leave it to the submit path, which resolves server-side regardless.
            });
    }

    zipInput.addEventListener('change', lookup);
    zipInput.addEventListener('blur', lookup);
    zipInput.addEventListener('input', function () {
        if ((zipInput.value || '').trim() !== lastLookedUp) invalidate();
    });
})();

/* W7 + W8 — the age-band repeater, and income shown only when the basis consumes it.

   The single hard rule: parameter names stay age1..ageN / count1..countN /
   income1..incomeN, contiguous from 1. This servlet is GET-only and its URLs carry
   state; renaming or gapping them would break the mode toggle, the hub cards, the
   proposal hand-off and every link verified this session. Every add and remove ends
   with renumber(), which is what makes that guarantee hold. */
(function () {
    var rows = document.getElementById('ageBandRows');
    var addBtn = document.getElementById('ageBandAdd');
    if (!rows || !addBtn) return;

    var maxRows = parseInt(addBtn.getAttribute('data-max'), 10) || 6;
    var maxNote = document.getElementById('ageBandMaxNote');
    var basisSelect = document.getElementById('affordabilityBasis');

    function rowList() {
        return Array.prototype.slice.call(rows.querySelectorAll('.age-band-row'));
    }

    /* Rewrites id/name/for on every row so the set is always 1..N with no gaps.
       Called after every structural change -- this is the parameter contract. */
    function renumber() {
        rowList().forEach(function (row, idx) {
            var n = idx + 1;
            [['age', '.age-band-age'], ['count', '.age-band-count'], ['income', '.age-band-income input']]
                .forEach(function (pair) {
                    var input = row.querySelector(pair[1]);
                    if (!input) return;
                    input.id = pair[0] + n;
                    input.name = pair[0] + n;
                    var label = row.querySelector('label[for^="' + pair[0] + '"]');
                    if (label) label.setAttribute('for', pair[0] + n);
                });
        });
        syncControls();
        syncTier();
    }

    /* The tier transition, and the only thing that expresses it in the UI.

       Zero bands: Eligible Employees is the input, and contribution/basis mean nothing
       because there is no band to apply them to. One or more: the bands carry the counts,
       their sum is the headcount, and the later tiers open up.

       Everything is hidden, never removed, so nothing is lost going either way -- and the
       servlet ignores headcount in AGE_BAND regardless, so a hidden field submitting is
       harmless. */
    function syncTier() {
        var hasBands = rowList().length > 0;
        var headcountField = document.getElementById('headcountField');
        if (headcountField) headcountField.style.display = hasBands ? 'none' : '';
        document.querySelectorAll('.age-band-dependent').forEach(function (el) {
            el.style.display = hasBands ? '' : 'none';
        });
    }

    function syncControls() {
        var list = rowList();
        // The remove control is hidden on a lone row: removing the only row would leave
        // a form that cannot be submitted, and disabling it silently is worse than not
        // offering it.
        list.forEach(function (row) {
            var btn = row.querySelector('.age-band-remove');
            if (btn) btn.style.display = (list.length > 1) ? '' : 'none';
        });
        var atMax = list.length >= maxRows;
        addBtn.style.display = atMax ? 'none' : '';
        if (maxNote) maxNote.style.display = atMax ? '' : 'none';
    }

    // W8: income follows the basis. Values are left in the DOM when hidden, so switching
    // away and back does not lose what was typed -- and a hidden input still submits,
    // which is harmless because the servlet only reads income on the INCOME basis.
    function syncIncome() {
        var show = basisSelect && basisSelect.value === 'INCOME';
        rows.querySelectorAll('.age-band-income').forEach(function (cell) {
            cell.style.display = show ? '' : 'none';
        });
    }

    addBtn.addEventListener('click', function () {
        var list = rowList();
        if (list.length >= maxRows) return;

        // With zero bands there is no row to clone from, so fall back to the hidden
        // template. Its class is swapped on the way in, which is what makes it count.
        var clone;
        if (list.length) {
            clone = list[list.length - 1].cloneNode(true);
            clone.querySelectorAll('input').forEach(function (input) { input.value = ''; });
        } else {
            var template = document.getElementById('ageBandTemplate');
            if (!template) return;
            clone = template.cloneNode(true);
            clone.removeAttribute('id');
            clone.removeAttribute('aria-hidden');
            clone.style.display = '';
            clone.className = 'age-band-row d-flex align-items-end gap-2 mb-2';
        }
        // Count carries a real default rather than a placeholder (W15) — entered and
        // assumed must never be indistinguishable.
        var count = clone.querySelector('.age-band-count');
        if (count) count.value = '1';

        rows.appendChild(clone);
        renumber();
        syncIncome();
        var firstInput = clone.querySelector('.age-band-age');
        if (firstInput) firstInput.focus();
    });

    rows.addEventListener('click', function (e) {
        var btn = e.target.closest ? e.target.closest('.age-band-remove') : null;
        if (!btn) return;
        if (rowList().length <= 1) return;
        var row = btn.closest('.age-band-row');
        if (row) row.parentNode.removeChild(row);
        renumber();
    });

    if (basisSelect) basisSelect.addEventListener('change', syncIncome);

    syncControls();
    syncIncome();
    syncTier();
})();

/* W14 — the collapsed input summary.

   Presentation only: the form is hidden, never emptied or detached, so Edit re-shows
   exactly what was submitted. Nothing re-submits and nothing is cleared.

   The summary is built from the live form controls rather than from server attributes,
   so it cannot disagree with what the form actually holds -- including after the ZIP
   field or the repeater has changed something client-side. */
(function () {
    var card = document.getElementById('inputCard');
    var summary = document.getElementById('inputSummary');
    var summaryText = document.getElementById('inputSummaryText');
    var editBtn = document.getElementById('inputSummaryEdit');
    if (!card || !summary || !summaryText || !editBtn) return;

    function describe() {
        var parts = [];

        var county = document.getElementById('countyFips');
        if (county && county.selectedIndex > 0) {
            parts.push(county.options[county.selectedIndex].text.trim());
        }

        var headcount = document.getElementById('headcount');
        if (headcount && headcount.value) {
            parts.push(headcount.value + ' employees');
        }

        var bands = card.querySelectorAll('.age-band-row');
        if (bands.length) {
            var lives = 0, filled = 0;
            bands.forEach(function (row) {
                var age = row.querySelector('.age-band-age');
                if (!age || !age.value) return;
                filled++;
                var count = row.querySelector('.age-band-count');
                lives += parseInt(count && count.value ? count.value : '1', 10) || 0;
            });
            if (filled) {
                parts.push(filled + (filled === 1 ? ' age band' : ' age bands'));
                parts.push(lives + (lives === 1 ? ' life' : ' lives'));
            }
        }

        var contribution = document.getElementById('contribution');
        if (contribution && contribution.value) {
            parts.push('$' + contribution.value + '/mo contribution');
        }

        summaryText.textContent = parts.join('  ' + String.fromCharCode(0x00B7) + '  ');
    }

    editBtn.addEventListener('click', function () {
        summary.style.display = 'none';
        card.style.display = '';
        var zip = document.getElementById('zip');
        if (zip) zip.focus();
    });

    /* K3-d — a stale result must not survive an edit.

       W1 already did this for the ZIP field. It is the same defect for every other input:
       open Edit, change the contribution or an age, and the table below still shows the
       previous answer while the form now describes a different question. The numbers are
       correct for inputs the agent has moved on from, which is exactly what made W1
       dangerous.

       Scoped to the form deliberately. The slider lives in the results card, OUTSIDE the
       form, so dragging it does not trip this -- verified by position: the form closes
       well before the slider is rendered. Clicking Edit alone does not trip it either;
       only an actual change does. */
    var results = document.getElementById('illustrationResults');
    var form = card.querySelector('form');
    if (results && form) {
        var clearResults = function () { results.style.display = 'none'; };
        form.addEventListener('input', clearResults);
        form.addEventListener('change', clearResults);
    }

    describe();
})();

/* W9 — initialise the two input popovers. Bootstrap 5.3.3 is already loaded by
   css-js.jsp; popovers are opt-in and need this call. Guarded so a missing bundle
   degrades to a plain button rather than a script error taking the slider with it. */
(function () {
    if (!window.bootstrap || !window.bootstrap.Popover) return;
    document.querySelectorAll('.ichra-info').forEach(function (el) {
        new window.bootstrap.Popover(el);
    });
})();
</script>
</body>
</html>
