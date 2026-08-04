<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Proposal Settings</title>
    <script src="https://cdn.jsdelivr.net/npm/sortablejs@1.15.0/Sortable.min.js"></script>
    <style>
        .section-card {
            border: 1px solid #dee2e6;
            border-radius: 0.375rem;
            padding: 0.5rem 0.75rem;
            margin-bottom: 0.5rem;
            cursor: pointer;
            transition: background-color 0.15s;
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }
        .section-card:hover { background-color: #f0f4f8; }
        .section-card.active { background-color: #e8f0fe; border-color: var(--ssa); }
        .section-card .drag-handle {
            cursor: grab;
            color: #adb5bd;
            font-size: 1.1rem;
        }
        .section-card .drag-handle:active { cursor: grabbing; }
        .section-card.pinned .drag-handle { visibility: hidden; }
        .section-card .section-info { flex: 1; min-width: 0; }
        .section-card .section-title { font-weight: 600; font-size: 0.9rem; }
        .section-card .section-badge {
            font-size: 0.7rem;
            padding: 0.15rem 0.4rem;
            border-radius: 0.25rem;
        }
        .section-card .section-toggle { font-size: 0.85rem; }

        /* ── Full-height flex layout (no page scroll) ──────────── */
        .ps-page { display: flex; flex-direction: column; height: calc(100vh - 64px); overflow: hidden; }
        .ps-page .alert { flex-shrink: 0; }
        .ps-row { flex: 1; min-height: 0; display: flex; gap: 1rem; padding: 0.75rem; }

        /* Left column — sticky header, scrollable card list */
        .ps-left { width: 33.33%; display: flex; flex-direction: column; min-height: 0; }
        .ps-left > .card { flex: 1; display: flex; flex-direction: column; min-height: 0; }
        .ps-left .card-body { flex: 1; overflow-y: auto; min-height: 0; }

        /* Right column — flex column, editor collapses, bottom card fills */
        .ps-right { flex: 1; display: flex; flex-direction: column; min-height: 0; }
        .editor-panel { flex: 1; display: flex; flex-direction: column; min-height: 0; }
        .editor-panel > .card { flex: 1; display: flex; flex-direction: column; min-height: 0; }
        .ps-editor-body { overflow-y: auto; min-height: 0; transition: max-height 0.3s ease, padding 0.3s ease, opacity 0.2s ease; }
        .ps-editor-body.collapsed { max-height: 0 !important; padding-top: 0 !important; padding-bottom: 0 !important; overflow: hidden; opacity: 0; }
        .ps-bottom-card { flex: 1; display: flex; flex-direction: column; min-height: 0; }
        .ps-bottom-card > .card { flex: 1; display: flex; flex-direction: column; min-height: 0; }
        .ps-bottom-card > .card > .card-body { flex: 1; overflow-y: auto; min-height: 0; }

        /* Collapse toggle chevron */
        .ps-collapse-toggle { cursor: pointer; transition: transform 0.3s; }
        .ps-collapse-toggle.collapsed { transform: rotate(-90deg); }

        /* Merge tokens modal */
        #mergeTokensModal code { color: var(--ssa); font-weight: 600; }
        #mergeTokensModal .modal-body { font-size: 0.88rem; }

        /* Agency-scoped section cards */
        .section-card.agency-scoped {
            margin-left: 1.5rem;
            border-left: 3px solid var(--ssa);
            font-size: 0.88rem;
        }
        .agency-badge {
            font-size: 0.68rem;
            padding: 0.1rem 0.35rem;
            border-radius: 0.25rem;
            background-color: #e8f0fe;
            color: var(--ssa);
            white-space: nowrap;
        }

        /* Ghost handle for sortable */
        .sortable-ghost { opacity: 0.4; }
        .sortable-chosen { background-color: #e8f0fe; }

        /* ── AI Builder Panel ───────────────────────────────────── */
        .ps-ai-panel {
            border: 1px solid var(--ssa, #0d5681);
            border-radius: 6px;
            background: #fff;
            display: flex;
            flex-direction: column;
            max-height: 480px;
            overflow: hidden;
            margin-top: 0.75rem;
        }
        .ps-ai-header {
            background: linear-gradient(135deg, #0d5681, #1a7ab5);
            color: #fff;
            padding: 0.5rem 0.75rem;
            font-size: 0.82rem;
            font-weight: 600;
            flex-shrink: 0;
        }
        .ps-ai-messages {
            flex: 1;
            overflow-y: auto;
            padding: 0.75rem;
            font-size: 0.8rem;
            background: #f8f9fa;
            min-height: 100px;
        }
        .ps-ai-input {
            padding: 0.5rem;
            border-top: 1px solid #dee2e6;
            flex-shrink: 0;
        }
        .ps-ai-msg { margin-bottom: 0.5rem; }
        .ps-ai-msg.user .ps-ai-bubble {
            background: var(--ssa, #0d5681);
            color: #fff;
            border-radius: 8px 8px 2px 8px;
            padding: 0.4rem 0.65rem;
            margin-left: 20%;
            font-size: 0.78rem;
        }
        .ps-ai-msg.assistant .ps-ai-bubble {
            background: #fff;
            border: 1px solid #dee2e6;
            border-radius: 8px 8px 8px 2px;
            padding: 0.4rem 0.65rem;
            margin-right: 10%;
            font-size: 0.78rem;
        }
        .ps-ai-canvas {
            background: #1e1e2e;
            color: #cdd6f4;
            font-family: monospace;
            font-size: 0.72rem;
            padding: 0.5rem;
            border-radius: 4px;
            margin-top: 0.35rem;
            white-space: pre-wrap;
            word-break: break-word;
        }
        .ps-ai-canvas-actions {
            display: flex;
            gap: 0.35rem;
            margin-top: 0.35rem;
        }
        .ps-ai-canvas-actions .btn { font-size: 0.68rem; padding: 0.15rem 0.5rem; }
        .ps-ai-canvas-tabs {
            display: flex;
            border-bottom: 1px solid #dee2e6;
        }
        .ps-ai-canvas-tabs button {
            flex: 1;
            border: none;
            background: #f0f0f0;
            padding: 0.25rem;
            font-size: 0.72rem;
            cursor: pointer;
        }
        .ps-ai-canvas-tabs button.active {
            background: #fff;
            font-weight: 600;
            border-bottom: 2px solid var(--ssa, #0d5681);
        }
        .ps-ai-canvas-wrapper {
            border: 1px solid #dee2e6;
            border-radius: 4px;
            margin-top: 0.35rem;
            overflow: hidden;
        }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<%-- Flash message --%>
<div class="ps-page">
<c:if test="${not empty sessionScope.flashMessage}">
    <div class="alert alert-success alert-dismissible fade show mx-3 mt-2 mb-0" role="alert" style="font-size:0.85rem;">
        <i class="bi bi-check-circle me-1"></i>${sessionScope.flashMessage}
        <button type="button" class="btn-close btn-sm" data-bs-dismiss="alert"></button>
    </div>
    <c:remove var="flashMessage" scope="session"/>
</c:if>

<div class="ps-row">

        <%-- Build-plan item 6: singleton check — the create action is a no-op once one
             exists, and this flag also hides the trigger so the admin isn't invited to
             try. --%>
        <c:set var="hasIchraSection" value="false"/>
        <c:forEach var="s" items="${sections}">
            <c:if test="${s.getSectionType() == 'ICHRA_ILLUSTRATION'}">
                <c:set var="hasIchraSection" value="true"/>
            </c:if>
        </c:forEach>

        <%-- S11-H: same singleton pattern as the ICHRA section above — a second MARKET
             section would render the page twice, so the trigger hides once one exists. --%>
        <c:set var="hasMarketSection" value="false"/>
        <c:forEach var="s" items="${sections}">
            <c:if test="${s.getSectionType() == 'MARKET'}">
                <c:set var="hasMarketSection" value="true"/>
            </c:if>
        </c:forEach>

        <%-- Left Panel: Section List --%>
        <div class="ps-left">
            <div class="card">
                <div class="hdr-bar d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-list-ul me-1"></i>Proposal Sections</span>
                    <div class="d-flex gap-1">
                        <c:if test="${!hasIchraSection && not empty allLos}">
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addIchraModal" title="Add ICHRA Illustration Section">
                                <i class="bi bi-heart-pulse"></i>
                            </button>
                        </c:if>
                        <c:if test="${!hasMarketSection}">
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addMarketModal" title="Add Market Section">
                                <i class="bi bi-graph-up"></i>
                            </button>
                        </c:if>
                        <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addCustomModal" title="Add Custom Page">
                            <i class="bi bi-plus-lg"></i>
                        </button>
                    </div>
                </div>
                <div class="card-body p-2" id="sectionList">
                    <c:forEach var="section" items="${sections}" varStatus="loop">
                        <div class="section-card ${section.getId() == param.sectionId || (empty param.sectionId && loop.first) ? 'active' : ''} ${section.getSectionType() == 'TITLE' || section.getSectionType() == 'CLOSING' ? 'pinned' : ''} ${section.getAgency() != null ? 'agency-scoped' : ''}"
                             data-id="${section.getId()}" data-type="${section.getSectionType()}"
                             onclick="selectSection(${section.getId()})">

                            <i class="bi bi-grip-vertical drag-handle"></i>

                            <c:choose>
                                <c:when test="${section.getAgency() != null}"><i class="bi bi-building" style="color: var(--ssa);"></i></c:when>
                                <c:when test="${section.getSectionType() == 'TITLE'}"><i class="bi bi-file-earmark-text" style="color: var(--ssa);"></i></c:when>
                                <c:when test="${section.getSectionType() == 'FEATURES'}"><i class="bi bi-check2-square" style="color: var(--ssa-alt);"></i></c:when>
                                <c:when test="${section.getSectionType() == 'PRICING'}"><i class="bi bi-tag" style="color: var(--ssa-alt);"></i></c:when>
                                <c:when test="${section.getSectionType() == 'CLOSING'}"><i class="bi bi-flag" style="color: var(--ssa);"></i></c:when>
                                <c:when test="${section.getSectionType() == 'CUSTOM'}"><i class="bi bi-file-code" style="color: #6c757d;"></i></c:when>
                            </c:choose>

                            <div class="section-info">
                                <div class="section-title">${section.getTitle()}</div>
                                <span class="section-badge bg-light text-dark">${section.getSectionType()}</span>
                                <c:if test="${section.getAgency() != null}">
                                    <span class="agency-badge"><i class="bi bi-building me-1"></i>${section.getAgency().getName()}</span>
                                </c:if>
                                <c:if test="${!section.isActive()}">
                                    <span class="section-badge bg-warning text-dark">Inactive</span>
                                </c:if>
                                <c:if test="${section.getSectionType() == 'CUSTOM' && section.getScope() == 'SCOPED'}">
                                    <span class="section-badge bg-info text-white">Scoped</span>
                                </c:if>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>

        <%-- Right Panel: Section Editor --%>
        <div class="ps-right">
            <c:forEach var="section" items="${sections}" varStatus="loop">
                <div class="editor-panel" id="editor-${section.getId()}"
                     style="display: ${section.getId() == param.sectionId || (empty param.sectionId && loop.first) ? 'flex' : 'none'};">

                    <div class="card">
                        <div class="hdr-bar d-flex justify-content-between align-items-center" style="flex-shrink:0;">
                            <span style="cursor:pointer;" onclick="toggleEditorBody(${section.getId()})">
                                <i class="bi bi-chevron-down ps-collapse-toggle me-1" id="collapse-icon-${section.getId()}"></i>
                                <c:choose>
                                    <c:when test="${section.getSectionType() == 'CUSTOM'}">
                                        <i class="bi bi-file-code me-1"></i>${section.getTitle()}
                                    </c:when>
                                    <c:otherwise>
                                        <i class="bi bi-pencil me-1"></i>${section.getTitle()}
                                    </c:otherwise>
                                </c:choose>
                            </span>
                            <div class="d-flex align-items-center gap-2">
                                <%-- Merge Tokens helper (TITLE/CLOSING/CUSTOM only) --%>
                                <c:if test="${section.getSectionType() == 'TITLE' || section.getSectionType() == 'CLOSING' || section.getSectionType() == 'CUSTOM'}">
                                    <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#mergeTokensModal" title="Merge Tokens">
                                        <i class="bi bi-braces"></i>
                                    </button>
                                </c:if>
                                <%-- Active/Inactive toggle (not for default TITLE/CLOSING; allowed for agency-scoped) --%>
                                <c:if test="${(section.getSectionType() != 'TITLE' && section.getSectionType() != 'CLOSING') || section.getAgency() != null}">
                                    <form method="post" action="ProposalSettings" class="d-inline">
                                        <input type="hidden" name="action" value="toggleActive"/>
                                        <input type="hidden" name="sectionId" value="${section.getId()}"/>
                                        <button type="submit" class="btn btn-sm ${section.isActive() ? 'btn-outline-light' : 'btn-warning'}" title="${section.isActive() ? 'Deactivate' : 'Activate'}">
                                            <i class="bi ${section.isActive() ? 'bi-eye' : 'bi-eye-slash'}"></i>
                                        </button>
                                    </form>
                                </c:if>
                                <%-- Delete button (CUSTOM only) --%>
                                <c:if test="${section.getSectionType() == 'CUSTOM'}">
                                    <form method="post" action="ProposalSettings" class="d-inline" onsubmit="return confirm('Delete this custom page?');">
                                        <input type="hidden" name="action" value="deleteCustom"/>
                                        <input type="hidden" name="sectionId" value="${section.getId()}"/>
                                        <button type="submit" class="btn btn-sm btn-outline-light" title="Delete"><i class="bi bi-trash"></i></button>
                                    </form>
                                </c:if>
                                <%-- Delete button (agency-scoped TITLE/CLOSING) --%>
                                <c:if test="${(section.getSectionType() == 'TITLE' || section.getSectionType() == 'CLOSING') && section.getAgency() != null}">
                                    <form method="post" action="ProposalSettings" class="d-inline" onsubmit="return confirm('Delete this agency override?');">
                                        <input type="hidden" name="action" value="deleteAgencySection"/>
                                        <input type="hidden" name="sectionId" value="${section.getId()}"/>
                                        <button type="submit" class="btn btn-sm btn-outline-light" title="Delete override"><i class="bi bi-trash"></i></button>
                                    </form>
                                </c:if>
                            </div>
                        </div>
                        <div class="card-body ps-editor-body" id="editorBody-${section.getId()}">

                            <c:choose>
                                <%-- TITLE, CLOSING, CUSTOM — editable HTML content --%>
                                <c:when test="${section.getSectionType() == 'TITLE' || section.getSectionType() == 'CLOSING' || section.getSectionType() == 'CUSTOM'}">

                                    <%-- Section title (editable for CUSTOM only) --%>
                                    <c:if test="${section.getSectionType() == 'CUSTOM'}">
                                        <form method="post" action="ProposalSettings" class="mb-3 d-flex align-items-center gap-2">
                                            <input type="hidden" name="action" value="renameSection"/>
                                            <input type="hidden" name="sectionId" value="${section.getId()}"/>
                                            <label class="form-label fw-semibold mb-0">Title:</label>
                                            <input type="text" name="title" class="form-control form-control-sm" style="max-width:300px;" value="${section.getTitle()}" required/>
                                            <button type="submit" class="btn btn-sm btn-outline-secondary"><i class="bi bi-check-lg"></i></button>
                                        </form>
                                    </c:if>

                                    <%-- Editor: raw-only for TITLE/CLOSING, CKEditor-optional for CUSTOM --%>
                                    <c:choose>

                                        <%-- ── TITLE, CLOSING, CUSTOM: raw textarea with render preview ── --%>
                                        <c:when test="${section.getSectionType() == 'TITLE' || section.getSectionType() == 'CLOSING' || section.getSectionType() == 'CUSTOM'}">

                                            <div class="mb-2 d-flex align-items-center gap-2 flex-wrap">
                                                <%-- Toggle between Edit (textarea) and Preview (rendered HTML) --%>
                                                <button type="button" class="btn btn-sm btn-outline-secondary"
                                                        id="btn-edit-${section.getId()}"
                                                        onclick="showRawMode(${section.getId()})" style="display:none;">
                                                    <i class="bi bi-code-slash me-1"></i>Edit HTML
                                                </button>
                                                <button type="button" class="btn btn-sm btn-outline-secondary"
                                                        id="btn-preview-${section.getId()}"
                                                        onclick="showPreviewMode(${section.getId()})">
                                                    <i class="bi bi-eye me-1"></i>Preview
                                                </button>
                                                <button type="button" class="btn btn-sm btn-outline-secondary"
                                                        onclick="clearSection(${section.getId()})"
                                                        title="Clear all content">
                                                    <i class="bi bi-trash me-1"></i>Clear
                                                </button>
                                                <button type="button" class="btn btn-sm btn-ssa"
                                                        onclick="openProposalAiBuilder(${section.getId()}, '${section.getSectionType()}')">
                                                    <i class="bi bi-robot me-1"></i>Build with AI
                                                </button>
                                                <span class="text-muted" style="font-size:0.8rem;">
                                                    <i class="bi bi-info-circle me-1"></i>Raw HTML mode — paste full HTML blocks directly.
                                                </span>
                                            </div>

                                            <form method="post" action="ProposalSettings" id="saveForm-${section.getId()}">
                                                <input type="hidden" name="action" value="saveContent"/>
                                                <input type="hidden" name="sectionId" value="${section.getId()}"/>

                                                <%-- Raw textarea (default visible) --%>
                                                <textarea id="ck-source-${section.getId()}" name="htmlContent"
                                                          class="form-control font-monospace" rows="22"
                                                          style="font-size:0.82rem;" spellcheck="false"
                                                          placeholder="Paste or type HTML here. Merge tokens like {{PROSPECT_NAME}} are supported. &lt;style&gt; blocks are allowed."
                                                >${fn:escapeXml(section.getHtmlContent())}</textarea>

                                                <%-- Render preview iframe (hidden by default) --%>
                                                <iframe id="preview-frame-${section.getId()}"
                                                        style="display:none; width:100%; border:1px solid #dee2e6; border-radius:0.375rem; background:#fff;"
                                                        scrolling="yes" frameborder="0"></iframe>
                                        </c:when>

                                    </c:choose>

                                        <%-- AI Builder Panel --%>
                                        <div id="aiBuilderPanel-${section.getId()}" class="ps-ai-panel" style="display:none;">
                                            <div class="ps-ai-header d-flex justify-content-between align-items-center">
                                                <span><i class="bi bi-robot me-1"></i>AI Page Builder &mdash; ${section.getTitle()}</span>
                                                <button type="button" class="btn-close btn-close-white btn-sm"
                                                        onclick="closeProposalAiBuilder(${section.getId()})"></button>
                                            </div>
                                            <div id="aiMessages-${section.getId()}" class="ps-ai-messages">
                                                <div class="ps-ai-welcome text-muted small text-center" style="margin-top:30px;">
                                                    <i class="bi bi-lightbulb me-1"></i>
                                                    Describe the page you want &mdash; style, colors, content &mdash; and I'll generate the HTML.
                                                </div>
                                            </div>
                                            <div class="ps-ai-input d-flex gap-2">
                                                <input type="text" id="aiInput-${section.getId()}" class="form-control form-control-sm"
                                                       placeholder="e.g. Create a dark navy About Us page with our team highlights..."
                                                       onkeydown="if(event.key==='Enter'){event.preventDefault();sendProposalAiQuestion(${section.getId()}, '${section.getSectionType()}')}">
                                                <button type="button" class="btn btn-sm btn-ssa"
                                                        onclick="sendProposalAiQuestion(${section.getId()}, '${section.getSectionType()}')">
                                                    <i class="bi bi-send"></i>
                                                </button>
                                            </div>
                                        </div>

                                        <div class="mt-2">
                                            <button type="button" class="btn btn-ssa" onclick="saveSection(${section.getId()})">
                                                <i class="bi bi-floppy me-1"></i>Save
                                            </button>
                                        </div>
                                    </form>

                                </c:when>

                                <%-- PRICING and FEATURES — read-only info --%>
                                <c:otherwise>
                                    <div class="text-center py-4">
                                        <i class="bi ${section.getSectionType() == 'FEATURES' ? 'bi-check2-square' : 'bi-tag'}" style="font-size: 2rem; color: var(--ssa);"></i>
                                        <p class="text-muted mt-2 mb-3">This section is auto-generated from your service catalog and rate tables.</p>
                                        <c:choose>
                                            <c:when test="${section.getSectionType() == 'FEATURES'}">
                                                <a href="ServiceManagerHome" class="btn btn-outline-ssa btn-sm"><i class="bi bi-diagram-3 me-1"></i>Service Manager</a>
                                            </c:when>
                                            <c:otherwise>
                                                <a href="PspAdminHome" class="btn btn-outline-ssa btn-sm"><i class="bi bi-cash-coin me-1"></i>Rate Manager</a>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div><%-- /ps-editor-body --%>

                        <%-- ── Bottom card: Display Scope (CUSTOM, ICHRA_ILLUSTRATION) or Agency Overrides (TITLE/CLOSING) ── --%>
                        <c:if test="${section.getSectionType() == 'CUSTOM' || section.getSectionType() == 'ICHRA_ILLUSTRATION'}">
                          <div class="ps-bottom-card">
                            <div class="card" style="border-top: 1px solid #dee2e6; border-radius: 0;">
                              <div class="card-header py-2" style="background-color: var(--ssa); color: white; flex-shrink: 0;">
                                <h6 class="mb-0 fw-semibold"><i class="bi bi-funnel me-2"></i>Display Scope</h6>
                              </div>
                              <div class="card-body">
                                <form method="post" action="ProposalSettings" id="scopeForm-${section.getId()}">
                                  <input type="hidden" name="action" value="updateScope"/>
                                  <input type="hidden" name="sectionId" value="${section.getId()}"/>

                                  <div class="form-check mb-2">
                                    <input class="form-check-input" type="radio" name="scope" value="ALL"
                                           id="scopeAll-${section.getId()}"
                                           ${section.getScope() != 'SCOPED' ? 'checked' : ''}
                                           onchange="toggleScopePanel(${section.getId()}, false)">
                                    <label class="form-check-label" for="scopeAll-${section.getId()}">
                                      Show on <strong>all</strong> proposals
                                    </label>
                                  </div>
                                  <div class="form-check mb-3">
                                    <input class="form-check-input" type="radio" name="scope" value="SCOPED"
                                           id="scopeScoped-${section.getId()}"
                                           ${section.getScope() == 'SCOPED' ? 'checked' : ''}
                                           onchange="toggleScopePanel(${section.getId()}, true)">
                                    <label class="form-check-label" for="scopeScoped-${section.getId()}">
                                      Show only when <strong>specific services</strong> are proposed
                                    </label>
                                  </div>

                                  <div id="scopeDetail-${section.getId()}"
                                       style="display: ${section.getScope() == 'SCOPED' ? 'block' : 'none'};">
                                    <p class="text-muted" style="font-size: 0.85rem;">
                                      This page appears on proposals that include at least one of the selected services.
                                    </p>

                                    <%-- LOS Checkboxes --%>
                                    <div class="mb-3">
                                      <label class="form-label fw-semibold" style="font-size: 0.9rem;">Lines of Service</label>
                                      <c:forEach var="los" items="${allLos}">
                                        <div class="form-check">
                                          <input class="form-check-input" type="checkbox" name="losIds"
                                                 value="${los.getId()}" id="psLos-${section.getId()}-${los.getId()}"
                                                 <c:forEach var="linked" items="${section.getLosList()}">
                                                   <c:if test="${linked.getId() == los.getId()}">checked</c:if>
                                                 </c:forEach>>
                                          <label class="form-check-label" for="psLos-${section.getId()}-${los.getId()}"
                                                 style="font-size: 0.85rem;">${los.getDescription()}</label>
                                        </div>
                                      </c:forEach>
                                    </div>

                                    <%-- Enhancement Checkboxes --%>
                                    <c:if test="${not empty allEnhancements}">
                                      <div class="mb-3">
                                        <label class="form-label fw-semibold" style="font-size: 0.9rem;">Enhancements</label>
                                        <c:forEach var="enh" items="${allEnhancements}">
                                          <div class="form-check">
                                            <input class="form-check-input" type="checkbox" name="enhIds"
                                                   value="${enh.getId()}" id="psEnh-${section.getId()}-${enh.getId()}"
                                                   <c:forEach var="linked" items="${section.getEnhancementList()}">
                                                     <c:if test="${linked.getId() == enh.getId()}">checked</c:if>
                                                   </c:forEach>>
                                            <label class="form-check-label" for="psEnh-${section.getId()}-${enh.getId()}"
                                                   style="font-size: 0.85rem;">${enh.getDescription()}</label>
                                          </div>
                                        </c:forEach>
                                      </div>
                                    </c:if>
                                  </div>

                                  <button type="submit" class="btn btn-sm btn-outline-ssa">
                                    <i class="bi bi-check-lg me-1"></i>Save Scope
                                  </button>
                                </form>
                              </div>
                            </div>
                          </div>
                        </c:if>

                        <%-- Agency Overrides card (default TITLE/CLOSING only) --%>
                        <c:if test="${(section.getSectionType() == 'TITLE' || section.getSectionType() == 'CLOSING') && section.getAgency() == null}">
                          <div class="ps-bottom-card">
                            <div class="card" style="border-top: 1px solid #dee2e6; border-radius: 0;">
                              <div class="card-header py-2" style="background-color: var(--ssa); color: white; flex-shrink: 0;">
                                <h6 class="mb-0 fw-semibold"><i class="bi bi-building me-2"></i>Agency Overrides</h6>
                              </div>
                              <div class="card-body">
                                <p class="text-muted mb-2" style="font-size:0.85rem;">
                                  Agencies with a custom <strong>${fn:toLowerCase(section.getSectionType())}</strong> page.
                                  Others will see the default above.
                                </p>

                                <%-- List existing agency overrides for this section type --%>
                                <c:set var="hasOverrides" value="false"/>
                                <c:forEach var="s" items="${sections}">
                                  <c:if test="${s.getSectionType() == section.getSectionType() && s.getAgency() != null}">
                                    <c:set var="hasOverrides" value="true"/>
                                    <div class="d-flex align-items-center gap-2 mb-2 ps-2" style="border-left: 3px solid var(--ssa);">
                                      <i class="bi bi-building text-muted"></i>
                                      <a href="#" onclick="selectSection(${s.getId()}); return false;" class="text-decoration-none fw-semibold" style="font-size:0.9rem;">
                                        ${s.getAgency().getName()}
                                      </a>
                                      <c:if test="${!s.isActive()}">
                                        <span class="badge bg-warning text-dark" style="font-size:0.7rem;">Inactive</span>
                                      </c:if>
                                      <form method="post" action="ProposalSettings" class="d-inline ms-auto" onsubmit="return confirm('Delete this agency override?');">
                                        <input type="hidden" name="action" value="deleteAgencySection"/>
                                        <input type="hidden" name="sectionId" value="${s.getId()}"/>
                                        <button type="submit" class="btn btn-sm btn-outline-danger" title="Delete override">
                                          <i class="bi bi-trash"></i>
                                        </button>
                                      </form>
                                    </div>
                                  </c:if>
                                </c:forEach>
                                <c:if test="${hasOverrides != 'true'}">
                                  <p class="text-muted fst-italic mb-2" style="font-size:0.85rem;">No agency overrides yet.</p>
                                </c:if>

                                <%-- Create new agency override --%>
                                <c:if test="${not empty agencyList}">
                                  <form method="post" action="ProposalSettings" class="d-flex align-items-center gap-2 mt-3 pt-2" style="border-top: 1px solid #dee2e6;">
                                    <input type="hidden" name="action" value="createAgencySection"/>
                                    <input type="hidden" name="sectionType" value="${section.getSectionType()}"/>
                                    <select name="agencyId" class="form-select form-select-sm" style="max-width: 250px;" required>
                                      <option value="" disabled selected>Select agency...</option>
                                      <c:forEach var="ag" items="${agencyList}">
                                        <c:set var="agHasOverride" value="false"/>
                                        <c:forEach var="s" items="${sections}">
                                          <c:if test="${s.getSectionType() == section.getSectionType() && s.getAgency() != null && s.getAgency().getId() == ag.getId()}">
                                            <c:set var="agHasOverride" value="true"/>
                                          </c:if>
                                        </c:forEach>
                                        <c:if test="${agHasOverride != 'true'}">
                                          <option value="${ag.getId()}">${ag.getName()}</option>
                                        </c:if>
                                      </c:forEach>
                                    </select>
                                    <button type="submit" class="btn btn-sm btn-outline-ssa">
                                      <i class="bi bi-plus-lg me-1"></i>Create Override
                                    </button>
                                  </form>
                                </c:if>
                              </div>
                            </div>
                          </div>
                        </c:if>

                    </div><%-- /card --%>
                </div><%-- /editor-panel --%>
            </c:forEach>
        </div>

</div>
</div>

<%-- Merge Tokens Modal --%>
<div class="modal fade" id="mergeTokensModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                <h6 class="modal-title fw-semibold"><i class="bi bi-braces me-2"></i>Available Merge Tokens</h6>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="row">
                    <div class="col-6">
                        <code>{{PROSPECT_NAME}}</code> — Prospect name<br>
                        <code>{{AGENT_NAME}}</code> — Agent full name<br>
                        <code>{{AGENT_EMAIL}}</code> — Agent email<br>
                        <code>{{AGENCY_NAME}}</code> — Agency name<br>
                        <code>{{PSP_NAME}}</code> — PSP full name<br>
                    </div>
                    <div class="col-6">
                        <code>{{DATE_CREATED}}</code> — Proposal date<br>
                        <code>{{APPLY_BUTTON}}</code> — Apply Now button<br>
                        <code>{{PROPOSAL_ID}}</code> — Proposal ID<br>
                    </div>
                </div>
                <div class="mt-3 text-muted" style="font-size:0.8rem;">
                    <i class="bi bi-info-circle me-1"></i>
                    <code>&lt;style&gt;</code> blocks and inline <code>style=</code> attributes are supported.
                    Paste or type HTML directly in the editor.
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Add ICHRA Illustration Section Modal (build-plan item 6) --%>
<div class="modal fade" id="addIchraModal" tabindex="-1">
    <div class="modal-dialog modal-sm">
        <div class="modal-content">
            <form method="post" action="ProposalSettings">
                <input type="hidden" name="action" value="createIchraSection"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-heart-pulse me-2"></i>Add ICHRA Illustration Section</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <label class="form-label fw-semibold">Line of Service <span class="text-danger">*</span></label>
                    <p class="text-muted" style="font-size: 0.85rem;">
                        This section only appears on proposals that include the selected service — it is scoped from creation and cannot be created unscoped.
                    </p>
                    <select name="losId" class="form-select" required>
                        <option value="" disabled selected>Select a line of service...</option>
                        <c:forEach var="los" items="${allLos}">
                            <option value="${los.getId()}">${los.getDescription()}</option>
                        </c:forEach>
                    </select>
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

<%-- Add Market Section Modal (S11-H) --%>
<div class="modal fade" id="addMarketModal" tabindex="-1">
    <div class="modal-dialog modal-sm">
        <div class="modal-content">
            <form method="post" action="ProposalSettings">
                <input type="hidden" name="action" value="createMarketSection"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-graph-up me-2"></i>Add Market Section</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p class="text-muted mb-2" style="font-size: 0.85rem;">
                        Shows an individual-market overview for the county captured on the proposal —
                        plan and carrier counts and the lowest available premiums at ages 21, 40 and 64.
                    </p>
                    <p class="text-muted mb-0" style="font-size: 0.85rem;">
                        No line of service to pick: this section appears on a proposal only when the
                        selling agency is ICHRA-enabled, the proposal quotes a plus-tier line of service,
                        <strong>and</strong> production rate data exists for that county. Otherwise it is
                        silently omitted. Set its position with the section list's reorder controls.
                    </p>
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

<%-- Add Custom Page Modal --%>
<div class="modal fade" id="addCustomModal" tabindex="-1">
    <div class="modal-dialog modal-sm">
        <div class="modal-content">
            <form method="post" action="ProposalSettings">
                <input type="hidden" name="action" value="createCustom"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>Add Custom Page</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <label class="form-label fw-semibold">Page Title <span class="text-danger">*</span></label>
                    <input type="text" name="title" class="form-control" required maxlength="200" placeholder="e.g. About Our Company"/>
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

<script>
    // ── Save Section ────────────────────────────────────────────────────
    function saveSection(sectionId) {
        document.getElementById('saveForm-' + sectionId).submit();
    }

    // ── Toggle Scope Detail Panel ──────────────────────────────────────
    function toggleScopePanel(sectionId, show) {
        document.getElementById('scopeDetail-' + sectionId).style.display = show ? 'block' : 'none';
    }

    // ── Clear Section ────────────────────────────────────────────────────
    function clearSection(sectionId) {
        if (!confirm('Clear all content for this section?')) return;
        document.getElementById('ck-source-' + sectionId).value = '';
    }

    // ── Preview / Edit toggle for TITLE and CLOSING raw sections ────────
    const PREVIEW_MIN_HEIGHT = 440; // match textarea rows="22" (~440px)

    function showPreviewMode(sectionId) {
        const textarea = document.getElementById('ck-source-' + sectionId);
        const frame    = document.getElementById('preview-frame-' + sectionId);
        const btnEdit  = document.getElementById('btn-edit-' + sectionId);
        const btnPrev  = document.getElementById('btn-preview-' + sectionId);
        if (!frame) return; // not a raw section

        // Write the current textarea content into the iframe
        const html = textarea.value;
        const doc  = frame.contentDocument || frame.contentWindow.document;
        doc.open();
        doc.write(html);
        doc.close();

        // Start with minimum height to prevent tiny box
        frame.style.height = PREVIEW_MIN_HEIGHT + 'px';

        textarea.style.display = 'none';
        frame.style.display    = 'block';
        btnEdit.style.display  = 'inline-flex';
        btnPrev.style.display  = 'none';

        // Auto-size after content renders (defer to let browser layout)
        setTimeout(function() {
            try {
                const contentHeight = frame.contentWindow.document.body.scrollHeight + 32;
                frame.style.height = Math.max(contentHeight, PREVIEW_MIN_HEIGHT) + 'px';
            } catch (e) {
                frame.style.height = '600px';
            }
        }, 50);
    }

    function showRawMode(sectionId) {
        const textarea = document.getElementById('ck-source-' + sectionId);
        const frame    = document.getElementById('preview-frame-' + sectionId);
        const btnEdit  = document.getElementById('btn-edit-' + sectionId);
        const btnPrev  = document.getElementById('btn-preview-' + sectionId);
        if (!frame) return;

        textarea.style.display = '';
        frame.style.display    = 'none';
        btnEdit.style.display  = 'none';
        btnPrev.style.display  = 'inline-flex';
        textarea.focus();
    }

    // ── Toggle editor body collapse ─────────────────────────────────────
    function toggleEditorBody(sectionId) {
        const body = document.getElementById('editorBody-' + sectionId);
        const icon = document.getElementById('collapse-icon-' + sectionId);
        if (!body) return;
        body.classList.toggle('collapsed');
        if (icon) icon.classList.toggle('collapsed');
    }

    // ── Select Section ──────────────────────────────────────────────────
    function selectSection(sectionId) {
        // Update left panel selection
        document.querySelectorAll('.section-card').forEach(c => c.classList.remove('active'));
        document.querySelector('.section-card[data-id="' + sectionId + '"]').classList.add('active');

        // Show/hide editor panels
        document.querySelectorAll('.editor-panel').forEach(p => p.style.display = 'none');
        const panel = document.getElementById('editor-' + sectionId);
        if (panel) panel.style.display = 'flex';
    }

    // ── Drag-and-Drop Reorder ───────────────────────────────────────────
    document.addEventListener('DOMContentLoaded', () => {
        const list = document.getElementById('sectionList');
        if (list) {
            Sortable.create(list, {
                handle: '.drag-handle',
                animation: 150,
                ghostClass: 'sortable-ghost',
                chosenClass: 'sortable-chosen',
                filter: '.pinned',
                onEnd: function() {
                    const cards = list.querySelectorAll('.section-card');
                    const ids = Array.from(cards).map(c => parseInt(c.dataset.id));

                    fetch('ProposalSettings?action=reorder', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(ids)
                    }).then(res => {
                        if (!res.ok) console.error('Reorder failed');
                    });
                }
            });
        }
    });

    // ── AI Page Builder ─────────────────────────────────────────────────

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function openProposalAiBuilder(sectionId, sectionType) {
        document.getElementById('aiBuilderPanel-' + sectionId).style.display = 'flex';
        document.getElementById('aiInput-' + sectionId).focus();
    }

    function closeProposalAiBuilder(sectionId) {
        document.getElementById('aiBuilderPanel-' + sectionId).style.display = 'none';
    }

    function sendProposalAiQuestion(sectionId, sectionType) {
        const input = document.getElementById('aiInput-' + sectionId);
        const question = input.value.trim();
        if (!question) return;
        input.value = '';

        const messagesDiv = document.getElementById('aiMessages-' + sectionId);

        // Clear welcome
        const welcome = messagesDiv.querySelector('.ps-ai-welcome');
        if (welcome) welcome.remove();

        // Show user message
        messagesDiv.innerHTML += '<div class="ps-ai-msg user"><div class="ps-ai-bubble">' + escapeHtml(question) + '</div></div>';
        messagesDiv.scrollTop = messagesDiv.scrollHeight;

        // Loading indicator
        const loadId = 'ps-loading-' + Date.now();
        messagesDiv.innerHTML += '<div class="ps-ai-msg assistant" id="' + loadId + '"><div class="ps-ai-bubble"><i class="bi bi-hourglass-split me-1"></i>Generating...</div></div>';
        messagesDiv.scrollTop = messagesDiv.scrollHeight;

        fetch('ProposalAiBuilder', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                question: question,
                sectionId: '' + sectionId,
                sectionType: sectionType
            })
        })
        .then(r => r.json())
        .then(data => {
            document.getElementById(loadId).remove();
            let html = '<div class="ps-ai-msg assistant"><div class="ps-ai-bubble">';
            html += formatProposalAiResponse(data.answer, sectionId);
            html += '</div></div>';
            messagesDiv.innerHTML += html;
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        })
        .catch(err => {
            document.getElementById(loadId).remove();
            messagesDiv.innerHTML += '<div class="ps-ai-msg assistant"><div class="ps-ai-bubble text-danger">Error: ' + escapeHtml(err.message) + '</div></div>';
        });
    }

    function formatProposalAiResponse(text, sectionId) {
        const codeBlockRegex = /```[\s\S]*?```/g;
        let result = text;
        let idx = 0;
        const canvasBlocks = [];  // store generated HTML blocks separately

        result = result.replace(codeBlockRegex, function(match) {
            const code = match.replace(/```\w*\n?/g, '').replace(/```$/g, '').trim();
            const canvasId = 'ps-canvas-' + sectionId + '-' + Date.now() + '-' + (idx);
            const placeholder = '@@CANVAS_BLOCK_' + (idx++) + '@@';

            // Build the dual-view canvas (code + iframe preview)
            let html = '<div class="ps-ai-canvas-wrapper">';

            // Tab bar
            html += '<div class="ps-ai-canvas-tabs">';
            html += '<button type="button" class="active" onclick="showCanvasTab(this,\'' + canvasId + '\',\'code\')">Code</button>';
            html += '<button type="button" onclick="showCanvasTab(this,\'' + canvasId + '\',\'preview\')">Preview</button>';
            html += '</div>';

            // Code view
            html += '<div id="' + canvasId + '-code" class="ps-ai-canvas" style="display:block; max-height:200px; overflow-y:auto;">' + escapeHtml(code) + '</div>';

            // Preview iframe
            html += '<div id="' + canvasId + '-preview" style="display:none;">';
            html += '<iframe id="' + canvasId + '-iframe" style="width:100%; min-height:250px; border:none; background:#f8f9fa;" sandbox="allow-same-origin"></iframe>';
            html += '</div>';

            // Store raw code in a JS data store (NOT in innerHTML where <br> replacement can corrupt it)
            html += '<div id="' + canvasId + '-raw" style="display:none;" data-raw-code></div>';

            // Action buttons — type="button" prevents form submission
            html += '<div class="ps-ai-canvas-actions">';
            html += '<button type="button" class="btn btn-sm btn-ssa" onclick="insertIntoSectionEditor(' + sectionId + ',\'' + canvasId + '\')">';
            html += '<i class="bi bi-box-arrow-in-down me-1"></i>Insert into Editor</button>';
            html += '<button type="button" class="btn btn-sm btn-outline-secondary" onclick="copyCanvasRaw(\'' + canvasId + '\')">';
            html += '<i class="bi bi-clipboard me-1"></i>Copy</button>';
            html += '</div>';

            html += '</div>';

            // Write the preview iframe content after a tick (so the DOM exists)
            // Also store the raw code via textContent (immune to <br> replacement)
            setTimeout(function() {
                // Store raw code safely via textContent
                const rawDiv = document.getElementById(canvasId + '-raw');
                if (rawDiv) rawDiv.textContent = code;

                const iframe = document.getElementById(canvasId + '-iframe');
                if (iframe) {
                    const doc = iframe.contentDocument || iframe.contentWindow.document;
                    doc.open();
                    doc.write('<body style="margin:0; background:#f8f9fa; display:flex; justify-content:center; padding:10px;"><div style="max-width:900px; width:100%;">' + code + '</div></body>');
                    doc.close();
                    // Auto-height
                    setTimeout(function() {
                        try { iframe.style.height = (doc.body.scrollHeight + 20) + 'px'; } catch(e) {}
                    }, 100);
                }
            }, 50);

            canvasBlocks.push(html);
            return placeholder;
        });

        // Apply <br> conversion ONLY to prose text (placeholders are single-line tokens)
        result = result.replace(/\n/g, '<br>');

        // Restore canvas blocks (their HTML is NOT affected by <br> replacement)
        for (let i = 0; i < canvasBlocks.length; i++) {
            result = result.replace('@@CANVAS_BLOCK_' + i + '@@', canvasBlocks[i]);
        }

        return result;
    }

    function showCanvasTab(btn, canvasId, tab) {
        // Toggle tab active state
        btn.parentElement.querySelectorAll('button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        // Toggle views
        document.getElementById(canvasId + '-code').style.display = tab === 'code' ? 'block' : 'none';
        document.getElementById(canvasId + '-preview').style.display = tab === 'preview' ? 'block' : 'none';
    }

    function insertIntoSectionEditor(sectionId, canvasId) {
        const rawDiv = document.getElementById(canvasId + '-raw');
        if (!rawDiv) return;
        // textContent holds the pristine code (no <br> corruption)
        const code = rawDiv.textContent;

        const textarea = document.getElementById('ck-source-' + sectionId);
        textarea.value = code;
        textarea.style.transition = 'background 0.3s';
        textarea.style.background = '#d4edda';
        setTimeout(() => { textarea.style.background = ''; }, 1000);

        // Switch to edit mode if in preview
        showRawMode(sectionId);
    }

    function copyCanvasRaw(canvasId) {
        const rawDiv = document.getElementById(canvasId + '-raw');
        if (!rawDiv) return;
        navigator.clipboard.writeText(rawDiv.textContent);
    }
</script>
</body>
</html>
