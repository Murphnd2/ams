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
        .hdr-bar { background-color: #87a948 !important; }
    </style>
    <style>
        /* ═══ 3-COLUMN LAYOUT ═══ */
        @media (min-width: 992px) {
            .bpo-layout {
                display: flex;
                flex-direction: column;
                height: calc(100vh - 76px);
                overflow: hidden;
            }
            .bpo-columns {
                display: flex;
                flex: 1;
                min-height: 0;
                gap: 0.75rem;
                padding-left: 0.75rem;
            }
            .bpo-col-left {
                width: 280px;
                min-width: 280px;
                display: flex;
                flex-direction: column;
            }
            .bpo-col-left > .card {
                flex: 1; display: flex; flex-direction: column; overflow: hidden;
                border: 1px solid #dee2e6; border-radius: 6px;
            }
            .bpo-col-left > .card > .card-body {
                flex: 1; overflow-y: auto;
            }
            .bpo-col-center {
                flex: 1;
                min-width: 0;
                display: flex;
                flex-direction: column;
                background: #f8f9fa;
            }
            .bpo-col-right {
                width: 0;
                min-width: 0;
                overflow: hidden;
                transition: width 0.25s ease, min-width 0.25s ease;
                background: #fff;
                display: flex;
                flex-direction: column;
                border-left: 0 solid #dee2e6;
            }
            .bpo-col-right.open {
                width: 420px;
                min-width: 420px;
                border-left-width: 1px;
            }
        }

        /* Mobile: right panel overlay */
        @media (max-width: 991px) {
            .bpo-col-left { display: none; }
            .bpo-col-right {
                position: fixed;
                top: 0; right: 0; bottom: 0;
                width: 0; z-index: 1050;
                background: white;
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

        /* ═══ DETAIL PANEL ═══ */
        .detail-inner {
            width: 420px; height: 100%; display: flex; flex-direction: column; overflow: hidden;
        }
        @media (max-width: 991px) { .detail-inner { width: 100%; } }
        .detail-header {
            padding: 0.6rem 0.85rem; border-bottom: 1px solid #dee2e6;
            display: flex; align-items: flex-start; gap: 0.5rem; flex-shrink: 0; background: #fff;
        }
        .detail-header-content { flex: 1; min-width: 0; }
        .detail-task-name { font-weight: 600; font-size: 0.9rem; color: #333; line-height: 1.3; }
        .detail-meta { display: flex; align-items: center; gap: 0.4rem; margin-top: 0.2rem; flex-wrap: wrap; }
        .detail-close-btn {
            background: none; border: none; font-size: 1.1rem; color: #999;
            cursor: pointer; padding: 0; line-height: 1;
        }
        .detail-close-btn:hover { color: #333; }
        .detail-body { flex: 1; overflow-y: auto; padding: 0; }
        .detail-section { padding: 0.6rem 0.85rem; border-bottom: 1px solid #f0f0f0; }
        .detail-label {
            font-size: 0.68rem; color: #6c757d; text-transform: uppercase;
            letter-spacing: 0.03em; margin-bottom: 0.25rem; font-weight: 600;
        }
        .detail-footer {
            padding: 0.5rem 0.85rem; border-top: 1px solid #dee2e6;
            display: flex; justify-content: center; flex-shrink: 0; background: #fff;
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
    <div style="height: 6px; background: #eef0f4;"></div>
    <div class="bpo-layout">
    <div class="bpo-columns">

        <%-- ════════════════════════════════════════════ --%>
        <%-- LEFT COLUMN: My Checklists (unchanged)      --%>
        <%-- ════════════════════════════════════════════ --%>
        <div class="bpo-col-left">
            <div class="card">
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
                <div class="card-body p-2">
                    <c:import url="/WEB-INF/view/a/pspHome/columns/toDos/toDoCurrentList25.jsp"/>
                </div>
            </div>
        </div>

        <%-- ════════════════════════════════════════════ --%>
        <%-- CENTER COLUMN: Delegated Tasks (tree view)   --%>
        <%-- ════════════════════════════════════════════ --%>
        <div class="bpo-col-center">
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
        <%-- RIGHT COLUMN: Task Detail Panel              --%>
        <%-- ════════════════════════════════════════════ --%>
        <div class="bpo-col-right" id="bpoDetailPanel">
            <div class="detail-inner">
                <%-- Panel Header --%>
                <div class="detail-header">
                    <div class="detail-header-content">
                        <div class="detail-task-name" id="panelTaskName"></div>
                        <div class="detail-meta">
                            <span class="bpo-badge-psp" id="panelPspBadge"></span>
                            <span class="bpo-type-badge" id="panelTypeBadge"></span>
                            <span id="panelDueDate" style="font-size:0.7rem;"></span>
                        </div>
                        <div style="font-size:0.75rem; color:#666; margin-top:0.2rem;">
                            <i class="bi bi-folder2-open" style="font-size:0.7rem;"></i>
                            <span id="panelActivityName"></span>
                        </div>
                    </div>
                    <button class="detail-close-btn" onclick="closeTaskPanel()" title="Close">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </div>

                <%-- Completed banner (hidden by default) --%>
                <div id="panelCompletedBanner" style="display:none; background:#d4edda; border-bottom:1px solid #c3e6cb; padding:0.45rem 0.75rem; font-size:0.78rem; color:#155724;">
                    <i class="bi bi-check-circle-fill me-1"></i>Completed by <strong id="panelCompletedBy"></strong>
                    <span id="panelCompletedDate" class="ms-1" style="color:#6c757d;"></span>
                </div>

                <%-- Scrollable Panel Body --%>
                <div class="detail-body">
                    <%-- Quick Links --%>
                    <div class="detail-section" id="panelLinksSection" style="display:none;">
                        <div class="detail-label">Quick Links</div>
                        <div class="d-flex gap-2">
                            <a id="panelGoToLink" href="#" target="_blank" class="btn btn-sm btn-outline-primary flex-fill" style="display:none;">
                                <i class="bi bi-box-arrow-up-right me-1"></i>Go To Activity
                            </a>
                            <a id="panelInfoLink" href="#" target="_blank" class="btn btn-sm btn-outline-secondary flex-fill" style="display:none;">
                                <i class="bi bi-info-circle me-1"></i>Info / Reference
                            </a>
                        </div>
                    </div>

                    <%-- Task Description --%>
                    <div class="detail-section" id="panelDescSection" style="display:none;">
                        <div class="detail-label">Task Description</div>
                        <div id="panelDescription" style="font-size:0.8rem; color:#555; line-height:1.5;"></div>
                    </div>

                    <%-- Assignment --%>
                    <c:if test="${sessionScope.isBpoAdmin}">
                        <div class="detail-section" id="panelAssignSection">
                            <div class="detail-label">Assignment</div>
                            <select id="panelAssignTo" class="form-select form-select-sm" onchange="assignTaskAjax()">
                                <option value="0">-- Unassigned --</option>
                                <c:forEach var="bpo" items="${applicationScope.global.getBpoUsers()}">
                                    <option value="${bpo.getId()}">${bpo.getFirstName()} ${bpo.getLastName()}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </c:if>

                    <%-- Notes --%>
                    <div class="detail-section" style="border-bottom:none;">
                        <div class="detail-label">Notes</div>
                        <div id="panelNotes" style="max-height:250px; overflow-y:auto; margin-bottom:0.5rem;">
                            <div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">Select a task</div>
                        </div>

                        <%-- Add note + file attach --%>
                        <div class="mb-2" id="panelAddNoteSection">
                            <div class="input-group input-group-sm">
                                <input type="text" class="form-control" id="panelNoteInput" placeholder="Add a note..."
                                       onkeydown="if(event.key==='Enter'){addNoteAjax(); event.preventDefault();}">
                                <button type="button" class="btn btn-sm btn-outline-ssa" onclick="addNoteAjax()">
                                    <i class="bi bi-chat-dots me-1"></i>Add
                                </button>
                            </div>
                            <div class="mt-1">
                                <label class="form-label mb-0" style="font-size:0.75rem; color:#6c757d; cursor:pointer;">
                                    <i class="bi bi-paperclip"></i> Attach file
                                    <input type="file" id="panelNoteFile" style="display:none;" onchange="updateFileLabel(this)">
                                </label>
                                <span id="panelFileLabel" style="font-size:0.75rem; color:#0d5681;"></span>
                                <span id="panelFileClear" style="display:none; font-size:0.75rem; color:#dc3545; cursor:pointer; margin-left:0.3rem;" onclick="clearFileInput()">&#10005;</span>
                            </div>
                        </div>
                    </div>

                    <%-- Past Runs (recurring cross-system tasks) --%>
                    <div class="detail-section" id="panelPastRunsSection" style="display:none; border-bottom:none;">
                        <div class="detail-label"><i class="bi bi-clock-history me-1"></i>Past Runs</div>
                        <div id="panelPastRunsLoading" class="text-muted fst-italic" style="font-size:0.78rem;">Loading...</div>
                        <div id="panelPastRunsAccordion" class="accordion accordion-flush" style="display:none; font-size:0.78rem;"></div>
                    </div>
                </div>

                <%-- Panel Footer --%>
                <div class="detail-footer" id="panelFooter">
                    <c:if test="${sessionScope.isBpoAdmin}">
                        <button type="button" class="ssa-action save" id="panelAcceptBtn" style="display:none;" onclick="acceptFromPanel()">
                            <i class="bi bi-check2-circle me-1"></i>Accept
                        </button>
                        <span id="panelAcceptSep" class="ssa-action-sep" style="display:none;">|</span>
                    </c:if>
                    <form method="post" action="BpoCompleteTask" class="d-inline" id="panelCompleteForm">
                        <input type="hidden" name="action" value="complete">
                        <input type="hidden" name="todoId" id="panelCompleteToDoId" value="">
                        <input type="hidden" name="todoGuid" id="panelCompleteTodoGuid" value="">
                        <input type="hidden" name="crossSystem" id="panelCrossSystem" value="false">
                        <button type="submit" class="ssa-action save">
                            <i class="bi bi-check-circle me-1"></i>Mark Complete
                        </button>
                    </form>
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
        const assignSection = document.getElementById('panelAssignSection');
        if (assignSection) assignSection.style.display = '';

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
        const description = row.dataset.taskDescription || '';
        const gotoUrl = row.dataset.goto || '';
        const infoUrl = row.dataset.info || '';
        const assignedTo = row.dataset.assignedTo || '0';

        // Populate header
        document.getElementById('panelTaskName').textContent = taskName;
        document.getElementById('panelPspBadge').textContent = pspName;
        document.getElementById('panelActivityName').textContent = activityName;

        // Type badge
        const typeBadge = document.getElementById('panelTypeBadge');
        typeBadge.textContent = activityType;
        typeBadge.className = 'bpo-type-badge bpo-type-' + activityType.toLowerCase();

        // Due date with color
        const dueDateEl = document.getElementById('panelDueDate');
        dueDateEl.textContent = dueDate;
        // Parse and color-code
        if (dueDate) {
            const parts = dueDate.split('/');
            if (parts.length === 3) {
                const d = new Date(parts[2], parts[0] - 1, parts[1]);
                const today = new Date(); today.setHours(0,0,0,0);
                if (d < today) dueDateEl.className = 'due-overdue';
                else if (d.getTime() === today.getTime()) dueDateEl.className = 'due-today';
                else dueDateEl.className = 'due-future';
            }
        }

        // Hidden form fields
        document.getElementById('panelCompleteToDoId').value = currentTodoId;
        document.getElementById('panelCompleteTodoGuid').value = currentTodoGuid;
        document.getElementById('panelCrossSystem').value = currentCrossSystem ? 'true' : 'false';

        // Show/hide Accept vs Complete based on pending state
        const acceptBtn = document.getElementById('panelAcceptBtn');
        const acceptSep = document.getElementById('panelAcceptSep');
        const completeForm = document.getElementById('panelCompleteForm');
        if (acceptBtn) {
            acceptBtn.style.display = currentIsPending ? '' : 'none';
            acceptSep.style.display = currentIsPending ? '' : 'none';
        }
        if (completeForm) {
            completeForm.style.display = currentIsPending ? 'none' : '';
        }

        // Description
        const descSection = document.getElementById('panelDescSection');
        const descEl = document.getElementById('panelDescription');
        if (description) {
            descEl.textContent = description;
            descSection.style.display = '';
        } else {
            descSection.style.display = 'none';
        }

        // Links
        const linksSection = document.getElementById('panelLinksSection');
        const goToLink = document.getElementById('panelGoToLink');
        const infoLink = document.getElementById('panelInfoLink');
        let hasLinks = false;
        if (gotoUrl) { goToLink.href = gotoUrl; goToLink.style.display = ''; hasLinks = true; }
        else { goToLink.style.display = 'none'; }
        if (infoUrl) { infoLink.href = infoUrl; infoLink.style.display = ''; hasLinks = true; }
        else { infoLink.style.display = 'none'; }
        linksSection.style.display = hasLinks ? '' : 'none';

        // Assignment dropdown
        const assignSelect = document.getElementById('panelAssignTo');
        if (assignSelect) assignSelect.value = assignedTo;

        // Load notes
        const notesDiv = document.getElementById('panelNotes');
        notesDiv.innerHTML = '<div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">Loading...</div>';
        const notesUrl = currentCrossSystem
            ? 'BpoGetNotes?todoGuid=' + encodeURIComponent(currentTodoGuid)
            : 'BpoGetNotes?todoId=' + currentTodoId;
        fetch(notesUrl)
            .then(r => r.json())
            .then(notes => renderNotes(notes))
            .catch(() => { notesDiv.innerHTML = '<div class="text-center text-muted py-2" style="font-size:0.8rem;">Could not load notes</div>'; });

        // Past Runs (recurring cross-system tasks)
        const recurringSeriesId = row.dataset.recurringSeriesId || '';
        const pspClientId = row.dataset.pspClientId || '';
        const pastRunsSection = document.getElementById('panelPastRunsSection');
        const pastRunsLoading = document.getElementById('panelPastRunsLoading');
        const pastRunsAccordion = document.getElementById('panelPastRunsAccordion');
        pastRunsSection.style.display = 'none';
        pastRunsLoading.style.display = 'block';
        pastRunsAccordion.style.display = 'none';
        pastRunsAccordion.innerHTML = '';

        if (recurringSeriesId && currentCrossSystem) {
            pastRunsSection.style.display = '';
            fetch('BpoRecurringHistory?recurringSeriesId=' + encodeURIComponent(recurringSeriesId) + '&pspClientId=' + encodeURIComponent(pspClientId))
                .then(r => r.json())
                .then(runs => {
                    pastRunsLoading.style.display = 'none';
                    if (!runs || runs.length === 0) { pastRunsSection.style.display = 'none'; return; }
                    pastRunsAccordion.style.display = 'block';
                    runs.forEach(function(run, idx) {
                        const colId = 'bpoPastRun' + idx;
                        const dueLabel = run.dueDate || '\u2014';
                        const doneBy = run.completedByName ? ' \u00b7 ' + run.completedByName : '';
                        const noteCount = run.noteCount || 0;
                        const noteBadge = noteCount > 0
                            ? '<span class="badge ms-1" style="background:#0d5681;font-size:0.6rem;">' + noteCount + (noteCount === 1 ? ' note' : ' notes') + '</span>'
                            : '';
                        const item = document.createElement('div');
                        item.className = 'accordion-item';
                        item.style.cssText = 'border:1px solid #dee2e6;border-radius:3px;margin-bottom:0.25rem;';
                        item.innerHTML =
                            '<h2 class="accordion-header">' +
                            '<button class="accordion-button collapsed py-1 px-2" type="button" ' +
                            'data-bs-toggle="collapse" data-bs-target="#' + colId + '" ' +
                            'style="font-size:0.75rem;background:#f8f9fa;" ' +
                            'onclick="loadPastRunNotes(\'' + colId + '\',\'' + (run.todoGuid || '') + '\')">' +
                            dueLabel + doneBy + noteBadge +
                            '</button></h2>' +
                            '<div id="' + colId + '" class="accordion-collapse collapse">' +
                            '<div class="accordion-body p-2" id="' + colId + '_body">' +
                            '<div class="text-muted fst-italic" style="font-size:0.75rem;">Loading notes...</div>' +
                            '</div></div>';
                        pastRunsAccordion.appendChild(item);
                    });
                })
                .catch(function() { pastRunsLoading.style.display = 'none'; });
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

        const taskName = row.dataset.taskName || '';
        const pspName = row.dataset.pspName || '';
        const activityName = row.dataset.activityName || '';
        const completedBy = row.dataset.completedBy || '';
        const completedDate = row.dataset.completedDate || '';
        const crossSystem = row.dataset.crossSystem === 'true';
        const todoId = row.dataset.todoId || '';
        const todoGuid = row.dataset.todoGuid || '';

        // Populate header
        document.getElementById('panelTaskName').textContent = taskName;
        document.getElementById('panelPspBadge').textContent = pspName;
        document.getElementById('panelActivityName').textContent = activityName;

        // Type badge — show "Completed"
        const typeBadge = document.getElementById('panelTypeBadge');
        typeBadge.textContent = 'Completed';
        typeBadge.className = 'bpo-type-badge';
        typeBadge.style.cssText = 'background:#d4edda; color:#155724;';

        // Due date — show completed date
        const dueDateEl = document.getElementById('panelDueDate');
        dueDateEl.textContent = completedDate;
        dueDateEl.className = '';

        // Completed banner
        document.getElementById('panelCompletedBanner').style.display = '';
        document.getElementById('panelCompletedBy').textContent = completedBy;
        document.getElementById('panelCompletedDate').textContent = completedDate ? '\u00b7 ' + completedDate : '';

        // Hide editable controls
        document.getElementById('panelAddNoteSection').style.display = 'none';
        document.getElementById('panelFooter').style.display = 'none';
        const assignSection = document.getElementById('panelAssignSection');
        if (assignSection) assignSection.style.display = 'none';

        // Hide links, description (not relevant for completed view)
        document.getElementById('panelLinksSection').style.display = 'none';
        document.getElementById('panelDescSection').style.display = 'none';

        // Load notes / history
        const notesDiv = document.getElementById('panelNotes');
        notesDiv.innerHTML = '<div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">Loading...</div>';
        const notesUrl = crossSystem
            ? 'BpoGetNotes?todoGuid=' + encodeURIComponent(todoGuid)
            : 'BpoGetNotes?todoId=' + todoId;
        fetch(notesUrl)
            .then(r => r.json())
            .then(notes => renderNotes(notes))
            .catch(() => { notesDiv.innerHTML = '<div class="text-center text-muted py-2" style="font-size:0.8rem;">Could not load notes</div>'; });

        // Hide past runs section for completed tasks
        document.getElementById('panelPastRunsSection').style.display = 'none';

        // Open panel
        document.getElementById('bpoDetailPanel').classList.add('open');
    }

    // ESC key closes panel
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') closeTaskPanel();
    });

    // ═══ NOTES ═══

    function renderNotes(notes) {
        const notesDiv = document.getElementById('panelNotes');
        if (!notes || notes.length === 0) {
            notesDiv.innerHTML = '<div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">No notes yet</div>';
            return;
        }
        let html = '';
        notes.forEach(n => {
            const badgeClass = n.source === 'BPO' ? 'background:#e8f4f8; color:#0d5681;' : 'background:#fff3cd; color:#856404;';
            html += '<div style="padding:0.35rem 0; border-bottom:1px solid #f0f0f0;">';
            html += '  <div class="d-flex align-items-center gap-2">';
            html += '    <span style="font-size:0.65rem; padding:0.1rem 0.4rem; border-radius:8px; ' + badgeClass + '">' + n.source + '</span>';
            html += '    <span style="font-size:0.8rem; font-weight:500;">' + n.author + '</span>';
            html += '    <span style="font-size:0.7rem; color:#999;">' + n.date + '</span>';
            html += '  </div>';
            html += '  <div style="font-size:0.85rem; margin-top:0.15rem; padding-left:0.2rem;">' + n.text + '</div>';
            if (n.attachments && n.attachments.length > 0) {
                html += '<div style="margin-top:0.25rem; padding-left:0.2rem;">';
                n.attachments.forEach(att => {
                    html += '<a href="' + att.url + '" target="_blank" '
                        + 'style="display:inline-block; font-size:0.75rem; padding:0.15rem 0.5rem; '
                        + 'background:#e8f4f8; color:#0d5681; border-radius:12px; text-decoration:none; '
                        + 'margin-right:0.3rem; margin-bottom:0.2rem;">'
                        + '<i class="bi bi-paperclip"></i> ' + att.name + '</a>';
                });
                html += '</div>';
            }
            html += '</div>';
        });
        notesDiv.innerHTML = html;
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
        const label = document.getElementById('panelFileLabel');
        const clear = document.getElementById('panelFileClear');
        if (input.files.length > 0) {
            label.textContent = input.files[0].name;
            clear.style.display = 'inline';
        } else {
            label.textContent = '';
            clear.style.display = 'none';
        }
    }

    function clearFileInput() {
        document.getElementById('panelNoteFile').value = '';
        document.getElementById('panelFileLabel').textContent = '';
        document.getElementById('panelFileClear').style.display = 'none';
    }

    function loadPastRunNotes(colId, todoGuid) {
        const body = document.getElementById(colId + '_body');
        if (!body || !todoGuid || body.dataset.loaded === 'true') return;
        body.dataset.loaded = 'true';

        fetch('BpoGetNotes?todoGuid=' + encodeURIComponent(todoGuid))
            .then(r => r.json())
            .then(notes => {
                if (!notes || notes.length === 0) {
                    body.innerHTML = '<div class="text-muted fst-italic" style="font-size:0.75rem;">No notes for this run.</div>';
                    return;
                }
                let html = '';
                notes.forEach(function(n) {
                    const badgeColor = n.sourceType === 'BPO' ? '#0d5681' : '#6c757d';
                    html += '<div class="mb-1 p-1 rounded" style="background:#f8f9fa;font-size:0.75rem;">';
                    html += '<span class="badge me-1" style="background:' + badgeColor + ';font-size:0.6rem;">' + (n.sourceType || '') + '</span>';
                    html += '<span style="color:#555;">' + (n.authorName || '') + '</span>';
                    if (n.createdDate) html += '<span class="text-muted ms-1" style="font-size:0.68rem;">' + n.createdDate + '</span>';
                    html += '<div style="color:#333;margin-top:2px;">' + (n.noteText || '') + '</div>';
                    html += '</div>';
                });
                body.innerHTML = html;
            })
            .catch(function() {
                body.innerHTML = '<div class="text-muted" style="font-size:0.75rem;">Could not load notes.</div>';
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
