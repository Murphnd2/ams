<%--
  S62-P2 -- T277's working replacement for the Demographics "Check response" signal
  (SummitResponseService.parse classifies on fields[0], which on the Demographics response is the
  participant key, so every line renders UNKNOWN there). Reached from
  detailSummitSetup25.jsp's Census row "Verify against Summit export" control.

  ⚠️ PII / no persistence (LA-40). Every row under "Participants" below comes straight from
  SummitDemographicsVerifyService, fetched fresh on this one request and never stored -- the
  participant-list export can carry names, addresses and phone numbers (summit_data_exchange.md's
  observed header). Do not add a server-side cache or a hidden field that resubmits raw export
  content; if this page is refreshed, it re-fetches.

  S62-P4 -- added the verdict banner (NO_PUSH_RECORDED / EXPORT_PREDATES_PUSH / EMPLOYER_KEY_ABSENT
  / ALL_CONFIRMED / PARTICIPANTS_UNCONFIRMED), which distinguishes "never pushed" from "pushed and
  missing" -- the prior unconditional warning collapsed both into the same red banner. The compare
  logic itself is unchanged; this is a display and classification change only.

  S62-P5 -- the export itself is Summit-side filtered to employers on PremiumPath or a regular
  ICHRA (TA-56), so EMPLOYER_KEY_ABSENT now names both live causes (out of scope, or a genuine key
  mismatch) rather than implying only the latter; added the export-scope sentence to "Source
  export"; added a stale-export caveat to EMPLOYER_KEY_ABSENT/PARTICIPANTS_UNCONFIRMED (no
  timestamp comparison in code -- EXPORT_PREDATES_PUSH stays unimplemented, D-104); and neutralized
  the Missing badge everywhere except PARTICIPANTS_UNCONFIRMED.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Summit Demographics Verify</title>
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
        .toolbar .t-title {
            font-weight: 700; color: var(--ssa, #0d5681); font-size: 0.95rem; margin: 0;
        }
        .rc-body {
            flex: 1; overflow-y: auto;
            padding: 0.75rem 1rem;
            background: #eef1f5;
        }
        .status-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 6px;
            padding: 0.9rem 1.1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem;
        }
        .resp-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; background: #fff; }
        .resp-table th {
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
            text-align: left; padding: 0.5rem 0.75rem; border-bottom: 2px solid #dee2e6;
            font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: #6c757d;
        }
        .resp-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; vertical-align: middle; }
        .resp-table tr.row-missing { background: #fdecea; }
        .resp-table tr.row-wrong { background: #fff8e1; }
        .badge-found { background: #198754; color: #fff; }
        .badge-wrong { background: #fd7e14; color: #fff; }
        .badge-missing { background: #dc3545; color: #fff; }
        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; }
        .num { text-align: right; }
        /* S62-P4 -- verdict banner, styled by severity. Distinct from .status-card's neutral
           white/grey so the one verdict that matters most on the page cannot be missed or, just as
           important, cannot be mistaken for a severity it isn't. */
        .verdict-banner {
            border-radius: 6px; padding: 0.9rem 1.1rem; margin-bottom: 0.9rem;
            font-size: 0.85rem; border: 1px solid transparent;
        }
        .verdict-neutral { background: #eef1f5; border-color: #dee2e6; color: #495057; }
        .verdict-amber { background: #fff8e1; border-color: #ffe69c; color: #7a5c00; }
        .verdict-success { background: #d1e7dd; border-color: #a3cfbb; color: #0f5132; }
        .verdict-red { background: #fdecea; border-color: #f5b8b1; color: #842029; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">
    <div class="toolbar">
        <h1 class="t-title m-0">
            <i class="bi bi-clipboard-check me-1"></i>Demographics — verify against Summit export
        </h1>
        <span class="text-muted" style="font-size: 0.8rem;">Proposal <c:out value="${proposalId}"/></span>
        <%-- Same rule as summitResponse25.jsp:76-84 -- only when the session's current activity is
             really this proposal's Setup; ViewActivity25 renders whatever activity is current with
             no parameters, so never show this link when that could open a different activity. --%>
        <c:if test="${backToSetup}">
            <a href="${pageContext.request.contextPath}/ViewActivity25" class="btn btn-outline-ssa btn-sm">
                <i class="bi bi-arrow-left"></i> Back to setup
            </a>
        </c:if>
    </div>

    <div class="rc-body">

        <c:choose>
            <c:when test="${outcome == 'OK'}">
                <%-- S62-P4 -- the verdict banner. Replaces the prior unconditional
                     "wrongEmployerCount > 0 or missingCount > 0" warning, which could not tell
                     "never pushed" apart from "pushed and missing" and so read a proposal with no
                     recorded push the same as a genuine push failure. --%>
                <c:choose>
                    <c:when test="${verdict == 'NO_PUSH_RECORDED'}">
                        <div class="verdict-banner verdict-neutral">
                            <div class="fw-semibold mb-1"><i class="bi bi-info-circle"></i> Nothing to verify yet</div>
                            <div>
                                No demographics push has been recorded for this proposal, so there is
                                nothing yet for the export to confirm or contradict. The participants
                                below are what AMS would expect to find <em>after</em> a push — not a
                                report that anything failed.
                            </div>
                            <c:if test="${manualMarkDoneOnly}">
                                <div class="mt-1">
                                    This step was marked done by hand (a manual override for a group
                                    already set up in Summit), which is not a push and confirms
                                    nothing here.
                                </div>
                            </c:if>
                        </div>
                    </c:when>
                    <c:when test="${verdict == 'EXPORT_PREDATES_PUSH'}">
                        <div class="verdict-banner verdict-amber">
                            <div class="fw-semibold mb-1"><i class="bi bi-exclamation-triangle"></i> Export predates the push</div>
                            <div>
                                This export was taken at <c:out value="${fileTimestampDisplay}"/>; the
                                most recent push was at <c:out value="${lastPushTimestampDisplay}"/>.
                                The result below is inconclusive — a newer export is needed to
                                confirm this push actually landed.
                            </div>
                        </div>
                    </c:when>
                    <c:when test="${verdict == 'EMPLOYER_KEY_ABSENT'}">
                        <div class="verdict-banner verdict-amber">
                            <div class="fw-semibold mb-1"><i class="bi bi-exclamation-triangle"></i> Employer key not present in this export</div>
                            <div>
                                No row in this export carries this employer's expected key,
                                <span class="mono"><c:out value="${expectedEmployerKey}"/></span> —
                                out of <c:out value="${totalDataRows}"/> total data row(s) in the
                                export. This could mean either that the employer is not on
                                PremiumPath or a regular ICHRA in Summit — this export covers only
                                those — or that the composed key does not match what Summit holds.
                                Neither is more likely than the other from this page alone.
                            </div>
                            <%-- S62-P5c -- shown only when a push is recorded, since that is the
                                 case where "the export just predates the push" is a live, unruled-out
                                 explanation. Both timestamps are already shown elsewhere on this
                                 page; this sentence does not compare them. --%>
                            <c:if test="${not empty lastPushTimestampDisplay}">
                                <div class="mt-1">
                                    This export's age relative to the push cannot be established —
                                    an export taken before the push would produce this same result.
                                </div>
                            </c:if>
                        </div>
                    </c:when>
                    <c:when test="${verdict == 'ALL_CONFIRMED'}">
                        <div class="verdict-banner verdict-success">
                            <div class="fw-semibold mb-1"><i class="bi bi-check-circle"></i> All confirmed</div>
                            <div>
                                Every expected participant was found in this export under this
                                employer's key.
                            </div>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="verdict-banner verdict-red">
                            <div class="fw-semibold mb-1"><i class="bi bi-exclamation-triangle"></i> Participants did not confirm cleanly</div>
                            <div>
                                A push is recorded and this employer's key appears in the export, but
                                one or more expected participants are missing or under a different
                                employer. Review the table below.
                            </div>
                            <div class="mt-1">
                                This export's age relative to the push cannot be established — an
                                export taken before the push would produce this same result.
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>

                <div class="status-card">
                    <div class="fw-semibold mb-1">Source export</div>
                    <div><c:out value="${fileName}"/></div>
                    <div class="text-muted" style="font-size: 0.78rem;">
                        <c:out value="${fileTimestampDisplay}"/>
                    </div>
                    <div class="text-muted mt-2" style="font-size: 0.78rem;">
                        This export covers only employers on PremiumPath or a regular ICHRA in Summit.
                    </div>
                    <div class="text-muted mt-2" style="font-size: 0.78rem;">
                        Expected employer key: <span class="mono"><c:out value="${expectedEmployerKey}"/></span><br/>
                        <c:out value="${totalDataRows}"/> total data row(s) in the export,
                        <c:out value="${employerMatchingRowCount}"/> under this employer's key<br/>
                        Most recent push:
                        <c:choose>
                            <c:when test="${not empty lastPushTimestampDisplay}">
                                <c:out value="${lastPushTimestampDisplay}"/>
                            </c:when>
                            <c:otherwise>no push recorded</c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <div class="status-card">
                    <div class="fw-semibold mb-2">Counts</div>
                    <div class="mb-2">
                        <span class="badge badge-found"><c:out value="${foundCount}"/> Found</span>
                        <span class="badge badge-wrong"><c:out value="${wrongEmployerCount}"/> Wrong employer</span>
                        <%-- S62-P5d -- Missing is only styled as an error under PARTICIPANTS_UNCONFIRMED,
                             the one verdict that is actually a participant-level finding. Under
                             NO_PUSH_RECORDED / EMPLOYER_KEY_ABSENT / ALL_CONFIRMED a Missing count is
                             an expectation, not a fault -- text unchanged, styling neutral. --%>
                        <span class="badge ${verdict == 'PARTICIPANTS_UNCONFIRMED' ? 'badge-missing' : 'bg-secondary'}"><c:out value="${missingCount}"/> Missing</span>
                        <span class="badge bg-secondary"><c:out value="${unkeyedCount}"/> Unkeyed (Summit-added)</span>
                    </div>
                    <div class="text-muted" style="font-size: 0.78rem;">
                        <c:out value="${expectedCount}"/> participant(s) expected from AMS's own roster.
                        "Unkeyed" rows are export rows under this employer with a blank
                        Participant Custom ID — per the 2026-09-10 observation, a participant added
                        directly in the Summit UI rather than by an AMS push. Not counted as an error.
                    </div>
                </div>

                <%-- S62-P4 -- row styling reflects the verdict, not the raw per-participant state:
                     a MISSING row is only an error (row-missing/row-wrong) when the verdict is
                     PARTICIPANTS_UNCONFIRMED, the only verdict that is actually a participant-level
                     finding. In every other verdict a MISSING row is an expectation (no push yet,
                     employer key absent, or genuinely all confirmed), so it renders with no error
                     styling. Badge text is unchanged either way. --%>
                <table class="resp-table">
                    <thead>
                    <tr>
                        <th>Name</th>
                        <th>Expected key</th>
                        <th>State</th>
                        <th>Summit UserStatus</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${participants}">
                        <tr class="${verdict == 'PARTICIPANTS_UNCONFIRMED' ? (p.state == 'MISSING' ? 'row-missing' : (p.state == 'FOUND_WRONG_EMPLOYER' ? 'row-wrong' : '')) : ''}">
                            <td><c:out value="${p.firstName}"/> <c:out value="${p.lastName}"/></td>
                            <td class="mono"><c:out value="${p.expectedKey}"/></td>
                            <td>
                                <c:choose>
                                    <c:when test="${p.state == 'FOUND'}">
                                        <span class="badge badge-found">Found</span>
                                    </c:when>
                                    <c:when test="${p.state == 'FOUND_WRONG_EMPLOYER'}">
                                        <span class="badge badge-wrong">Wrong employer</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge ${verdict == 'PARTICIPANTS_UNCONFIRMED' ? 'badge-missing' : 'bg-secondary'}">Missing</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td><c:out value="${p.userStatus}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:when>

            <c:when test="${outcome == 'NO_EXPORT_FOUND'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">No matching export file found</div>
                    <div class="text-muted">
                        No file matching the configured participant-export prefix was found in
                        Summit's export directory, or none had a parseable timestamp. Nothing was
                        compared.
                    </div>
                </div>
            </c:when>

            <c:when test="${outcome == 'MISSING_HEADERS'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">Required header(s) absent</div>
                    <div class="text-muted mb-2">
                        <c:if test="${not empty fileName}">
                            <c:out value="${fileName}"/> — <c:out value="${fileTimestampDisplay}"/><br/>
                        </c:if>
                        The export is missing one or more columns this compare requires:
                    </div>
                    <ul>
                        <c:forEach var="h" items="${missingHeaderNames}">
                            <li><c:out value="${h}"/></li>
                        </c:forEach>
                    </ul>
                </div>
            </c:when>

            <c:when test="${outcome == 'PREFIX_NOT_CONFIGURED'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">Summit TPA prefix not configured</div>
                    <div class="text-muted">
                        SUMMIT_TPA_ID_PREFIX is absent, blank, or contains a non-alphanumeric
                        character on this installation. No employer or participant key can be
                        composed, so nothing was compared.
                    </div>
                </div>
            </c:when>

            <c:when test="${outcome == 'EXPORT_DIR_NOT_CONFIGURED'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">Export directory not configured</div>
                    <div class="text-muted">
                        SUMMIT_SFTP_IMPORT_DIR does not resolve to an ImportFiles directory, so no
                        sibling ExportFiles directory could be derived.
                    </div>
                </div>
            </c:when>

            <c:when test="${outcome == 'PROSPECT_NOT_FOUND'}">
                <div class="status-card">
                    <div class="fw-semibold mb-1">Proposal has no prospect</div>
                    <div class="text-muted">
                        This proposal does not resolve to a prospect record. Nothing was compared.
                    </div>
                </div>
            </c:when>

            <c:otherwise>
                <div class="status-card">
                    <div class="fw-semibold mb-1">Could not reach the Summit export</div>
                    <div class="text-danger">
                        <c:out value="${errorMessage}"/>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>

    </div>
</div>
</body>
</html>
