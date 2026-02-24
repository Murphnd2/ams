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
    </style>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
    <div class="row g-3 mt-1">

        <%-- ═══ LEFT COLUMN: Personal Checklists ═══ --%>
        <div class="col-lg-4 col-xl-3">
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
        <div class="col-lg-8 col-xl-9">
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
                            <c:when test="${not empty bpoToDos}">
                                <c:forEach var="row" items="${bpoToDos}">
                                    <c:set var="todo" value="${row[0]}"/>
                                    <c:set var="activityName" value="${row[1]}"/>
                                    <c:set var="dueDate" value="${row[2]}"/>
                                    <c:set var="pspName" value="${row[3]}"/>
                                    <c:set var="activityType" value="${row[4].getSimpleName()}"/>
                                    <%-- Due date styling --%>
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
                                         data-sort0="${todo.getTask().getDescription()}"
                                         data-sort1="${pspName}"
                                         data-sort2="${dueDate}"
                                         data-todo-id="${todo.getId()}"
                                         data-task-name="${todo.getTask().getDescription()}"
                                         data-psp-name="${pspName}"
                                         data-activity-name="${activityName} ${activityType}"
                                         data-due-date="<fmt:formatDate value='${dueDate}' pattern='MM/dd/yyyy'/>"
                                         data-goto="${todo.getTask().hasGoTo() && todo.getTask().getGoToLink() != null ? todo.getTask().getGoToLink().getLinkPath() : ''}"
                                         data-info="${todo.getTask().hasInfo() && todo.getTask().getInfoLink() != null ? todo.getTask().getInfoLink().getLinkPath() : ''}"
                                         onclick="openBpoModal(this)"
                                         style="cursor:pointer;">
                                        <div class="col-5">
                                            <div style="font-size:0.85rem; font-weight:500;">
                                                    ${todo.getTask().getDescription()}
                                            </div>
                                            <span style="font-size:0.7rem; color:#6c757d;">${activityName} ${activityType}</span>
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
                    </div>

                </div>
            </div>
        </div>

    </div>
</div>

<%-- ============================================================
     ADD THIS: BPO Task Detail Modal
     Place this just before the closing </body> tag in bpoHome25.jsp
     ============================================================ --%>

<%-- ═══ TASK DETAIL MODAL ═══ --%>
<div class="modal fade" id="bpoTaskModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
        <div class="modal-content">
            <div class="modal-header" style="background:#0d5681; color:white; padding:0.6rem 1rem;">
                <h6 class="modal-title fw-bold m-0">
                    <i class="bi bi-clipboard-check me-1"></i><span id="modalTaskName"></span>
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

                <hr style="margin:0.5rem 0;">

                <%-- Notes section --%>
                <div style="font-size:0.75rem; color:#6c757d; text-transform:uppercase; letter-spacing:0.03em; margin-bottom:0.4rem;">
                    Notes
                </div>
                <div id="modalNotes" style="max-height:200px; overflow-y:auto; margin-bottom:0.75rem;">
                    <div class="text-center text-muted fst-italic py-2" style="font-size:0.8rem;">Loading...</div>
                </div>

                    <%-- Add note (AJAX) --%>
                    <div class="input-group input-group-sm mb-2">
                        <input type="text" class="form-control" id="modalNoteInput" placeholder="Add a note...">
                        <button type="button" class="btn btn-sm btn-outline-ssa" onclick="addNoteAjax()">
                            <i class="bi bi-chat-dots me-1"></i>Add
                        </button>
                    </div>

            </div>
            <div class="modal-footer" style="padding:0.5rem 1rem;">
                <form method="post" action="BpoCompleteTask">
                    <input type="hidden" name="action" value="complete">
                    <input type="hidden" name="todoId" id="modalCompleteToDoId" value="">
                    <button type="submit" class="btn btn-ssa">
                        <i class="bi bi-check-circle me-1"></i>Mark Complete
                    </button>
                </form>
                <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>
<script>
    function openBpoModal(row) {
        const todoId = row.dataset.todoId;
        const taskName = row.dataset.taskName;
        const pspName = row.dataset.pspName;
        const activityName = row.dataset.activityName;
        const dueDate = row.dataset.dueDate;
        const gotoUrl = row.dataset.goto;
        const infoUrl = row.dataset.info;

        // Populate fields
        document.getElementById('modalTaskName').textContent = taskName;
        document.getElementById('modalPspName').textContent = pspName;
        document.getElementById('modalActivityName').textContent = activityName;
        document.getElementById('modalDueDate').textContent = dueDate;
        document.getElementById('modalCompleteToDoId').value = todoId;

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

        fetch('BpoGetNotes?todoId=' + todoId)
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
    function addNoteAjax() {
        const input = document.getElementById('modalNoteInput');
        const noteText = input.value.trim();
        if (!noteText) return;

        const todoId = document.getElementById('modalCompleteToDoId').value;
        input.disabled = true;

        fetch('BpoCompleteTask', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'action=addNote&todoId=' + todoId + '&noteText=' + encodeURIComponent(noteText)
        })
            .then(() => {
                input.value = '';
                input.disabled = false;
                input.focus();
                // Reload notes
                return fetch('BpoGetNotes?todoId=' + todoId);
            })
            .then(r => r.json())
            .then(notes => {
                renderNotes(notes);
            })
            .catch(() => {
                input.disabled = false;
            });
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
</body>
</html>
