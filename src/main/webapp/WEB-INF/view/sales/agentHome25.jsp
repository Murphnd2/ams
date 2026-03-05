<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Agent Pipeline</title>
    <style>
        :root { --ssa: #0d5681; }

        /* ── Stat strip ── */
        .stat-strip { display: flex; gap: 0.5rem; padding: 0.4rem 1rem; background: #fff; border-bottom: 1px solid #dee2e6; flex-wrap: wrap; }
        .stat-pill { font-size: 0.75rem; padding: 0.2rem 0.65rem; border-radius: 12px; font-weight: 600; white-space: nowrap; }
        .stat-pill .stat-val { font-weight: 800; }

        /* ── Board ── */
        .board-wrap {
            display: flex; flex-direction: row; gap: 0.75rem;
            overflow-x: auto; overflow-y: hidden;
            padding: 0.75rem 1rem;
            background: #eef1f5;
            flex: 1; min-height: 0;
        }

        /* ── Kanban column ── */
        .kanban-col {
            min-width: 240px; max-width: 280px; flex: 0 0 260px;
            display: flex; flex-direction: column;
            background: #fff; border-radius: 8px;
            border: 1px solid #e2e6ea;
            max-height: 100%;
        }
        .kanban-col-header {
            padding: 0.45rem 0.65rem; font-size: 0.78rem; font-weight: 700;
            border-bottom: 1px solid #e2e6ea; display: flex;
            justify-content: space-between; align-items: center;
            border-radius: 8px 8px 0 0; flex-shrink: 0;
        }
        .kanban-col-header .count-badge {
            font-size: 0.65rem; font-weight: 800; padding: 0.1rem 0.45rem;
            border-radius: 10px; background: rgba(0,0,0,0.12); color: inherit;
        }
        .kanban-col-body { overflow-y: auto; flex: 1; padding: 0.4rem; min-height: 0; }
        .kanban-col-footer { padding: 0.3rem 0.5rem; border-top: 1px solid #e2e6ea; flex-shrink: 0; }
        .kanban-col-footer button {
            font-size: 0.72rem; color: #888; background: none; border: none;
            cursor: pointer; width: 100%; text-align: left; padding: 0.15rem 0.25rem; border-radius: 4px;
        }
        .kanban-col-footer button:hover { background: #f0f0f0; color: #333; }

        /* ── Stage colors ── */
        .stage-c-NEW           { --sc:#2563eb; --sb:#dbeafe; }
        .stage-c-CONTACTED     { --sc:#059669; --sb:#d1fae5; }
        .stage-c-QUALIFIED     { --sc:#d97706; --sb:#fef3c7; }
        .stage-c-PROPOSAL_SENT { --sc:#7c3aed; --sb:#ede9fe; }
        .stage-c-NEGOTIATION   { --sc:#db2777; --sb:#fce7f3; }
        .stage-c-ON_HOLD       { --sc:#64748b; --sb:#f1f5f9; }
        .stage-c-WON           { --sc:#16a34a; --sb:#dcfce7; }
        .stage-c-LOST          { --sc:#dc2626; --sb:#fee2e2; }
        .kanban-col-header { background: var(--sb); color: var(--sc); }

        /* ── Cards ── */
        .kb-card {
            background: #fff; border-radius: 7px; border: 1px solid #e2e6ea;
            padding: 0.5rem 0.6rem; margin-bottom: 0.4rem; cursor: pointer;
            box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: all 0.15s;
        }
        .kb-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.12); transform: translateY(-1px); }
        .kb-card.selected { border-color: var(--ssa); box-shadow: 0 0 0 2px #e8f0f7; }
        .kb-card-name { font-size: 0.8rem; font-weight: 600; line-height: 1.2; margin-bottom: 0.15rem; text-transform: capitalize; }
        .kb-card-agent { font-size: 0.67rem; color: #888; }
        .kb-card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 0.3rem; font-size: 0.68rem; color: #6c757d; }

        /* ── Drawer ── */
        .detail-drawer {
            position: fixed; top: 0; right: 0; width: 380px; height: 100vh;
            background: #fff; z-index: 200; display: flex; flex-direction: column;
            box-shadow: -4px 0 20px rgba(0,0,0,0.15);
            transform: translateX(100%); transition: transform 0.25s ease;
        }
        .detail-drawer.open { transform: translateX(0); }
        .drawer-header {
            background: var(--ssa); color: #fff; padding: 0.6rem 0.8rem;
            display: flex; align-items: center; justify-content: space-between; flex-shrink: 0;
        }
        .drawer-header h5 { font-size: 0.9rem; margin: 0; font-weight: 600; }
        .drawer-header .stage-pill {
            font-size: 0.65rem; font-weight: 700; padding: 0.12rem 0.45rem;
            border-radius: 4px; text-transform: uppercase;
        }
        .drawer-close { background: none; border: none; color: rgba(255,255,255,0.7); font-size: 1.1rem; cursor: pointer; padding: 0; line-height: 1; }
        .drawer-close:hover { color: #fff; }
        .drawer-body { flex: 1; overflow-y: auto; padding: 0.65rem 0.8rem; font-size: 0.82rem; }
        .drawer-section-title { font-size: 0.7rem; font-weight: 700; text-transform: uppercase; color: #888; margin: 0.7rem 0 0.3rem; letter-spacing: 0.03em; }
        .drawer-section-title:first-child { margin-top: 0; }
        .drawer-field { display: flex; align-items: baseline; padding: 0.2rem 0; }
        .drawer-field-label { min-width: 80px; color: #888; font-size: 0.78rem; flex-shrink: 0; }
        .drawer-field-value { font-size: 0.82rem; }
        .drawer-footer {
            border-top: 1px solid #dee2e6; padding: 0.5rem 0.8rem; flex-shrink: 0;
            display: flex; flex-direction: column; gap: 0.3rem;
        }
        .drawer-footer .btn { font-size: 0.75rem; }

        /* ── Inline editable fields ── */
        .inline-edit { display: flex; align-items: center; gap: 0.35rem; }
        .inline-edit input, .inline-edit select {
            font-size: 0.8rem; padding: 0.15rem 0.35rem; border: 1px solid #ced4da;
            border-radius: 4px; height: 26px;
        }
        .inline-edit input[type=number] { width: 90px; }
        .inline-edit input[type=date] { width: 130px; }
        .inline-edit .save-status {
            font-size: 0.7rem; min-width: 50px; transition: opacity 0.3s;
        }
        .save-status .spinner-border { width: 0.75rem; height: 0.75rem; border-width: 0.12em; }

        /* ── Drawer proposal rows ── */
        .drawer-prop-row {
            display: flex; align-items: center; gap: 0.35rem;
            font-size: 0.78rem; padding: 0.35rem 0.45rem; border-radius: 5px;
            border: 1px solid #e2e6ea; background: #fff; margin-bottom: 0.3rem;
            text-decoration: none; color: #333;
        }
        .drawer-prop-row:hover { background: #f0f6fc; border-color: #b0c4d8; }
        .drawer-prop-id { font-weight: 700; color: var(--ssa); white-space: nowrap; }
        .drawer-prop-los { font-size: 0.68rem; color: #888; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .drawer-prop-linked { font-size: 0.6rem; color: var(--ssa); }
        .drawer-new-prop {
            display: flex; align-items: center; justify-content: center; gap: 0.3rem;
            font-size: 0.75rem; padding: 0.3rem 0.5rem; border-radius: 5px;
            border: 1px dashed #b0c4d8; background: #f8fbfe; margin-bottom: 0.4rem;
            text-decoration: none; color: var(--ssa); font-weight: 600;
        }
        .drawer-new-prop:hover { background: #e8f0f7; border-color: var(--ssa); }

        /* ── Full page flex ── */
        .agent-page-wrap {
            display: flex; flex-direction: column; height: calc(100vh - 56px); overflow: hidden;
        }

        /* ── Overlay ── */
        .drawer-overlay {
            display: none; position: fixed; inset: 0; z-index: 199; background: rgba(0,0,0,0.15);
        }
        .drawer-overlay.show { display: block; }
    </style>
</head>
<body>
<div class="container-fluid p-0">
    <%-- Navbar --%>
    <c:set var="pageTitle" value="Agent Pipeline" scope="request"/>
    <c:set var="pageIcon" value="bi-kanban" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <c:choose>
        <c:when test="${empty agency}">
            <div class="alert alert-warning mt-4 mx-3">
                <i class="bi bi-exclamation-triangle me-2"></i>Your account is not associated with an agency. Please contact your administrator.
            </div>
        </c:when>
        <c:otherwise>
            <div class="agent-page-wrap">

                <%-- ══ Hdr-bar ══ --%>
                <div class="hdr-bar d-flex justify-content-between align-items-center">
                    <span>
                        <i class="bi bi-kanban me-1"></i>Agent Pipeline
                        <small class="text-white-50 ms-2">${fn:escapeXml(agency.getName())}</small>
                    </span>
                    <div class="d-flex gap-2">
                        <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#newOppModal">
                            <i class="bi bi-plus-circle me-1"></i>New Opportunity
                        </button>
                        <a href="ReviewApplications" class="btn btn-sm btn-outline-light">
                            <i class="bi bi-clipboard-check me-1"></i>Applications
                        </a>
                    </div>
                </div>

                <%-- ══ Stat strip ══ --%>
                <%-- compute proposal-out and negotiation counts --%>
                <c:set var="proposalOutCount" value="0"/>
                <c:set var="negotiationCount" value="0"/>
                <c:forEach var="opp" items="${opportunities}">
                    <c:if test="${opp.getStage() == 'PROPOSAL_SENT'}"><c:set var="proposalOutCount" value="${proposalOutCount + 1}"/></c:if>
                    <c:if test="${opp.getStage() == 'NEGOTIATION'}"><c:set var="negotiationCount" value="${negotiationCount + 1}"/></c:if>
                </c:forEach>
                <div class="stat-strip">
                    <span class="stat-pill" style="background:#dbeafe; color:#2563eb;">Active <span class="stat-val">${activeCount}</span></span>
                    <span class="stat-pill" style="background:#ede9fe; color:#7c3aed;">Proposal Out <span class="stat-val">${proposalOutCount}</span></span>
                    <span class="stat-pill" style="background:#fce7f3; color:#db2777;">Negotiation <span class="stat-val">${negotiationCount}</span></span>
                    <span class="stat-pill" style="background:#dcfce7; color:#16a34a;">Won <span class="stat-val">${wonCount}</span></span>
                    <span class="stat-pill" style="background:#e0f2fe; color:#0369a1;">Pipeline Value <span class="stat-val">$<fmt:formatNumber value="${pipelineValue}" pattern="#,##0"/></span></span>
                </div>

                <%-- ══ Kanban board ══ --%>
                <div class="board-wrap">
                    <c:set var="boardStages" value="NEW,CONTACTED,QUALIFIED,PROPOSAL_SENT,NEGOTIATION,ON_HOLD"/>
                    <c:forTokens var="stage" items="${boardStages}" delims=",">
                        <c:set var="stageOpps" value="${pipelineMap[stage]}"/>
                        <c:set var="stageCount" value="${fn:length(stageOpps)}"/>
                        <div class="kanban-col stage-c-${stage}">
                            <div class="kanban-col-header">
                                <span>${fn:replace(stage, '_', ' ')}</span>
                                <span class="count-badge">${stageCount}</span>
                            </div>
                            <div class="kanban-col-body">
                                <c:forEach var="opp" items="${stageOpps}">
                                    <div class="kb-card" data-opp-id="${opp.getId()}" onclick="openDrawer(${opp.getId()}, this)">
                                        <div class="kb-card-name">${fn:escapeXml(fn:toLowerCase(opp.getFullName()))}</div>
                                        <c:if test="${sessionScope.isAgencyAdmin && opp.getAssignedTo() != null}">
                                            <div class="kb-card-agent">${opp.getAssignedTo().getFirstName()} ${opp.getAssignedTo().getLastName()}</div>
                                        </c:if>
                                        <div class="kb-card-footer">
                                            <span>
                                                <c:if test="${opp.getEstimatedValue() != null}">
                                                    $<fmt:formatNumber value="${opp.getEstimatedValue()}" pattern="#,##0"/>
                                                </c:if>
                                            </span>
                                            <span>
                                                <c:if test="${opp.getEstimatedEmployees() != null}">
                                                    <i class="bi bi-people"></i> ${opp.getEstimatedEmployees()}
                                                </c:if>
                                            </span>
                                        </div>
                                    </div>
                                </c:forEach>
                                <c:if test="${stageCount == 0}">
                                    <div class="text-muted text-center py-3" style="font-size:0.75rem;">No opportunities</div>
                                </c:if>
                            </div>
                            <div class="kanban-col-footer">
                                <button onclick="newOppWithStage('${stage}')"><i class="bi bi-plus me-1"></i>Add</button>
                            </div>
                        </div>
                    </c:forTokens>
                </div>
            </div>

            <%-- ══ Drawer overlay ══ --%>
            <div class="drawer-overlay" id="drawerOverlay" onclick="closeDrawer()"></div>

            <%-- ══ Detail Drawer ══ --%>
            <div class="detail-drawer" id="detailDrawer">
                <div class="drawer-header">
                    <div style="min-width:0; flex:1;">
                        <h5 id="drName" class="text-truncate"></h5>
                        <span class="stage-pill" id="drStagePill"></span>
                    </div>
                    <button class="drawer-close" onclick="closeDrawer()"><i class="bi bi-x-lg"></i></button>
                </div>
                <div class="drawer-body">
                    <%-- Details --%>
                    <div class="drawer-section-title">Details</div>
                    <div class="drawer-field"><span class="drawer-field-label">Prospect</span><span class="drawer-field-value" id="drProspect"></span></div>
                    <div class="drawer-field"><span class="drawer-field-label">Contact</span><span class="drawer-field-value" id="drContact"></span></div>
                    <div class="drawer-field"><span class="drawer-field-label">Agency</span><span class="drawer-field-value" id="drAgency"></span></div>
                    <div class="drawer-field"><span class="drawer-field-label">Managed By</span><span class="drawer-field-value" id="drManagedBy"></span></div>

                    <%-- Pipeline Data --%>
                    <div class="drawer-section-title">Pipeline Data</div>
                    <div class="drawer-field">
                        <span class="drawer-field-label">Employees</span>
                        <div class="inline-edit">
                            <input type="number" min="1" id="drEEs" placeholder="—" onchange="saveField('estimatedEmployees', this.value, this)">
                            <span class="save-status" id="statusEEs"></span>
                        </div>
                    </div>
                    <div class="drawer-field">
                        <span class="drawer-field-label">Est. Value</span>
                        <div class="inline-edit">
                            <span style="font-size:0.8rem; color:#666;">$</span>
                            <input type="number" min="0" step="0.01" id="drValue" placeholder="—" onchange="saveField('estimatedValue', this.value, this)">
                            <span class="save-status" id="statusValue"></span>
                        </div>
                    </div>
                    <div class="drawer-field">
                        <span class="drawer-field-label">Close Date</span>
                        <div class="inline-edit">
                            <input type="date" id="drCloseDate" onchange="saveField('expectedCloseDate', this.value, this)">
                            <span class="save-status" id="statusCloseDate"></span>
                        </div>
                    </div>

                    <%-- Stage --%>
                    <div class="drawer-section-title">Stage</div>
                    <div class="inline-edit">
                        <select id="drStageSelect" onchange="saveStage(this.value)">
                            <option value="NEW">New</option>
                            <option value="CONTACTED">Contacted</option>
                            <option value="QUALIFIED">Qualified</option>
                            <option value="PROPOSAL_SENT">Proposal Sent</option>
                            <option value="NEGOTIATION">Negotiation</option>
                            <option value="ON_HOLD">On Hold</option>
                            <option value="WON">Won</option>
                            <option value="LOST">Lost</option>
                        </select>
                        <span class="save-status" id="statusStage"></span>
                    </div>

                    <%-- Proposals --%>
                    <div class="drawer-section-title">Proposals</div>
                    <div id="drProposals"></div>
                </div>
                <div class="drawer-footer">
                    <a id="drFullDetail" href="#" class="btn btn-sm btn-primary w-100">
                        <i class="bi bi-box-arrow-up-right me-1"></i>Full Detail View
                    </a>
                    <div class="d-flex gap-2">
                        <a id="drEmail" href="#" class="btn btn-sm btn-outline-secondary flex-fill">
                            <i class="bi bi-envelope me-1"></i>Email
                        </a>
                        <a id="drLogNote" href="#" class="btn btn-sm btn-outline-secondary flex-fill">
                            <i class="bi bi-chat-left-text me-1"></i>Log Note
                        </a>
                        <button class="btn btn-sm btn-outline-danger flex-fill" onclick="markLost()">
                            <i class="bi bi-x-circle me-1"></i>Lost
                        </button>
                    </div>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%-- ═══════════════════════════════════════════════════════════════ --%>
<%-- NEW OPPORTUNITY MODAL                                          --%>
<%-- ═══════════════════════════════════════════════════════════════ --%>
<div class="modal fade" id="newOppModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="CreateOpportunity" id="newOppForm">
                <input type="hidden" name="agencyId" value="${agency != null ? agency.getId() : ''}" />
                <input type="hidden" name="prospectMode" id="prospectMode" value="new" />
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>New Opportunity</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                </div>
                <div class="modal-body">
                    <%-- Prospect Mode Toggle --%>
                    <div class="btn-group w-100 mb-3" role="group">
                        <input type="radio" class="btn-check" name="prospectToggle" id="togNew" autocomplete="off" checked onclick="showProspectMode('new')">
                        <label class="btn btn-outline-primary" for="togNew"><i class="bi bi-plus-circle me-1"></i>New Prospect</label>
                        <input type="radio" class="btn-check" name="prospectToggle" id="togExisting" autocomplete="off" onclick="showProspectMode('existing')">
                        <label class="btn btn-outline-primary" for="togExisting"><i class="bi bi-search me-1"></i>Existing Prospect</label>
                    </div>

                    <%-- NEW PROSPECT FIELDS --%>
                    <div id="newProspectFields">
                        <div class="mb-2">
                            <label class="form-label fw-semibold mb-1">Company Name <span class="text-danger">*</span></label>
                            <input type="text" name="companyName" class="form-control form-control-sm" id="companyName" placeholder="e.g. Acme Corp">
                        </div>
                        <div class="row mb-2">
                            <div class="col-6">
                                <label class="form-label fw-semibold mb-1">Contact First Name</label>
                                <input type="text" name="contactFirst" class="form-control form-control-sm" placeholder="First">
                            </div>
                            <div class="col-6">
                                <label class="form-label fw-semibold mb-1">Contact Last Name</label>
                                <input type="text" name="contactLast" class="form-control form-control-sm" placeholder="Last">
                            </div>
                        </div>
                        <div class="mb-2">
                            <label class="form-label fw-semibold mb-1">Contact Email</label>
                            <input type="email" name="contactEmail" class="form-control form-control-sm" placeholder="email@example.com">
                        </div>
                    </div>

                    <%-- EXISTING PROSPECT DROPDOWN --%>
                    <div id="existingProspectFields" style="display: none;">
                        <div class="mb-2">
                            <label class="form-label fw-semibold mb-1">Select Prospect</label>
                            <select name="prospectId" class="form-select form-select-sm" id="prospectSelect">
                                <option value="">-- Select Prospect --</option>
                                <c:forEach var="p" items="${prospects}">
                                    <option value="${p.getId()}">${fn:escapeXml(p.getName())}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>

                    <%-- Optional pipeline fields --%>
                    <hr class="my-2">
                    <div class="text-muted" style="font-size:0.75rem; margin-bottom:0.4rem;">Optional</div>
                    <div class="row mb-2">
                        <div class="col-4">
                            <label class="form-label fw-semibold mb-1" style="font-size:0.8rem;">Est. Employees</label>
                            <input type="number" name="estimatedEmployees" class="form-control form-control-sm" min="1" placeholder="—">
                        </div>
                        <div class="col-4">
                            <label class="form-label fw-semibold mb-1" style="font-size:0.8rem;">Est. Value ($)</label>
                            <input type="number" name="estimatedValue" class="form-control form-control-sm" min="0" step="0.01" placeholder="—">
                        </div>
                        <div class="col-4">
                            <label class="form-label fw-semibold mb-1" style="font-size:0.8rem;">Expected Close</label>
                            <input type="date" name="expectedCloseDate" class="form-control form-control-sm">
                        </div>
                    </div>
                </div>
                <div class="modal-footer justify-content-center border-0">
                    <button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Create</button>
                    <span class="ssa-action-sep">|</span>
                    <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- ═══════════════════════════════════════════════════════════════ --%>
<%-- OPPORTUNITY DATA MAP (for drawer)                              --%>
<%-- ═══════════════════════════════════════════════════════════════ --%>
<script>
const OPPS = {
    <c:forEach var="opp" items="${opportunities}" varStatus="s">
    ${opp.getId()}: {
        id: ${opp.getId()},
        name: '${fn:replace(fn:replace(fn:escapeXml(opp.getFullName()), "\\", "\\\\"), "'", "\\'")}',
        stage: '${opp.getStage()}',
        agent: '${opp.getAssignedTo() != null ? fn:escapeXml(opp.getAssignedTo().getFirstName()) : ""}${opp.getAssignedTo() != null ? " " : ""}${opp.getAssignedTo() != null ? fn:escapeXml(opp.getAssignedTo().getLastName()) : ""}',
        agentId: ${opp.getAssignedTo() != null ? opp.getAssignedTo().getId() : 0},
        contact: '${opp.getPrimaryContact() != null ? fn:escapeXml(opp.getPrimaryContact().getFirstName()) : ""}${opp.getPrimaryContact() != null ? " " : ""}${opp.getPrimaryContact() != null ? fn:escapeXml(opp.getPrimaryContact().getLastName()) : ""}',
        email: '${opp.getPrimaryContact() != null && opp.getPrimaryContact().getEmail() != null ? opp.getPrimaryContact().getEmail() : ""}',
        agency: '${opp.getAgency() != null ? fn:replace(fn:escapeXml(opp.getAgency().getName()), "'", "\\'") : ""}',
        managedBy: '${opp.getManagedBy() != null ? fn:escapeXml(opp.getManagedBy().getFirstName()) : ""}${opp.getManagedBy() != null ? " " : ""}${opp.getManagedBy() != null ? fn:escapeXml(opp.getManagedBy().getLastName()) : ""}',
        managedById: ${opp.getManagedBy() != null ? opp.getManagedBy().getId() : 0},
        estimatedEEs: <c:choose><c:when test="${opp.getEstimatedEmployees() != null}">${opp.getEstimatedEmployees()}</c:when><c:otherwise>null</c:otherwise></c:choose>,
        estimatedValue: <c:choose><c:when test="${opp.getEstimatedValue() != null}">${opp.getEstimatedValue()}</c:when><c:otherwise>null</c:otherwise></c:choose>,
        closeDate: '<c:if test="${opp.getExpectedCloseDate() != null}"><fmt:formatDate value="${opp.getExpectedCloseDate()}" pattern="yyyy-MM-dd"/></c:if>',
        prospectId: ${opp.getProspect() != null ? opp.getProspect().getId() : 0},
        proposals: [
            <c:if test="${opp.getProspect() != null && opp.getProspect().getProposalList() != null}">
            <c:forEach var="prop" items="${opp.getProspect().getProposalList()}" varStatus="ps">
            { id: ${prop.getId()}, status: '${prop.getStatus()}',
              los: '<c:forEach var="los" items="${prop.getLosList()}" varStatus="ls">${fn:escapeXml(los.getShortText())}<c:if test="${!ls.last}">, </c:if></c:forEach>',
              sourceId: ${prop.getSourceActivity() != null ? prop.getSourceActivity().getId() : 0}
            }<c:if test="${!ps.last}">,</c:if>
            </c:forEach>
            </c:if>
        ]
    }<c:if test="${!s.last}">,</c:if>
    </c:forEach>
};

/* ── Stage color map ── */
const STAGE_COLORS = {
    'NEW':          { sc:'#2563eb', sb:'#dbeafe' },
    'CONTACTED':    { sc:'#059669', sb:'#d1fae5' },
    'QUALIFIED':    { sc:'#d97706', sb:'#fef3c7' },
    'PROPOSAL_SENT':{ sc:'#7c3aed', sb:'#ede9fe' },
    'NEGOTIATION':  { sc:'#db2777', sb:'#fce7f3' },
    'ON_HOLD':      { sc:'#64748b', sb:'#f1f5f9' },
    'WON':          { sc:'#16a34a', sb:'#dcfce7' },
    'LOST':         { sc:'#dc2626', sb:'#fee2e2' }
};
const STAGE_LABELS = {
    'NEW':'New','CONTACTED':'Contacted','QUALIFIED':'Qualified',
    'PROPOSAL_SENT':'Proposal Sent','NEGOTIATION':'Negotiation',
    'ON_HOLD':'On Hold','WON':'Won','LOST':'Lost'
};

let selectedOppId = null;
let selectedCardEl = null;

/* ═══ Drawer open/close ═══ */
function openDrawer(oppId, cardEl) {
    // Deselect old card
    if (selectedCardEl) selectedCardEl.classList.remove('selected');
    // Select new
    cardEl.classList.add('selected');
    selectedCardEl = cardEl;
    selectedOppId = oppId;

    const opp = OPPS[oppId];
    if (!opp) return;

    // Header
    document.getElementById('drName').textContent = opp.name.toLowerCase();
    document.getElementById('drName').style.textTransform = 'capitalize';
    const pill = document.getElementById('drStagePill');
    const sc = STAGE_COLORS[opp.stage] || { sc:'#666', sb:'#eee' };
    pill.textContent = STAGE_LABELS[opp.stage] || opp.stage;
    pill.style.background = sc.sb;
    pill.style.color = sc.sc;

    // Details
    document.getElementById('drProspect').textContent = opp.name || '\u2014';
    const contactHtml = opp.contact ? opp.contact : '\u2014';
    const emailHtml = opp.email ? ' <a href="mailto:' + opp.email + '" style="font-size:0.75rem;">' + opp.email + '</a>' : '';
    document.getElementById('drContact').innerHTML = contactHtml + emailHtml;
    document.getElementById('drAgency').textContent = opp.agency || '\u2014';
    document.getElementById('drManagedBy').textContent = opp.managedBy.trim() || 'None';

    // Pipeline data
    document.getElementById('drEEs').value = opp.estimatedEEs != null ? opp.estimatedEEs : '';
    document.getElementById('drValue').value = opp.estimatedValue != null ? opp.estimatedValue : '';
    document.getElementById('drCloseDate').value = opp.closeDate || '';
    // Clear save statuses
    document.querySelectorAll('.save-status').forEach(function(el) { el.innerHTML = ''; });

    // Stage
    document.getElementById('drStageSelect').value = opp.stage;

    // Proposals — stacked list, newest first, + New on top
    let propHtml = '';
    if (opp.prospectId) {
        propHtml += '<a href="ProposalBuilder?prospectId=' + opp.prospectId + '&sourceActivityId=' + oppId
            + '" class="drawer-new-prop"><i class="bi bi-plus-circle me-1"></i>New Proposal</a>';
    }
    if (opp.proposals && opp.proposals.length > 0) {
        // Sort by id descending (most recent first)
        var sorted = opp.proposals.slice().sort(function(a, b) { return b.id - a.id; });
        sorted.forEach(function(p) {
            var linkedTag = (p.sourceId === oppId)
                ? '<span class="drawer-prop-linked"><i class="bi bi-link-45deg"></i></span>' : '';
            propHtml += '<a href="ProposalDetail?id=' + p.id + '" class="drawer-prop-row">'
                + '<span class="drawer-prop-id">#' + p.id + '</span>'
                + '<span class="badge bg-secondary" style="font-size:0.6rem;">' + p.status + '</span>'
                + '<span class="drawer-prop-los">' + (p.los || '') + '</span>'
                + linkedTag
                + '</a>';
        });
    } else if (!opp.prospectId) {
        propHtml = '<span class="text-muted" style="font-size:0.78rem;">No proposals yet</span>';
    }
    document.getElementById('drProposals').innerHTML = propHtml;

    // Footer links
    document.getElementById('drFullDetail').href = 'ViewById?id=' + oppId;
    document.getElementById('drEmail').href = 'CreateEmail25?activityId=' + oppId;
    document.getElementById('drLogNote').href = 'ViewById?id=' + oppId;

    // Open
    document.getElementById('detailDrawer').classList.add('open');
    document.getElementById('drawerOverlay').classList.add('show');
}

function closeDrawer() {
    document.getElementById('detailDrawer').classList.remove('open');
    document.getElementById('drawerOverlay').classList.remove('show');
    if (selectedCardEl) selectedCardEl.classList.remove('selected');
    selectedCardEl = null;
    selectedOppId = null;
}

/* ═══ AJAX: save individual field ═══ */
function saveField(fieldName, value, inputEl) {
    if (!selectedOppId) return;

    // Map field name to status element
    const statusMap = { estimatedEmployees: 'statusEEs', estimatedValue: 'statusValue', expectedCloseDate: 'statusCloseDate' };
    const statusEl = document.getElementById(statusMap[fieldName]);
    statusEl.innerHTML = '<span class="spinner-border spinner-border-sm text-muted"></span>';

    const params = new URLSearchParams();
    params.append('oppId', selectedOppId);
    params.append(fieldName, value);
    params.append('ajax', 'true');

    fetch('UpdateOpportunityStage', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: params.toString() })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.ok) {
                statusEl.innerHTML = '<span class="text-success">\u2713 Saved</span>';
                setTimeout(function() { statusEl.innerHTML = ''; }, 2000);

                // Update local JS data
                const opp = OPPS[selectedOppId];
                if (fieldName === 'estimatedEmployees') opp.estimatedEEs = value ? parseInt(value) : null;
                if (fieldName === 'estimatedValue') opp.estimatedValue = value ? parseFloat(value) : null;
                if (fieldName === 'expectedCloseDate') opp.closeDate = value || '';

                // Update card footer if visible
                updateCardFooter(selectedOppId);
            } else {
                statusEl.innerHTML = '<span class="text-danger">Error</span>';
            }
        })
        .catch(function() {
            statusEl.innerHTML = '<span class="text-danger">Error</span>';
        });
}

/* ═══ AJAX: save stage ═══ */
function saveStage(newStage) {
    if (!selectedOppId) return;
    const statusEl = document.getElementById('statusStage');
    statusEl.innerHTML = '<span class="spinner-border spinner-border-sm text-muted"></span>';

    const params = new URLSearchParams();
    params.append('oppId', selectedOppId);
    params.append('stage', newStage);
    params.append('ajax', 'true');

    fetch('UpdateOpportunityStage', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: params.toString() })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.ok) {
                statusEl.innerHTML = '<span class="text-success">\u2713 Saved</span>';
                setTimeout(function() { statusEl.innerHTML = ''; }, 2000);
                OPPS[selectedOppId].stage = newStage;

                // Update header pill
                const pill = document.getElementById('drStagePill');
                const sc = STAGE_COLORS[newStage] || { sc:'#666', sb:'#eee' };
                pill.textContent = STAGE_LABELS[newStage] || newStage;
                pill.style.background = sc.sb;
                pill.style.color = sc.sc;

                // Reload page to refresh board columns
                setTimeout(function() { window.location.href = 'AgentHome'; }, 600);
            }
        })
        .catch(function() {
            statusEl.innerHTML = '<span class="text-danger">Error</span>';
        });
}

/* ═══ Mark Lost ═══ */
function markLost() {
    if (!selectedOppId) return;
    if (!confirm('Mark this opportunity as Lost?')) return;
    const params = new URLSearchParams();
    params.append('oppId', selectedOppId);
    params.append('stage', 'LOST');
    params.append('ajax', 'true');
    fetch('UpdateOpportunityStage', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: params.toString() })
        .then(function() { window.location.href = 'AgentHome'; })
        .catch(function() { alert('Failed to update.'); });
}

/* ═══ Update card footer inline ═══ */
function updateCardFooter(oppId) {
    const opp = OPPS[oppId];
    const card = document.querySelector('.kb-card[data-opp-id="' + oppId + '"]');
    if (!card || !opp) return;
    const footer = card.querySelector('.kb-card-footer');
    if (!footer) return;
    let valHtml = opp.estimatedValue != null ? '$' + Number(opp.estimatedValue).toLocaleString('en-US', {maximumFractionDigits:0}) : '';
    let eeHtml = opp.estimatedEEs != null ? '<i class="bi bi-people"></i> ' + opp.estimatedEEs : '';
    footer.innerHTML = '<span>' + valHtml + '</span><span>' + eeHtml + '</span>';
}

/* ═══ New Opp with pre-selected stage (currently unused, stage always defaults to NEW) ═══ */
function newOppWithStage(stage) {
    var modal = new bootstrap.Modal(document.getElementById('newOppModal'));
    modal.show();
}

/* ═══ Prospect mode toggle ═══ */
function showProspectMode(mode) {
    document.getElementById('prospectMode').value = mode;
    if (mode === 'new') {
        document.getElementById('newProspectFields').style.display = '';
        document.getElementById('existingProspectFields').style.display = 'none';
        document.getElementById('companyName').required = true;
        document.getElementById('prospectSelect').required = false;
    } else {
        document.getElementById('newProspectFields').style.display = 'none';
        document.getElementById('existingProspectFields').style.display = '';
        document.getElementById('companyName').required = false;
        document.getElementById('prospectSelect').required = true;
    }
}
showProspectMode('new');
</script>
</body>
</html>
