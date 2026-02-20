<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <title>Rate Manager</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        .rate-card { cursor: pointer; transition: all 0.15s; }
        .rate-card:hover { background-color: #f0f4f8; }
        .rate-card.active { border-left: 4px solid #2B5F8A; background-color: #e8eef4; }
        .module-header { background-color: #2B5F8A; color: white; font-weight: 600; padding: 0.4rem 0.75rem; border-radius: 4px; margin-top: 0.75rem; margin-bottom: 0.25rem; font-size: 0.9rem; }
        .rate-row { border-bottom: 1px solid #eee; padding: 0.35rem 0; }
        .rate-row:last-child { border-bottom: none; }
        .agency-chip { display: inline-block; background: #e9ecef; border-radius: 20px; padding: 0.25rem 0.75rem; margin: 0.2rem; font-size: 0.85rem; }
        .price-display { font-family: 'Segoe UI', monospace; font-weight: 600; }
        .empty-state { text-align: center; color: #6c757d; padding: 3rem 1rem; }
        .empty-state i { font-size: 2.5rem; margin-bottom: 0.5rem; display: block; }
        .fee-type-item.suppressed-hidden { display: none !important; }
        .fee-type-item.suppressed-visible { opacity: 0.5; }
        .lock-icon { color: #dc3545; font-size: 0.8rem; }
        .locked-banner { background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 6px; padding: 0.6rem 1rem; margin-bottom: 0.75rem; }
    </style>
</head>
<body class="bg-light">
<div class="container-fluid py-3" style="max-width: 1400px;">

    <%-- Header --%>
    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h4 class="mb-0"><i class="bi bi-cash-coin me-2"></i>Rate Manager</h4>
            <small class="text-muted">Manage pricing tiers, fee types, and rate assignments</small>
        </div>
        <div class="d-flex gap-2">
            <a href="PspAgencyHome" class="btn btn-outline-primary btn-sm">
                <i class="bi bi-people-fill me-1"></i>Agency Manager
            </a>
            <a href="ViewHome25" class="btn btn-outline-dark btn-sm">
                <i class="bi bi-house me-1"></i>Home
            </a>
        </div>
    </div>

    <div class="row g-3">

        <%-- ======================== LEFT COLUMN: Rate List ======================== --%>
        <div class="col-lg-3">
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center" style="background-color: #2B5F8A; color: white;">
                    <span class="fw-bold"><i class="bi bi-tags me-1"></i>Rates</span>
                    <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addRateModal">
                        <i class="bi bi-plus-lg"></i>
                    </button>
                </div>
                <div class="card-body p-0">
                    <c:choose>
                        <c:when test="${not empty rateList}">
                            <c:forEach var="rate" items="${rateList}">
                                <a href="PspAdminHome?rateId=${rate.getId()}" class="d-block text-decoration-none text-dark">
                                    <div class="rate-card p-2 ps-3 ${selectedRate != null && selectedRate.getId() == rate.getId() ? 'active' : ''}">
                                        <div class="d-flex justify-content-between align-items-center">
                                            <span class="fw-semibold">${rate.getDescription()}</span>
                                            <c:if test="${lockedRateIds.contains(rate.getId())}">
                                                <i class="bi bi-lock-fill lock-icon" title="In use by proposals — locked"></i>
                                            </c:if>
                                        </div>
                                    </div>
                                </a>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <div class="empty-state py-4">
                                <i class="bi bi-tags"></i>
                                <p class="mb-0">No rates created yet</p>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <%-- Fee Types card --%>
            <div class="card mt-3">
                <div class="card-header d-flex justify-content-between align-items-center" style="background-color: #5a8a6a; color: white;">
                    <span class="fw-bold"><i class="bi bi-receipt me-1"></i>Fee Types</span>
                    <div class="d-flex gap-1">
                        <button type="button" class="btn btn-sm btn-outline-light" id="toggleSuppressedBtn" title="Show suppressed fee types" onclick="toggleSuppressedView()">
                            <i class="bi bi-eye-slash" id="toggleSuppressedIcon"></i>
                        </button>
                        <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addPriceItemModal">
                            <i class="bi bi-plus-lg"></i>
                        </button>
                    </div>
                </div>
                <div class="card-body p-0" style="max-height: 340px; overflow-y: auto;">
                    <c:choose>
                        <c:when test="${not empty priceItemList}">
                            <div id="feeTypeList">
                                <c:forEach var="pi" items="${priceItemList}">
                                    <div class="fee-type-item d-flex justify-content-between align-items-center px-2 py-2 ${pi.isSuppressed() ? 'suppressed-hidden suppressed-visible' : ''}"
                                         data-pi-id="${pi.getId()}" data-is-suppressed="${pi.isSuppressed()}" draggable="true"
                                         style="border-bottom: 1px solid #eee; cursor: grab;">
                                        <div class="d-flex align-items-center gap-2">
                                            <i class="bi bi-grip-vertical text-muted" style="font-size: 0.9rem;"></i>
                                            <span class="fw-semibold fee-desc">${pi.getDescription()}</span>
                                        </div>
                                        <div class="d-flex align-items-center gap-1">
                                            <c:choose>
                                                <c:when test="${pi.isSuppressed()}">
                                                    <button type="button" class="btn btn-sm btn-outline-success fee-suppress-btn"
                                                            data-pi-id="${pi.getId()}" data-suppressed="true"
                                                            style="padding: 0.1rem 0.4rem; font-size: 0.7rem;"
                                                            title="Click to restore">
                                                        <i class="bi bi-eye"></i>
                                                    </button>
                                                </c:when>
                                                <c:otherwise>
                                                    <button type="button" class="btn btn-sm btn-outline-secondary fee-suppress-btn"
                                                            data-pi-id="${pi.getId()}" data-suppressed="false"
                                                            style="padding: 0.1rem 0.4rem; font-size: 0.7rem;"
                                                            title="Click to suppress">
                                                        <i class="bi bi-eye-slash"></i>
                                                    </button>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="text-center text-muted py-3">
                                <small>No fee types defined</small>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <%-- ======================== CENTER COLUMN: Rate Detail / Pricing Grid ======================== --%>
        <div class="col-lg-6">
            <c:choose>
                <c:when test="${not empty selectedRate}">
                    <%-- Rate Header --%>
                    <div class="card mb-3">
                        <div class="card-body py-2">
                            <div class="d-flex justify-content-between align-items-center">
                                <div>
                                    <h5 class="mb-0">
                                        ${selectedRate.getDescription()}
                                        <c:if test="${isLocked}">
                                            <i class="bi bi-lock-fill lock-icon ms-1" title="Locked"></i>
                                        </c:if>
                                    </h5>
                                </div>
                                <div class="d-flex gap-1">
                                    <c:choose>
                                        <c:when test="${isLocked}">
                                            <form method="post" action="RateTableAction" class="d-inline"
                                                  onsubmit="return confirm('This will create an editable copy of this rate with the same pricing and agency assignments. The current rate will be archived. Continue?');">
                                                <input type="hidden" name="action" value="cloneRate"/>
                                                <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                                                <button type="submit" class="btn btn-warning btn-sm">
                                                    <i class="bi bi-copy me-1"></i>Create Editable Copy
                                                </button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <button type="button" class="btn btn-outline-primary btn-sm" data-bs-toggle="modal" data-bs-target="#editRateModal">
                                                <i class="bi bi-pencil me-1"></i>Edit
                                            </button>
                                            <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-toggle="modal" data-bs-target="#addRateTableRowModal">
                                                <i class="bi bi-plus-lg me-1"></i>Add Item
                                            </button>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </div>

                    <%-- Locked Banner --%>
                    <c:if test="${isLocked}">
                        <div class="locked-banner">
                            <i class="bi bi-info-circle me-1"></i>
                            <strong>This rate is in use by one or more proposals</strong> and cannot be edited directly.
                            Click <strong>Create Editable Copy</strong> to make a new version with the same pricing and agency assignments.
                        </div>
                    </c:if>

                    <%-- Pricing Grid --%>
                    <div class="card">
                        <div class="card-header fw-bold">
                            <i class="bi bi-grid-3x3-gap me-1"></i>Pricing Grid
                        </div>
                        <div class="card-body p-2">
                            <c:choose>
                                <c:when test="${not empty rateTableList}">
                                    <c:set var="currentModule" value=""/>
                                    <c:forEach var="rt" items="${rateTableList}">
                                        <%-- Module header when module changes --%>
                                        <c:if test="${rt.getModule().getDescription() != currentModule}">
                                            <c:set var="currentModule" value="${rt.getModule().getDescription()}"/>
                                            <div class="module-header">
                                                <i class="bi bi-puzzle me-1"></i>${currentModule}
                                                <small class="opacity-75 ms-1">(${rt.getModule().getShortText()})</small>
                                            </div>
                                        </c:if>
                                        <div class="rate-row d-flex justify-content-between align-items-center px-3">
                                            <span>${rt.getPriceItem().getDescription()}</span>
                                            <div class="d-flex align-items-center gap-2">
                                                <span class="price-display">
                                                    <fmt:formatNumber value="${rt.getPrice()}" type="currency"/>
                                                </span>
                                                <c:if test="${!isLocked}">
                                                    <form method="post" action="RateTableAction" class="d-inline" onsubmit="return confirm('Delete this pricing row?');">
                                                        <input type="hidden" name="action" value="deleteRow"/>
                                                        <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                                                        <input type="hidden" name="priceItemId" value="${rt.getPriceItem().getId()}"/>
                                                        <input type="hidden" name="moduleId" value="${rt.getModule().getId()}"/>
                                                        <button type="submit" class="btn btn-sm btn-outline-danger" style="padding: 0.1rem 0.35rem; font-size: 0.75rem;">
                                                            <i class="bi bi-x-lg"></i>
                                                        </button>
                                                    </form>
                                                </c:if>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="empty-state">
                                        <i class="bi bi-grid-3x3-gap"></i>
                                        <p class="mb-1">No pricing rows for this rate</p>
                                        <c:if test="${!isLocked}">
                                            <button type="button" class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#addRateTableRowModal">
                                                <i class="bi bi-plus-lg me-1"></i>Add First Item
                                            </button>
                                        </c:if>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="card">
                        <div class="card-body empty-state">
                            <i class="bi bi-arrow-left-circle"></i>
                            <p class="mb-0 fs-5">Select a rate to view its pricing grid</p>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <%-- ======================== RIGHT COLUMN: Assigned Agencies ======================== --%>
        <div class="col-lg-3">
            <c:if test="${not empty selectedRate}">
                <div class="card">
                    <div class="card-header fw-bold" style="background-color: #6c757d; color: white;">
                        <i class="bi bi-people me-1"></i>Assigned Agencies
                    </div>
                    <div class="card-body">
                        <c:choose>
                            <c:when test="${not empty assignedAgencies}">
                                <c:forEach var="agency" items="${assignedAgencies}">
                                    <div class="d-flex justify-content-between align-items-center mb-2">
                                        <span class="agency-chip">
                                            <i class="bi bi-building me-1"></i>${agency.getName()}
                                        </span>
                                        <c:if test="${!isLocked}">
                                            <form method="post" action="RateTableAction" class="d-inline">
                                                <input type="hidden" name="action" value="removeAgencyFromRate"/>
                                                <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                                                <input type="hidden" name="agencyId" value="${agency.getId()}"/>
                                                <button type="submit" class="btn btn-sm btn-outline-danger" style="padding: 0.1rem 0.35rem; font-size: 0.75rem;"
                                                        onclick="return confirm('Remove ${agency.getName()} from this rate?');">
                                                    <i class="bi bi-x-lg"></i>
                                                </button>
                                            </form>
                                        </c:if>
                                    </div>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <div class="text-center text-muted py-3">
                                    <i class="bi bi-people" style="font-size: 1.5rem; display: block;"></i>
                                    <small>No agencies assigned</small>
                                </div>
                            </c:otherwise>
                        </c:choose>

                        <c:if test="${!isLocked}">
                            <hr>
                            <%-- Assign Agency Form --%>
                            <form method="post" action="RateTableAction">
                                <input type="hidden" name="action" value="assignAgencyToRate"/>
                                <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                                <label class="form-label fw-semibold small">Assign Agency</label>
                                <select name="agencyId" class="form-select form-select-sm mb-2">
                                    <option value="">-- Select --</option>
                                    <c:forEach var="agency" items="${agencyList}">
                                        <option value="${agency.getId()}">${agency.getName()}</option>
                                    </c:forEach>
                                </select>
                                <button type="submit" class="btn btn-primary btn-sm w-100">
                                    <i class="bi bi-plus-lg me-1"></i>Assign
                                </button>
                            </form>
                        </c:if>
                    </div>
                </div>

                <%-- Service Modules reference card --%>
                <div class="card mt-3">
                    <div class="card-header fw-bold" style="background-color: #8a6a2b; color: white;">
                        <i class="bi bi-puzzle me-1"></i>Service Modules
                    </div>
                    <div class="card-body p-0">
                        <c:forEach var="sm" items="${serviceModuleList}">
                            <div class="px-3 py-2" style="border-bottom: 1px solid #eee;">
                                <span class="fw-semibold">${sm.getDescription()}</span>
                                <br><small class="text-muted">${sm.getShortText()} · Sort: ${sm.getSortOrder()}</small>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </c:if>
        </div>

    </div><%-- end row --%>
</div>

<%-- ======================== MODALS ======================== --%>

<%-- Add New Rate Modal --%>
<div class="modal fade" id="addRateModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="RateTableAction">
                <input type="hidden" name="action" value="createRate"/>
                <div class="modal-header">
                    <h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>New Rate</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Rate Name</label>
                        <input type="text" name="description" class="form-control" required placeholder="e.g. Standard Rate">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Create Rate</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- Edit Rate Modal (only rendered for unlocked rates) --%>
<c:if test="${not empty selectedRate && !isLocked}">
<div class="modal fade" id="editRateModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="RateTableAction">
                <input type="hidden" name="action" value="editRate"/>
                <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                <div class="modal-header">
                    <h5 class="modal-title"><i class="bi bi-pencil me-2"></i>Edit Rate</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Rate Name</label>
                        <input type="text" name="description" class="form-control" required value="${selectedRate.getDescription()}">
                    </div>
                    <div class="form-check">
                        <input type="checkbox" class="form-check-input" name="suppressed" id="editSuppressed"
                               <c:if test="${selectedRate.isSuppressed()}">checked</c:if>>
                        <label class="form-check-label" for="editSuppressed">Suppressed (hidden from agents)</label>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Save Changes</button>
                </div>
            </form>
        </div>
    </div>
</div>
</c:if>

<%-- Add Rate Table Row Modal (only rendered for unlocked rates) --%>
<c:if test="${not empty selectedRate && !isLocked}">
<div class="modal fade" id="addRateTableRowModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="RateTableAction">
                <input type="hidden" name="action" value="addRateTableRow"/>
                <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                <div class="modal-header">
                    <h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>Add Pricing Row</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Service Module</label>
                        <select name="moduleId" class="form-select" required>
                            <option value="">-- Select Module --</option>
                            <c:forEach var="sm" items="${serviceModuleList}">
                                <option value="${sm.getId()}">${sm.getDescription()} (${sm.getShortText()})</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Fee Type</label>
                        <select name="priceItemId" class="form-select" required>
                            <option value="">-- Select Fee Type --</option>
                            <c:forEach var="pi" items="${priceItemList}">
                                <c:if test="${!pi.isSuppressed()}">
                                    <option value="${pi.getId()}">${pi.getDescription()}</option>
                                </c:if>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Price ($)</label>
                        <input type="number" name="price" class="form-control" step="0.01" min="0" required placeholder="0.00">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Add Row</button>
                </div>
            </form>
        </div>
    </div>
</div>
</c:if>

<%-- Add Price Item (Fee Type) Modal --%>
<div class="modal fade" id="addPriceItemModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="RateTableAction">
                <input type="hidden" name="action" value="createPriceItem"/>
                <div class="modal-header">
                    <h5 class="modal-title"><i class="bi bi-receipt me-2"></i>New Fee Type</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Description</label>
                        <input type="text" name="description" class="form-control" required placeholder="e.g. Monthly Administration Fee">
                    </div>
                    <small class="text-muted"><i class="bi bi-info-circle me-1"></i>New fee types are added at the end. Drag to reorder.</small>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary">Create Fee Type</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
    // ---- Fee Type Drag & Drop ----
    (function() {
        const list = document.getElementById('feeTypeList');
        if (!list) return;

        let dragItem = null;
        let dragPlaceholder = null;

        list.addEventListener('dragstart', function(e) {
            const item = e.target.closest('.fee-type-item');
            if (!item) return;
            dragItem = item;
            dragItem.style.opacity = '0.4';
            e.dataTransfer.effectAllowed = 'move';
        });

        list.addEventListener('dragend', function(e) {
            if (dragItem) dragItem.style.opacity = dragItem.dataset.suppressed === 'true' ? '0.5' : '1';
            if (dragPlaceholder && dragPlaceholder.parentNode) dragPlaceholder.parentNode.removeChild(dragPlaceholder);
            dragItem = null;
            dragPlaceholder = null;
            // Remove all drag-over styling
            list.querySelectorAll('.fee-type-item').forEach(el => el.style.borderTop = '');
        });

        list.addEventListener('dragover', function(e) {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            const target = e.target.closest('.fee-type-item');
            if (!target || target === dragItem) return;

            // Visual indicator
            list.querySelectorAll('.fee-type-item').forEach(el => el.style.borderTop = '');
            const rect = target.getBoundingClientRect();
            const midY = rect.top + rect.height / 2;
            if (e.clientY < midY) {
                target.style.borderTop = '2px solid #2B5F8A';
            } else {
                target.style.borderTop = '';
                const next = target.nextElementSibling;
                if (next) next.style.borderTop = '2px solid #2B5F8A';
            }
        });

        list.addEventListener('drop', function(e) {
            e.preventDefault();
            const target = e.target.closest('.fee-type-item');
            if (!target || !dragItem || target === dragItem) return;

            const rect = target.getBoundingClientRect();
            const midY = rect.top + rect.height / 2;
            if (e.clientY < midY) {
                list.insertBefore(dragItem, target);
            } else {
                list.insertBefore(dragItem, target.nextElementSibling);
            }

            // Save new order via AJAX
            const items = list.querySelectorAll('.fee-type-item');
            const ids = Array.from(items).map(el => el.dataset.piId);
            fetch('PriceItemAction', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: 'action=reorder&order=' + ids.join(',')
            }).then(r => r.json()).then(data => {
                if (data.status !== 'ok') console.error('Reorder failed:', data);
            }).catch(err => console.error('Reorder error:', err));
        });
    })();

    // ---- Show/Hide Suppressed Toggle ----
    let showingSuppressed = false;
    function toggleSuppressedView() {
        showingSuppressed = !showingSuppressed;
        const icon = document.getElementById('toggleSuppressedIcon');
        const btn = document.getElementById('toggleSuppressedBtn');
        const items = document.querySelectorAll('.fee-type-item');
        items.forEach(item => {
            if (item.dataset.isSuppressed === 'true') {
                if (showingSuppressed) {
                    item.classList.remove('suppressed-hidden');
                    item.classList.add('suppressed-visible');
                } else {
                    item.classList.add('suppressed-hidden');
                }
            }
        });
        if (showingSuppressed) {
            icon.className = 'bi bi-eye';
            btn.title = 'Hide suppressed fee types';
        } else {
            icon.className = 'bi bi-eye-slash';
            btn.title = 'Show suppressed fee types';
        }
    }

    // ---- Fee Type Suppress Toggle ----
    document.querySelectorAll('.fee-suppress-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const piId = this.dataset.piId;
            const currentlySuppressed = this.dataset.suppressed === 'true';
            const desc = this.closest('.fee-type-item').querySelector('.fee-desc').textContent;
            const verb = currentlySuppressed ? 'restore' : 'suppress';

            if (!confirm('Are you sure you want to ' + verb + ' "' + desc + '"?')) return;

            fetch('PriceItemAction', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: 'action=toggleSuppress&priceItemId=' + piId
            }).then(r => r.json()).then(data => {
                if (data.status === 'ok') {
                    const item = this.closest('.fee-type-item');
                    if (data.suppressed) {
                        item.dataset.isSuppressed = 'true';
                        item.classList.add('suppressed-visible');
                        if (!showingSuppressed) item.classList.add('suppressed-hidden');
                        this.className = 'btn btn-sm btn-outline-success fee-suppress-btn';
                        this.title = 'Click to restore';
                        this.innerHTML = '<i class="bi bi-eye"></i>';
                        this.dataset.suppressed = 'true';
                    } else {
                        item.dataset.isSuppressed = 'false';
                        item.classList.remove('suppressed-visible', 'suppressed-hidden');
                        this.className = 'btn btn-sm btn-outline-secondary fee-suppress-btn';
                        this.title = 'Click to suppress';
                        this.innerHTML = '<i class="bi bi-eye-slash"></i>';
                        this.dataset.suppressed = 'false';
                    }
                }
            }).catch(err => console.error('Suppress error:', err));
        });
    });
</script>
</body>
</html>
