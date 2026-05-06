<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Knowledge Manager</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,100..1000;1,9..40,100..1000&display=swap" rel="stylesheet">
    <style>
        * { font-family: 'DM Sans', sans-serif; }

        /* ── Header bar ── */
        .hdr-bar {
            background: #f8f9fa;
            border-bottom: 1px solid #dee2e6;
            padding: 0.65rem 1rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .hdr-bar h5 { margin: 0; font-size: 1rem; font-weight: 600; color: #0d5681; }

        /* ── KB strip ── */
        .km-kb-strip {
            background: #fff;
            border-bottom: 1px solid #dee2e6;
            padding: 0.6rem 1rem;
            display: flex;
            gap: 0.5rem;
            flex-wrap: wrap;
            align-items: flex-start;
        }
        .km-kb-card {
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 0.5rem 0.75rem;
            cursor: pointer;
            min-width: 140px;
            max-width: 200px;
            transition: box-shadow 0.15s, border-color 0.15s;
            background: #fff;
        }
        .km-kb-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-color: #0d5681; }
        .km-kb-card.active { border-color: #0d5681; border-width: 2px; background: #f0f7fd; }
        .km-kb-card .kb-label { font-size: 0.8rem; font-weight: 600; color: #0d5681; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .km-kb-card .kb-stats { font-size: 0.72rem; color: #6c757d; margin-top: 2px; }
        .km-kb-card .kb-badges { display: flex; gap: 3px; flex-wrap: wrap; margin-top: 4px; }

        /* ── Filter bar ── */
        .km-filter-bar {
            background: #f8f9fa;
            border-bottom: 1px solid #dee2e6;
            padding: 0.5rem 1rem;
            display: flex;
            gap: 0.5rem;
            flex-wrap: wrap;
            align-items: center;
        }

        /* ── Buttons ── */
        .btn-ssa {
            background: #0d5681; color: #fff; border: none;
            font-size: 0.82rem; padding: 0.35rem 0.85rem; border-radius: 6px;
        }
        .btn-ssa:hover { background: #094369; color: #fff; }

        /* ── Chunk cards ── */
        .chunk-card {
            border: 1px solid #dee2e6;
            border-left: 5px solid #dee2e6;
            border-radius: 8px;
            padding: 0.85rem;
            background: #fff;
            transition: box-shadow 0.15s;
            height: 100%;
        }
        .chunk-card:hover { box-shadow: 0 2px 10px rgba(0,0,0,0.07); }
        .chunk-card.inactive { opacity: 0.6; }

        /* Chunk type color-coding via left border */
        .ct-style      { border-left-color: #6f42c1; }
        .ct-federal    { border-left-color: #0d6efd; }
        .ct-ssa        { border-left-color: #198754; }
        .ct-summit     { border-left-color: #fd7e14; }
        .ct-scenario   { border-left-color: #9c27b0; }
        .ct-escalation { border-left-color: #dc3545; }

        .chunk-title    { font-size: 0.85rem; font-weight: 700; color: #212529; margin-bottom: 0.15rem; }
        .chunk-section  { font-size: 0.72rem; color: #6c757d; }
        .chunk-preview  {
            font-size: 0.78rem; color: #555; margin-top: 0.35rem;
            display: -webkit-box; -webkit-line-clamp: 2;
            -webkit-box-orient: vertical; overflow: hidden;
        }
        .chunk-meta     { font-size: 0.72rem; color: #6c757d; margin-top: 0.4rem; }

        /* ── Modal shared ── */
        .modal-header-blue { background: #0d5681; color: #fff; }
        .modal-header-blue .btn-close { filter: invert(1); }

        /* ── History table ── */
        .hist-table { font-size: 0.78rem; }
        .hist-table td { vertical-align: top; padding: 0.3rem 0.4rem; }
        .change-badge-UPDATE     { background: #0d6efd; }
        .change-badge-DELETE     { background: #dc3545; }
        .change-badge-DEACTIVATE { background: #6c757d; }
    </style>
</head>
<body>
<c:set var="pageTitle" value="Knowledge Manager" scope="request"/>
<c:set var="pageIcon"  value="bi-book" scope="request"/>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<%-- ── Header bar ── --%>
<div class="hdr-bar">
    <h5><i class="bi bi-book me-2"></i>Knowledge Manager</h5>
    <div class="d-flex gap-2">
        <button class="btn btn-sm btn-outline-secondary" onclick="openBulkImportModal()" title="Bulk import chunks from JSON">
            <i class="bi bi-cloud-upload me-1"></i>Bulk Import
        </button>
        <form method="post" action="KnowledgeManager" class="d-inline">
            <input type="hidden" name="action" value="reloadCache">
            <input type="hidden" name="kbKey"  value="${selectedKbKey}">
            <button type="submit" class="btn btn-sm btn-outline-info" title="Force reload knowledge cache from DB+JSON">
                <i class="bi bi-arrow-clockwise me-1"></i>Reload Cache
            </button>
        </form>
        <button class="btn-ssa" onclick="openCreateModal()">
            <i class="bi bi-plus-lg me-1"></i>Add Chunk
        </button>
    </div>
</div>

<%-- ── Flash messages ── --%>
<c:if test="${not empty kmMessage}">
    <div class="alert alert-success alert-dismissible fade show mb-0 rounded-0" role="alert" style="font-size:0.85rem;">
        <i class="bi bi-check-circle me-2"></i>${kmMessage}
        <button type="button" class="btn-close btn-sm" data-bs-dismiss="alert"></button>
    </div>
</c:if>
<c:if test="${not empty kmError}">
    <div class="alert alert-danger alert-dismissible fade show mb-0 rounded-0" role="alert" style="font-size:0.85rem;">
        <i class="bi bi-exclamation-triangle me-2"></i>${kmError}
        <button type="button" class="btn-close btn-sm" data-bs-dismiss="alert"></button>
    </div>
</c:if>

<%-- ── KB strip ── --%>
<div class="km-kb-strip">
    <c:forEach var="kb" items="${allKbs}">
        <div class="km-kb-card ${kb.kbKey == selectedKbKey ? 'active' : ''}"
             onclick="location='KnowledgeManager?kbKey=${kb.kbKey}'">
            <div class="kb-label" title="${kb.label}">${kb.label}</div>
            <div class="kb-stats">
                <c:set var="cnt" value="${kbActiveCount[kb.id]}"/>
                <c:choose>
                    <c:when test="${not empty cnt}">${cnt} active</c:when>
                    <c:otherwise>0 active</c:otherwise>
                </c:choose>
                chunk<c:if test="${empty cnt || cnt != 1}">s</c:if>
            </div>
            <div class="kb-badges">
                <span class="badge rounded-pill ${kb.source == 'DB' ? 'bg-primary' : 'bg-secondary'}"
                      style="font-size:0.62rem;">${kb.source}</span>
                <span class="badge rounded-pill ${kb.reloadStrategy == 'ALWAYS_LOAD' ? 'bg-warning text-dark' : 'bg-light text-dark border'}"
                      style="font-size:0.62rem;">${kb.reloadStrategy}</span>
                <c:if test="${not kb.active}">
                    <span class="badge rounded-pill bg-danger" style="font-size:0.62rem;">off</span>
                </c:if>
            </div>
        </div>
    </c:forEach>
</div>

<%-- ── Filter bar ── --%>
<div class="km-filter-bar">
    <form method="get" action="KnowledgeManager" class="d-flex gap-2 flex-wrap align-items-center w-100">
        <input type="hidden" name="kbKey" value="${selectedKbKey}">

        <select name="chunkType" class="form-select form-select-sm" style="width:auto;"
                onchange="this.form.submit()">
            <option value="">All Types</option>
            <c:forEach var="ct" items="${chunkTypes}">
                <option value="${ct}" ${chunkTypeFilter == ct.name() ? 'selected' : ''}>${ct}</option>
            </c:forEach>
        </select>

        <input type="text" name="accountType" class="form-control form-control-sm" style="width:140px;"
               placeholder="Account type" value="${accountTypeFilter}">

        <select name="activeOnly" class="form-select form-select-sm" style="width:auto;"
                onchange="this.form.submit()">
            <option value="true"  ${activeOnly ? 'selected' : ''}>Active only</option>
            <option value="false" ${!activeOnly ? 'selected' : ''}>All (incl. inactive)</option>
        </select>

        <input type="text" name="search" class="form-control form-control-sm" style="width:200px;"
               placeholder="Search title / content…" value="${search}">

        <button type="submit" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-search"></i>
        </button>
        <a href="KnowledgeManager?kbKey=${selectedKbKey}" class="btn btn-sm btn-outline-secondary">
            <i class="bi bi-x-circle me-1"></i>Clear
        </a>

        <span class="ms-auto text-muted" style="font-size:0.78rem;">
            ${fn:length(chunks)} chunk<c:if test="${fn:length(chunks) != 1}">s</c:if>
            <c:if test="${not empty selectedKb}"> in <strong>${selectedKb.label}</strong></c:if>
        </span>
    </form>
</div>

<%-- ── Chunk card grid ── --%>
<div class="container-fluid p-3">

    <c:if test="${empty chunks}">
        <div class="text-center text-muted py-5">
            <i class="bi bi-inbox" style="font-size:3rem; opacity:0.3;"></i>
            <p class="mt-2">No chunks found. Use <strong>Add Chunk</strong> to create one,
               or <strong>Bulk Import</strong> to load from JSON.</p>
        </div>
    </c:if>

    <div class="row g-3">
        <c:forEach var="chunk" items="${chunks}">

            <%-- Determine border color CSS class by chunk type prefix --%>
            <c:set var="typeClass" value=""/>
            <c:choose>
                <c:when test="${fn:startsWith(chunk.chunkType.name(), 'STYLE')}">
                    <c:set var="typeClass" value="ct-style"/>
                </c:when>
                <c:when test="${fn:startsWith(chunk.chunkType.name(), 'FEDERAL')}">
                    <c:set var="typeClass" value="ct-federal"/>
                </c:when>
                <c:when test="${fn:startsWith(chunk.chunkType.name(), 'SSA')}">
                    <c:set var="typeClass" value="ct-ssa"/>
                </c:when>
                <c:when test="${fn:startsWith(chunk.chunkType.name(), 'SUMMIT')}">
                    <c:set var="typeClass" value="ct-summit"/>
                </c:when>
                <c:when test="${fn:startsWith(chunk.chunkType.name(), 'SCENARIO')}">
                    <c:set var="typeClass" value="ct-scenario"/>
                </c:when>
                <c:when test="${fn:startsWith(chunk.chunkType.name(), 'ESCALATION')}">
                    <c:set var="typeClass" value="ct-escalation"/>
                </c:when>
            </c:choose>

            <div class="col-lg-6 col-xl-4">
                <div class="chunk-card ${typeClass} ${!chunk.active ? 'inactive' : ''}">

                    <%-- Title + type badge --%>
                    <div class="d-flex justify-content-between align-items-start mb-1">
                        <div class="chunk-title">${chunk.title}</div>
                        <span class="badge bg-light text-dark border ms-1" style="font-size:0.62rem; white-space:nowrap;">${chunk.chunkType}</span>
                    </div>

                    <%-- Section + visibility --%>
                    <div class="d-flex gap-2 align-items-center">
                        <c:if test="${not empty chunk.section}">
                            <span class="chunk-section"><i class="bi bi-folder2 me-1"></i>${chunk.section}</span>
                        </c:if>
                        <span class="badge rounded-pill ${chunk.visibility == 'PUBLIC' ? 'bg-success' : chunk.visibility == 'ADMIN_ONLY' ? 'bg-danger' : 'bg-secondary'}"
                              style="font-size:0.62rem;">${chunk.visibility}</span>
                        <c:if test="${not chunk.active}">
                            <span class="badge rounded-pill bg-secondary" style="font-size:0.62rem;">inactive</span>
                        </c:if>
                    </div>

                    <%-- Content preview --%>
                    <div class="chunk-preview">${chunk.content}</div>

                    <%-- Keywords --%>
                    <c:if test="${not empty chunk.keywords}">
                        <div class="mt-1" style="font-size:0.72rem; color:#6c757d;">
                            <i class="bi bi-tags me-1"></i>${chunk.keywords}
                        </div>
                    </c:if>

                    <%-- Meta: account type + last modified --%>
                    <div class="chunk-meta d-flex gap-3">
                        <c:if test="${not empty chunk.accountType}">
                            <span><i class="bi bi-building me-1"></i>${chunk.accountType}</span>
                        </c:if>
                        <c:if test="${not empty chunk.dateModified}">
                            <span><i class="bi bi-clock me-1"></i>${chunk.dateModified}</span>
                        </c:if>
                    </div>

                    <%-- Action buttons --%>
                    <div class="d-flex gap-1 mt-2 flex-wrap">
                        <button class="btn btn-sm btn-outline-primary" onclick="editChunk(${chunk.id})" title="Edit">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-secondary" onclick="openHistoryModal(${chunk.id}, '${chunk.title}')" title="View history">
                            <i class="bi bi-clock-history"></i>
                        </button>

                        <%-- Activate / Deactivate toggle --%>
                        <c:choose>
                            <c:when test="${chunk.active}">
                                <form method="post" action="KnowledgeManager" class="d-inline">
                                    <input type="hidden" name="action"  value="deactivateChunk">
                                    <input type="hidden" name="chunkId" value="${chunk.id}">
                                    <input type="hidden" name="kbKey"   value="${selectedKbKey}">
                                    <button type="submit" class="btn btn-sm btn-outline-warning" title="Deactivate">
                                        <i class="bi bi-pause-circle"></i>
                                    </button>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <form method="post" action="KnowledgeManager" class="d-inline">
                                    <input type="hidden" name="action"  value="activateChunk">
                                    <input type="hidden" name="chunkId" value="${chunk.id}">
                                    <input type="hidden" name="kbKey"   value="${selectedKbKey}">
                                    <button type="submit" class="btn btn-sm btn-outline-success" title="Activate">
                                        <i class="bi bi-play-circle"></i>
                                    </button>
                                </form>
                            </c:otherwise>
                        </c:choose>

                        <%-- Hard delete --%>
                        <form method="post" action="KnowledgeManager" class="d-inline"
                              onsubmit="return confirm('Permanently delete this chunk? This cannot be undone.')">
                            <input type="hidden" name="action"  value="deleteChunk">
                            <input type="hidden" name="chunkId" value="${chunk.id}">
                            <input type="hidden" name="kbKey"   value="${selectedKbKey}">
                            <button type="submit" class="btn btn-sm btn-outline-danger" title="Hard delete">
                                <i class="bi bi-trash"></i>
                            </button>
                        </form>
                    </div>
                </div>
            </div>

        </c:forEach>
    </div><%-- /row --%>
</div><%-- /container-fluid --%>

<%-- ══════════════════════════════════════════════════════
     CREATE / EDIT MODAL
════════════════════════════════════════════════════════ --%>
<div class="modal fade" id="chunkModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-scrollable">
        <div class="modal-content">
            <form method="post" action="KnowledgeManager" id="chunkForm">
                <input type="hidden" name="action"     id="cmAction"  value="createChunk">
                <input type="hidden" name="chunkId"    id="cmChunkId" value="">
                <input type="hidden" name="kbKey"      value="${selectedKbKey}">

                <div class="modal-header modal-header-blue">
                    <h5 class="modal-title" id="cmTitle"><i class="bi bi-plus-lg me-2"></i>Add Chunk</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>

                <div class="modal-body">
                    <div class="row g-3">

                        <%-- KB selector (shown on create; hidden on edit since KB doesn't change) --%>
                        <div class="col-md-4" id="cmKbGroup">
                            <label class="form-label fw-semibold">Knowledge Base <span class="text-danger">*</span></label>
                            <select class="form-select form-select-sm" name="chunkKbKey" id="cmKbKey" required>
                                <c:forEach var="kb" items="${allKbs}">
                                    <option value="${kb.kbKey}"
                                            ${kb.kbKey == selectedKbKey ? 'selected' : ''}>${kb.label}</option>
                                </c:forEach>
                            </select>
                        </div>

                        <%-- Chunk type --%>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold">Chunk Type <span class="text-danger">*</span></label>
                            <select class="form-select form-select-sm" name="chunkType" id="cmChunkType" required>
                                <option value="">— select —</option>
                                <c:forEach var="ct" items="${chunkTypes}">
                                    <option value="${ct}">${ct}</option>
                                </c:forEach>
                            </select>
                        </div>

                        <%-- Visibility --%>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold">Visibility</label>
                            <select class="form-select form-select-sm" name="visibility" id="cmVisibility">
                                <c:forEach var="v" items="${visibilities}">
                                    <option value="${v}" ${v.name() == 'INTERNAL' ? 'selected' : ''}>${v}</option>
                                </c:forEach>
                            </select>
                        </div>

                        <%-- Title --%>
                        <div class="col-md-8">
                            <label class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                            <input type="text" class="form-control form-control-sm" name="title" id="cmTitle2"
                                   required maxlength="200" placeholder="Short descriptive title">
                        </div>

                        <%-- Section --%>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold">Section</label>
                            <input type="text" class="form-control form-control-sm" name="section" id="cmSection"
                                   maxlength="100" placeholder="e.g. Tone, Deadlines">
                        </div>

                        <%-- Content --%>
                        <div class="col-12">
                            <label class="form-label fw-semibold">Content <span class="text-danger">*</span></label>
                            <textarea class="form-control form-control-sm" name="content" id="cmContent"
                                      rows="8" required style="font-family:monospace; font-size:0.8rem;"
                                      placeholder="The full knowledge content that will be injected into AI prompts."></textarea>
                        </div>

                        <%-- Keywords --%>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Keywords
                                <small class="text-muted">(comma-separated, used for search routing)</small>
                            </label>
                            <input type="text" class="form-control form-control-sm" name="keywords" id="cmKeywords"
                                   maxlength="500" placeholder="cobra,election,qualifying event">
                        </div>

                        <%-- Account type --%>
                        <div class="col-md-3">
                            <label class="form-label fw-semibold">Account Type</label>
                            <input type="text" class="form-control form-control-sm" name="accountType" id="cmAccountType"
                                   maxlength="50" placeholder="e.g. COBRA, FSA">
                        </div>

                        <%-- Active checkbox (edit only) --%>
                        <div class="col-md-3 d-flex align-items-end" id="cmActiveGroup" style="display:none!important;">
                            <div class="form-check mb-2">
                                <input type="checkbox" class="form-check-input" name="active" id="cmActive" value="true">
                                <label class="form-check-label fw-semibold" for="cmActive">Active</label>
                            </div>
                        </div>

                        <%-- Effective dates --%>
                        <div class="col-md-3">
                            <label class="form-label fw-semibold">Effective Start</label>
                            <input type="date" class="form-control form-control-sm" name="effectiveStart" id="cmEffectiveStart">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label fw-semibold">Effective End</label>
                            <input type="date" class="form-control form-control-sm" name="effectiveEnd" id="cmEffectiveEnd">
                        </div>

                        <%-- Source citation --%>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Source Citation</label>
                            <input type="text" class="form-control form-control-sm" name="sourceCitation" id="cmSourceCitation"
                                   maxlength="500" placeholder="IRS Notice 2024-01, internal policy doc…">
                        </div>

                        <%-- Change note (edit only) --%>
                        <div class="col-12" id="cmChangeNoteGroup" style="display:none;">
                            <label class="form-label fw-semibold">Change Note
                                <small class="text-muted">(optional — recorded in history)</small>
                            </label>
                            <input type="text" class="form-control form-control-sm" name="changeNote" id="cmChangeNote"
                                   maxlength="500" placeholder="Describe what changed and why…">
                        </div>

                    </div>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-sm btn-ssa" id="cmSaveBtn">Save Chunk</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- ══════════════════════════════════════════════════════
     BULK IMPORT MODAL
════════════════════════════════════════════════════════ --%>
<div class="modal fade" id="bulkImportModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <div class="modal-content">
            <form method="post" action="KnowledgeManager">
                <input type="hidden" name="action" value="bulkImportChunks">
                <input type="hidden" name="kbKey"  value="${selectedKbKey}">

                <div class="modal-header modal-header-blue">
                    <h5 class="modal-title"><i class="bi bi-cloud-upload me-2"></i>Bulk Import Chunks</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>

                <div class="modal-body">
                    <p class="text-muted small mb-2">
                        Paste a JSON array of chunk objects. Each entry may include a <code>kbKey</code>
                        to target a specific knowledge base; if omitted the <em>default KB</em> below is used.
                    </p>

                    <div class="mb-3">
                        <label class="form-label fw-semibold">Default Knowledge Base</label>
                        <select class="form-select form-select-sm" name="importKbKey" style="width:auto;">
                            <c:forEach var="kb" items="${allKbs}">
                                <option value="${kb.kbKey}"
                                        ${kb.kbKey == selectedKbKey ? 'selected' : ''}>${kb.label}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <label class="form-label fw-semibold">JSON Array <span class="text-danger">*</span></label>
                    <textarea class="form-control form-control-sm" name="importJson" rows="14" required
                              style="font-family:monospace; font-size:0.78rem;"
                              placeholder='[&#10;  {&#10;    "kbKey": "style_voice",&#10;    "title": "Be concise",&#10;    "section": "Tone",&#10;    "content": "Use short sentences. Avoid jargon.",&#10;    "keywords": "tone,concise,writing",&#10;    "chunkType": "STYLE_RULE",&#10;    "visibility": "INTERNAL"&#10;  }&#10;]'></textarea>

                    <div class="mt-2 p-2 bg-light rounded" style="font-size:0.75rem; color:#555;">
                        <strong>Required fields:</strong> <code>title</code>, <code>content</code>, <code>chunkType</code><br>
                        <strong>Optional:</strong> <code>kbKey</code>, <code>section</code>, <code>keywords</code>,
                        <code>accountType</code>, <code>visibility</code>, <code>sourceCitation</code>,
                        <code>effectiveStart</code>, <code>effectiveEnd</code> (dates as YYYY-MM-DD)<br>
                        <strong>chunkType values:</strong> STYLE_RULE, STYLE_EXAMPLE_GOOD, STYLE_EXAMPLE_BAD,
                        FEDERAL_RULE, FEDERAL_LIMIT, FEDERAL_DEADLINE, SSA_OFFERING, SSA_PROCEDURE, SSA_PRICING,
                        SSA_CONTACT, SUMMIT_HOWTO, SUMMIT_GOTCHA, SCENARIO_PLAYBOOK, ESCALATION_TRIGGER
                    </div>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-sm btn-ssa"
                            onclick="return confirm('Import these chunks? Each will be created as active.')">
                        <i class="bi bi-cloud-upload me-1"></i>Import
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- ══════════════════════════════════════════════════════
     HISTORY VIEWER MODAL
════════════════════════════════════════════════════════ --%>
<div class="modal fade" id="historyModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header modal-header-blue">
                <h5 class="modal-title"><i class="bi bi-clock-history me-2"></i>
                    Change History — <span id="hmChunkTitle"></span></h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body p-2">
                <div id="hmEmpty" class="text-center text-muted py-4" style="display:none;">
                    <i class="bi bi-clock" style="font-size:2rem; opacity:0.3;"></i>
                    <p class="mt-1 small">No history recorded yet.</p>
                </div>
                <table class="table table-sm hist-table mb-0" id="hmTable" style="display:none;">
                    <thead class="table-light">
                        <tr>
                            <th style="width:90px;">Change</th>
                            <th>Note</th>
                            <th>Title Before</th>
                            <th style="width:70px;">Was Active</th>
                            <th>By</th>
                            <th style="width:130px;">When</th>
                        </tr>
                    </thead>
                    <tbody id="hmBody"></tbody>
                </table>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>

<%-- ══════════════════════════════════════════════════════
     SERVER-SIDE DATA INJECTION
════════════════════════════════════════════════════════ --%>
<script>
    // Server-rendered JSON maps — Gson html-escaping makes these safe to embed.
    const _chunkData   = ${chunkDataJson};
    const _historyData = ${historyDataJson};
</script>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    // ── Create modal ────────────────────────────────────
    function openCreateModal() {
        document.getElementById('cmAction').value  = 'createChunk';
        document.getElementById('cmChunkId').value = '';
        document.getElementById('cmTitle').textContent = ' Add Chunk';
        document.getElementById('cmTitle2').value  = '';
        document.getElementById('cmSection').value = '';
        document.getElementById('cmContent').value = '';
        document.getElementById('cmKeywords').value = '';
        document.getElementById('cmAccountType').value = '';
        document.getElementById('cmChunkType').value   = '';
        document.getElementById('cmVisibility').value  = 'INTERNAL';
        document.getElementById('cmEffectiveStart').value = '';
        document.getElementById('cmEffectiveEnd').value   = '';
        document.getElementById('cmSourceCitation').value = '';
        document.getElementById('cmChangeNote').value = '';
        document.getElementById('cmChangeNoteGroup').style.display = 'none';
        document.getElementById('cmActiveGroup').style.setProperty('display', 'none', 'important');
        document.getElementById('cmKbGroup').style.display = '';
        document.getElementById('cmSaveBtn').textContent = 'Save Chunk';
        new bootstrap.Modal(document.getElementById('chunkModal')).show();
    }

    // ── Edit modal ──────────────────────────────────────
    function editChunk(id) {
        const d = _chunkData[id];
        if (!d) return;
        document.getElementById('cmAction').value       = 'updateChunk';
        document.getElementById('cmChunkId').value      = id;
        const titleEl = document.getElementById('cmTitle');
        titleEl.innerHTML = '<i class="bi bi-pencil me-2"></i>Edit Chunk';
        document.getElementById('cmTitle2').value        = d.title;
        document.getElementById('cmSection').value       = d.section;
        document.getElementById('cmContent').value       = d.content;
        document.getElementById('cmKeywords').value      = d.keywords;
        document.getElementById('cmAccountType').value   = d.accountType;
        document.getElementById('cmChunkType').value     = d.chunkType;
        document.getElementById('cmVisibility').value    = d.visibility;
        document.getElementById('cmEffectiveStart').value = d.effectiveStart;
        document.getElementById('cmEffectiveEnd').value   = d.effectiveEnd;
        document.getElementById('cmSourceCitation').value = d.sourceCitation;
        document.getElementById('cmChangeNote').value   = '';
        document.getElementById('cmChangeNoteGroup').style.display = '';
        document.getElementById('cmActiveGroup').style.removeProperty('display');
        document.getElementById('cmActive').checked     = d.active;
        // KB selector hidden on edit (KB cannot change)
        document.getElementById('cmKbGroup').style.display = 'none';
        document.getElementById('cmSaveBtn').textContent = 'Save Changes';
        new bootstrap.Modal(document.getElementById('chunkModal')).show();
    }

    // ── Bulk import modal ───────────────────────────────
    function openBulkImportModal() {
        new bootstrap.Modal(document.getElementById('bulkImportModal')).show();
    }

    // ── History modal ───────────────────────────────────
    function openHistoryModal(chunkId, chunkTitle) {
        document.getElementById('hmChunkTitle').textContent = chunkTitle;
        const hist = _historyData[chunkId] || [];
        const tbody  = document.getElementById('hmBody');
        const table  = document.getElementById('hmTable');
        const empty  = document.getElementById('hmEmpty');
        tbody.innerHTML = '';

        if (hist.length === 0) {
            table.style.display  = 'none';
            empty.style.display  = '';
        } else {
            empty.style.display  = 'none';
            table.style.display  = '';
            hist.forEach(h => {
                const badgeClass = 'change-badge-' + h.changeType;
                const tr = document.createElement('tr');
                tr.innerHTML =
                    `<td><span class="badge \${badgeClass}" style="font-size:0.65rem;">\${h.changeType}</span></td>` +
                    `<td>\${escHtml(h.changeNote)}</td>` +
                    `<td class="text-muted">\${escHtml(h.titleBefore)}</td>` +
                    `<td class="text-center">\${h.activeBefore !== null ? (h.activeBefore ? '✓' : '✗') : '—'}</td>` +
                    `<td>\${escHtml(h.modifiedBy)}</td>` +
                    `<td class="text-muted">\${escHtml(h.modifiedOn ? h.modifiedOn.substring(0,19) : '')}</td>`;
                tbody.appendChild(tr);
            });
        }
        new bootstrap.Modal(document.getElementById('historyModal')).show();
    }

    // Minimal HTML escape for JS-constructed DOM content
    function escHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
</script>
</body>
</html>
