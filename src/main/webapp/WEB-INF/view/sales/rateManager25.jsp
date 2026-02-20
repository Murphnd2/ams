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
        :root { --ssa: #0d5681; --ssa-alt: #87a948; --ssa-gray: #5a6268; }
        body { background: #f8f9fa; }
        .btn-ssa { background: var(--ssa); border-color: var(--ssa); color: white; }
        .btn-ssa:hover { background: #06357a; border-color: #06357a; color: white; }
        .btn-outline-ssa { background: white; border-color: var(--ssa); color: var(--ssa); }
        .btn-outline-ssa:hover { background: var(--ssa); color: white; }
        .rate-card { cursor: pointer; transition: all 0.15s; }
        .rate-card:hover { background-color: #f0f4f8; }
        .rate-card.active { border-left: 4px solid var(--ssa); background-color: #e8eef4; }
        .module-header { background-color: var(--ssa-alt); color: white; font-weight: 600; padding: 0.4rem 0.75rem; border-radius: 4px; margin-top: 0.75rem; margin-bottom: 0.25rem; font-size: 0.9rem; }
        .rate-row { border-bottom: 1px solid #eee; padding: 0.35rem 0; }
        .rate-row:last-child { border-bottom: none; }
        .price-display { font-family: 'Segoe UI', monospace; font-weight: 600; }
        .agency-chip { display: inline-flex; align-items: center; background: #e9ecef; border-radius: 20px; padding: 0.2rem 0.6rem; margin: 0.15rem; font-size: 0.82rem; gap: 0.35rem; }
        .agency-chip .btn-remove { border: none; background: none; color: #dc3545; padding: 0; font-size: 0.7rem; cursor: pointer; line-height: 1; }
        .agency-chip .btn-remove:hover { color: #a71d2a; }
        .empty-state { text-align: center; color: #6c757d; padding: 3rem 1rem; }
        .empty-state i { font-size: 2.5rem; margin-bottom: 0.5rem; display: block; }
        .lock-icon { color: #dc3545; font-size: 0.8rem; }
        .locked-banner { background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 6px; padding: 0.5rem 0.75rem; font-size: 0.85rem; }
        .fee-type-item.suppressed-hidden, .mod-item.suppressed-hidden { display: none !important; }
        .fee-type-item.suppressed-visible, .mod-item.suppressed-visible { opacity: 0.5; }
        .hdr-bar { background-color: var(--ssa); color: white; padding: 0.5rem 0.75rem; font-weight: 600; font-size: 1rem; border-radius: 6px 6px 0 0; }
        .hdr-bar .btn-outline-light { padding: 0.15rem 0.5rem; }
        .nav-tabs.ref-tabs { background-color: var(--ssa); border-bottom: none; padding: 0 0 0 0; border-radius: 6px 6px 0 0; }
        .nav-tabs.ref-tabs .nav-link { color: rgba(255,255,255,0.7); font-weight: 600; font-size: 1rem; border: none; padding: 0.5rem 0.75rem; border-radius: 0; }
        .nav-tabs.ref-tabs .nav-link:first-child { border-radius: 6px 0 0 0; }
        .nav-tabs.ref-tabs .nav-link:hover { color: white; }
        .nav-tabs.ref-tabs .nav-link.active { color: var(--ssa); background: white; }
        .nav-tabs.ref-tabs .tab-tools { display: flex; align-items: center; gap: 4px; margin-left: auto; padding-right: 0.75rem; }
        .nav-tabs.ref-tabs .tab-tools .btn { color: rgba(255,255,255,0.8); border-color: rgba(255,255,255,0.5); padding: 0.15rem 0.5rem; font-size: 1rem; }
        .nav-tabs.ref-tabs .tab-tools .btn:hover { color: white; border-color: white; }
        .rate-scroll { max-height: 180px; overflow-y: auto; }
        .ref-scroll { overflow-y: auto; }
        .grid-scroll { overflow-y: auto; }
        @media (min-width: 992px) { .ref-scroll { max-height: calc(100vh - 480px); } .grid-scroll { max-height: calc(100vh - 180px); } }
    </style>
</head>
<body>
<div class="container-fluid py-3" style="max-width: 1400px;">

    <%-- ======================== TOP HEADER BAR ======================== --%>
    <div class="row align-items-center mb-3">
        <div class="col-lg-4">
            <h4 class="mb-0" style="font-size: 1.5rem; font-weight: 500;">
                <i class="bi bi-cash-coin me-2"></i>Rate Manager
            </h4>
        </div>
        <div class="col-lg-5">
            <c:if test="${not empty selectedRate}">
                <h4 class="mb-0" style="font-size: 1.5rem; font-weight: 500;">
                    <i class="bi bi-currency-dollar me-1"></i>${selectedRate.getDescription()}
                    <c:if test="${isLocked}"><i class="bi bi-lock-fill lock-icon ms-1"></i></c:if>
                    <c:if test="${!isLocked}">
                        <a href="#" data-bs-toggle="modal" data-bs-target="#editRateModal" style="color: inherit; font-size: 1rem; opacity: 0.6;" title="Edit rate name"><i class="bi bi-pencil"></i></a>
                    </c:if>
                </h4>
            </c:if>
        </div>
        <div class="col-lg-3 text-end">
            <div class="d-flex gap-2 justify-content-end align-items-center">
                <c:if test="${not empty selectedRate && isLocked}">
                    <form method="post" action="RateTableAction" class="d-inline"
                          onsubmit="return confirm('Create an editable copy with the same pricing and agency assignments? The current rate will be archived.');">
                        <input type="hidden" name="action" value="cloneRate"/>
                        <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                        <button type="submit" class="btn btn-warning"><i class="bi bi-copy me-1"></i>Create Editable Copy</button>
                    </form>
                    <div class="vr mx-1"></div>
                </c:if>
                <a href="PspAgencyHome" class="btn btn-outline-ssa"><i class="bi bi-people-fill me-1"></i>Agencies</a>
                <a href="ViewHome25" class="btn btn-ssa"><i class="bi bi-house me-1"></i>Home</a>
            </div>
        </div>
    </div>

    <div class="row g-3">

        <%-- ======================== LEFT COLUMN ======================== --%>
        <div class="col-lg-4">

            <%-- Rates List --%>
            <div class="card mb-3">
                <div class="hdr-bar d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-tags me-1"></i>Rates</span>
                    <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addRateModal"><i class="bi bi-plus-lg"></i></button>
                </div>
                <div class="rate-scroll">
                    <c:choose>
                        <c:when test="${not empty rateList}">
                            <c:forEach var="rate" items="${rateList}">
                                <a href="PspAdminHome?rateId=${rate.getId()}" class="d-block text-decoration-none text-dark">
                                    <div class="rate-card p-2 ps-3 ${selectedRate != null && selectedRate.getId() == rate.getId() ? 'active' : ''}">
                                        <div class="d-flex justify-content-between align-items-center">
                                            <span class="fw-semibold">${rate.getDescription()}</span>
                                            <c:if test="${lockedRateIds.contains(rate.getId())}"><i class="bi bi-lock-fill lock-icon" title="In use by proposals"></i></c:if>
                                        </div>
                                    </div>
                                </a>
                            </c:forEach>
                        </c:when>
                        <c:otherwise><div class="empty-state py-4"><i class="bi bi-tags"></i><p class="mb-0">No rates yet</p></div></c:otherwise>
                    </c:choose>
                </div>
            </div>

            <%-- Agencies (always visible when rate selected, otherwise placeholder) --%>
            <c:choose>
                <c:when test="${not empty selectedRate}">
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-people me-1"></i>Agencies</span>
                            <c:if test="${!isLocked}">
                                <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#assignAgencyModal"><i class="bi bi-plus-lg"></i></button>
                            </c:if>
                        </div>
                        <div class="card-body py-2 px-3">
                            <div class="d-flex align-items-center flex-wrap gap-1">
                                <c:choose>
                                    <c:when test="${not empty assignedAgencies}">
                                        <c:forEach var="agency" items="${assignedAgencies}">
                                            <span class="agency-chip">
                                                ${agency.getName()}
                                                <c:if test="${!isLocked}">
                                                    <form method="post" action="RateTableAction" class="d-inline" style="margin:0;">
                                                        <input type="hidden" name="action" value="removeAgencyFromRate"/>
                                                        <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                                                        <input type="hidden" name="agencyId" value="${agency.getId()}"/>
                                                        <button type="submit" class="btn-remove" onclick="return confirm('Remove ${agency.getName()}?');"><i class="bi bi-x-lg"></i></button>
                                                    </form>
                                                </c:if>
                                            </span>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise><span class="text-muted" style="font-size: 0.85rem;">None assigned</span></c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="card mb-3">
                        <div class="hdr-bar"><i class="bi bi-people me-1"></i>Agencies</div>
                        <div class="card-body py-2 text-center text-muted"><small>Select a rate to manage agencies</small></div>
                    </div>
                </c:otherwise>
            </c:choose>

            <%-- Tabbed: Fee Types / Service Modules --%>
            <div class="card">
                <ul class="nav nav-tabs ref-tabs" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link active" id="feeTab" data-bs-toggle="tab" data-bs-target="#feePanel" type="button" role="tab"><i class="bi bi-receipt me-1"></i>Fee Types</button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link" id="modTab" data-bs-toggle="tab" data-bs-target="#modPanel" type="button" role="tab"><i class="bi bi-puzzle me-1"></i>Modules</button>
                    </li>
                    <li class="tab-tools">
                        <button type="button" class="btn btn-sm" id="toggleSuppressedBtn" title="Show suppressed" onclick="toggleSuppressed()"><i class="bi bi-eye-slash" id="toggleSuppressedIcon"></i></button>
                        <button type="button" class="btn btn-sm" id="addRefBtn" data-bs-toggle="modal" data-bs-target="#addPriceItemModal"><i class="bi bi-plus-lg"></i></button>
                    </li>
                </ul>
                <div class="tab-content">
                    <%-- Fee Types --%>
                    <div class="tab-pane fade show active" id="feePanel" role="tabpanel">
                        <div class="ref-scroll">
                            <c:choose>
                                <c:when test="${not empty priceItemList}">
                                    <div id="feeTypeList">
                                        <c:forEach var="pi" items="${priceItemList}">
                                            <div class="fee-type-item d-flex justify-content-between align-items-center px-2 py-2 ${pi.isSuppressed() ? 'suppressed-hidden suppressed-visible' : ''}"
                                                 data-pi-id="${pi.getId()}" data-is-suppressed="${pi.isSuppressed()}" draggable="true" style="border-bottom:1px solid #eee;cursor:grab;">
                                                <div class="d-flex align-items-center gap-2">
                                                    <i class="bi bi-grip-vertical text-muted" style="font-size:0.9rem;"></i>
                                                    <span class="fw-semibold fee-desc">${pi.getDescription()}</span>
                                                </div>
                                                <c:choose>
                                                    <c:when test="${pi.isSuppressed()}"><button type="button" class="btn btn-sm btn-outline-success fee-suppress-btn" data-pi-id="${pi.getId()}" data-suppressed="true" style="padding:0.1rem 0.4rem;font-size:0.7rem;" title="Restore"><i class="bi bi-eye"></i></button></c:when>
                                                    <c:otherwise><button type="button" class="btn btn-sm btn-outline-secondary fee-suppress-btn" data-pi-id="${pi.getId()}" data-suppressed="false" style="padding:0.1rem 0.4rem;font-size:0.7rem;" title="Suppress"><i class="bi bi-eye-slash"></i></button></c:otherwise>
                                                </c:choose>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:when>
                                <c:otherwise><div class="text-center text-muted py-3"><small>No fee types defined</small></div></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <%-- Modules --%>
                    <div class="tab-pane fade" id="modPanel" role="tabpanel">
                        <div class="ref-scroll">
                            <c:choose>
                                <c:when test="${not empty serviceModuleList}">
                                    <div id="moduleList">
                                        <c:forEach var="sm" items="${serviceModuleList}">
                                            <div class="mod-item d-flex justify-content-between align-items-center px-2 py-2 ${sm.isSuppressed() ? 'suppressed-hidden suppressed-visible' : ''}"
                                                 data-sm-id="${sm.getId()}" data-is-suppressed="${sm.isSuppressed()}" draggable="true" style="border-bottom:1px solid #eee;cursor:grab;">
                                                <div class="d-flex align-items-center gap-2">
                                                    <i class="bi bi-grip-vertical text-muted" style="font-size:0.9rem;"></i>
                                                    <span class="fw-semibold mod-desc">${sm.getDescription()}</span>
                                                </div>
                                                <c:choose>
                                                    <c:when test="${sm.isSuppressed()}"><button type="button" class="btn btn-sm btn-outline-success mod-suppress-btn" data-sm-id="${sm.getId()}" data-suppressed="true" style="padding:0.1rem 0.4rem;font-size:0.7rem;" title="Restore"><i class="bi bi-eye"></i></button></c:when>
                                                    <c:otherwise><button type="button" class="btn btn-sm btn-outline-secondary mod-suppress-btn" data-sm-id="${sm.getId()}" data-suppressed="false" style="padding:0.1rem 0.4rem;font-size:0.7rem;" title="Suppress"><i class="bi bi-eye-slash"></i></button></c:otherwise>
                                                </c:choose>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:when>
                                <c:otherwise><div class="text-center text-muted py-3"><small>No service modules defined</small></div></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <%-- ======================== RIGHT COLUMN: Pricing Grid ======================== --%>
        <div class="col-lg-8">
            <c:choose>
                <c:when test="${not empty selectedRate}">
                    <c:if test="${isLocked}">
                        <div class="locked-banner mb-2"><i class="bi bi-info-circle me-1"></i><strong>Locked</strong> — in use by proposals. Click <strong>Create Editable Copy</strong> to make an editable version.</div>
                    </c:if>
                    <div class="card">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-grid-3x3-gap me-1"></i>Pricing Grid</span>
                            <c:if test="${!isLocked}">
                                <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addRateTableRowModal"><i class="bi bi-plus-lg"></i></button>
                            </c:if>
                        </div>
                        <div class="grid-scroll p-2">
                            <c:choose>
                                <c:when test="${not empty rateTableList}">
                                    <c:set var="currentModule" value=""/>
                                    <c:forEach var="rt" items="${rateTableList}">
                                        <c:if test="${rt.getModule().getDescription() != currentModule}">
                                            <c:set var="currentModule" value="${rt.getModule().getDescription()}"/>
                                            <div class="module-header"><i class="bi bi-puzzle me-1"></i>${currentModule} <small class="opacity-75 ms-1">(${rt.getModule().getShortText()})</small></div>
                                        </c:if>
                                        <div class="rate-row d-flex justify-content-between align-items-center px-3">
                                            <span>${rt.getPriceItem().getDescription()}</span>
                                            <div class="d-flex align-items-center gap-2">
                                                <span class="price-display"><fmt:formatNumber value="${rt.getPrice()}" type="currency"/></span>
                                                <c:if test="${!isLocked}">
                                                    <form method="post" action="RateTableAction" class="d-inline" onsubmit="return confirm('Delete this pricing row?');">
                                                        <input type="hidden" name="action" value="deleteRow"/>
                                                        <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                                                        <input type="hidden" name="priceItemId" value="${rt.getPriceItem().getId()}"/>
                                                        <input type="hidden" name="moduleId" value="${rt.getModule().getId()}"/>
                                                        <button type="submit" class="btn btn-sm btn-outline-danger" style="padding:0.1rem 0.35rem;font-size:0.75rem;"><i class="bi bi-x-lg"></i></button>
                                                    </form>
                                                </c:if>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="empty-state"><i class="bi bi-grid-3x3-gap"></i><p class="mb-1">No pricing rows</p>
                                        <c:if test="${!isLocked}"><button type="button" class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#addRateTableRowModal"><i class="bi bi-plus-lg me-1"></i>Add First Item</button></c:if>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="card"><div class="card-body empty-state"><i class="bi bi-arrow-left-circle"></i><p class="mb-0 fs-5">Select a rate to view its pricing grid</p></div></div>
                </c:otherwise>
            </c:choose>
        </div>

    </div>
</div>

<%-- ======================== MODALS ======================== --%>
<div class="modal fade" id="addRateModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="RateTableAction"><input type="hidden" name="action" value="createRate"/>
        <div class="modal-header"><h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>New Rate</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body"><div class="mb-3"><label class="form-label fw-semibold">Rate Name</label><input type="text" name="description" class="form-control" required placeholder="e.g. Standard Rate"></div></div>
        <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button><button type="submit" class="btn btn-primary">Create</button></div>
    </form>
</div></div></div>

<c:if test="${not empty selectedRate && !isLocked}">
<div class="modal fade" id="editRateModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="RateTableAction"><input type="hidden" name="action" value="editRate"/><input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
        <div class="modal-header"><h5 class="modal-title"><i class="bi bi-pencil me-2"></i>Edit Rate</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div class="mb-3"><label class="form-label fw-semibold">Rate Name</label><input type="text" name="description" class="form-control" required value="${selectedRate.getDescription()}"></div>
            <div class="form-check"><input type="checkbox" class="form-check-input" name="suppressed" id="editSuppressed" <c:if test="${selectedRate.isSuppressed()}">checked</c:if>><label class="form-check-label" for="editSuppressed">Suppressed</label></div>
        </div>
        <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button><button type="submit" class="btn btn-primary">Save</button></div>
    </form>
</div></div></div>

<div class="modal fade" id="addRateTableRowModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="RateTableAction"><input type="hidden" name="action" value="addRateTableRow"/><input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
        <div class="modal-header"><h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>Add Pricing Row</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div class="mb-3"><label class="form-label fw-semibold">Service Module</label><select name="moduleId" class="form-select" id="modalModuleSelect" required><option value="">-- Select --</option><c:forEach var="sm" items="${serviceModuleList}"><c:if test="${!sm.isSuppressed()}"><option value="${sm.getId()}">${sm.getDescription()} (${sm.getShortText()})</option></c:if></c:forEach></select></div>
            <div class="mb-3"><label class="form-label fw-semibold">Fee Type</label><select name="priceItemId" class="form-select" id="modalFeeSelect" required><option value="">-- Select --</option><c:forEach var="pi" items="${priceItemList}"><c:if test="${!pi.isSuppressed()}"><option value="${pi.getId()}">${pi.getDescription()}</option></c:if></c:forEach></select></div>
            <div class="mb-3"><label class="form-label fw-semibold">Price ($)</label><input type="number" name="price" class="form-control" step="0.01" min="0" required placeholder="0.00"></div>
        </div>
        <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button><button type="submit" class="btn btn-primary">Add Row</button></div>
    </form>
</div></div></div>

<div class="modal fade" id="assignAgencyModal" tabindex="-1"><div class="modal-dialog modal-sm"><div class="modal-content">
    <form method="post" action="RateTableAction"><input type="hidden" name="action" value="assignAgencyToRate"/><input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
        <div class="modal-header py-2"><h6 class="modal-title"><i class="bi bi-briefcase me-1"></i>Assign Agency</h6><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body py-2"><select name="agencyId" class="form-select form-select-sm" required><option value="">-- Select --</option>
            <c:forEach var="agency" items="${agencyList}">
                <c:set var="alreadyAssigned" value="false"/>
                <c:forEach var="assigned" items="${assignedAgencies}">
                    <c:if test="${assigned.getId() == agency.getId()}"><c:set var="alreadyAssigned" value="true"/></c:if>
                </c:forEach>
                <c:if test="${alreadyAssigned == 'false'}">
                    <option value="${agency.getId()}">${agency.getName()}</option>
                </c:if>
            </c:forEach>
        </select></div>
        <div class="modal-footer py-1"><button type="submit" class="btn btn-primary btn-sm">Assign</button></div>
    </form>
</div></div></div>
</c:if>

<div class="modal fade" id="addPriceItemModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="RateTableAction"><input type="hidden" name="action" value="createPriceItem"/>
        <div class="modal-header"><h5 class="modal-title"><i class="bi bi-receipt me-2"></i>New Fee Type</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body"><div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text" name="description" class="form-control" required placeholder="e.g. Monthly Administration Fee"></div><small class="text-muted"><i class="bi bi-info-circle me-1"></i>Added at end. Drag to reorder.</small></div>
        <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button><button type="submit" class="btn btn-primary">Create</button></div>
    </form>
</div></div></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const addRefBtn = document.getElementById('addRefBtn');
    document.getElementById('feeTab').addEventListener('shown.bs.tab', () => { addRefBtn.style.display = ''; });
    document.getElementById('modTab').addEventListener('shown.bs.tab', () => { addRefBtn.style.display = 'none'; });

    let showFees = false, showMods = false;
    function toggleSuppressed() {
        const feeActive = document.getElementById('feePanel').classList.contains('show');
        const icon = document.getElementById('toggleSuppressedIcon');
        if (feeActive) { showFees = !showFees; document.querySelectorAll('.fee-type-item').forEach(i => { if (i.dataset.isSuppressed==='true') { showFees ? i.classList.remove('suppressed-hidden') : i.classList.add('suppressed-hidden'); if(showFees) i.classList.add('suppressed-visible'); } }); icon.className = showFees ? 'bi bi-eye' : 'bi bi-eye-slash'; }
        else { showMods = !showMods; document.querySelectorAll('.mod-item').forEach(i => { if (i.dataset.isSuppressed==='true') { showMods ? i.classList.remove('suppressed-hidden') : i.classList.add('suppressed-hidden'); if(showMods) i.classList.add('suppressed-visible'); } }); icon.className = showMods ? 'bi bi-eye' : 'bi bi-eye-slash'; }
    }
    document.getElementById('feeTab').addEventListener('shown.bs.tab', () => { document.getElementById('toggleSuppressedIcon').className = showFees ? 'bi bi-eye' : 'bi bi-eye-slash'; });
    document.getElementById('modTab').addEventListener('shown.bs.tab', () => { document.getElementById('toggleSuppressedIcon').className = showMods ? 'bi bi-eye' : 'bi bi-eye-slash'; });

    function removeOpt(s,v){const o=document.getElementById(s)?.querySelector('option[value="'+v+'"]');if(o)o.remove();}
    function addOpt(s,v,l){const el=document.getElementById(s);if(!el||el.querySelector('option[value="'+v+'"]'))return;const o=document.createElement('option');o.value=v;o.textContent=l;el.appendChild(o);}

    function initDrag(listId,cls,endpoint,attr){
        const list=document.getElementById(listId);if(!list)return;let drag=null;
        list.addEventListener('dragstart',e=>{drag=e.target.closest('.'+cls);if(drag){drag.style.opacity='0.4';e.dataTransfer.effectAllowed='move';}});
        list.addEventListener('dragend',()=>{if(drag)drag.style.opacity='';drag=null;list.querySelectorAll('.'+cls).forEach(el=>el.style.borderTop='');});
        list.addEventListener('dragover',e=>{e.preventDefault();const t=e.target.closest('.'+cls);if(!t||t===drag)return;list.querySelectorAll('.'+cls).forEach(el=>el.style.borderTop='');const r=t.getBoundingClientRect();if(e.clientY<r.top+r.height/2)t.style.borderTop='2px solid var(--ssa)';else{const n=t.nextElementSibling;if(n)n.style.borderTop='2px solid var(--ssa)';}});
        list.addEventListener('drop',e=>{e.preventDefault();const t=e.target.closest('.'+cls);if(!t||!drag||t===drag)return;const r=t.getBoundingClientRect();(e.clientY<r.top+r.height/2)?list.insertBefore(drag,t):list.insertBefore(drag,t.nextElementSibling);const ids=Array.from(list.querySelectorAll('.'+cls)).map(el=>el.dataset[attr]);fetch(endpoint,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'action=reorder&order='+ids.join(',')});});
    }
    initDrag('feeTypeList','fee-type-item','PriceItemAction','piId');
    initDrag('moduleList','mod-item','ServiceModuleAction','smId');

    document.querySelectorAll('.fee-suppress-btn').forEach(btn=>{btn.addEventListener('click',function(){
        const id=this.dataset.piId,sup=this.dataset.suppressed==='true',item=this.closest('.fee-type-item'),desc=item.querySelector('.fee-desc').textContent;
        if(!confirm((sup?'Restore':'Suppress')+' "'+desc+'"?'))return;
        fetch('PriceItemAction',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'action=toggleSuppress&priceItemId='+id}).then(r=>r.json()).then(d=>{if(d.status==='ok'){
            if(d.suppressed){item.dataset.isSuppressed='true';item.classList.add('suppressed-visible');if(!showFees)item.classList.add('suppressed-hidden');this.className='btn btn-sm btn-outline-success fee-suppress-btn';this.title='Restore';this.innerHTML='<i class="bi bi-eye"></i>';this.dataset.suppressed='true';removeOpt('modalFeeSelect',id);}
            else{item.dataset.isSuppressed='false';item.classList.remove('suppressed-visible','suppressed-hidden');this.className='btn btn-sm btn-outline-secondary fee-suppress-btn';this.title='Suppress';this.innerHTML='<i class="bi bi-eye-slash"></i>';this.dataset.suppressed='false';addOpt('modalFeeSelect',id,desc);}
        }});
    });});

    document.querySelectorAll('.mod-suppress-btn').forEach(btn=>{btn.addEventListener('click',function(){
        const id=this.dataset.smId,sup=this.dataset.suppressed==='true',item=this.closest('.mod-item'),desc=item.querySelector('.mod-desc').textContent;
        if(!confirm((sup?'Restore':'Suppress')+' "'+desc+'"?'))return;
        fetch('ServiceModuleAction',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'action=toggleSuppress&moduleId='+id}).then(r=>r.json()).then(d=>{if(d.status==='ok'){
            if(d.suppressed){item.dataset.isSuppressed='true';item.classList.add('suppressed-visible');if(!showMods)item.classList.add('suppressed-hidden');this.className='btn btn-sm btn-outline-success mod-suppress-btn';this.title='Restore';this.innerHTML='<i class="bi bi-eye"></i>';this.dataset.suppressed='true';removeOpt('modalModuleSelect',id);}
            else{item.dataset.isSuppressed='false';item.classList.remove('suppressed-visible','suppressed-hidden');this.className='btn btn-sm btn-outline-secondary mod-suppress-btn';this.title='Suppress';this.innerHTML='<i class="bi bi-eye-slash"></i>';this.dataset.suppressed='false';addOpt('modalModuleSelect',id,desc);}
        }});
    });});
</script>
</body>
</html>
