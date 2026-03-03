<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Proposal Settings</title>
    <script src="https://cdn.ckeditor.com/ckeditor5/36.0.1/classic/ckeditor.js"></script>
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

        .ck-editor__editable {
            min-height: 250px;
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

                                    <%-- CKEditor / HTML source toggle --%>
                                    <div class="mb-2">
                                        <button type="button" class="btn btn-sm btn-outline-secondary" onclick="toggleSource(${section.getId()})">
                                            <i class="bi bi-code-slash me-1"></i>HTML Source
                                        </button>
                                    </div>

                                    <form method="post" action="ProposalSettings" id="saveForm-${section.getId()}">
                                        <input type="hidden" name="action" value="saveContent"/>
                                        <input type="hidden" name="sectionId" value="${section.getId()}"/>

                                        <%-- CKEditor container --%>
                                        <div id="ck-wrap-${section.getId()}">
                                            <div id="ck-editor-${section.getId()}">${section.getHtmlContent()}</div>
                                        </div>

                                        <%-- Raw HTML source textarea (hidden by default) --%>
                                        <textarea id="ck-source-${section.getId()}" name="htmlContent" class="form-control font-monospace" rows="12"
                                                  style="display:none; font-size:0.85rem;">${fn:escapeXml(section.getHtmlContent())}</textarea>

                                        <div class="mt-2">
                                            <button type="button" class="btn btn-ssa" onclick="saveSection(${section.getId()})">
                                                <i class="bi bi-floppy me-1"></i>Save
                                            </button>
                                        </div>
                                    </form>

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
    // ── CKEditor instances ──────────────────────────────────────────────
    const editors = {};
    const sourceMode = {};

    document.querySelectorAll('[id^="ck-editor-"]').forEach(el => {
        const sectionId = el.id.replace('ck-editor-', '');
        ClassicEditor
            .create(el, {
                toolbar: ['bold', 'italic', 'underline', 'link', '|', 'bulletedList', 'numberedList', '|', 'heading', 'alignment']
            })
            .then(editor => {
                editors[sectionId] = editor;
            })
            .catch(err => console.error('CKEditor init error:', err));
    });

    // ── Toggle HTML Source ──────────────────────────────────────────────
    function toggleSource(sectionId) {
        const ckWrap = document.getElementById('ck-wrap-' + sectionId);
        const source = document.getElementById('ck-source-' + sectionId);

        if (sourceMode[sectionId]) {
            // Switching back to CKEditor
            if (editors[sectionId]) {
                editors[sectionId].setData(source.value);
            }
            ckWrap.style.display = '';
            source.style.display = 'none';
            sourceMode[sectionId] = false;
        } else {
            // Switching to source view
            if (editors[sectionId]) {
                source.value = editors[sectionId].getData();
            }
            ckWrap.style.display = 'none';
            source.style.display = '';
            sourceMode[sectionId] = true;
        }
    }

    // ── Save Section ────────────────────────────────────────────────────
    function saveSection(sectionId) {
        const source = document.getElementById('ck-source-' + sectionId);
        // Sync CKEditor data to textarea if in WYSIWYG mode
        if (!sourceMode[sectionId] && editors[sectionId]) {
            source.value = editors[sectionId].getData();
        }
        document.getElementById('saveForm-' + sectionId).submit();
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
