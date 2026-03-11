<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Sequence Template Manager</title>
    <style>
        .seq-panel { background: #fff; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); display: flex; flex-direction: column; height: calc(100vh - 80px); }
        .seq-panel-header { background: #0d6681; color: #fff; padding: 12px 16px; border-radius: 8px 8px 0 0; font-weight: 600; font-size: 0.95rem; flex-shrink: 0; }
        .seq-item { padding: 10px 14px; border-bottom: 1px solid #eee; cursor: pointer; transition: background 0.15s; display: flex; align-items: center; gap: 10px; text-decoration: none; color: inherit; }
        .seq-item:hover { background: #f0f7fa; color: inherit; }
        .seq-item.active { background: #e8f4f8; border-left: 3px solid #0d6681; }
        .task-count-badge { font-size: 0.75rem; background: #e9ecef; border-radius: 10px; padding: 2px 8px; color: #555; white-space: nowrap; }
        .seq-name { font-size: 0.9rem; font-weight: 500; }
        .stat-chip { font-size: 0.7rem; padding: 2px 6px; border-radius: 3px; font-weight: 600; }
        .stat-chip.ticket { background: #e0f2fe; color: #0369a1; }
        .stat-chip.renewal { background: #ede9fe; color: #6d28d9; }
        .stat-chip.setup { background: #f0fdf4; color: #15803d; }
        .builder-area { background: #fff; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); min-height: 400px; }
        .builder-header { padding: 16px 20px; border-bottom: 1px solid #eee; }
        .task-row { display: flex; align-items: center; gap: 8px; padding: 8px 20px; border-bottom: 1px solid #f0f0f0; transition: background 0.1s; }
        .task-row:hover { background: #fafbfc; }
        .task-row.dragging { opacity: 0.4; background: #e8f4f8; }
        .task-row.drag-over { border-top: 2px solid #0d6681; }
        .step-badge { width: 30px; height: 30px; border-radius: 50%; background: #e8f4f8; color: #0d6681; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.8rem; flex-shrink: 0; }
        .drag-handle { cursor: grab; color: #bbb; font-size: 1.1rem; flex-shrink: 0; }
        .drag-handle:hover { color: #666; }
        .task-flags .flag { width: 26px; height: 26px; border-radius: 4px; border: 1px solid #ddd; display: inline-flex; align-items: center; justify-content: center; font-size: 0.75rem; color: #aaa; cursor: pointer; transition: all 0.15s; }
        .task-flags .flag.on { background: #e8f4f8; color: #0d6681; border-color: #0d6681; }
        .task-flags .flag:hover { border-color: #0d6681; }
        .add-task-bar { padding: 12px 20px; border-top: 2px dashed #ddd; background: #fafbfc; }
        .save-bar { position: sticky; bottom: 0; background: #fff; border-top: 1px solid #eee; padding: 12px 20px; border-radius: 0 0 8px 8px; }
        .empty-state { text-align: center; padding: 60px 20px; color: #999; }
        .empty-state i { font-size: 3rem; margin-bottom: 16px; display: block; color: #ccc; }
        .new-seq-form { padding: 12px; border-top: 1px solid #eee; background: #fafbfc; border-radius: 0 0 8px 8px; flex-shrink: 0; }
        .seq-list-scroll { flex: 1 1 0; min-height: 80px; overflow-y: auto; }
        .filter-tabs .btn { font-size: 0.78rem; padding: 4px 8px; }
        .filter-tabs .btn.active-filter { background: #0d6681; color: #fff; border-color: #0d6681; }
        /* Suppressed sequences: hidden by default, shown via toggle */
        .seq-suppressed { display: none !important; }
        .seq-list-scroll.show-suppressed .seq-suppressed { display: flex !important; opacity: 0.45; font-style: italic; }
        .seq-list-scroll.show-suppressed .seq-suppressed.active { opacity: 0.7; }
        .suppress-toggle { font-size: 0.75rem; cursor: pointer; user-select: none; }
        .suppress-toggle:hover { color: #0d6681; }
        /* Composite ordering styles */
        .composite-btn { font-size: 0.75rem; padding: 3px 8px; border-radius: 4px; white-space: nowrap; }
        .composite-btn.has-order { border-color: #87a948; color: #87a948; }
        .composite-btn.has-order:hover { background: #87a948; color: #fff; }
        .seq-source-pill { font-size: 0.65rem; padding: 1px 5px; border-radius: 3px; font-weight: 600; margin-right: 2px; display: inline-block; }
        .seq-source-pill.setup { background: #f0fdf4; color: #15803d; }
        .seq-source-pill.renewal { background: #ede9fe; color: #6d28d9; }
        .seq-source-pill.ticket { background: #e0f2fe; color: #0369a1; }
        .task-row.unordered { border-left: 3px dashed #fbbf24; background: #fffbeb; }
    </style>
</head>
<body>
<c:set var="pageTitle" value="Sequence Manager" scope="request"/>
<c:set var="pageIcon" value="bi-signpost-split" scope="request"/>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-3 px-lg-4 mt-2">
    <div class="row g-3">

        <%-- ====================== LEFT PANEL ====================== --%>
        <div class="col-lg-4 col-xl-3">
            <div class="seq-panel">
                <div class="seq-panel-header d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-collection"></i> Template Sequences</span>
                    <span class="badge bg-light text-dark">${seqTotalCount}</span>
                </div>

                <%-- Filter Tabs --%>
                <div class="px-2 pt-2 pb-2" style="background: #fafbfc; border-bottom: 1px solid #eee; flex-shrink: 0;">
                    <div class="btn-group w-100 filter-tabs" role="group">
                        <button type="button" class="btn btn-outline-secondary btn-sm active-filter" onclick="filterSeq('all',this)">
                            All
                        </button>
                        <button type="button" class="btn btn-outline-secondary btn-sm" onclick="filterSeq('ticket',this)">
                            <i class="bi bi-ticket-detailed"></i> <span id="countTicket">${seqTicketCount}</span>
                        </button>
                        <button type="button" class="btn btn-outline-secondary btn-sm" onclick="filterSeq('renewal',this)">
                            <i class="bi bi-repeat"></i> <span id="countRenewal">${seqRenewalCount}</span>
                        </button>
                        <button type="button" class="btn btn-outline-secondary btn-sm" onclick="filterSeq('setup',this)">
                            <i class="bi bi-buildings"></i> <span id="countSetup">${seqSetupCount}</span>
                        </button>
                    </div>
                </div>

                <%-- Composite Order Button — shown when a specific type filter is active --%>
                <div class="px-2 pt-1 pb-1 d-none" id="compositeBar" style="background: #f5f7f0; border-bottom: 1px solid #e0e5d6; flex-shrink: 0;">
                    <a id="compositeBtn" href="#" class="btn btn-outline-secondary composite-btn w-100">
                        <i class="bi bi-layers"></i> <span id="compositeBtnLabel">Composite Order</span>
                    </a>
                </div>

                <%-- Search + Suppressed Toggle --%>
                <div class="p-2 d-flex align-items-center gap-2" style="border-bottom: 1px solid #eee; flex-shrink: 0;">
                    <div class="input-group input-group-sm flex-fill">
                        <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
                        <input type="text" class="form-control border-start-0" placeholder="Filter sequences..." id="seqSearch" oninput="searchSeq()">
                    </div>
                    <span class="suppress-toggle text-muted" onclick="toggleSuppressed(this)" title="Show/hide suppressed sequences">
                        <i class="bi bi-eye-slash"></i>
                    </span>
                </div>

                <%-- Sequence List --%>
                <div class="seq-list-scroll" id="seqListContainer">
                    <c:forEach var="tix" items="${sessionScope.seqTicketDisplay}">
                        <c:set var="isActive" value="" />
                        <c:if test="${sessionScope.sbSelectedId == tix.getId()}"><c:set var="isActive" value="active" /></c:if>
                        <c:set var="suppressedClass" value="" />
                        <c:set var="suppressedBadge" value="" />
                        <c:set var="suppressedAttr" value="false" />
                        <c:if test="${tix.getServiceItem() != null && tix.getServiceItem().isSuppressed()}">
                            <c:set var="suppressedClass" value="seq-suppressed" />
                            <c:set var="suppressedBadge"><span class="badge bg-warning text-dark ms-1" style="font-size:0.65rem;">hidden</span></c:set>
                            <c:set var="suppressedAttr" value="true" />
                        </c:if>
                        <a href="SequenceBuilder25?load=${tix.getId()}" class="seq-item ${isActive} ${suppressedClass}" data-type="ticket" data-searchname="${tix.getDescription()}" data-suppressed="${suppressedAttr}">
                            <span class="stat-chip ticket">Ticket</span>
                            <span class="seq-name flex-fill">${tix.getDescription()}${suppressedBadge}</span>
                            <span class="task-count-badge">${taskCountMap[tix.getId()]} tasks</span>
                        </a>
                    </c:forEach>

                    <c:forEach var="ren" items="${sessionScope.seqRenewalList}">
                        <c:set var="isActive" value="" />
                        <c:if test="${sessionScope.sbSelectedId == ren.getId()}"><c:set var="isActive" value="active" /></c:if>
                        <a href="SequenceBuilder25?load=${ren.getId()}" class="seq-item ${isActive}" data-type="renewal" data-searchname="${ren.getDescription()}" data-suppressed="false">
                            <span class="stat-chip renewal">Renewal</span>
                            <span class="seq-name flex-fill">${ren.getDescription()}</span>
                            <span class="task-count-badge">${taskCountMap[ren.getId()]} tasks</span>
                        </a>
                    </c:forEach>
                    <c:forEach var="setup" items="${sessionScope.seqSetupList}">
                        <c:set var="isActive" value="" />
                        <c:if test="${sessionScope.sbSelectedId == setup.getId()}"><c:set var="isActive" value="active" /></c:if>
                        <a href="SequenceBuilder25?load=${setup.getId()}" class="seq-item ${isActive}" data-type="setup" data-searchname="${setup.getDescription()}" data-suppressed="false">
                            <span class="stat-chip setup">Setup</span>
                            <span class="seq-name flex-fill">${setup.getDescription()}</span>
                            <span class="task-count-badge">${taskCountMap[setup.getId()]} tasks</span>
                        </a>
                    </c:forEach>
                </div>

                <%-- New Sequence --%>
                <div class="new-seq-form">
                    <button class="btn btn-sm w-100 fw-bold text-white" style="background:#0d6681;"
                            data-bs-toggle="collapse" data-bs-target="#newSeqCollapse">
                        <i class="bi bi-plus-circle"></i> New Sequence
                    </button>
                    <div class="collapse mt-2" id="newSeqCollapse">
                        <form method="post" action="SequenceAction25" id="newSeqForm">
                            <input type="hidden" name="action" value="CREATE">
                            <input type="hidden" name="f" value="${param.f}">
                            <input type="hidden" name="ss" value="${param.ss}">
                            <div class="mb-2">
                                <select class="form-select form-select-sm" name="newSeqType" id="newSeqType" onchange="toggleNewSeqFields()" required>
                                    <option value="" selected disabled>Activity Type</option>
                                    <option value="ticket">Ticket</option>
                                    <option value="renewal">Renewal</option>
                                    <option value="setup">Setup</option>
                                </select>
                            </div>
                            <div class="mb-2 d-none" id="ticketCatRow">
                                <select class="form-select form-select-sm" name="ticketCategoryId" id="ticketCatSelect" onchange="toggleNewCatFields()">
                                    <option selected disabled>Ticket Category</option>
                                    <c:forEach var="cat" items="${sessionScope.sbTicketCategories}">
                                        <option value="${cat.getId()}">${cat.getDescription()}</option>
                                    </c:forEach>
                                    <option value="-1">+ New Category...</option>
                                </select>
                            </div>
                            <div class="mb-2 d-none" id="newCatRow">
                                <div class="input-group input-group-sm">
                                    <span class="input-group-text" style="font-size:0.75rem;">Category Name</span>
                                    <input type="text" class="form-control form-control-sm" name="newCategoryName" id="newCategoryName" placeholder="e.g. Payroll Issues">
                                </div>
                                <div class="input-group input-group-sm mt-1">
                                    <span class="input-group-text" style="font-size:0.75rem;">Short Code</span>
                                    <input type="text" class="form-control form-control-sm" name="newCategoryShort" id="newCategoryShort" placeholder="e.g. Payroll" maxlength="20">
                                </div>
                            </div>
                            <div class="mb-2 d-none" id="renewalPurposeRow">
                                <select class="form-select form-select-sm" name="purposeId">
                                    <option selected disabled>Select Benefit Type</option>
                                    <c:forEach var="tp" items="${sessionScope.sbUnassignedRenewal}">
                                        <option value="${tp.getId()}">${tp.getDescription()}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="mb-2 d-none" id="setupPurposeRow">
                                <select class="form-select form-select-sm" name="purposeId">
                                    <option selected disabled>Select LOS/Module</option>
                                    <c:forEach var="tp" items="${sessionScope.sbUnassignedSetup}">
                                        <option value="${tp.getId()}">${tp.getDescription()}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="mb-2" id="seqNameRow">
                                <input type="text" class="form-control form-control-sm" name="seqName" id="seqNameInput" placeholder="Sequence name">
                            </div>
                            <button type="submit" class="btn btn-sm btn-success w-100"><i class="bi bi-check-lg"></i> Create</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>

        <%-- ====================== RIGHT PANEL ====================== --%>
        <div class="col-lg-8 col-xl-9">
            <div class="builder-area">
                <c:choose>
                    <%-- ===== COMPOSITE ORDER VIEW ===== --%>
                    <c:when test="${compositeGroupId > 0}">
                        <div class="builder-header">
                            <h5 class="mb-1" style="color:#0d6681; font-weight:700;">
                                <i class="bi bi-layers"></i> Composite Order: ${compositeGroupName}
                            </h5>
                            <small class="text-muted">
                                ${compositeTaskCount} unique tasks across all ${compositeGroupName} sequences
                                <c:if test="${hasExistingComposite}">
                                    &middot; <span style="color:#87a948;"><i class="bi bi-check-circle-fill"></i> Saved</span>
                                </c:if>
                            </small>
                        </div>

                        <form method="post" action="SequenceAction25" id="compositeForm">
                            <input type="hidden" name="action" value="SAVE_COMPOSITE">
                            <input type="hidden" name="compositeGroupId" value="${compositeGroupId}">
                            <input type="hidden" name="compositeOrder" id="compositeOrderField" value="">
                            <input type="hidden" name="f" value="${param.f}">
                            <input type="hidden" name="ss" value="${param.ss}">

                            <div id="compositeTaskList">
                                <c:forEach var="ctv" items="${compositeTaskList}" varStatus="idx">
                                    <div class="task-row ${!ctv.ordered ? 'unordered' : ''}" draggable="true"
                                         data-task-id="${ctv.task.id}"
                                         data-desc="${ctv.task.description}">
                                        <span class="drag-handle"><i class="bi bi-grip-vertical"></i></span>
                                        <span class="step-badge">${idx.count}</span>
                                        <div style="flex:1; font-size:0.9rem; padding:4px 8px;">
                                            ${ctv.task.description}
                                            <div style="margin-top:2px;">
                                                <c:forEach var="seqName" items="${ctv.sequenceNames}">
                                                    <span class="seq-source-pill <c:choose><c:when test='${compositeGroupId == 1}'>renewal</c:when><c:when test='${compositeGroupId == 2}'>setup</c:when><c:otherwise>ticket</c:otherwise></c:choose>">${seqName}</span>
                                                </c:forEach>
                                            </div>
                                        </div>
                                        <div class="task-flags">
                                            <span class="flag ${ctv.reusable ? 'on' : ''}" title="Reusable"><i class="bi bi-floppy"></i></span>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>

                            <c:if test="${compositeTaskCount == 0}">
                                <div class="empty-state">
                                    <i class="bi bi-layers"></i>
                                    <h5>No Tasks Found</h5>
                                    <p class="text-muted">There are no tasks in any ${compositeGroupName} sequences yet.<br>
                                        Add tasks to individual sequences first, then use this view to set the composite order.</p>
                                </div>
                            </c:if>

                            <c:if test="${compositeTaskCount > 0}">
                                <div class="save-bar d-flex justify-content-between">
                                    <a href="SequenceBuilder25?f=${param.f}&ss=${param.ss}" class="btn btn-sm btn-outline-secondary"><i class="bi bi-x-lg"></i> Cancel</a>
                                    <button type="submit" class="btn btn-sm btn-dark" onclick="prepareCompositeSubmit()"><i class="bi bi-check2-all"></i> Save Composite Order</button>
                                </div>
                            </c:if>
                        </form>
                    </c:when>

                    <%-- ===== SINGLE SEQUENCE BUILDER VIEW ===== --%>
                    <c:when test="${sessionScope.sbSelectedId > 0}">

                        <%-- Header --%>
                        <div class="builder-header">
                            <h5 class="mb-1" style="color:#0d6681; font-weight:700;">
                                <c:if test="${sessionScope.sbSelectedGroupId == 3}"><i class="bi bi-ticket-detailed text-info"></i></c:if>
                                <c:if test="${sessionScope.sbSelectedGroupId == 1}"><i class="bi bi-repeat text-primary"></i></c:if>
                                <c:if test="${sessionScope.sbSelectedGroupId == 2}"><i class="bi bi-buildings text-secondary"></i></c:if>
                                    ${sessionScope.sbSelectedName}
                            </h5>
                            <small class="text-muted">
                                Sequence #${sessionScope.sbSelectedId} &middot; ${sessionScope.sbListBuilder.size()} tasks
                            </small>
                                <%-- Suppress toggle — ticket sequences only --%>
                            <c:if test="${sessionScope.sbSelectedGroupId == 3}">
                                <form method="post" action="SequenceAction25" class="d-inline ms-2">
                                    <input type="hidden" name="action" value="SUPPRESS">
                                    <input type="hidden" name="sequenceId" value="${sessionScope.sbSelectedId}">
                                    <input type="hidden" name="f" value="${param.f}">
                                    <input type="hidden" name="ss" value="${param.ss}">
                                    <c:choose>
                                        <c:when test="${sessionScope.sbIsSuppressed}">
                                            <button type="submit" class="btn btn-sm btn-outline-success"
                                                    title="This sequence is hidden from the Create Ticket dropdown. Click to restore.">
                                                <i class="bi bi-eye"></i> Restore to Dropdown
                                            </button>
                                        </c:when>
                                        <c:otherwise>
                                            <button type="submit" class="btn btn-sm btn-outline-warning"
                                                    title="Hide this reason from the Create Ticket dropdown. The sequence will still be available here.">
                                                <i class="bi bi-eye-slash"></i> Hide from Dropdown
                                            </button>
                                        </c:otherwise>
                                    </c:choose>
                                </form>
                            </c:if>
                        </div>

                        <%-- Task Rows --%>
                        <form method="post" action="SequenceAction25" id="builderForm">
                            <input type="hidden" name="action" value="SAVE">
                            <input type="hidden" name="sequenceId" value="${sessionScope.sbSelectedId}">
                            <input type="hidden" name="taskOrder" id="taskOrderField" value="">
                            <input type="hidden" name="f" value="${param.f}">
                            <input type="hidden" name="ss" value="${param.ss}">

                            <div id="taskList">
                                <c:forEach var="gs" items="${sessionScope.sbListBuilder}" varStatus="idx">
                                    <c:if test="${gs.getDescription() != null and gs.getDescription().length() > 0}">
                                        <div class="task-row" draggable="true"
                                             data-task-id="${gs.getTask() != null ? gs.getTask().getId() : -1}"
                                             data-desc="${gs.getDescription()}"
                                             data-reusable="${gs.getTask() != null && gs.getTask().isReUsable()}">
                                            <span class="drag-handle"><i class="bi bi-grip-vertical"></i></span>
                                            <span class="step-badge">${idx.count}</span>
                                            <div style="flex:1; font-size:0.9rem; padding:4px 8px;">${gs.getDescription()}</div>
                                            <div class="task-flags">
                                                <span class="flag ${gs.getTask() != null && gs.getTask().isReUsable() ? 'on' : ''}" title="Reusable"><i class="bi bi-floppy"></i></span>
                                            </div>
                                            <button type="button" class="btn btn-sm btn-outline-danger" onclick="removeTask(this)"><i class="bi bi-x-lg"></i></button>
                                        </div>
                                    </c:if>
                                </c:forEach>
                            </div>

                            <%-- Add Task Bar --%>
                            <div class="add-task-bar">
                                <div class="row g-2 align-items-center">
                                    <div class="col-auto">
                                        <div class="btn-group btn-group-sm" role="group">
                                            <input type="radio" name="addMode" id="addExisting" class="btn-check" autocomplete="off" checked onclick="toggleAddMode()">
                                            <label class="btn btn-outline-secondary" for="addExisting"><i class="bi bi-journal-text"></i> Existing</label>
                                            <input type="radio" name="addMode" id="addNew" class="btn-check" autocomplete="off" onclick="toggleAddMode()">
                                            <label class="btn btn-outline-secondary" for="addNew"><i class="bi bi-journal-plus"></i> New</label>
                                        </div>
                                    </div>
                                    <div class="col" id="existingTaskInput">
                                        <select class="form-select form-select-sm" id="existingTaskSelect">
                                            <option value="-1">Select a reusable task...</option>
                                            <c:forEach var="rt" items="${sessionScope.sbReusableTasks}">
                                                <option value="${rt.getId()}" data-desc="${rt.getDescription()}">${rt.getDescription()}</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                    <div class="col d-none" id="newTaskInput">
                                        <div class="input-group input-group-sm">
                                            <input type="text" class="form-control" id="newTaskDesc" placeholder="New task description">
                                            <span class="input-group-text">
                                                <input type="checkbox" class="form-check-input me-1" id="saveReusable"> <label for="saveReusable" style="font-size:0.75rem; cursor:pointer;">Save</label>
                                            </span>
                                        </div>
                                    </div>
                                    <div class="col-auto">
                                        <button type="button" class="btn btn-sm btn-primary" onclick="addTask()"><i class="bi bi-plus-lg"></i> Add</button>
                                    </div>
                                </div>
                            </div>

                                <%-- Save Bar --%>
                            <div class="save-bar d-flex justify-content-between">
                                <a href="SequenceBuilder25" class="btn btn-sm btn-outline-secondary"><i class="bi bi-x-lg"></i> Cancel</a>
                                <button type="submit" class="btn btn-sm btn-dark" onclick="prepareSubmit()"><i class="bi bi-check2-all"></i> Save Sequence</button>
                            </div>
                        </form>

                    </c:when>
                    <c:otherwise>
                        <div class="empty-state">
                            <i class="bi bi-collection"></i>
                            <h5>Select a Sequence</h5>
                            <p class="text-muted">Choose a sequence from the left panel to view and edit its tasks,<br>
                                or create a new one with the button below the list.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

    </div>
</div>

<script>
    /* ── View state from URL params ── */
    var urlParams = new URLSearchParams(window.location.search);
    var currentFilter = urlParams.get('f') || 'all';
    var showSuppressedInit = urlParams.get('ss') === '1';

    /* ── Helper: build query string for current view state ── */
    function viewQS() {
        var container = document.getElementById('seqListContainer');
        var ss = container.classList.contains('show-suppressed') ? '1' : '0';
        var f = currentFilter;
        return 'f=' + f + '&ss=' + ss;
    }

    /* ── Rewrite all sequence links to carry view state ── */
    function updateSeqLinks() {
        var qs = viewQS();
        document.querySelectorAll('.seq-item').forEach(function(a) {
            var base = a.href.split('?')[0];
            var loadParam = new URL(a.href).searchParams.get('load');
            a.href = base + '?load=' + loadParam + '&' + qs;
        });
        /* Also update all form hidden fields for f and ss */
        document.querySelectorAll('input[name="f"]').forEach(function(el) { el.value = currentFilter; });
        var ssVal = document.getElementById('seqListContainer').classList.contains('show-suppressed') ? '1' : '0';
        document.querySelectorAll('input[name="ss"]').forEach(function(el) { el.value = ssVal; });
    }

    /* ── Update filter tab counts based on visible items ── */
    function updateCounts() {
        var showSuppressed = document.getElementById('seqListContainer').classList.contains('show-suppressed');
        var tickets = 0, renewals = 0, setups = 0;
        document.querySelectorAll('.seq-item').forEach(function(item) {
            var isSuppressed = item.getAttribute('data-suppressed') === 'true';
            if (isSuppressed && !showSuppressed) return;
            var type = item.getAttribute('data-type');
            if (type === 'ticket') tickets++;
            else if (type === 'renewal') renewals++;
            else if (type === 'setup') setups++;
        });
        document.getElementById('countTicket').textContent = tickets;
        document.getElementById('countRenewal').textContent = renewals;
        document.getElementById('countSetup').textContent = setups;
    }

    /* ── Suppressed toggle ── */
    function toggleSuppressed(el) {
        var container = document.getElementById('seqListContainer');
        var showing = container.classList.toggle('show-suppressed');
        el.innerHTML = showing ? '<i class="bi bi-eye"></i>' : '<i class="bi bi-eye-slash"></i>';
        el.title = showing ? 'Hide suppressed sequences' : 'Show suppressed sequences';
        updateCounts();
        applyFilter();
        updateSeqLinks();
    }

    /* ── Apply current filter to items ── */
    function applyFilter() {
        var type = currentFilter;
        var showSuppressed = document.getElementById('seqListContainer').classList.contains('show-suppressed');
        document.querySelectorAll('.seq-item').forEach(function(item) {
            var matchesType = (type === 'all' || item.getAttribute('data-type') === type);
            var isSuppressed = item.getAttribute('data-suppressed') === 'true';
            if (!matchesType) {
                item.style.display = 'none';
            } else if (isSuppressed && !showSuppressed) {
                item.style.display = '';
            } else {
                item.style.display = 'flex';
            }
        });
    }

    /* ── Filter by type (from button click) ── */
    function filterSeq(type, btn) {
        document.querySelectorAll('.filter-tabs .btn').forEach(function(b) { b.classList.remove('active-filter'); });
        btn.classList.add('active-filter');
        currentFilter = type;
        applyFilter();
        updateSeqLinks();
    }

    /* ── Search filter ── */
    function searchSeq() {
        var term = document.getElementById('seqSearch').value.toLowerCase();
        var showSuppressed = document.getElementById('seqListContainer').classList.contains('show-suppressed');
        document.querySelectorAll('.seq-item').forEach(function(item) {
            var name = (item.getAttribute('data-searchname') || '').toLowerCase();
            var matchesSearch = name.indexOf(term) >= 0;
            var isSuppressed = item.getAttribute('data-suppressed') === 'true';
            if (!matchesSearch) {
                item.style.display = 'none';
            } else if (isSuppressed && !showSuppressed) {
                item.style.display = '';
            } else {
                item.style.display = 'flex';
            }
        });
    }

    /* ── Restore view state on page load ── */
    (function initViewState() {
        var container = document.getElementById('seqListContainer');
        var toggle = document.querySelector('.suppress-toggle');

        // Restore show-suppressed state
        if (showSuppressedInit) {
            container.classList.add('show-suppressed');
            if (toggle) { toggle.innerHTML = '<i class="bi bi-eye"></i>'; toggle.title = 'Hide suppressed sequences'; }
        }
        // Auto-show if selected sequence is suppressed (even if ss param wasn't set)
        <c:if test="${sessionScope.sbIsSuppressed}">
        if (!container.classList.contains('show-suppressed')) {
            container.classList.add('show-suppressed');
            if (toggle) { toggle.innerHTML = '<i class="bi bi-eye"></i>'; toggle.title = 'Hide suppressed sequences'; }
        }
        </c:if>

        // Restore active filter tab
        if (currentFilter !== 'all') {
            var btns = document.querySelectorAll('.filter-tabs .btn');
            btns.forEach(function(b) {
                b.classList.remove('active-filter');
                var onclick = b.getAttribute('onclick') || '';
                if (onclick.indexOf("'" + currentFilter + "'") >= 0) {
                    b.classList.add('active-filter');
                }
            });
            applyFilter();
        }

        updateCounts();
        updateSeqLinks();
    })();

    function toggleAddMode() {
        var isNew = document.getElementById('addNew').checked;
        document.getElementById('newTaskInput').className = isNew ? 'col' : 'col d-none';
        document.getElementById('existingTaskInput').className = isNew ? 'col d-none' : 'col';
    }

    function addTask() {
        var isNew = document.getElementById('addNew').checked;
        var desc, taskId, reusable;
        if (isNew) {
            desc = document.getElementById('newTaskDesc').value.trim();
            if (!desc) return;
            taskId = -1;
            reusable = document.getElementById('saveReusable').checked;
            document.getElementById('newTaskDesc').value = '';
        } else {
            var sel = document.getElementById('existingTaskSelect');
            if (sel.value === '-1') return;
            desc = sel.options[sel.selectedIndex].getAttribute('data-desc');
            taskId = sel.value;
            reusable = true;
            sel.value = '-1';
        }
        var list = document.getElementById('taskList');
        var count = list.children.length + 1;
        var row = document.createElement('div');
        row.className = 'task-row';
        row.draggable = true;
        row.setAttribute('data-task-id', taskId);
        row.setAttribute('data-desc', desc);
        row.setAttribute('data-reusable', reusable);
        row.innerHTML =
            '<span class="drag-handle"><i class="bi bi-grip-vertical"></i></span>' +
            '<span class="step-badge">' + count + '</span>' +
            '<div style="flex:1;font-size:0.9rem;padding:4px 8px;">' + escapeHtml(desc) + '</div>' +
            '<div class="task-flags">' +
            '<span class="flag ' + (reusable ? 'on' : '') + '" title="Reusable"><i class="bi bi-floppy"></i></span>' +
            '</div>' +
            '<button type="button" class="btn btn-sm btn-outline-danger" onclick="removeTask(this)"><i class="bi bi-x-lg"></i></button>';
        attachDragEvents(row);
        list.appendChild(row);
        renumber();
    }

    function removeTask(btn) { btn.closest('.task-row').remove(); renumber(); }

    function renumber() {
        document.querySelectorAll('#taskList .task-row').forEach(function(row, i) {
            row.querySelector('.step-badge').textContent = i + 1;
        });
    }

    var draggedRow = null;
    function attachDragEvents(row) {
        row.addEventListener('dragstart', function(e) { draggedRow = row; row.classList.add('dragging'); e.dataTransfer.effectAllowed = 'move'; });
        row.addEventListener('dragover', function(e) { e.preventDefault(); document.querySelectorAll('.drag-over').forEach(function(r){r.classList.remove('drag-over');}); row.classList.add('drag-over'); });
        row.addEventListener('dragleave', function() { row.classList.remove('drag-over'); });
        row.addEventListener('drop', function(e) {
            e.preventDefault();
            if (draggedRow && draggedRow !== row) {
                var list = document.getElementById('taskList');
                var rows = Array.from(list.children);
                if (rows.indexOf(draggedRow) < rows.indexOf(row)) { row.after(draggedRow); } else { row.before(draggedRow); }
                renumber();
            }
            document.querySelectorAll('.drag-over').forEach(function(r){r.classList.remove('drag-over');});
        });
        row.addEventListener('dragend', function() { if(draggedRow) draggedRow.classList.remove('dragging'); document.querySelectorAll('.drag-over').forEach(function(r){r.classList.remove('drag-over');}); draggedRow=null; });
    }
    document.querySelectorAll('#taskList .task-row').forEach(attachDragEvents);

    document.addEventListener('click', function(e) { var flag = e.target.closest('.task-flags .flag'); if(flag) flag.classList.toggle('on'); });

    function toggleNewSeqFields() {
        var v = document.getElementById('newSeqType').value;
        document.getElementById('ticketCatRow').className = v === 'ticket' ? 'mb-2' : 'mb-2 d-none';
        document.getElementById('renewalPurposeRow').className = v === 'renewal' ? 'mb-2' : 'mb-2 d-none';
        document.getElementById('setupPurposeRow').className = v === 'setup' ? 'mb-2' : 'mb-2 d-none';
        if (v !== 'ticket') { document.getElementById('newCatRow').className = 'mb-2 d-none'; }
        // Hide sequence name for renewal/setup (auto-derived from ServiceItem)
        var showName = (v === 'ticket' || v === '');
        document.getElementById('seqNameRow').className = showName ? 'mb-2' : 'mb-2 d-none';
        document.getElementById('seqNameInput').required = (v === 'ticket');
    }

    function toggleNewCatFields() {
        var sel = document.getElementById('ticketCatSelect');
        var isNew = sel.value === '-1';
        document.getElementById('newCatRow').className = isNew ? 'mb-2' : 'mb-2 d-none';
        if (isNew) { document.getElementById('newCategoryName').focus(); }
    }

    function prepareSubmit() {
        var rows = document.querySelectorAll('#taskList .task-row');
        var tasks = [];
        rows.forEach(function(row, i) {
            var reusableFlag = row.querySelector('.task-flags .flag');
            tasks.push({
                order: i,
                taskId: row.getAttribute('data-task-id') || -1,
                desc: row.getAttribute('data-desc') || '',
                reusable: reusableFlag && reusableFlag.classList.contains('on')
            });
        });
        document.getElementById('taskOrderField').value = JSON.stringify(tasks);
    }

    function escapeHtml(text) { var d = document.createElement('div'); d.textContent = text; return d.innerHTML; }

    /* ── Composite Order ── */

    // Composite existence flags from server
    var hasCompositeMap = {
        ticket: ${hasCompositeTicket != null ? hasCompositeTicket : false},
        renewal: ${hasCompositeRenewal != null ? hasCompositeRenewal : false},
        setup: ${hasCompositeSetup != null ? hasCompositeSetup : false}
    };
    var groupIdMap = { ticket: 3, renewal: 1, setup: 2 };

    // Show/hide composite button based on active filter
    function updateCompositeBar() {
        var bar = document.getElementById('compositeBar');
        var btn = document.getElementById('compositeBtn');
        var label = document.getElementById('compositeBtnLabel');
        if (currentFilter === 'all' || !groupIdMap[currentFilter]) {
            bar.classList.add('d-none');
            return;
        }
        bar.classList.remove('d-none');
        var gid = groupIdMap[currentFilter];
        var typeName = currentFilter.charAt(0).toUpperCase() + currentFilter.slice(1);
        label.textContent = 'Composite Order: ' + typeName + 's';
        btn.href = 'SequenceBuilder25?composite=' + gid + '&' + viewQS();
        // Add indicator if composite exists
        if (hasCompositeMap[currentFilter]) {
            btn.classList.add('has-order');
            label.innerHTML = '<i class="bi bi-check-circle-fill"></i> Composite Order: ' + typeName + 's';
        } else {
            btn.classList.remove('has-order');
        }
    }

    // Hook into filterSeq to update composite bar
    var origFilterSeq = filterSeq;
    filterSeq = function(type, btn) {
        origFilterSeq(type, btn);
        updateCompositeBar();
    };

    // Initialize composite bar on load
    updateCompositeBar();

    // Composite drag-and-drop (reuse same pattern as single-sequence)
    document.querySelectorAll('#compositeTaskList .task-row').forEach(function(row) {
        row.addEventListener('dragstart', function(e) { draggedRow = row; row.classList.add('dragging'); e.dataTransfer.effectAllowed = 'move'; });
        row.addEventListener('dragover', function(e) { e.preventDefault(); document.querySelectorAll('.drag-over').forEach(function(r){r.classList.remove('drag-over');}); row.classList.add('drag-over'); });
        row.addEventListener('dragleave', function() { row.classList.remove('drag-over'); });
        row.addEventListener('drop', function(e) {
            e.preventDefault();
            if (draggedRow && draggedRow !== row) {
                var list = document.getElementById('compositeTaskList');
                var rows = Array.from(list.children);
                if (rows.indexOf(draggedRow) < rows.indexOf(row)) { row.after(draggedRow); } else { row.before(draggedRow); }
                renumberComposite();
            }
            document.querySelectorAll('.drag-over').forEach(function(r){r.classList.remove('drag-over');});
        });
        row.addEventListener('dragend', function() { if(draggedRow) draggedRow.classList.remove('dragging'); document.querySelectorAll('.drag-over').forEach(function(r){r.classList.remove('drag-over');}); draggedRow=null; });
    });

    function renumberComposite() {
        document.querySelectorAll('#compositeTaskList .task-row').forEach(function(row, i) {
            row.querySelector('.step-badge').textContent = i + 1;
            // Once dragged into position, remove unordered styling
            row.classList.remove('unordered');
        });
    }

    function prepareCompositeSubmit() {
        var rows = document.querySelectorAll('#compositeTaskList .task-row');
        var tasks = [];
        rows.forEach(function(row, i) {
            tasks.push({
                taskId: parseInt(row.getAttribute('data-task-id')),
                order: i * 10
            });
        });
        document.getElementById('compositeOrderField').value = JSON.stringify(tasks);
    }

    // If in composite mode, auto-activate the correct filter tab
    <c:if test="${compositeGroupId > 0}">
    (function() {
        var typeMap = {1: 'renewal', 2: 'setup', 3: 'ticket'};
        var type = typeMap[${compositeGroupId}];
        if (type) {
            currentFilter = type;
            var btns = document.querySelectorAll('.filter-tabs .btn');
            btns.forEach(function(b) {
                b.classList.remove('active-filter');
                var onclick = b.getAttribute('onclick') || '';
                if (onclick.indexOf("'" + type + "'") >= 0) b.classList.add('active-filter');
            });
            applyFilter();
            updateCompositeBar();
        }
    })();
    </c:if>
</script>
</body>
</html>
