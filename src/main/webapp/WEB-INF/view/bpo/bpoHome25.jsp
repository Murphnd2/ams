<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>BPO Dashboard</title>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <style>
        .hdr-bar { background-color: #87a948 !important; border-radius: 0 !important; }
        .bpo-col-left .card { border-radius: 0 !important; border: none !important; }
    </style>
    <style>
        /* ═══ TONAL ZONE LAYOUT (matches PSP ViewHome25 pattern) ═══ */
        body { background-color: #eef0f4; }

        .bpo-layout {
            display: flex;
            flex-direction: column;
            height: calc(100vh - 62px);
            overflow: hidden;
            margin-left: -0.75rem;
            margin-right: -0.75rem;
        }
        .bpo-columns {
            display: flex;
            flex: 1;
            align-items: stretch;
            min-height: 0;
        }
        .bpo-columns > [class*="bpo-col-"] {
            display: flex;
            flex-direction: column;
            min-height: 0;
        }

        /* Left column — warm sage */
        .bpo-col-left {
            flex: 0 0 25%;
            background: #f9faf4;
            border-right: 1px solid #d4dbb8;
            overflow-y: auto;
        }
        .bpo-col-left > .card {
            flex: 1; display: flex; flex-direction: column; overflow: hidden;
            border: none; border-radius: 0; background: transparent;
        }
        .bpo-col-left > .card > .card-body {
            flex: 1; display: flex; flex-direction: column; min-height: 0;
            padding: 0;
        }
        .bpo-col-left .zone-stripe { height: 3px; background: #87a948; margin-bottom: 0; }
        .bpo-col-left .hdr-bar {
            background: #f9faf4 !important; color: #4a5a2c !important;
            border-bottom: 1px solid #d4dbb8 !important;
        }
        .bpo-col-left .hdr-bar i { color: #6b8a3e; }
        .bpo-col-left .hdr-bar .btn-outline-light {
            border-color: #d4dbb8 !important; color: #6b8a3e !important;
        }
        .bpo-col-left .hdr-bar .btn-outline-light:hover {
            background: rgba(107,138,62,0.08) !important;
        }
        .bpo-col-left .todo-body { background: #f9faf4; border-color: #d4dbb8; }
        .bpo-col-left .td-section-toggle { background: rgba(0,0,0,0.025); border-top-color: #d4dbb8; }
        .bpo-col-left .td-count { background: #e8eeda; color: #4a5a2c; }

        /* Center column — white (green header unchanged) */
        .bpo-col-center {
            flex: 1;
            min-width: 0;
            background: white;
            border-left: 1px solid #e5eaf1;
            border-right: 1px solid #e5eaf1;
            overflow: hidden;
        }
        .bpo-col-center .zone-stripe { height: 3px; background: #87a948; margin-bottom: 0; }

        /* Right column — cool slate (slide panel) */
        .bpo-col-right {
            width: 0;
            min-width: 0;
            overflow: hidden;
            transition: width 0.25s ease, min-width 0.25s ease;
            background: #f4f6f9;
            border-left: 0 solid #c8d1dc;
        }
        .bpo-col-right.open {
            width: 420px;
            min-width: 420px;
            border-left-width: 1px;
        }
        .bpo-col-right .zone-stripe { height: 3px; background: #4a7a9b; margin-bottom: 0; }

        /* Mobile: stack columns, reset tonal backgrounds */
        @media (max-width: 991px) {
            .bpo-layout { height: auto; overflow: visible; }
            .bpo-columns { flex-direction: column; }
            .bpo-col-left {
                display: none;
            }
            .bpo-col-center, .bpo-col-left {
                background: white;
                border: none;
                border-bottom: 2px solid #eef0f4;
            }
            .bpo-col-left .hdr-bar {
                background: #87a948 !important; color: white !important;
                border-bottom: none !important;
            }
            .bpo-col-left .hdr-bar i { color: white; }
            .bpo-col-left .hdr-bar .btn-outline-light {
                border-color: rgba(255,255,255,0.4) !important;
                color: rgba(255,255,255,0.85) !important;
            }
            .bpo-col-left .zone-stripe, .bpo-col-center .zone-stripe { display: none; }
            .bpo-col-right {
                position: fixed;
                top: 0; right: 0; bottom: 0;
                width: 0; z-index: 1050;
                background: #f4f6f9;
                box-shadow: -4px 0 16px rgba(0,0,0,0.15);
                transition: width 0.25s ease;
                overflow: hidden;
            }
            .bpo-col-right.open { width: 100%; }
        }

        /* ═══ PSP ACCORDION (Level 1) ═══ */
        .bpo-psp-group { margin-bottom: 0.35rem; border-radius: 6px; overflow: hidden; border: 1px solid #dee2e6; background: #fff; }
        .bpo-psp-header {
            display: flex; align-items: center; padding: 0.55rem 0.75rem;
            cursor: pointer; background: #fff; font-weight: 600; font-size: 0.82rem;
            color: #333; user-select: none; gap: 0.5rem;
            border-bottom: 1px solid transparent; transition: background 0.15s;
        }
        .bpo-psp-header:hover { background: #f8f9fa; }
        .bpo-psp-header.open { border-bottom-color: #dee2e6; }
        .bpo-psp-header .psp-chevron { transition: transform 0.2s; font-size: 0.7rem; color: #999; }
        .bpo-psp-header.open .psp-chevron { transform: rotate(90deg); }
        .bpo-psp-count {
            font-size: 0.65rem; font-weight: 600; padding: 0.12rem 0.45rem;
            border-radius: 10px; background: #e8f4f8; color: #0d5681; margin-left: auto;
        }
        .bpo-psp-body { display: none; }
        .bpo-psp-header.open + .bpo-psp-body { display: block; }

        /* ═══ ACTIVITY ROW (Level 2) ═══ */
        .bpo-activity-group { border-bottom: 1px solid #f0f0f0; }
        .bpo-activity-group:last-child { border-bottom: none; }
        .bpo-activity-header {
            display: flex; align-items: center; padding: 0.4rem 0.75rem 0.4rem 1.4rem;
            cursor: pointer; font-size: 0.78rem; gap: 0.5rem;
            user-select: none; transition: background 0.15s;
        }
        .bpo-activity-header:hover { background: #f8f9fa; }
        .bpo-activity-header .act-chevron { transition: transform 0.2s; font-size: 0.6rem; color: #bbb; }
        .bpo-activity-header.open .act-chevron { transform: rotate(90deg); }
        .bpo-activity-name { flex: 1; font-weight: 500; color: #444; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .bpo-type-badge {
            font-size: 0.6rem; font-weight: 600; padding: 0.08rem 0.35rem;
            border-radius: 3px; text-transform: uppercase; letter-spacing: 0.03em;
        }
        .bpo-type-renewal { background: #e8f0fe; color: #1a56db; }
        .bpo-type-setup { background: #fef3e2; color: #b45309; }
        .bpo-type-ticket { background: #fce8e8; color: #c53030; }
        .bpo-type-checklist { background: #e2e3e5; color: #383d41; }
        .bpo-activity-due { font-size: 0.7rem; font-weight: 500; white-space: nowrap; }
        .bpo-activity-count { font-size: 0.65rem; color: #999; white-space: nowrap; }
        .bpo-activity-tasks { display: none; }
        .bpo-activity-header.open + .bpo-activity-tasks { display: block; }

        /* ═══ TASK ROW (Level 3) ═══ */
        .bpo-task-row {
            display: flex; align-items: center; padding: 0.35rem 0.75rem 0.35rem 2.4rem;
            font-size: 0.78rem; cursor: pointer; gap: 0.5rem;
            transition: background 0.15s; border-top: 1px solid #f5f5f5;
        }
        .bpo-task-row:hover { background: #f0f7fb; }
        .bpo-task-row.active {
            background: #e8f4f8; border-left: 3px solid #0d5681;
            padding-left: calc(2.4rem - 3px);
        }
        .bpo-task-icon { font-size: 0.7rem; color: #aaa; width: 16px; text-align: center; }
        .bpo-task-row.active .bpo-task-icon { color: #0d5681; }
        .bpo-task-name { flex: 1; font-weight: 400; color: #555; }
        .bpo-task-row.active .bpo-task-name { font-weight: 500; color: #0d5681; }

        /* ═══ SHARED BADGES ═══ */
        .bpo-badge-psp { background: #e8f4f8; color: #0d5681; font-size: 0.7rem; padding: 0.12rem 0.45rem; border-radius: 10px; }
        .bpo-badge-status { font-size: 0.65rem; padding: 0.1rem 0.4rem; border-radius: 10px; font-weight: 500; }
        .bpo-badge-unassigned { background: #fff3cd; color: #856404; }
        .bpo-badge-assigned { background: #d4edda; color: #155724; }
        .bpo-badge-pending { background: #f8d7da; color: #842029; }
        .due-overdue { color: #dc3545; font-weight: 600; }
        .due-today { color: #fd7e14; font-weight: 600; }
        .due-future { color: #6c757d; }

        /* ═══ PENDING TOOLBAR ═══ */
        .pending-check { width: 16px; height: 16px; cursor: pointer; accent-color: #0d5681; }
        .pending-toolbar {
            background: #fff3cd; padding: 0.4rem 0.75rem; border-bottom: 1px solid #dee2e6;
            display: flex; align-items: center; gap: 0.5rem; font-size: 0.78rem; flex-shrink: 0;
        }
        .pending-count-badge { background: #dc3545; color: white; font-size: 0.6rem; padding: 0.1rem 0.4rem; border-radius: 8px; margin-left: 0.25rem; }

        /* ═══ DETAIL PANEL — TWO-ZONE SPLIT ═══ */
        .detail-inner {
            width: 420px; height: 100%; display: flex; flex-direction: column; overflow: hidden;
        }
        @media (max-width: 991px) { .detail-inner { width: 100%; } }

        /* Context card (top zone) */
        .detail-context {
            flex-shrink: 0; background: #f4f6f9;
            padding: 0.55rem 0.75rem 0.5rem;
            border-bottom: 2px solid #d0d7e0;
        }
        .detail-context-top {
            display: flex; align-items: flex-start; gap: 0.4rem; margin-bottom: 0.4rem;
        }
        .detail-task-name {
            flex: 1; font-weight: 700; font-size: 0.9rem; color: #1a1a1a; line-height: 1.3;
            min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
        }
        .detail-close-btn {
            background: none; border: none; font-size: 1rem; color: #999;
            cursor: pointer; padding: 0.1rem; line-height: 1; flex-shrink: 0;
        }
        .detail-close-btn:hover { color: #333; }
        .detail-fields {
            display: grid; grid-template-columns: 1fr 1fr;
            gap: 0.3rem 0.75rem; font-size: 0.78rem;
        }
        .detail-field { min-width: 0; }
        .detail-field.full { grid-column: 1 / -1; }
        .detail-field-label {
            font-size: 0.6rem; color: #8899a6; text-transform: uppercase;
            letter-spacing: 0.04em; font-weight: 600; margin-bottom: 0.05rem;
        }
        .detail-field-value {
            color: #333; font-weight: 500;
            white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
        }
        .detail-field-value select {
            font-size: 0.76rem; padding: 0.08rem 1.2rem 0.08rem 0.25rem;
            border: 1px solid #ccd; border-radius: 3px; background: white;
            width: 100%; max-width: 160px;
        }
        .detail-links {
            display: flex; gap: 0.35rem; margin-top: 0.4rem;
        }
        .detail-links a {
            font-size: 0.72rem; padding: 0.18rem 0.55rem; border-radius: 4px;
            text-decoration: none; border: 1px solid #c8d1dc; color: #0d5681;
            transition: background 0.15s;
        }
        .detail-links a:hover { background: rgba(13,86,129,0.06); }
        .detail-links a i { margin-right: 0.2rem; font-size: 0.68rem; }

        /* Completed banner */
        .completed-banner {
            background: #d4edda; padding: 0.35rem 0.75rem;
            font-size: 0.78rem; color: #155724; flex-shrink: 0;
        }

        /* Bottom zone: tabbed content */
        .detail-bottom-zone {
            flex: 1; min-height: 0; display: flex;
            flex-direction: column; background: #fff;
        }
        .detail-tabs {
            display: flex; border-bottom: 1px solid #e0e5ec;
            flex-shrink: 0; background: #fafbfc;
        }
        .detail-tab {
            flex: 1; padding: 0.45rem 0.5rem; font-size: 0.72rem; font-weight: 600;
            text-transform: uppercase; letter-spacing: 0.04em; color: #8899a6;
            border: none; background: none; cursor: pointer; text-align: center;
            border-bottom: 2px solid transparent; transition: color 0.15s, border-color 0.15s;
        }
        .detail-tab:hover { color: #556; }
        .detail-tab.active { color: #0d5681; border-bottom-color: #0d5681; }
        .detail-tab .tab-count {
            display: inline-block; font-size: 0.58rem; min-width: 1rem;
            padding: 0 0.25rem; border-radius: 8px; background: #e0e5ec;
            color: #666; margin-left: 0.25rem; font-weight: 700; line-height: 1.4;
        }
        .detail-tab.active .tab-count { background: #0d5681; color: #fff; }
        .detail-tab-pane {
            display: none; flex: 1; min-height: 0; flex-direction: column;
        }
        .detail-tab-pane.active { display: flex; }
        .detail-notes-stream { flex: 1; overflow-y: auto; padding: 0 0.75rem; }
        .detail-note-bar {
            padding: 0.35rem 0.75rem; border-top: 1px solid #eee;
            flex-shrink: 0; background: #fafbfc;
        }
        .detail-past-runs-list { flex: 1; overflow-y: auto; padding: 0.4rem 0.75rem; }
        .past-run-item {
            padding: 0.4rem 0.55rem; background: #f8f9fa; border-radius: 4px;
            margin-bottom: 0.35rem; border: 1px solid #eee; font-size: 0.78rem;
            display: flex; align-items: center; gap: 0.35rem; cursor: pointer;
        }
        .past-run-item:hover { background: #eef1f5; }
        .past-run-item .run-date { font-weight: 500; color: #333; }
        .past-run-item .run-assignee { color: #6c757d; }
        .past-run-item .run-notes-badge {
            font-size: 0.58rem; background: #0d5681; color: #fff;
            padding: 0.05rem 0.35rem; border-radius: 8px; font-weight: 700; margin-left: auto;
        }
        /* Note bubbles (used by renderNotes) */
        .note-item { padding: 0.35rem 0; border-bottom: 1px solid #f0f0f0; }
        .note-item:last-child { border-bottom: none; }
        .note-header { display: flex; align-items: center; gap: 0.35rem; margin-bottom: 0.1rem; }
        .note-source {
            font-size: 0.6rem; padding: 0.08rem 0.35rem; border-radius: 8px; font-weight: 600;
        }
        .note-source.bpo { background: #e8f4f8; color: #0d5681; }
        .note-source.psp { background: #fff3cd; color: #856404; }
        .note-author { font-size: 0.78rem; font-weight: 500; color: #333; }
        .note-date { font-size: 0.68rem; color: #999; }
        .note-text { font-size: 0.82rem; color: #444; line-height: 1.45; padding-left: 0.15rem; }
        .note-attachment {
            display: inline-block; font-size: 0.72rem; padding: 0.1rem 0.45rem;
            background: #e8f4f8; color: #0d5681; border-radius: 12px;
            text-decoration: none; margin-top: 0.2rem;
        }

        /* Detail footer */
        .detail-footer {
            padding: 0.45rem 0.75rem; border-top: 1px solid #c8d1dc;
            display: flex; justify-content: center; flex-shrink: 0; background: #f4f6f9;
        }

        /* ═══ COMPLETED ═══ */
        .bpo-completed-row { cursor: pointer;
            border-left: 4px solid #198754; border-radius: 3px;
            padding: 0.25rem 0.4rem; margin-bottom: 0.2rem;
            background: #f8f9fa; opacity: 0.75;
        }

        /* ═══ EMPTY STATE ═══ */
        .bpo-empty-state {
            text-align: center; padding: 2rem 1rem; color: #aaa; font-size: 0.85rem;
        }
        .bpo-empty-state i { font-size: 1.5rem; display: block; margin-bottom: 0.3rem; color: #c8c8c8; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
    <div style="height: 6px; background: #eef0f4; margin-left: -0.75rem; margin-right: -0.75rem;"></div>
    <div class="bpo-layout">
    <div class="bpo-columns">

        <%-- ════════════════════════════════════════════ --%>
        <%-- LEFT COLUMN: My Checklists (unchanged)      --%>
        <%-- ════════════════════════════════════════════ --%>
        <div class="bpo-col-left">
            <div class="card">
                <div class="zone-stripe"></div>
                <div class="hdr-bar d-flex align-items-center justify-content-between">
                    <span><i class="bi bi-check2-square me-2"></i>My Checklists</span>
                    <div class="d-flex gap-1">
                        <button class="btn btn-sm btn-outline-light" style="font-size:0.7rem; padding:0.1rem 0.4rem;"
                                data-bs-toggle="modal" data-bs-target="#addReminderModal" title="Add Reminder">
                            <i class="bi bi-bell"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-light" style="font-size:0.7rem; padding:0.1rem 0.4rem;"
                                data-bs-toggle="modal" data-bs-target="#addSimpleChecklistModal" title="Add Checklist">
                            <i class="bi bi-plus-lg"></i>
                        </button>
                    </div>
                </div>
                <div class="card-body">
                    <c:import url="/WEB-INF/view/a/pspHome/columns/toDos/toDoCurrentList25.jsp"/>
                </div>
            </div>
        </div>

        <%-- ════════════════════════════════════════════ --%>
        <%-- CENTER COLUMN: Delegated Tasks (tree view)   --%>
        <%-- ════════════════════════════════════════════ --%>
        <div class="bpo-col-center">
            <div class="zone-stripe"></div>
            <%-- Toolbar --%>
            <div class="hdr-bar d-flex align-items-center justify-content-between">
                <span><i class="bi bi-list-task me-2"></i>Delegated Tasks</span>
                <div class="d-flex gap-2 align-items-center">
                    <input type="text" id="bpoSearch" class="form-control form-control-sm"
                           placeholder="Search tasks..." oninput="filterTree(this.value)"
                           style="font-size:0.7rem; width:160px; background:rgba(255,255,255,0.9); border:1px solid rgba(255,255,255,0.5);">
                    <form method="get" action="BpoHome" class="d-flex gap-1 m-0">
                        <c:if test="${sessionScope.isBpoAdmin}">
                            <button type="submit" name="viewMode" value="pending"
                                    class="btn btn-sm ${viewMode == 'pending' ? 'btn-light' : 'btn-outline-light'}"
                                    style="font-size:0.7rem; padding:0.15rem 0.5rem;">
                                Pending<c:if test="${pendingCount != null && pendingCount > 0}"><span class="pending-count-badge">${pendingCount}</span></c:if>
                            </button>
                        </c:if>
                        <button type="submit" name="viewMode" value="mine"
                                class="btn btn-sm ${viewMode == 'mine' ? 'btn-light' : 'btn-outline-light'}"
                                style="font-size:0.7rem; padding:0.15rem 0.5rem;">
                            My Tasks
                        </button>
                        <button type="submit" name="viewMode" value="unassigned"
                                class="btn btn-sm ${viewMode == 'unassigned' ? 'btn-light' : 'btn-outline-light'}"
                                style="font-size:0.7rem; padding:0.15rem 0.5rem;">
                            Unassigned
                        </button>
                        <button type="submit" name="viewMode" value="all"
                                class="btn btn-sm ${viewMode == 'all' ? 'btn-light' : 'btn-outline-light'}"
                                style="font-size:0.7rem; padding:0.15rem 0.5rem;">
                            All Open
                        </button>
                    </form>
                </div>
            </div>

            <%-- Pending acceptance toolbar (BPO Admin, pending view) --%>
            <c:if test="${viewMode == 'pending' && sessionScope.isBpoAdmin}">
                <div class="pending-toolbar" id="pendingToolbar">
                    <input type="checkbox" class="pending-check" id="pendingSelectAll" onclick="toggleSelectAllPending(this)" title="Select all">
                    <span id="pendingSelectedCount" style="font-weight:500;">0 selected</span>
                    <div class="ms-auto d-flex gap-1 align-items-center">
                        <select id="pendingAssignTo" class="form-select form-select-sm" style="font-size:0.7rem; padding:0.15rem 0.4rem; width:auto; min-width:120px;">
                            <option value="0">-- No Assignment --</option>
                            <c:forEach var="bpo" items="${applicationScope.global.getBpoUsers()}">
                                <option value="${bpo.getId()}">${bpo.getFirstName()} ${bpo.getLastName()}</option>
                            </c:forEach>
                        </select>
                        <button type="button" class="btn btn-sm btn-success" onclick="acceptSelectedPending()" style="font-size:0.7rem; padding:0.15rem 0.5rem; white-space:nowrap;">
                            <i class="bi bi-check-lg me-1"></i>Accept Selected
                        </button>
                        <button type="button" class="btn btn-sm btn-outline-success" onclick="acceptAllPending()" style="font-size:0.7rem; padding:0.15rem 0.5rem; white-space:nowrap;">
                            Accept All
                        </button>
                    </div>
                </div>
            </c:if>

            <%-- Scrollable tree body --%>
            <div id="bpoTreeBody" style="flex:1; overflow-y:auto; padding:0.6rem 0.75rem;">
              <c:choose>

                <%-- ═══ CROSS-SYSTEM MODE ═══ --%>
                <c:when test="${crossSystemMode}">
                  <c:choose>
                    <c:when test="${not empty pspGroups}">
                      <c:forEach var="pspGroup" items="${pspGroups}">
                        <div class="bpo-psp-group">
                          <div class="bpo-psp-header open" onclick="togglePsp(this)">
                              <i class="bi bi-chevron-right psp-chevron"></i>
                              <span>${fn:escapeXml(pspGroup.pspName)}</span>
                              <span class="bpo-psp-count">${pspGroup.taskCount}</span>
                          </div>
                          <div class="bpo-psp-body">
                            <c:forEach var="actGroup" items="${pspGroup.activities}">
                              <%-- Due date class for activity --%>
                              <c:choose>
                                  <c:when test="${actGroup.dueDate != null && actGroup.dueDate < Date.valueOf(LocalDate.now())}">
                                      <c:set var="actDueClass" value="due-overdue"/>
                                  </c:when>
                                  <c:when test="${actGroup.dueDate != null && actGroup.dueDate == Date.valueOf(LocalDate.now())}">
                                      <c:set var="actDueClass" value="due-today"/>
                                  </c:when>
                                  <c:otherwise>
                                      <c:set var="actDueClass" value="due-future"/>
                                  </c:otherwise>
                              </c:choose>
                              <div class="bpo-activity-group">
                                <div class="bpo-activity-header" onclick="toggleActivity(this)">
                                    <i class="bi bi-chevron-right act-chevron"></i>
                                    <span class="bpo-type-badge bpo-type-${fn:toLowerCase(actGroup.activityType)}">${actGroup.activityType}</span>
                                    <span class="bpo-activity-name">${fn:escapeXml(actGroup.activityName)}</span>
                                    <c:if test="${actGroup.dueDate != null}">
                                        <span class="bpo-activity-due ${actDueClass}"><i class="bi bi-calendar3" style="font-size:0.6rem;"></i> <fmt:formatDate value="${actGroup.dueDate}" pattern="MM/dd"/></span>
                                    </c:if>
                                    <span class="bpo-activity-count">(${actGroup.taskCount})</span>
                                </div>
                                <div class="bpo-activity-tasks">
                                  <c:forEach var="dt" items="${actGroup.delegatedTasks}">
                                    <div class="bpo-task-row"
                                         data-todo-id="${dt.id}"
                                         data-todo-guid="${dt.todoGuid}"
                                         data-cross-system="true"
                                         data-task-name="${fn:escapeXml(dt.taskName)}"
                                         data-task-description="${fn:escapeXml(dt.taskDescription)}"
                                         data-psp-name="${fn:escapeXml(dt.pspClient.pspName)}"
                                         data-activity-name="${fn:escapeXml(dt.activityName)}"
                                         data-activity-type="${dt.activityType}"
                                         data-employer-name="${fn:escapeXml(dt.employerName)}"
                                         data-due-date="<fmt:formatDate value='${dt.dueDate}' pattern='MM/dd/yyyy'/>"
                                         data-goto="${dt.gotoLink != null ? dt.gotoLink : ''}"
                                         data-info="${dt.infoLink != null ? dt.infoLink : ''}"
                                         data-assigned-to="${dt.assignedTo != null ? dt.assignedTo.id : '0'}"
                                         data-recurring-series-id="${dt.recurringSeriesId != null ? dt.recurringSeriesId : ''}"
                                         data-psp-client-id="${dt.pspClient.id}"
                                         data-pending="${viewMode == 'pending' ? 'true' : 'false'}"
                                         onclick="openTaskPanel(this)">
                                        <c:if test="${viewMode == 'pending'}">
                                            <input type="checkbox" class="pending-check pending-row-check"
                                                   value="${dt.id}" onclick="event.stopPropagation(); updatePendingCount();">
                                        </c:if>
                                        <i class="bi bi-card-checklist bpo-task-icon"></i>
                                        <span class="bpo-task-name">${fn:escapeXml(dt.taskName)}</span>
                                        <c:choose>
                                            <c:when test="${viewMode == 'pending'}">
                                                <span class="bpo-badge-status bpo-badge-pending ms-auto">Pending</span>
                                            </c:when>
                                            <c:when test="${dt.assignedTo == null}">
                                                <span class="bpo-badge-status bpo-badge-unassigned ms-auto">Unassigned</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="bpo-badge-status bpo-badge-assigned ms-auto">${dt.assignedTo.firstName} ${fn:substring(dt.assignedTo.lastName, 0, 1)}.</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                  </c:forEach>
                                </div>
                              </div>
                            </c:forEach>
                          </div>
                        </div>
                      </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <div class="bpo-empty-state">
                            <i class="bi bi-inbox"></i>
                            <c:choose>
                                <c:when test="${viewMode == 'pending'}">No pending tasks</c:when>
                                <c:otherwise>No delegated tasks</c:otherwise>
                            </c:choose>
                        </div>
                    </c:otherwise>
                  </c:choose>
                </c:when>

                <%-- ═══ CO-LOCATED MODE ═══ --%>
                <c:otherwise>
                  <c:choose>
                    <c:when test="${not empty pspGroups}">
                      <c:forEach var="pspGroup" items="${pspGroups}">
                        <div class="bpo-psp-group">
                          <div class="bpo-psp-header open" onclick="togglePsp(this)">
                              <i class="bi bi-chevron-right psp-chevron"></i>
                              <span>${fn:escapeXml(pspGroup.pspName)}</span>
                              <span class="bpo-psp-count">${pspGroup.taskCount}</span>
                          </div>
                          <div class="bpo-psp-body">
                            <c:forEach var="actGroup" items="${pspGroup.activities}">
                              <c:choose>
                                  <c:when test="${actGroup.dueDate != null && actGroup.dueDate < Date.valueOf(LocalDate.now())}">
                                      <c:set var="actDueClass" value="due-overdue"/>
                                  </c:when>
                                  <c:when test="${actGroup.dueDate != null && actGroup.dueDate == Date.valueOf(LocalDate.now())}">
                                      <c:set var="actDueClass" value="due-today"/>
                                  </c:when>
                                  <c:otherwise>
                                      <c:set var="actDueClass" value="due-future"/>
                                  </c:otherwise>
                              </c:choose>
                              <div class="bpo-activity-group">
                                <div class="bpo-activity-header" onclick="toggleActivity(this)">
                                    <i class="bi bi-chevron-right act-chevron"></i>
                                    <span class="bpo-type-badge bpo-type-${fn:toLowerCase(actGroup.activityType)}">${actGroup.activityType}</span>
                                    <span class="bpo-activity-name">${fn:escapeXml(actGroup.activityName)}</span>
                                    <c:if test="${actGroup.dueDate != null}">
                                        <span class="bpo-activity-due ${actDueClass}"><i class="bi bi-calendar3" style="font-size:0.6rem;"></i> <fmt:formatDate value="${actGroup.dueDate}" pattern="MM/dd"/></span>
                                    </c:if>
                                    <span class="bpo-activity-count">(${actGroup.taskCount})</span>
                                </div>
                                <div class="bpo-activity-tasks">
                                  <c:forEach var="row" items="${actGroup.localTasks}">
                                    <c:set var="todo" value="${row[0]}"/>
                                    <c:set var="activityName" value="${row[1]}"/>
                                    <c:set var="dueDate" value="${row[2]}"/>
                                    <c:set var="pspName" value="${row[3]}"/>
                                    <%-- Derive display activity name with type suffix --%>
                                    <c:choose>
                                        <c:when test="${todo.checkList.renewal != null}"><c:set var="displayActName" value="${todo.checkList.renewal.fullName} Renewal"/></c:when>
                                        <c:when test="${todo.checkList.setup != null}"><c:set var="displayActName" value="${todo.checkList.setup.fullName} Setup"/></c:when>
                                        <c:when test="${todo.checkList.ticket != null}"><c:set var="displayActName" value="${todo.checkList.ticket.fullName} Ticket"/></c:when>
                                        <c:otherwise><c:set var="displayActName" value="${activityName}"/></c:otherwise>
                                    </c:choose>
                                    <div class="bpo-task-row"
                                         data-todo-id="${todo.id}"
                                         data-cross-system="false"
                                         data-task-name="${fn:escapeXml(todo.task.plainDescription)}"
                                         data-task-description=""
                                         data-psp-name="${fn:escapeXml(pspName)}"
                                         data-activity-name="${fn:escapeXml(displayActName)}"
                                         data-activity-type="${actGroup.activityType}"
                                         data-due-date="<fmt:formatDate value='${dueDate}' pattern='MM/dd/yyyy'/>"
                                         data-goto="${todo.task.hasGoTo() && todo.task.goToLink != null ? todo.task.goToLink.linkPath : ''}"
                                         data-info="${todo.task.hasInfo() && todo.task.infoLink != null ? todo.task.infoLink.linkPath : ''}"
                                         data-assigned-to="${todo.bpoAssignedTo != null ? todo.bpoAssignedTo.id : '0'}"
                                         onclick="openTaskPanel(this)">
                                        <i class="bi bi-card-checklist bpo-task-icon"></i>
                                        <span class="bpo-task-name">${fn:escapeXml(todo.task.plainDescription)}</span>
                                        <c:choose>
                                            <c:when test="${todo.bpoAssignedTo == null}">
                                                <span class="bpo-badge-status bpo-badge-unassigned ms-auto">Unassigned</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="bpo-badge-status bpo-badge-assigned ms-auto">Assigned</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                  </c:forEach>
                                </div>
                              </div>
                            </c:forEach>
                          </div>
                        </div>
                      </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <div class="bpo-empty-state">
                            <i class="bi bi-inbox"></i>No delegated tasks
                        </div>
                    </c:otherwise>
                  </c:choose>
                </c:otherwise>

              </c:choose>
            </div>

            <%-- Completed section (same AJAX pattern) --%>
            <div class="px-3 pb-2 pt-1" style="border-top:1px solid #dee2e6; flex-shrink:0; background:#fff;">
                <div class="d-flex align-items-center gap-2">
                    <a class="d-flex align-items-center" data-bs-toggle="collapse" href="#bpoCompletedSection" role="button" aria-expanded="false"
                       style="font-size:0.8rem; color:#6c757d; cursor:pointer; text-decoration:none;">
                        <i class="bi bi-chevron-right me-1" id="completedChevron"></i>Completed
                        <span id="completedCount" class="text-muted ms-1"></span>
                    </a>
                    <div class="ms-auto d-flex gap-1">
                        <button type="button" class="btn btn-sm btn-outline-ssa completed-range active" data-days="1" onclick="setCompletedRange(this)" style="font-size:0.68rem; padding:0.1rem 0.45rem;">1 Day</button>
                        <button type="button" class="btn btn-sm btn-outline-ssa completed-range" data-days="3" onclick="setCompletedRange(this)" style="font-size:0.68rem; padding:0.1rem 0.45rem;">3 Day</button>
                        <button type="button" class="btn btn-sm btn-outline-ssa completed-range" data-days="7" onclick="setCompletedRange(this)" style="font-size:0.68rem; padding:0.1rem 0.45rem;">1 Week</button>
                    </div>
                </div>
                <div class="collapse" id="bpoCompletedSection">
                    <div id="completedTasksList" class="mt-1"></div>
                </div>
            </div>
        </div>

        <%-- ════════════════════════════════════════════ --%>
        <%-- RIGHT COLUMN: Task Detail Panel (Two-Zone)   --%>
        <%-- ════════════════════════════════════════════ --%>
        <div class="bpo-col-right" id="bpoDetailPanel">
            <div class="detail-inner">
                <div class="zone-stripe"></div>

                <%-- TOP ZONE: Context Card --%>
                <div class="detail-context" id="contextCard">
                    <div class="detail-context-top">
                        <div class="detail-task-name" id="panelTaskName"></div>
                        <button class="detail-close-btn" onclick="closeTaskPanel()" title="Close">
                            <i class="bi bi-x-lg"></i>
                        </button>
                    </div>
                    <div class="detail-fields">
                        <div class="detail-field">
                            <div class="detail-field-label">PSP</div>
                            <div class="detail-field-value" id="panelPspName"></div>
                        </div>
                        <div class="detail-field">
                            <div class="detail-field-label">Type</div>
                            <div class="detail-field-value"><span class="bpo-type-badge" id="panelTypeBadge"></span></div>
                        </div>
                        <div class="detail-field full">
                            <div class="detail-field-label">Activity</div>
                            <div class="detail-field-value" id="panelActivityName"></div>
                        </div>
                        <div class="detail-field">
                            <div class="detail-field-label">Due</div>
                            <div class="detail-field-value" id="panelDueDate"></div>
                        </div>
                        <c:if test="${sessionScope.isBpoAdmin}">
                        <div class="detail-field" id="panelAssignField">
                            <div class="detail-field-label">Assigned To</div>
                            <div class="detail-field-value">
                                <select id="panelAssignTo" onchange="assignTaskAjax()">
                                    <option value="0">-- Unassigned --</option>
                                    <c:forEach var="bpo" items="${applicationScope.global.getBpoUsers()}">
                                        <option value="${bpo.getId()}">${bpo.getFirstName()} ${bpo.getLastName()}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        </c:if>
                    </div>
                    <div class="detail-links" id="panelLinksRow" style="display:none;">
                        <a id="panelGoToLink" href="#" target="_blank"><i class="bi bi-box-arrow-up-right"></i>Go To</a>
                        <a id="panelInfoLink" href="#" target="_blank"><i class="bi bi-info-circle"></i>Info</a>
                    </div>
                </div>

                <%-- Completed banner (hidden by default) --%>
                <div id="panelCompletedBanner" class="completed-banner" style="display:none;">
                    <i class="bi bi-check-circle-fill me-1"></i>Completed by <strong id="panelCompletedBy"></strong>
                    <span id="panelCompletedDate" class="ms-1" style="color:#6c757d;"></span>
                </div>

                <%-- BOTTOM ZONE: Tabbed Content --%>
                <div class="detail-bottom-zone">
                    <div class="detail-tabs">
                        <button class="detail-tab active" id="panelNotesTab" onclick="switchDetailTab('notes')">
                            <i class="bi bi-chat-left-text me-1"></i>Notes<span class="tab-count" id="panelNotesCount">0</span>
                        </button>
                        <button class="detail-tab" id="panelHistoryTab" onclick="switchDetailTab('history')">
                            <i class="bi bi-clock-history me-1"></i>Past Runs<span class="tab-count" id="panelHistoryCount">0</span>
                        </button>
                    </div>

                    <%-- Notes tab pane --%>
                    <div class="detail-tab-pane active" id="panelNotesPane">
                        <div class="detail-notes-stream" id="panelNotesStream">
                            <div class="text-center text-muted fst-italic py-3" style="font-size:0.8rem;">Select a task</div>
                        </div>
                        <div class="detail-note-bar" id="panelAddNoteSection">
                            <div class="input-group input-group-sm">
                                <input type="text" class="form-control" id="panelNoteInput" placeholder="Add a note..."
                                       onkeydown="if(event.key==='Enter'){addNoteAjax(); event.preventDefault();}">
                                <label class="btn btn-sm btn-outline-secondary mb-0" style="display:flex; align-items:center; cursor:pointer;">
                                    <i class="bi bi-paperclip"></i>
                                    <input type="file" id="panelNoteFile" style="display:none;" onchange="updateFileLabel(this)">
                                </label>
                                <button type="button" class="btn btn-sm btn-outline-success" onclick="addNoteAjax()">
                                    <i class="bi bi-send"></i>
                                </button>
                            </div>
                            <div id="panelFileRow" style="display:none; margin-top:0.2rem; font-size:0.75rem;">
                                <span id="panelFileLabel" style="color:#0d5681;"></span>
                                <span id="panelFileClear" style="color:#dc3545; cursor:pointer; margin-left:0.3rem;" onclick="clearFileInput()">&#10005;</span>
                            </div>
                        </div>
                    </div>

                    <%-- Past Runs tab pane --%>
                    <div class="detail-tab-pane" id="panelHistoryPane">
                        <div class="detail-past-runs-list" id="panelPastRunsList">
                            <div class="text-center text-muted fst-italic py-3" style="font-size:0.8rem;">No past runs</div>
                        </div>
                    </div>
                </div>

                <%-- Footer --%>
                <div class="detail-footer" id="panelFooter">
                    <c:if test="${sessionScope.isBpoAdmin}">
                        <button type="button" class="ssa-action save" id="panelAcceptBtn" style="display:none;" onclick="acceptFromPanel()">
                            <i class="bi bi-check2-circle me-1"></i>Accept
                        </button>
                        <span id="panelAcceptSep" class="ssa-action-sep" style="display:none;">|</span>
                    </c:if>
                    <button type="button" class="ssa-action save" id="panelCompleteBtn" onclick="markCompleteAjax()">
                        <i class="bi bi-check-circle me-1"></i>Mark Complete
                    </button>
                    <span class="ssa-action-sep">|</span>
                    <button type="button" class="ssa-action cancel" onclick="closeTaskPanel()"><i class="bi bi-x-lg me-1"></i>Close</button>
                </div>
            </div>
        </div>

    </div>
    </div>
</div>

<script>
    // ═══ TREE EXPAND/COLLAPSE ═══

    function togglePsp(header) {
        header.classList.toggle('open');
    }

    function toggleActivity(header) {
        header.classList.toggle('open');
    }

    // ═══ TASK DETAIL PANEL ═══

    var currentTaskRow = null;
    var currentCrossSystem = false;
    var currentTodoGuid = '';
    var currentIsPending = false;
    var currentTodoId = '';

    function openTaskPanel(row) {
        // Reset completed/read-only state — ensure editable controls visible
        document.getElementById('panelCompletedBanner').style.display = 'none';
        document.getElementById('panelAddNoteSection').style.display = '';
        document.getElementById('panelFooter').style.display = '';
        const assignField = document.getElementById('panelAssignField');
        if (assignField) assignField.style.display = '';

        // Reset to Notes tab
        switchDetailTab('notes');

        // Deselect previous
        if (currentTaskRow) currentTaskRow.classList.remove('active');
        row.classList.add('active');
        currentTaskRow = row;

        // Read data attributes
        currentTodoId = row.dataset.todoId;
        currentCrossSystem = row.dataset.crossSystem === 'true';
        currentTodoGuid = row.dataset.todoGuid || '';
        currentIsPending = row.dataset.pending === 'true';

        const taskName = row.dataset.taskName || '';
        const pspName = row.dataset.pspName || '';
        const activityName = row.dataset.activityName || '';
        const activityType = row.dataset.activityType || '';
        const dueDate = row.dataset.dueDate || '';
        const gotoUrl = row.dataset.goto || '';
        const infoUrl = row.dataset.info || '';
        const assignedTo = row.dataset.assignedTo || '0';

        // Populate context card
        document.getElementById('panelTaskName').textContent = taskName;
        document.getElementById('panelPspName').textContent = pspName;
        document.getElementById('panelActivityName').textContent = activityName;

        // Type badge
        const typeBadge = document.getElementById('panelTypeBadge');
        typeBadge.textContent = activityType;
        typeBadge.className = 'bpo-type-badge bpo-type-' + activityType.toLowerCase();
        typeBadge.style.cssText = '';

        // Due date with color
        const dueDateEl = document.getElementById('panelDueDate');
        if (dueDate) {
            const parts = dueDate.split('/');
            if (parts.length === 3) {
                const d = new Date(parts[2], parts[0] - 1, parts[1]);
                const today = new Date(); today.setHours(0,0,0,0);
                const diffDays = Math.round((today - d) / 86400000);
                if (d < today) {
                    dueDateEl.innerHTML = '<span class="due-overdue">' + parts[0] + '/' + parts[1] + ' \u00b7 ' + diffDays + ' day' + (diffDays !== 1 ? 's' : '') + ' overdue</span>';
                } else if (d.getTime() === today.getTime()) {
                    dueDateEl.innerHTML = '<span class="due-today">' + parts[0] + '/' + parts[1] + ' \u00b7 Due today</span>';
                } else {
                    dueDateEl.innerHTML = '<span class="due-future">' + parts[0] + '/' + parts[1] + '</span>';
                }
            } else {
                dueDateEl.textContent = dueDate;
            }
        } else {
            dueDateEl.textContent = '\u2014';
        }

        // Show/hide Accept vs Complete based on pending state
        const acceptBtn = document.getElementById('panelAcceptBtn');
        const acceptSep = document.getElementById('panelAcceptSep');
        const completeBtn = document.getElementById('panelCompleteBtn');
        if (acceptBtn) {
            acceptBtn.style.display = currentIsPending ? '' : 'none';
            acceptSep.style.display = currentIsPending ? '' : 'none';
        }
        if (completeBtn) {
            completeBtn.style.display = currentIsPending ? 'none' : '';
        }

        // Links
        const linksRow = document.getElementById('panelLinksRow');
        const goToLink = document.getElementById('panelGoToLink');
        const infoLink = document.getElementById('panelInfoLink');
        let hasLinks = false;
        if (gotoUrl) { goToLink.href = gotoUrl; goToLink.style.display = ''; hasLinks = true; }
        else { goToLink.style.display = 'none'; }
        if (infoUrl) { infoLink.href = infoUrl; infoLink.style.display = ''; hasLinks = true; }
        else { infoLink.style.display = 'none'; }
        linksRow.style.display = hasLinks ? '' : 'none';

        // Assignment dropdown
        const assignSelect = document.getElementById('panelAssignTo');
        if (assignSelect) assignSelect.value = assignedTo;

        // Load notes
        const notesStream = document.getElementById('panelNotesStream');
        notesStream.innerHTML = '<div class="text-center text-muted fst-italic py-3" style="font-size:0.8rem;">Loading...</div>';
        const notesUrl = currentCrossSystem
            ? 'BpoGetNotes?todoGuid=' + encodeURIComponent(currentTodoGuid)
            : 'BpoGetNotes?todoId=' + currentTodoId;
        fetch(notesUrl)
            .then(r => r.json())
            .then(notes => renderNotes(notes))
            .catch(() => { notesStream.innerHTML = '<div class="text-center text-muted py-3" style="font-size:0.8rem;">Could not load notes</div>'; });

        // Past Runs (recurring cross-system tasks)
        const recurringSeriesId = row.dataset.recurringSeriesId || '';
        const pspClientId = row.dataset.pspClientId || '';
        const pastRunsList = document.getElementById('panelPastRunsList');
        const historyCount = document.getElementById('panelHistoryCount');
        pastRunsList.innerHTML = '<div class="text-center text-muted fst-italic py-3" style="font-size:0.8rem;">No past runs</div>';
        historyCount.textContent = '0';

        if (recurringSeriesId && currentCrossSystem) {
            pastRunsList.innerHTML = '<div class="text-center text-muted fst-italic py-3" style="font-size:0.8rem;">Loading...</div>';
            fetch('BpoRecurringHistory?recurringSeriesId=' + encodeURIComponent(recurringSeriesId) + '&pspClientId=' + encodeURIComponent(pspClientId))
                .then(r => r.json())
                .then(runs => {
                    if (!runs || runs.length === 0) {
                        pastRunsList.innerHTML = '<div class="text-center text-muted fst-italic py-3" style="font-size:0.8rem;">No past runs</div>';
                        historyCount.textContent = '0';
                        return;
                    }
                    historyCount.textContent = runs.length;
                    let html = '';
                    runs.forEach(function(run) {
                        const dueLabel = run.dueDate || '\u2014';
                        const doneBy = run.completedByName || '';
                        const noteCount = run.noteCount || 0;
                        const notesBadge = noteCount > 0
                            ? '<span class="run-notes-badge">' + noteCount + ' note' + (noteCount !== 1 ? 's' : '') + '</span>'
                            : '';
                        html += '<div class="past-run-item" data-todo-guid="' + (run.todoGuid || '') + '" onclick="loadPastRunNotesInline(this)">';
                        html += '<span class="run-date">' + dueLabel + '</span>';
                        if (doneBy) html += '<span>\u00b7</span><span class="run-assignee">' + doneBy + '</span>';
                        html += notesBadge;
                        html += '</div>';
                    });
                    pastRunsList.innerHTML = html;
                })
                .catch(function() {
                    pastRunsList.innerHTML = '<div class="text-center text-muted py-3" style="font-size:0.8rem;">Could not load history</div>';
                });
        }

        // Clear note input
        document.getElementById('panelNoteInput').value = '';
        clearFileInput();

        // Open panel
        document.getElementById('bpoDetailPanel').classList.add('open');
    }

    function closeTaskPanel() {
        document.getElementById('bpoDetailPanel').classList.remove('open');
        if (currentTaskRow) {
            currentTaskRow.classList.remove('active');
            currentTaskRow = null;
        }
    }

    function openCompletedPanel(row) {
        // Deselect previous
        if (currentTaskRow) currentTaskRow.classList.remove('active');
        row.classList.add('active');
        currentTaskRow = row;

        // Reset to Notes tab
        switchDetailTab('notes');

        const taskName = row.dataset.taskName || '';
        const pspName = row.dataset.pspName || '';
        const activityName = row.dataset.activityName || '';
        const completedBy = row.dataset.completedBy || '';
        const completedDate = row.dataset.completedDate || '';
        const crossSystem = row.dataset.crossSystem === 'true';
        const todoId = row.dataset.todoId || '';
        const todoGuid = row.dataset.todoGuid || '';

        // Populate context card
        document.getElementById('panelTaskName').textContent = taskName;
        document.getElementById('panelPspName').textContent = pspName;
        document.getElementById('panelActivityName').textContent = activityName;

        // Type badge — show "Completed"
        const typeBadge = document.getElementById('panelTypeBadge');
        typeBadge.textContent = 'COMPLETED';
        typeBadge.className = 'bpo-type-badge';
        typeBadge.style.cssText = 'background:#d4edda; color:#155724;';

        // Due date — show completed date
        const dueDateEl = document.getElementById('panelDueDate');
        dueDateEl.innerHTML = '<span style="color:#6c757d;">Completed ' + completedDate + '</span>';

        // Completed banner
        document.getElementById('panelCompletedBanner').style.display = '';
        document.getElementById('panelCompletedBy').textContent = completedBy;
        document.getElementById('panelCompletedDate').textContent = completedDate ? '\u00b7 ' + completedDate : '';

        // Hide editable controls
        document.getElementById('panelAddNoteSection').style.display = 'none';
        document.getElementById('panelFooter').style.display = 'none';
        const assignField = document.getElementById('panelAssignField');
        if (assignField) assignField.style.display = 'none';

        // Hide links
        document.getElementById('panelLinksRow').style.display = 'none';

        // Clear past runs tab
        document.getElementById('panelPastRunsList').innerHTML = '<div class="text-center text-muted fst-italic py-3" style="font-size:0.8rem;">No past runs</div>';
        document.getElementById('panelHistoryCount').textContent = '0';

        // Load notes
        const notesStream = document.getElementById('panelNotesStream');
        notesStream.innerHTML = '<div class="text-center text-muted fst-italic py-3" style="font-size:0.8rem;">Loading...</div>';
        const notesUrl = crossSystem
            ? 'BpoGetNotes?todoGuid=' + encodeURIComponent(todoGuid)
            : 'BpoGetNotes?todoId=' + todoId;
        fetch(notesUrl)
            .then(r => r.json())
            .then(notes => renderNotes(notes))
            .catch(() => { notesStream.innerHTML = '<div class="text-center text-muted py-3" style="font-size:0.8rem;">Could not load notes</div>'; });

        // Open panel
        document.getElementById('bpoDetailPanel').classList.add('open');
    }

    // ESC key closes panel
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') closeTaskPanel();
    });

    // ═══ TAB SWITCHING ═══

    function switchDetailTab(tab) {
        document.getElementById('panelNotesTab').classList.toggle('active', tab === 'notes');
        document.getElementById('panelHistoryTab').classList.toggle('active', tab === 'history');
        document.getElementById('panelNotesPane').classList.toggle('active', tab === 'notes');
        document.getElementById('panelHistoryPane').classList.toggle('active', tab === 'history');
    }

    // ═══ MARK COMPLETE (AJAX) ═══

    function markCompleteAjax() {
        if (!currentTodoId) return;
        const btn = document.getElementById('panelCompleteBtn');
        btn.disabled = true;
        btn.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Completing...';

        let body = 'action=complete&todoId=' + currentTodoId;
        if (currentCrossSystem) {
            body += '&crossSystem=true&todoGuid=' + encodeURIComponent(currentTodoGuid);
        }

        fetch('BpoCompleteTask', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: body
        })
        .then(r => {
            if (r.ok) {
                // Remove task row from tree
                if (currentTaskRow) {
                    const actTasks = currentTaskRow.closest('.bpo-activity-tasks');
                    const actHeader = actTasks ? actTasks.previousElementSibling : null;
                    const actGroup = actTasks ? actTasks.closest('.bpo-activity-group') : null;
                    const pspBody = currentTaskRow.closest('.bpo-psp-body');
                    const pspGroup = pspBody ? pspBody.closest('.bpo-psp-group') : null;
                    const pspHeader = pspGroup ? pspGroup.querySelector('.bpo-psp-header') : null;

                    currentTaskRow.remove();

                    // Clean up empty groups
                    if (actTasks && actTasks.querySelectorAll('.bpo-task-row').length === 0 && actGroup) {
                        actGroup.remove();
                    } else if (actHeader) {
                        const actCount = actHeader.querySelector('.bpo-activity-count');
                        if (actCount && actTasks) actCount.textContent = '(' + actTasks.querySelectorAll('.bpo-task-row').length + ')';
                    }
                    if (pspBody && pspBody.querySelectorAll('.bpo-task-row').length === 0 && pspGroup) {
                        pspGroup.remove();
                    } else if (pspHeader) {
                        const pspCount = pspHeader.querySelector('.bpo-psp-count');
                        if (pspCount && pspBody) pspCount.textContent = pspBody.querySelectorAll('.bpo-task-row').length;
                    }
                    currentTaskRow = null;
                }
                closeTaskPanel();
            } else {
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Mark Complete';
                alert('Could not complete task. Please try again.');
            }
        })
        .catch(() => {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-check-circle me-1"></i>Mark Complete';
        });
    }

    // ═══ NOTES ═══

    function renderNotes(notes) {
        const notesStream = document.getElementById('panelNotesStream');
        const notesCount = document.getElementById('panelNotesCount');
        if (!notes || notes.length === 0) {
            notesStream.innerHTML = '<div class="text-center text-muted fst-italic py-3" style="font-size:0.8rem;">No notes yet</div>';
            notesCount.textContent = '0';
            return;
        }
        notesCount.textContent = notes.length;
        let html = '';
        notes.forEach(n => {
            const sourceClass = n.source === 'BPO' ? 'bpo' : 'psp';
            html += '<div class="note-item">';
            html += '<div class="note-header">';
            html += '<span class="note-source ' + sourceClass + '">' + n.source + '</span>';
            html += '<span class="note-author">' + n.author + '</span>';
            html += '<span class="note-date">' + n.date + '</span>';
            html += '</div>';
            html += '<div class="note-text">' + n.text + '</div>';
            if (n.attachments && n.attachments.length > 0) {
                n.attachments.forEach(att => {
                    html += '<a href="' + att.url + '" target="_blank" class="note-attachment">'
                        + '<i class="bi bi-paperclip"></i> ' + att.name + '</a>';
                });
            }
            html += '</div>';
        });
        notesStream.innerHTML = html;
    }

    function addNoteAjax() {
        const input = document.getElementById('panelNoteInput');
        const noteText = input.value.trim();
        if (!noteText) return;

        const fileInput = document.getElementById('panelNoteFile');
        input.disabled = true;

        const formData = new FormData();
        formData.append('action', 'addNote');
        formData.append('todoId', currentTodoId);
        formData.append('noteText', noteText);
        if (currentCrossSystem) {
            formData.append('crossSystem', 'true');
            formData.append('todoGuid', currentTodoGuid);
        }
        if (fileInput.files.length > 0) {
            formData.append('noteFile', fileInput.files[0]);
        }

        fetch('BpoCompleteTask', { method: 'POST', body: formData })
            .then(() => {
                input.value = '';
                input.disabled = false;
                clearFileInput();
                input.focus();
                const notesUrl = currentCrossSystem
                    ? 'BpoGetNotes?todoGuid=' + encodeURIComponent(currentTodoGuid)
                    : 'BpoGetNotes?todoId=' + currentTodoId;
                return fetch(notesUrl);
            })
            .then(r => r.json())
            .then(notes => renderNotes(notes))
            .catch(() => { input.disabled = false; });
    }

    function updateFileLabel(input) {
        const fileRow = document.getElementById('panelFileRow');
        const label = document.getElementById('panelFileLabel');
        if (input.files.length > 0) {
            label.textContent = input.files[0].name;
            fileRow.style.display = '';
        } else {
            label.textContent = '';
            fileRow.style.display = 'none';
        }
    }

    function clearFileInput() {
        document.getElementById('panelNoteFile').value = '';
        document.getElementById('panelFileLabel').textContent = '';
        document.getElementById('panelFileRow').style.display = 'none';
    }

    function loadPastRunNotesInline(runItem) {
        const todoGuid = runItem.dataset.todoGuid;
        if (!todoGuid || runItem.dataset.loaded === 'true') return;
        runItem.dataset.loaded = 'true';

        // Toggle expand: add a notes sub-area below the run item
        const notesArea = document.createElement('div');
        notesArea.style.cssText = 'padding:0.3rem 0.4rem; border-top:1px solid #eee; margin-top:0.25rem;';
        notesArea.innerHTML = '<div class="text-muted fst-italic" style="font-size:0.75rem;">Loading notes...</div>';
        runItem.appendChild(notesArea);
        runItem.style.flexWrap = 'wrap';

        fetch('BpoGetNotes?todoGuid=' + encodeURIComponent(todoGuid))
            .then(r => r.json())
            .then(notes => {
                if (!notes || notes.length === 0) {
                    notesArea.innerHTML = '<div class="text-muted fst-italic" style="font-size:0.75rem;">No notes for this run.</div>';
                    return;
                }
                let html = '';
                notes.forEach(function(n) {
                    const sourceClass = n.source === 'BPO' ? 'bpo' : 'psp';
                    html += '<div style="padding:0.2rem 0; font-size:0.75rem;">';
                    html += '<span class="note-source ' + sourceClass + '" style="font-size:0.55rem;">' + (n.source || '') + '</span> ';
                    html += '<span style="font-weight:500; color:#555;">' + (n.author || '') + '</span>';
                    if (n.date) html += ' <span style="color:#999; font-size:0.68rem;">' + n.date + '</span>';
                    html += '<div style="color:#333; margin-top:1px;">' + (n.text || '') + '</div>';
                    if (n.attachments && n.attachments.length > 0) {
                        n.attachments.forEach(function(att) {
                            html += '<a href="' + att.url + '" target="_blank" class="note-attachment" style="font-size:0.68rem;">'
                                + '<i class="bi bi-paperclip"></i> ' + att.name + '</a>';
                        });
                    }
                    html += '</div>';
                });
                notesArea.innerHTML = html;
                notesArea.style.width = '100%';
            })
            .catch(function() {
                notesArea.innerHTML = '<div class="text-muted" style="font-size:0.75rem;">Could not load notes.</div>';
            });
    }

    // ═══ ASSIGNMENT ═══

    function assignTaskAjax() {
        const assignSelect = document.getElementById('panelAssignTo');
        const assigneeId = assignSelect.value;

        assignSelect.disabled = true;

        let body = 'action=assign&todoId=' + currentTodoId + '&assigneeId=' + assigneeId;
        if (currentCrossSystem) body += '&crossSystem=true';

        fetch('BpoCompleteTask', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: body
        })
        .then(r => {
            if (r.ok && currentTaskRow) {
                currentTaskRow.dataset.assignedTo = assigneeId;
                const badge = currentTaskRow.querySelector('.bpo-badge-status');
                if (badge) {
                    if (assigneeId === '0') {
                        badge.className = 'bpo-badge-status bpo-badge-unassigned ms-auto';
                        badge.textContent = 'Unassigned';
                    } else {
                        badge.className = 'bpo-badge-status bpo-badge-assigned ms-auto';
                        // Get name from dropdown option text
                        const opt = assignSelect.options[assignSelect.selectedIndex];
                        const parts = opt.textContent.trim().split(' ');
                        badge.textContent = parts[0] + ' ' + (parts.length > 1 ? parts[1].charAt(0) + '.' : '');
                    }
                }
            }
        })
        .catch(() => {})
        .finally(() => { assignSelect.disabled = false; });
    }

    // ═══ PENDING TASK ACCEPTANCE ═══

    function toggleSelectAllPending(master) {
        document.querySelectorAll('.pending-row-check').forEach(cb => { cb.checked = master.checked; });
        updatePendingCount();
    }

    function updatePendingCount() {
        const checked = document.querySelectorAll('.pending-row-check:checked');
        const label = document.getElementById('pendingSelectedCount');
        if (label) label.textContent = checked.length + ' selected';
    }

    function getSelectedPendingIds() {
        return Array.from(document.querySelectorAll('.pending-row-check:checked')).map(cb => cb.value);
    }

    function acceptSelectedPending() {
        const ids = getSelectedPendingIds();
        if (ids.length === 0) return;
        const assigneeId = document.getElementById('pendingAssignTo')?.value || '0';
        doAcceptTasks(ids, assigneeId);
    }

    function acceptAllPending() {
        const ids = Array.from(document.querySelectorAll('.pending-row-check')).map(cb => cb.value);
        if (ids.length === 0) return;
        const assigneeId = document.getElementById('pendingAssignTo')?.value || '0';
        doAcceptTasks(ids, assigneeId);
    }

    function acceptFromPanel() {
        if (!currentTodoId) return;
        const assignSelect = document.getElementById('panelAssignTo');
        const assigneeId = assignSelect ? assignSelect.value : '0';
        doAcceptTasks([currentTodoId], assigneeId, function() {
            closeTaskPanel();
        });
    }

    function doAcceptTasks(ids, assigneeId, callback) {
        let body = 'action=accept&todoIds=' + ids.join(',');
        if (assigneeId && assigneeId !== '0') body += '&assigneeId=' + assigneeId;

        fetch('BpoCompleteTask', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: body
        })
        .then(r => {
            if (r.ok) {
                ids.forEach(id => {
                    const row = document.querySelector('.bpo-task-row[data-todo-id="' + id + '"]');
                    if (row) {
                        const actTasks = row.closest('.bpo-activity-tasks');
                        const actHeader = actTasks ? actTasks.previousElementSibling : null;
                        const actGroup = actTasks ? actTasks.closest('.bpo-activity-group') : null;
                        const pspBody = row.closest('.bpo-psp-body');
                        const pspGroup = pspBody ? pspBody.closest('.bpo-psp-group') : null;
                        const pspHeader = pspGroup ? pspGroup.querySelector('.bpo-psp-header') : null;

                        row.remove();

                        // Remove empty activity group
                        if (actTasks && actTasks.querySelectorAll('.bpo-task-row').length === 0 && actGroup) {
                            actGroup.remove();
                        } else if (actHeader) {
                            // Update activity task count
                            const actCount = actHeader.querySelector('.bpo-activity-count');
                            if (actCount && actTasks) {
                                actCount.textContent = '(' + actTasks.querySelectorAll('.bpo-task-row').length + ')';
                            }
                        }

                        // Remove empty PSP group or update count
                        if (pspBody && pspBody.querySelectorAll('.bpo-task-row').length === 0 && pspGroup) {
                            pspGroup.remove();
                        } else if (pspHeader) {
                            const pspCount = pspHeader.querySelector('.bpo-psp-count');
                            if (pspCount && pspBody) {
                                pspCount.textContent = pspBody.querySelectorAll('.bpo-task-row').length;
                            }
                        }
                    }
                });
                updatePendingCount();

                // Update pending count badge in toolbar button
                const remaining = document.querySelectorAll('.pending-row-check').length;
                const badge = document.querySelector('.pending-count-badge');
                if (badge) {
                    if (remaining > 0) badge.textContent = remaining;
                    else badge.remove();
                }

                // Show empty state if no tasks left
                if (remaining === 0) {
                    const toolbar = document.getElementById('pendingToolbar');
                    if (toolbar) toolbar.style.display = 'none';
                    const treeBody = document.getElementById('bpoTreeBody');
                    if (treeBody) {
                        treeBody.innerHTML = '<div class="bpo-empty-state"><i class="bi bi-inbox"></i>No pending tasks</div>';
                    }
                }
                if (callback) callback();
            }
        })
        .catch(() => {});
    }

    // ═══ SEARCH / FILTER ═══

    function filterTree(query) {
        query = query.toLowerCase().trim();

        document.querySelectorAll('.bpo-psp-group').forEach(pspGroup => {
            const pspHeader = pspGroup.querySelector('.bpo-psp-header');
            const pspBody = pspGroup.querySelector('.bpo-psp-body');
            let pspHasMatch = false;

            pspBody.querySelectorAll('.bpo-activity-group').forEach(actGroup => {
                const actHeader = actGroup.querySelector('.bpo-activity-header');
                const actTasks = actGroup.querySelector('.bpo-activity-tasks');
                let actHasMatch = false;

                actTasks.querySelectorAll('.bpo-task-row').forEach(taskRow => {
                    const taskName = (taskRow.dataset.taskName || '').toLowerCase();
                    const actName = (taskRow.dataset.activityName || '').toLowerCase();
                    const pspName = (taskRow.dataset.pspName || '').toLowerCase();
                    const match = !query || taskName.includes(query) || actName.includes(query) || pspName.includes(query);
                    taskRow.style.display = match ? '' : 'none';
                    if (match) actHasMatch = true;
                });

                actGroup.style.display = actHasMatch ? '' : 'none';
                // Auto-expand matching groups when searching
                if (query && actHasMatch && !actHeader.classList.contains('open')) {
                    actHeader.classList.add('open');
                }
                if (actHasMatch) pspHasMatch = true;
            });

            pspGroup.style.display = pspHasMatch ? '' : 'none';
            if (query && pspHasMatch && !pspHeader.classList.contains('open')) {
                pspHeader.classList.add('open');
            }
        });
    }

    // ═══ COMPLETED TASKS ═══

    var completedDays = 1;

    document.getElementById('bpoCompletedSection')?.addEventListener('show.bs.collapse', function(){
        document.getElementById('completedChevron')?.classList.replace('bi-chevron-right','bi-chevron-down');
        loadCompletedTasks();
    });
    document.getElementById('bpoCompletedSection')?.addEventListener('hide.bs.collapse', function(){
        document.getElementById('completedChevron')?.classList.replace('bi-chevron-down','bi-chevron-right');
    });

    function setCompletedRange(btn) {
        document.querySelectorAll('.completed-range').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        completedDays = parseInt(btn.dataset.days);
        loadCompletedTasks();
    }

    function loadCompletedTasks() {
        const container = document.getElementById('completedTasksList');
        container.innerHTML = '<div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">Loading...</div>';

        fetch('BpoCompletedTasks?days=' + completedDays)
            .then(r => r.json())
            .then(items => {
                document.getElementById('completedCount').textContent = '(' + items.length + ')';
                if (items.length === 0) {
                    container.innerHTML = '<div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">' +
                        '<i class="bi bi-check-circle" style="font-size:1.1rem; display:block; margin-bottom:0.15rem; color:#c8c8c8;"></i>' +
                        'No completed tasks</div>';
                    return;
                }
                let html = '';
                items.forEach(item => {
                    html += '<div class="bpo-completed-row"'
                        + ' data-todo-id="' + (item.todoId || '') + '"'
                        + ' data-todo-guid="' + (item.todoGuid || '') + '"'
                        + ' data-cross-system="' + (item.crossSystem || 'false') + '"'
                        + ' data-task-name="' + (item.taskName || '').replace(/"/g, '&quot;') + '"'
                        + ' data-activity-name="' + (item.activityName || '').replace(/"/g, '&quot;') + '"'
                        + ' data-psp-name="' + (item.pspName || '').replace(/"/g, '&quot;') + '"'
                        + ' data-completed="true"'
                        + ' data-completed-by="' + (item.completedBy || '') + '"'
                        + ' data-completed-date="' + (item.completedDate || '') + '"'
                        + ' onclick="openCompletedPanel(this)">';
                    html += '  <div class="d-flex align-items-center">';
                    html += '    <i class="bi bi-check-circle-fill text-success me-2" style="font-size:0.85rem;"></i>';
                    html += '    <div class="flex-grow-1" style="min-width:0;">';
                    html += '      <div style="font-size:0.8rem; font-weight:500; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; text-decoration:line-through; color:#6c757d;">' + item.taskName + '</div>';
                    html += '      <div style="font-size:0.7rem; color:#999;">' + item.activityName + ' &middot; ' + item.pspName + '</div>';
                    html += '    </div>';
                    html += '    <span style="font-size:0.68rem; color:#6c757d; white-space:nowrap; margin-left:0.5rem;">' + item.completedBy + (item.completedDate ? ' &middot; ' + item.completedDate : '') + '</span>';
                    html += '  </div>';
                    html += '</div>';
                });
                container.innerHTML = html;
            })
            .catch(() => {
                container.innerHTML = '<div class="text-center text-danger py-2" style="font-size:0.8rem;">Error loading tasks</div>';
            });
    }
</script>
</body>
</html>
