<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Review Application — ${application.getProposal().getProspect().getName()}</title>
    <style>
        .status-badge { font-size: 0.85rem; }
        .section-card { border-left: 3px solid #2B5F8A; }
        .field-label { color: #6c757d; font-size: 0.85rem; margin-bottom: 2px; }
        .field-value { font-size: 0.95rem; margin-bottom: 12px; }
        .field-value.empty { color: #adb5bd; font-style: italic; }
        .plan-card { border-left: 3px solid #7AB648; }
        .tier-table th { font-size: 0.8rem; background-color: #f8f9fa; }
        .tier-table td { font-size: 0.9rem; }
        .action-card { position: sticky; top: 1rem; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:set var="pageTitle" value="Review Application" scope="request"/>
    <c:set var="pageIcon" value="bi-clipboard-check" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <%-- Subheader --%>
    <div class="hdr-bar mt-2 d-flex justify-content-between align-items-center">
        <span>
            <i class="bi bi-clipboard-check me-1"></i>
            Application Review &mdash; ${application.getProposal().getProspect().getName()}
            <small class="text-white-50 ms-2">Proposal #${application.getProposal().getId()}</small>
            <span class="ms-2">
                <c:choose>
                    <c:when test="${application.getStatus() == 'SUBMITTED'}">
                        <span class="badge bg-warning text-dark status-badge">Submitted</span>
                    </c:when>
                    <c:when test="${application.getStatus() == 'UNDER_REVIEW'}">
                        <span class="badge bg-info status-badge">Under Review</span>
                    </c:when>
                    <c:when test="${application.getStatus() == 'MORE_INFO'}">
                        <span class="badge bg-secondary status-badge">More Info Needed</span>
                    </c:when>
                    <c:when test="${application.getStatus() == 'APPROVED'}">
                        <span class="badge bg-success status-badge">Approved</span>
                    </c:when>
                    <c:when test="${application.getStatus() == 'DENIED'}">
                        <span class="badge bg-danger status-badge">Denied</span>
                    </c:when>
                    <c:otherwise>
                        <span class="badge bg-light text-dark status-badge">${application.getStatus()}</span>
                    </c:otherwise>
                </c:choose>
            </span>
        </span>
        <div class="d-flex gap-2">
            <a href="ReviewApplications" class="btn btn-sm btn-outline-light">
                <i class="bi bi-arrow-left me-1"></i>Back to List
            </a>
            <c:choose>
                <c:when test="${sessionScope.isAgent || sessionScope.isAgencyAdmin}">
                    <a href="AgentHome" class="btn btn-sm btn-outline-light">
                        <i class="bi bi-kanban me-1"></i>Pipeline
                    </a>
                </c:when>
                <c:otherwise>
                    <a href="ViewHome25" class="btn btn-sm btn-outline-light">
                        <i class="bi bi-house me-1"></i>Home
                    </a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- Summary Bar --%>
    <div class="card mb-3">
        <div class="card-body py-2">
            <div class="row align-items-center">
                <div class="col-md-4">
                    <small class="text-muted d-block">Contact</small>
                    <span class="fw-semibold">
                        ${application.getProposal().getProspect().getContact().getFirstName()}
                        ${application.getProposal().getProspect().getContact().getLastName()}
                    </span>
                    <c:if test="${application.getProposal().getProspect().getContact().getEmail() != null}">
                        <br><small class="text-muted">${application.getProposal().getProspect().getContact().getEmail()}</small>
                    </c:if>
                </div>
                <div class="col-md-4">
                    <small class="text-muted d-block">Lines of Service</small>
                    <c:forEach var="los" items="${application.getProposal().getLosList()}">
                        <span class="badge bg-primary me-1" style="font-size: 0.75rem;">${los.getDescription()}</span>
                    </c:forEach>
                </div>
                <div class="col-md-2">
                    <small class="text-muted d-block">Submitted</small>
                    <c:if test="${application.getDateSubmitted() != null}">
                        <fmt:formatDate value="${application.getDateSubmitted()}" pattern="MM/dd/yyyy"/>
                        <br><small class="text-muted"><fmt:formatDate value="${application.getDateSubmitted()}" pattern="h:mm a"/></small>
                    </c:if>
                </div>
                <div class="col-md-2">
                    <c:if test="${application.getDateReviewed() != null}">
                        <small class="text-muted d-block">Reviewed</small>
                        <fmt:formatDate value="${application.getDateReviewed()}" pattern="MM/dd/yyyy"/>
                        <br><small class="text-muted">by ${application.getReviewedBy().getFirstName()} ${application.getReviewedBy().getLastName()}</small>
                    </c:if>
                </div>
            </div>
        </div>
    </div>

    <%-- Setup Link (if approved) --%>
    <c:if test="${application.getSetup() != null}">
    <div class="alert alert-success py-2">
        <i class="bi bi-building-check me-1"></i>
        Setup <strong>#${application.getSetup().getId()}</strong> created —
        <em>${application.getSetup().getFullName()}</em>
        <c:if test="${application.getSetup().getDueDate() != null}">
            &middot; Due <fmt:formatDate value="${application.getSetup().getDueDate()}" pattern="MM/dd/yyyy"/>
        </c:if>
    </div>
    </c:if>

    <%-- Review Notes (if any) --%>
    <c:if test="${application.getReviewNotes() != null && not empty application.getReviewNotes()}">
    <div class="alert alert-info py-2">
        <i class="bi bi-chat-left-text me-1"></i>
        <strong>Review Notes:</strong> ${application.getReviewNotes()}
    </div>
    </c:if>

    <div class="row">
        <%-- Left Column: Application Data --%>
        <div class="col-lg-8">

            <%-- Sections with field values --%>
            <c:forEach var="section" items="${sections}">
                <%-- Check if this section has any values --%>
            <c:set var="sectionHasValues" value="false"/>
            <c:forEach var="field" items="${section.getFieldList()}">
                <c:if test="${valueMap.containsKey(field.getFieldKey()) && not empty valueMap[field.getFieldKey()]}">
                    <c:set var="sectionHasValues" value="true"/>
                </c:if>
            </c:forEach>

            <c:if test="${sectionHasValues == 'true'}">
            <div class="card mb-3 section-card">
                <div class="card-header bg-white py-2">
                    <h6 class="mb-0 fw-semibold">${section.getName()}</h6>
                    <c:if test="${section.getScope() == 'LOS'}">
                        <small class="text-muted">
                            <c:forEach var="los" items="${section.getLosList()}" varStatus="s">
                                ${los.getShortText()}<c:if test="${!s.last}">, </c:if>
                            </c:forEach>
                        </small>
                    </c:if>
                </div>
                <div class="card-body py-2">
                    <div class="row">
                        <c:forEach var="field" items="${section.getFieldList()}">
                        <c:set var="val" value="${valueMap[field.getFieldKey()]}"/>

                            <%-- Skip the JSON benefit plans field — rendered separately below --%>
                        <c:if test="${field.getFieldKey() != 'bill_benefit_plans' && val != null && not empty val}">

                            <%-- Pick column width based on field type --%>
                        <c:choose>
                        <c:when test="${field.getFieldType() == 'TEXTAREA'}">
                        <div class="col-12">
                            </c:when>
                            <c:otherwise>
                            <div class="col-md-6">
                                </c:otherwise>
                                </c:choose>

                                <div class="field-label">${field.getLabel()}</div>
                                <div class="field-value">
                                    <c:choose>
                                        <%-- BOOLEAN: show Yes/No --%>
                                        <c:when test="${field.getFieldType() == 'BOOLEAN'}">
                                            <c:choose>
                                                <c:when test="${val == 'true' || val == '1' || val == 'Yes' || val == 'checked'}">
                                                    <span class="text-success"><i class="bi bi-check-circle me-1"></i>Yes</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-danger"><i class="bi bi-x-circle me-1"></i>No</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:when>

                                        <%-- CHECKBOX: split pipe-delimited --%>
                                        <c:when test="${field.getFieldType() == 'CHECKBOX'}">
                                            <c:forEach var="cbVal" items="${fn:split(val, '|')}">
                                                <span class="badge bg-light text-dark border me-1 mb-1">${cbVal}</span>
                                            </c:forEach>
                                        </c:when>

                                        <%-- TEXTAREA: preserve line breaks --%>
                                        <c:when test="${field.getFieldType() == 'TEXTAREA'}">
                                            <div class="bg-light rounded p-2 small" style="white-space: pre-wrap;">${fn:escapeXml(val)}</div>
                                        </c:when>

                                        <%-- Default: plain text --%>
                                        <c:otherwise>
                                            ${fn:escapeXml(val)}
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                            </c:if>
                            </c:forEach>
                        </div>
                    </div>
                </div>
                </c:if>
                </c:forEach>

                <%-- Benefit Plans (JSON) --%>
                <div id="benefitPlansSection"></div>

            </div>

            <%-- Right Column: Actions --%>
            <div class="col-lg-4">
                <div class="card action-card">
                    <div class="card-header bg-white py-2">
                        <h6 class="mb-0 fw-semibold"><i class="bi bi-gear me-1"></i>Actions</h6>
                    </div>
                    <div class="card-body">
                        <c:choose>
                            <%-- Already approved — show setup link --%>
                            <c:when test="${application.getStatus() == 'APPROVED'}">
                                <div class="text-center text-success mb-2">
                                    <i class="bi bi-check-circle" style="font-size: 2rem;"></i>
                                    <p class="mb-0 fw-semibold">Approved</p>
                                </div>
                                <c:if test="${application.getSetup() != null}">
                                    <p class="text-muted small text-center">Setup #${application.getSetup().getId()} created</p>
                                </c:if>
                            </c:when>

                            <%-- Already denied --%>
                            <c:when test="${application.getStatus() == 'DENIED'}">
                                <div class="text-center text-danger mb-2">
                                    <i class="bi bi-x-circle" style="font-size: 2rem;"></i>
                                    <p class="mb-0 fw-semibold">Denied</p>
                                </div>
                            </c:when>

                            <%-- Actionable states: SUBMITTED, UNDER_REVIEW, MORE_INFO --%>
                            <c:otherwise>
                                <%-- Mark as Under Review (if still SUBMITTED) --%>
                                <c:if test="${application.getStatus() == 'SUBMITTED'}">
                                    <form method="post" action="ReviewApplication" class="mb-3">
                                        <input type="hidden" name="id" value="${application.getProposal().getId()}">
                                        <input type="hidden" name="action" value="under_review">
                                        <button type="submit" class="btn btn-info btn-sm w-100">
                                            <i class="bi bi-eye me-1"></i>Mark Under Review
                                        </button>
                                    </form>
                                </c:if>

                                <%-- Review Notes --%>
                                <div class="mb-3">
                                    <label for="reviewNotes" class="form-label small fw-semibold">Review Notes</label>
                                    <textarea class="form-control form-control-sm" id="reviewNotes" rows="3"
                                              placeholder="Optional notes about this decision...">${application.getReviewNotes()}</textarea>
                                </div>

                                <%-- Approve --%>
                                <form method="post" action="ReviewApplication" class="mb-2" id="approveForm"
                                      onsubmit="return confirmAction('approve this application and create a Setup activity')">
                                    <input type="hidden" name="id" value="${application.getProposal().getId()}">
                                    <input type="hidden" name="action" value="approve">
                                    <input type="hidden" name="reviewNotes" id="approveNotes">
                                    <button type="submit" class="btn btn-success w-100">
                                        <i class="bi bi-check-circle me-1"></i>Approve &amp; Create Setup
                                    </button>
                                </form>

                                <%-- Request More Info --%>
                                <form method="post" action="ReviewApplication" class="mb-2" id="moreInfoForm">
                                    <input type="hidden" name="id" value="${application.getProposal().getId()}">
                                    <input type="hidden" name="action" value="more_info">
                                    <input type="hidden" name="reviewNotes" id="moreInfoNotes">
                                    <button type="submit" class="btn btn-outline-secondary w-100">
                                        <i class="bi bi-question-circle me-1"></i>Request More Info
                                    </button>
                                </form>

                                <%-- Deny --%>
                                <form method="post" action="ReviewApplication" id="denyForm"
                                      onsubmit="return confirmAction('deny this application')">
                                    <input type="hidden" name="id" value="${application.getProposal().getId()}">
                                    <input type="hidden" name="action" value="deny">
                                    <input type="hidden" name="reviewNotes" id="denyNotes">
                                    <button type="submit" class="btn btn-outline-danger w-100">
                                        <i class="bi bi-x-circle me-1"></i>Deny
                                    </button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>

    </div>

    <script>
        // Copy review notes into whichever form is submitted
        function confirmAction(actionDescription) {
            var notes = document.getElementById('reviewNotes').value;
            document.getElementById('approveNotes').value = notes;
            document.getElementById('denyNotes').value = notes;
            document.getElementById('moreInfoNotes').value = notes;
            return confirm('Are you sure you want to ' + actionDescription + '?');
        }

        // Also sync notes for non-confirm forms
        document.getElementById('moreInfoForm')?.addEventListener('submit', function() {
            document.getElementById('moreInfoNotes').value = document.getElementById('reviewNotes').value;
        });

        // Render benefit plans from JSON
        (function() {
            var plansJson = '${fn:replace(fn:replace(plansJson, "'", "\\'"), newline, "")}';
            var container = document.getElementById('benefitPlansSection');
            var plans;
            try {
                plans = JSON.parse(plansJson);
            } catch(e) {
                plans = [];
            }
            if (!plans || plans.length === 0) return;

            var downloadUrls = {};
            <c:forEach var="entry" items="${downloadUrls}">
            downloadUrls['${fn:escapeXml(entry.key)}'] = '${fn:escapeXml(entry.value)}';
            </c:forEach>

            var html = '<div class="card mb-3 plan-card">';
            html += '<div class="card-header bg-white py-2">';
            html += '<h6 class="mb-0 fw-semibold"><i class="bi bi-bar-chart-steps me-1"></i>Benefit Plans (' + plans.length + ')</h6>';
            html += '</div>';
            html += '<div class="card-body py-2">';

            plans.forEach(function(plan, idx) {
                html += '<div class="border rounded p-3 mb-2">';

                // Plan header
                html += '<div class="d-flex justify-content-between align-items-start mb-2">';
                html += '<div>';
                html += '<strong>' + escHtml(plan.planName || ('Plan ' + (idx + 1))) + '</strong>';
                if (plan.benefitTypeName) {
                    html += ' <span class="badge bg-light text-dark border ms-1">' + escHtml(plan.benefitTypeName) + '</span>';
                }
                if (plan.billingTypeName) {
                    html += ' <span class="badge bg-light text-dark border ms-1">' + escHtml(plan.billingTypeName) + '</span>';
                }
                html += '</div>';

                // Rate sheet download
                if (plan.storageKey && downloadUrls[plan.storageKey]) {
                    html += '<a href="' + downloadUrls[plan.storageKey] + '" target="_blank" class="btn btn-sm btn-outline-success">';
                    html += '<i class="bi bi-file-earmark-arrow-down me-1"></i>' + escHtml(plan.fileName || 'Rate Sheet');
                    html += '</a>';
                }
                html += '</div>';

                // Dates
                if (plan.effectiveDate || plan.renewalDate) {
                    html += '<div class="row mb-2">';
                    if (plan.effectiveDate) {
                        html += '<div class="col-md-6"><span class="field-label">Effective Date</span><br>' + escHtml(plan.effectiveDate) + '</div>';
                    }
                    if (plan.renewalDate) {
                        html += '<div class="col-md-6"><span class="field-label">Renewal Date</span><br>' + escHtml(plan.renewalDate) + '</div>';
                    }
                    html += '</div>';
                }

                // Tiers
                if (plan.tiers && plan.tiers.length > 0) {
                    html += '<table class="table table-sm tier-table mb-1">';
                    html += '<thead><tr><th>Tier</th><th class="text-end">Rate</th></tr></thead>';
                    html += '<tbody>';
                    plan.tiers.forEach(function(tier) {
                        var tierName = tier.name || tier.label || tier;
                        var tierRate = tier.rate || tier.amount || tier.value || '';
                        if (typeof tier === 'string') {
                            html += '<tr><td>' + escHtml(tier) + '</td><td></td></tr>';
                        } else {
                            html += '<tr><td>' + escHtml(tierName) + '</td><td class="text-end">' + escHtml(tierRate) + '</td></tr>';
                        }
                    });
                    html += '</tbody></table>';
                }

                // Flat rate
                if (plan.flatRate) {
                    html += '<div class="mb-1"><span class="field-label">Flat Rate</span><br>' + escHtml(plan.flatRate) + '</div>';
                }

                // Notes
                if (plan.notes) {
                    html += '<div class="bg-light rounded p-2 small mt-1" style="white-space: pre-wrap;">' + escHtml(plan.notes) + '</div>';
                }

                html += '</div>';
            });

            html += '</div></div>';
            container.innerHTML = html;
        })();

        function escHtml(str) {
            if (!str) return '';
            var div = document.createElement('div');
            div.appendChild(document.createTextNode(str.toString()));
            return div.innerHTML;
        }
    </script>
</body>
</html>
