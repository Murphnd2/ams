<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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
        .bpo-todo-row { padding: 0.5rem 0.75rem; border-bottom: 1px solid #eee; transition: background 0.15s; cursor: pointer; }
        .bpo-todo-row:hover { background: #f0f7fb; }
        .bpo-todo-row:last-child { border-bottom: none; }
        .bpo-badge-psp { background: #e8f4f8; color: #0d5681; font-size: 0.7rem; padding: 0.15rem 0.5rem; border-radius: 10px; }
        .bpo-badge-status { font-size: 0.7rem; padding: 0.15rem 0.5rem; border-radius: 10px; }
        .bpo-badge-unassigned { background: #fff3cd; color: #856404; }
        .bpo-badge-assigned { background: #d4edda; color: #155724; }
        .due-overdue { color: #dc3545; font-weight: 600; }
        .due-today { color: #fd7e14; font-weight: 600; }
        .due-future { color: #6c757d; }
        .sort-header { cursor: pointer; user-select: none; }
        .sort-header:hover { color: #0d5681; }

        /* Full-viewport flex layout on desktop */
        @media (min-width: 992px) {
            .bpo-layout {
                display: flex;
                flex-direction: column;
                height: calc(100vh - 70px);
                overflow: hidden;
            }
            .bpo-columns {
                flex: 1;
                min-height: 0;
            }
            .bpo-col-left {
                display: flex;
                flex-direction: column;
            }
            .bpo-col-left > .card {
                flex: 1;
                display: flex;
                flex-direction: column;
                overflow: hidden;
            }
            .bpo-col-left > .card > .card-body {
                flex: 1;
                overflow-y: auto;
            }
            .bpo-col-right {
                display: flex;
                flex-direction: column;
            }
            .bpo-col-right > .card {
                flex: 1;
                display: flex;
                flex-direction: column;
                overflow: hidden;
            }
            .bpo-col-right > .card > .card-body {
                flex: 1;
                overflow-y: auto;
            }
        }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
    <div class="bpo-layout">
    <div class="row g-3 mt-1 bpo-columns">

        <%-- ═══ LEFT COLUMN: Personal Checklists ═══ --%>
        <div class="col-lg-4 col-xl-3 bpo-col-left">
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

        <%-- ═══ RIGHT COLUMN: Delegated ToDos ═══ --%>
        <div class="col-lg-8 col-xl-9 bpo-col-right">
            <div class="card">
                <div class="hdr-bar d-flex align-items-center justify-content-between">
                    <span><i class="bi bi-list-task me-2"></i>Delegated Tasks</span>
                    <div class="d-flex gap-2 align-items-center">
                        <form method="get" action="BpoHome" class="d-flex gap-1 m-0">
                            <button type="submit" name="viewMode" value="mine"
                                    class="btn btn-sm ${viewMode == 'mine' ? 'btn-light' : 'btn-outline-light'}"
                                    style="font-size:0.7rem; padding:0.15rem 0.5rem;">
                                My Tasks
                            </button>
                            <button type="submit" name="viewMode" value="all"
                                    class="btn btn-sm ${viewMode == 'all' ? 'btn-light' : 'btn-outline-light'}"
                                    style="font-size:0.7rem; padding:0.15rem 0.5rem;">
                                All Open
                            </button>
                        </form>
                    </div>
                </div>
                <div class="card-body p-0">

                    <%-- Table header --%>
                    <div class="row g-0 px-3 py-2" style="background:#f8f9fa; border-bottom:2px solid #dee2e6; font-size:0.75rem; font-weight:600; color:#495057;">
                        <div class="col-5 sort-header" onclick="sortTable(0)">
                            Task Name <i class="bi bi-arrow-down-up" style="font-size:0.65rem;"></i>
                        </div>
                        <div class="col-3 sort-header" onclick="sortTable(1)">
                            PSP <i class="bi bi-arrow-down-up" style="font-size:0.65rem;"></i>
                        </div>
                        <div class="col-2 sort-header" onclick="sortTable(2)">
                            Due Date <i class="bi bi-arrow-down-up" style="font-size:0.65rem;"></i>
                        </div>
                        <div class="col-2 text-end">Status</div>
                    </div>

                    <%-- ToDo rows --%>
                    <div id="bpoToDoList">
                      <c:choose>

                        <%-- ═══ CROSS-SYSTEM MODE: DelegatedToDo ═══ --%>
                        <c:when test="${crossSystemMode}">
                            <c:choose>
                                <c:when test="${not empty delegatedToDos}">
                                    <c:forEach var="dt" items="${delegatedToDos}">
                                        <c:choose>
                                            <c:when test="${dt.getDueDate() != null && dt.getDueDate() < Date.valueOf(LocalDate.now())}">
                                                <c:set var="dueClass" value="due-overdue"/>
                                            </c:when>
                                            <c:when test="${dt.getDueDate() != null && dt.getDueDate() == Date.valueOf(LocalDate.now())}">
                                                <c:set var="dueClass" value="due-today"/>
                                            </c:when>
                                            <c:otherwise>
                                                <c:set var="dueClass" value="due-future"/>
                                            </c:otherwise>
                                        </c:choose>
                                        <div class="bpo-todo-row row g-0 px-3 align-items-center"
                                             data-sort0="${dt.getTaskName()}"
                                             data-sort1="${dt.getPspClient().getPspName()}"
                                             data-sort2="${dt.getDueDate()}"
                                             data-todo-id="${dt.getId()}"
                                             data-todo-guid="${dt.getTodoGuid()}"
                                             data-cross-system="true"
                                             data-task-name="${dt.getTaskName()}"
                                             data-psp-name="${dt.getPspClient().getPspName()}"
                                             data-activity-name="${dt.getActivityName()}"
                                             data-due-date="<fmt:formatDate value='${dt.getDueDate()}' pattern='MM/dd/yyyy'/>"
                                             data-goto="${dt.getGotoLink() != null ? dt.getGotoLink() : ''}"
                                             data-info="${dt.getInfoLink() != null ? dt.getInfoLink() : ''}"
                                             data-assigned-to="${dt.getAssignedTo() != null ? dt.getAssignedTo().getId() : '0'}"
                                             onclick="openBpoModal(this)"
                                             style="cursor:pointer;">
                                            <div class="col-5">
                                                <div style="font-size:0.85rem; font-weight:500;">${dt.getTaskName()}</div>
                                                <span style="font-size:0.7rem; color:#6c757d;">${dt.getActivityName()}</span>
                                            </div>
                                            <div class="col-3">
                                                <span class="bpo-badge-psp">${dt.getPspClient().getPspName()}</span>
                                            </div>
                                            <div class="col-2 ${dueClass}" style="font-size:0.85rem;">
                                                <fmt:formatDate value="${dt.getDueDate()}" pattern="MM/dd/yyyy"/>
                                            </div>
                                            <div class="col-2 text-end">
                                                <c:choose>
                                                    <c:when test="${dt.getAssignedTo() == null}">
                                                        <span class="bpo-badge-status bpo-badge-unassigned">Unassigned</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="bpo-badge-status bpo-badge-assigned">Assigned</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="text-center text-muted fst-italic py-4" style="font-size:0.85rem;">
                                        <i class="bi bi-inbox" style="font-size:1.5rem; display:block; margin-bottom:0.3rem; color:#c8c8c8;"></i>
                                        No delegated tasks
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </c:when>

                        <%-- ═══ CO-LOCATED MODE: local ToDo ═══ --%>
                        <c:otherwise>
                            <c:choose>
                                <c:when test="${not empty bpoToDos}">
                                    <c:forEach var="row" items="${bpoToDos}">
                                        <c:set var="todo" value="${row[0]}"/>
                                        <c:set var="activityName" value="${row[1]}"/>
                                        <c:set var="dueDate" value="${row[2]}"/>
                                        <c:set var="pspName" value="${row[3]}"/>
                                        <c:choose>
                                            <c:when test="${dueDate < Date.valueOf(LocalDate.now())}">
                                                <c:set var="dueClass" value="due-overdue"/>
                                            </c:when>
                                            <c:when test="${dueDate == Date.valueOf(LocalDate.now())}">
                                                <c:set var="dueClass" value="due-today"/>
                                            </c:when>
                                            <c:otherwise>
                                                <c:set var="dueClass" value="due-future"/>
                                            </c:otherwise>
                                        </c:choose>
                                        <div class="bpo-todo-row row g-0 px-3 align-items-center"
                                             data-sort0="${todo.getTask().getPlainDescription()}"
                                             data-sort1="${pspName}"
                                             data-sort2="${dueDate}"
                                             data-todo-id="${todo.getId()}"
                                             data-cross-system="false"
                                             data-task-name="${todo.getTask().getPlainDescription()}"
                                             data-psp-name="${pspName}"
                                             data-activity-name="${todo.getCheckList().getRenewal() != null ? todo.getCheckList().getRenewal().getFullName().concat(' Renewal') : todo.getCheckList().getSetup() != null ? todo.getCheckList().getSetup().getFullName().concat(' Setup') : todo.getCheckList().getTicket() != null ? todo.getCheckList().getTicket().getFullName().concat(' Ticket') : activityName}"
                                             data-due-date="<fmt:formatDate value='${dueDate}' pattern='MM/dd/yyyy'/>"
                                             data-goto="${todo.getTask().hasGoTo() && todo.getTask().getGoToLink() != null ? todo.getTask().getGoToLink().getLinkPath() : ''}"
                                             data-info="${todo.getTask().hasInfo() && todo.getTask().getInfoLink() != null ? todo.getTask().getInfoLink().getLinkPath() : ''}"
                                             data-assigned-to="${todo.getBpoAssignedTo() != null ? todo.getBpoAssignedTo().getId() : '0'}"
                                             onclick="openBpoModal(this)"
                                             style="cursor:pointer;">
                                            <div class="col-5">
                                                <div style="font-size:0.85rem; font-weight:500;">${todo.getTask().getPlainDescription()}</div>
                                                <c:choose>
                                                    <c:when test="${todo.getCheckList().getRenewal() != null}">
                                                        <span style="font-size:0.7rem; color:#6c757d;">${todo.getCheckList().getRenewal().getFullName()} Renewal</span>
                                                    </c:when>
                                                    <c:when test="${todo.getCheckList().getSetup() != null}">
                                                        <span style="font-size:0.7rem; color:#6c757d;">${todo.getCheckList().getSetup().getFullName()} Setup</span>
                                                    </c:when>
                                                    <c:when test="${todo.getCheckList().getTicket() != null}">
                                                        <span style="font-size:0.7rem; color:#6c757d;">${todo.getCheckList().getTicket().getFullName()} Ticket</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span style="font-size:0.7rem; color:#6c757d;">${activityName}</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                            <div class="col-3">
                                                <span class="bpo-badge-psp">${pspName}</span>
                                            </div>
                                            <div class="col-2 ${dueClass}" style="font-size:0.85rem;">
                                                <fmt:formatDate value="${dueDate}" pattern="MM/dd/yyyy"/>
                                            </div>
                                            <div class="col-2 text-end">
                                                <c:choose>
                                                    <c:when test="${todo.getBpoAssignedTo() == null}">
                                                        <span class="bpo-badge-status bpo-badge-unassigned">Unassigned</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="bpo-badge-status bpo-badge-assigned">Assigned</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="text-center text-muted fst-italic py-4" style="font-size:0.85rem;">
                                        <i class="bi bi-inbox" style="font-size:1.5rem; display:block; margin-bottom:0.3rem; color:#c8c8c8;"></i>
                                        No delegated tasks
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </c:otherwise>

                      </c:choose>
                    </div>

                        <%-- ===== COMPLETED TASKS (collapsed) ===== --%>
                        <div class="px-3 pb-2 pt-1" style="border-top:1px solid #dee2e6;">
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
                                <div id="completedTasksList" class="mt-1">
                                </div>
                            </div>
                        </div>

                </div>
            </div>
        </div>

    </div>
    </div>
</div>

<%-- ═══ TASK DETAIL MODAL ═══ --%>
<div class="modal fade" id="bpoTaskModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
        <div class="modal-content">
            <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                <h6 class="modal-title fw-semibold">
                    <i class="bi bi-clipboard-check me-2"></i><span id="modalTaskName"></span>
                </h6>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">

                <%-- Task info row --%>
                <div class="row mb-3">
                    <div class="col-sm-4">
                        <div style="font-size:0.75rem; color:#6c757d; text-transform:uppercase; letter-spacing:0.03em;">PSP</div>
                        <div id="modalPspName" style="font-size:0.9rem; font-weight:500;"></div>
                    </div>
                    <div class="col-sm-4">
                        <div style="font-size:0.75rem; color:#6c757d; text-transform:uppercase; letter-spacing:0.03em;">Activity</div>
                        <div id="modalActivityName" style="font-size:0.9rem; font-weight:500;"></div>
                    </div>
                    <div class="col-sm-4">
                        <div style="font-size:0.75rem; color:#6c757d; text-transform:uppercase; letter-spacing:0.03em;">Due Date</div>
                        <div id="modalDueDate" style="font-size:0.9rem; font-weight:500;"></div>
                    </div>
                </div>

                <%-- Links row --%>
                <div id="modalLinksRow" class="row mb-3 d-none">
                    <div class="col-sm-6" id="modalGoToWrap">
                        <a id="modalGoToLink" href="#" target="_blank" class="btn btn-sm btn-outline-ssa w-100">
                            <i class="bi bi-box-arrow-up-right me-1"></i>Go To Task
                        </a>
                    </div>
                    <div class="col-sm-6" id="modalInfoWrap">
                        <a id="modalInfoLink" href="#" target="_blank" class="btn btn-sm btn-outline-secondary w-100">
                            <i class="bi bi-info-circle me-1"></i>Instructions
                        </a>
                    </div>
                </div>

                <%-- Assign To (BPO Admin only) --%>
                <c:if test="${sessionScope.isBpoAdmin}">
                    <div class="row mb-3">
                        <div class="col-sm-6">
                            <div style="font-size:0.75rem; color:#6c757d; text-transform:uppercase; letter-spacing:0.03em;">Assign To</div>
                            <select id="modalAssignTo" class="form-select form-select-sm mt-1" onchange="assignTaskAjax()">
                                <option value="0">-- Unassigned --</option>
                                <c:forEach var="bpo" items="${applicationScope.global.getBpoUsers()}">
                                    <option value="${bpo.getId()}">${bpo.getFirstName()} ${bpo.getLastName()}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                </c:if>

                <hr style="margin:0.5rem 0;">

                <%-- Notes section --%>
                <div style="font-size:0.75rem; color:#6c757d; text-transform:uppercase; letter-spacing:0.03em; margin-bottom:0.4rem;">
                    Notes
                </div>
                <div id="modalNotes" style="max-height:200px; overflow-y:auto; margin-bottom:0.75rem;">
                    <div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">Loading...</div>
                </div>

                    <%-- Add note with optional attachment --%>
                    <div class="mb-2">
                        <div class="input-group input-group-sm">
                            <input type="text" class="form-control" id="modalNoteInput" placeholder="Add a note...">
                            <button type="button" class="btn btn-sm btn-outline-ssa" onclick="addNoteAjax()">
                                <i class="bi bi-chat-dots me-1"></i>Add
                            </button>
                        </div>
                        <div class="mt-1">
                            <label class="form-label mb-0" style="font-size:0.75rem; color:#6c757d; cursor:pointer;">
                                <i class="bi bi-paperclip"></i> Attach file
                                <input type="file" id="modalNoteFile" style="display:none;" onchange="updateFileLabel(this)">
                            </label>
                            <span id="modalFileLabel" style="font-size:0.75rem; color:#0d5681;"></span>
                            <span id="modalFileClear" style="display:none; font-size:0.75rem; color:#dc3545; cursor:pointer; margin-left:0.3rem;" onclick="clearFileInput()">&#10005;</span>
                        </div>
                    </div>

            </div>
            <div class="modal-footer justify-content-center border-0" style="padding:0.5rem 1rem;">
                <form method="post" action="BpoCompleteTask" class="d-inline">
                    <input type="hidden" name="action" value="complete">
                    <input type="hidden" name="todoId" id="modalCompleteToDoId" value="">
                    <input type="hidden" name="todoGuid" id="modalCompleteTodoGuid" value="">
                    <input type="hidden" name="crossSystem" id="modalCrossSystem" value="false">
                    <button type="submit" class="ssa-action save">
                        <i class="bi bi-check-circle me-1"></i>Mark Complete
                    </button>
                </form>
                <span class="ssa-action-sep">|</span>
                <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Close</button>
            </div>
        </div>
    </div>
</div>
<script>
    var currentCrossSystem = false;
    var currentTodoGuid = '';

    function openBpoModal(row) {
        const todoId = row.dataset.todoId;
        const taskName = row.dataset.taskName;
        const pspName = row.dataset.pspName;
        const activityName = row.dataset.activityName;
        const dueDate = row.dataset.dueDate;
        const gotoUrl = row.dataset.goto;
        const infoUrl = row.dataset.info;
        const assignedTo = row.dataset.assignedTo || '0';
        currentCrossSystem = row.dataset.crossSystem === 'true';
        currentTodoGuid = row.dataset.todoGuid || '';

        // Populate fields
        document.getElementById('modalTaskName').textContent = taskName;
        document.getElementById('modalPspName').textContent = pspName;
        document.getElementById('modalActivityName').textContent = activityName;
        document.getElementById('modalDueDate').textContent = dueDate;
        document.getElementById('modalCompleteToDoId').value = todoId;
        document.getElementById('modalCompleteTodoGuid').value = currentTodoGuid;
        document.getElementById('modalCrossSystem').value = currentCrossSystem ? 'true' : 'false';

        // Pre-select Assign To dropdown (if present)
        const assignSelect = document.getElementById('modalAssignTo');
        if (assignSelect) {
            assignSelect.value = assignedTo;
        }

        // Links
        const linksRow = document.getElementById('modalLinksRow');
        const goToWrap = document.getElementById('modalGoToWrap');
        const infoWrap = document.getElementById('modalInfoWrap');
        const goToLink = document.getElementById('modalGoToLink');
        const infoLink = document.getElementById('modalInfoLink');

        let hasLinks = false;
        if (gotoUrl) {
            goToLink.href = gotoUrl;
            goToWrap.classList.remove('d-none');
            hasLinks = true;
        } else {
            goToWrap.classList.add('d-none');
        }
        if (infoUrl) {
            infoLink.href = infoUrl;
            infoWrap.classList.remove('d-none');
            hasLinks = true;
        } else {
            infoWrap.classList.add('d-none');
        }
        linksRow.classList.toggle('d-none', !hasLinks);

        // Load notes via AJAX
        const notesDiv = document.getElementById('modalNotes');
        notesDiv.innerHTML = '<div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">Loading...</div>';

        const notesUrl = currentCrossSystem
            ? 'BpoGetNotes?todoGuid=' + encodeURIComponent(currentTodoGuid)
            : 'BpoGetNotes?todoId=' + todoId;

        fetch(notesUrl)
            .then(r => r.json())
            .then(notes => {
                renderNotes(notes);
            })
            .catch(() => {
                notesDiv.innerHTML = '<div class="text-center text-muted py-2" style="font-size:0.8rem;">Could not load notes</div>';
            });

        // Show modal
        new bootstrap.Modal(document.getElementById('bpoTaskModal')).show();
    }

    function assignTaskAjax() {
        const todoId = document.getElementById('modalCompleteToDoId').value;
        const assignSelect = document.getElementById('modalAssignTo');
        const assigneeId = assignSelect.value;

        assignSelect.disabled = true;

        let body = 'action=assign&todoId=' + todoId + '&assigneeId=' + assigneeId;
        if (currentCrossSystem) body += '&crossSystem=true';

        fetch('BpoCompleteTask', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: body
        })
            .then(r => {
                if (r.ok) {
                    const row = document.querySelector('.bpo-todo-row[data-todo-id="' + todoId + '"]');
                    if (row) {
                        row.dataset.assignedTo = assigneeId;
                        const badge = row.querySelector('.bpo-badge-status');
                        if (badge) {
                            if (assigneeId === '0') {
                                badge.className = 'bpo-badge-status bpo-badge-unassigned';
                                badge.textContent = 'Unassigned';
                            } else {
                                badge.className = 'bpo-badge-status bpo-badge-assigned';
                                badge.textContent = 'Assigned';
                            }
                        }
                    }
                }
            })
            .catch(() => {})
            .finally(() => {
                assignSelect.disabled = false;
            });
    }

    function addNoteAjax() {
        const input = document.getElementById('modalNoteInput');
        const noteText = input.value.trim();
        if (!noteText) return;

        const todoId = document.getElementById('modalCompleteToDoId').value;
        const fileInput = document.getElementById('modalNoteFile');
        input.disabled = true;

        const formData = new FormData();
        formData.append('action', 'addNote');
        formData.append('todoId', todoId);
        formData.append('noteText', noteText);
        if (currentCrossSystem) {
            formData.append('crossSystem', 'true');
            formData.append('todoGuid', currentTodoGuid);
        }
        if (fileInput.files.length > 0) {
            formData.append('noteFile', fileInput.files[0]);
        }

        fetch('BpoCompleteTask', {
            method: 'POST',
            body: formData
        })
            .then(() => {
                input.value = '';
                input.disabled = false;
                clearFileInput();
                input.focus();
                const notesUrl = currentCrossSystem
                    ? 'BpoGetNotes?todoGuid=' + encodeURIComponent(currentTodoGuid)
                    : 'BpoGetNotes?todoId=' + todoId;
                return fetch(notesUrl);
            })
            .then(r => r.json())
            .then(notes => {
                renderNotes(notes);
            })
            .catch(() => {
                input.disabled = false;
            });
    }

    function updateFileLabel(input) {
        const label = document.getElementById('modalFileLabel');
        const clear = document.getElementById('modalFileClear');
        if (input.files.length > 0) {
            label.textContent = input.files[0].name;
            clear.style.display = 'inline';
        } else {
            label.textContent = '';
            clear.style.display = 'none';
        }
    }

    function clearFileInput() {
        document.getElementById('modalNoteFile').value = '';
        document.getElementById('modalFileLabel').textContent = '';
        document.getElementById('modalFileClear').style.display = 'none';
    }

    function renderNotes(notes) {
        const notesDiv = document.getElementById('modalNotes');
        if (notes.length === 0) {
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
            // Attachments
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
</script>
<script>
    // Client-side column sorting
    let sortStates = [0, 0, 0]; // 0=none, 1=asc, -1=desc
    function sortTable(colIndex) {
        const list = document.getElementById('bpoToDoList');
        const rows = Array.from(list.querySelectorAll('.bpo-todo-row'));
        if (rows.length === 0) return;

        // Toggle sort direction
        sortStates[colIndex] = sortStates[colIndex] === 1 ? -1 : 1;
        const dir = sortStates[colIndex];

        rows.sort((a, b) => {
            const aVal = a.getAttribute('data-sort' + colIndex) || '';
            const bVal = b.getAttribute('data-sort' + colIndex) || '';
            return aVal.localeCompare(bVal) * dir;
        });

        rows.forEach(row => list.appendChild(row));
    }
</script>
<script>
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
                    html += '<div style="border-left:4px solid #198754; border-radius:3px; padding:0.25rem 0.4rem; margin-bottom:0.2rem; background:#f8f9fa; opacity:0.75;">';
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
