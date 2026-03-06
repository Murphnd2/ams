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

        .editor-panel { min-height: 400px; }
        .token-ref {
            background: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 0.375rem;
            padding: 0.75rem;
            font-size: 0.85rem;
        }
        .token-ref code {
            color: var(--ssa);
            font-weight: 600;
        }

        /* Ghost handle for sortable */
        .sortable-ghost { opacity: 0.4; }
        .sortable-chosen { background-color: #e8f0fe; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<%-- Flash message --%>
<c:if test="${not empty sessionScope.flashMessage}">
    <div class="alert alert-success alert-dismissible fade show mx-3 mt-2 mb-0" role="alert" style="font-size:0.85rem;">
        <i class="bi bi-check-circle me-1"></i>${sessionScope.flashMessage}
        <button type="button" class="btn-close btn-sm" data-bs-dismiss="alert"></button>
    </div>
    <c:remove var="flashMessage" scope="session"/>
</c:if>

<div class="container-fluid px-3 py-3">
    <div class="row g-3">

        <%-- Left Panel: Section List (col-4) --%>
        <div class="col-4">
            <div class="card">
                <div class="hdr-bar d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-list-ul me-1"></i>Proposal Sections</span>
                    <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addCustomModal" title="Add Custom Page">
                        <i class="bi bi-plus-lg"></i>
                    </button>
                </div>
                <div class="card-body p-2" id="sectionList">
                    <c:forEach var="section" items="${sections}" varStatus="loop">
                        <div class="section-card ${section.getId() == param.sectionId || (empty param.sectionId && loop.first) ? 'active' : ''} ${section.getSectionType() == 'TITLE' || section.getSectionType() == 'CLOSING' ? 'pinned' : ''}"
                             data-id="${section.getId()}" data-type="${section.getSectionType()}"
                             onclick="selectSection(${section.getId()})">

                            <i class="bi bi-grip-vertical drag-handle"></i>

                            <c:choose>
                                <c:when test="${section.getSectionType() == 'TITLE'}"><i class="bi bi-file-earmark-text" style="color: var(--ssa);"></i></c:when>
                                <c:when test="${section.getSectionType() == 'FEATURES'}"><i class="bi bi-check2-square" style="color: var(--ssa-alt);"></i></c:when>
                                <c:when test="${section.getSectionType() == 'PRICING'}"><i class="bi bi-tag" style="color: var(--ssa-alt);"></i></c:when>
                                <c:when test="${section.getSectionType() == 'CLOSING'}"><i class="bi bi-flag" style="color: var(--ssa);"></i></c:when>
                                <c:when test="${section.getSectionType() == 'CUSTOM'}"><i class="bi bi-file-code" style="color: #6c757d;"></i></c:when>
                            </c:choose>

                            <div class="section-info">
                                <div class="section-title">${section.getTitle()}</div>
                                <span class="section-badge bg-light text-dark">${section.getSectionType()}</span>
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

        <%-- Right Panel: Section Editor (col-8) --%>
        <div class="col-8">
            <c:forEach var="section" items="${sections}" varStatus="loop">
                <div class="editor-panel" id="editor-${section.getId()}"
                     style="display: ${section.getId() == param.sectionId || (empty param.sectionId && loop.first) ? 'block' : 'none'};">

                    <div class="card">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span>
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
                                <%-- Active/Inactive toggle (not for TITLE/CLOSING) --%>
                                <c:if test="${section.getSectionType() != 'TITLE' && section.getSectionType() != 'CLOSING'}">
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
                            </div>
                        </div>
                        <div class="card-body">

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

                                        <div class="mt-2">
                                            <button type="button" class="btn btn-ssa" onclick="saveSection(${section.getId()})">
                                                <i class="bi bi-floppy me-1"></i>Save
                                            </button>
                                        </div>
                                    </form>

                                    <%-- Display Scope (CUSTOM sections only) --%>
                                    <c:if test="${section.getSectionType() == 'CUSTOM'}">
                                      <div class="card mt-3">
                                        <div class="card-header py-2" style="background-color: var(--ssa); color: white;">
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
                                    </c:if>

                                    <%-- Available Tokens reference --%>
                                    <div class="token-ref mt-3">
                                        <div class="fw-semibold mb-2"><i class="bi bi-braces me-1"></i>Available Merge Tokens</div>
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
                                                <code>{{PRIMARY_COLOR}}</code> — Brand primary color<br>
                                                <code>{{ACCENT_COLOR}}</code> — Brand accent color<br>
                                                <code>{{APPLY_BUTTON}}</code> — Apply Now button<br>
                                                <code>{{PROPOSAL_ID}}</code> — Proposal ID<br>
                                            </div>
                                        </div>
                                        <div class="mt-2 text-muted" style="font-size:0.8rem;">
                                            <i class="bi bi-info-circle me-1"></i>
                                            <code>&lt;style&gt;</code> blocks and inline <code>style=</code> attributes are supported.
                                            Use the <strong>Paste HTML</strong> or <strong>HTML Source</strong> button to insert raw markup.
                                        </div>
                                    </div>
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

                        </div>
                    </div>
                </div>
            </c:forEach>
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

        // Auto-size the iframe to its content height (+ small buffer)
        frame.onload = function () {
            try {
                frame.style.height = (frame.contentWindow.document.body.scrollHeight + 32) + 'px';
            } catch (e) {
                frame.style.height = '600px';
            }
        };
        // Trigger onload if already loaded
        try {
            frame.style.height = (frame.contentWindow.document.body.scrollHeight + 32) + 'px';
        } catch (e) {
            frame.style.height = '600px';
        }

        textarea.style.display = 'none';
        frame.style.display    = 'block';
        btnEdit.style.display  = 'inline-flex';
        btnPrev.style.display  = 'none';
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

    // ── Select Section ──────────────────────────────────────────────────
    function selectSection(sectionId) {
        // Update left panel selection
        document.querySelectorAll('.section-card').forEach(c => c.classList.remove('active'));
        document.querySelector('.section-card[data-id="' + sectionId + '"]').classList.add('active');

        // Show/hide editor panels
        document.querySelectorAll('.editor-panel').forEach(p => p.style.display = 'none');
        const panel = document.getElementById('editor-' + sectionId);
        if (panel) panel.style.display = 'block';
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
</script>
</body>
</html>
