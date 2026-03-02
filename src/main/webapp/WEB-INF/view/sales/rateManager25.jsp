<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Rate Manager</title>
    <style>
        .rate-card { cursor: pointer; transition: all 0.15s; }
        .rate-card:hover { background-color: #f0f4f8; }
        .rate-card.active { border-left: 4px solid var(--ssa); background-color: #e8eef4; }
        .module-header { background-color: var(--ssa-alt); color: white; font-weight: 600; padding: 0.4rem 0.75rem; border-radius: 4px; margin-top: 0.75rem; margin-bottom: 0.25rem; font-size: 0.9rem; }
        .module-header:first-child { margin-top: 0; }
        .rate-row { border-bottom: 1px solid #eee; padding: 0.35rem 0; }
        .rate-row:last-child { border-bottom: none; }
        .price-display { font-family: 'Segoe UI', monospace; font-weight: 600; }
        .price-editable { cursor: pointer; padding: 0.1rem 0.3rem; border-radius: 3px; transition: background 0.15s; }
        .price-editable:hover { background-color: #e7f1ff; }
        .price-input { width: 90px; font-family: 'Segoe UI', monospace; font-weight: 600; text-align: right; padding: 0.1rem 0.3rem; border: 1px solid var(--ssa); border-radius: 3px; font-size: inherit; }
        .agency-chip { display: inline-flex; align-items: center; background: #e9ecef; border-radius: 20px; padding: 0.2rem 0.6rem; margin: 0.15rem; font-size: 0.82rem; gap: 0.35rem; }
        .agency-chip .btn-remove { border: none; background: none; color: #dc3545; padding: 0; font-size: 0.7rem; cursor: pointer; line-height: 1; }
        .agency-chip .btn-remove:hover { color: #a71d2a; }
        .lock-icon { color: #dc3545; font-size: 0.8rem; }
        .locked-banner { background-color: #fff3cd; border: 1px solid #ffc107; border-radius: 6px; padding: 0.5rem 0.75rem; font-size: 0.85rem; }
        .locked-banner a.clone-link { color: #0d5681; font-weight: 600; text-decoration: underline; cursor: pointer; }
        .locked-banner a.clone-link:hover { color: #06357a; }
        .fee-type-item.suppressed-hidden { display: none !important; }
        .fee-type-item.suppressed-visible { opacity: 0.5; }
        .nav-tabs.ref-tabs { background-color: var(--ssa); border-bottom: none; padding: 0; border-radius: 6px 6px 0 0; }
        .nav-tabs.ref-tabs .nav-link { color: rgba(255,255,255,0.7); font-weight: 600; font-size: 1rem; border: none; padding: 0.5rem 0.75rem; border-radius: 0; }
        .nav-tabs.ref-tabs .nav-link:first-child { border-radius: 6px 0 0 0; }
        .nav-tabs.ref-tabs .nav-link.active { color: white; background-color: rgba(255,255,255,0.15); }
        .ref-scroll { max-height: 220px; overflow-y: auto; }
        .rate-scroll { max-height: 200px; overflow-y: auto; }
        .grid-scroll { max-height: calc(100vh - 260px); overflow-y: auto; }
        .tab-tools { margin-left: auto; display: flex; align-items: center; gap: 0.25rem; padding-right: 0.5rem; }
        .tab-tools .btn { color: rgba(255,255,255,0.7); padding: 0.1rem 0.4rem; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:set var="pageTitle" value="Rate Manager" scope="request"/>
    <c:set var="pageIcon" value="bi-cash-coin" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <div class="row g-3 mt-3">

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

            <%-- Agencies --%>
            <c:choose>
                <c:when test="${not empty selectedRate}">
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-people me-1"></i>Agencies</span>
                            <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#assignAgencyModal"><i class="bi bi-plus-lg"></i></button>
                        </div>
                        <div class="card-body py-2 px-3">
                            <div class="d-flex align-items-center flex-wrap gap-1">
                                <c:choose>
                                    <c:when test="${not empty assignedAgencies}">
                                        <c:forEach var="agency" items="${assignedAgencies}">
                                            <span class="agency-chip">
                                                ${agency.getName()}
                                                <form method="post" action="RateTableAction" class="d-inline" style="margin:0;">
                                                    <input type="hidden" name="action" value="removeAgencyFromRate"/>
                                                    <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                                                    <input type="hidden" name="agencyId" value="${agency.getId()}"/>
                                                    <button type="submit" class="btn-remove" onclick="return confirm('Remove ${agency.getName()}?');"><i class="bi bi-x-lg"></i></button>
                                                </form>
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

            <%-- Tabbed: Fee Types / Grid Sort --%>
            <div class="card">
                <ul class="nav nav-tabs ref-tabs" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link active" id="feeTab" data-bs-toggle="tab" data-bs-target="#feePanel" type="button" role="tab"><i class="bi bi-receipt me-1"></i>Fee Types</button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link" id="modTab" data-bs-toggle="tab" data-bs-target="#modPanel" type="button" role="tab"><i class="bi bi-sort-numeric-down me-1"></i>Grid Sort</button>
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
                    <%-- Grid Sort (modules in this rate — drag to reorder) --%>
                    <div class="tab-pane fade" id="modPanel" role="tabpanel">
                        <div class="ref-scroll">
                            <c:choose>
                                <c:when test="${not empty rateModuleList}">
                                    <div id="moduleList">
                                        <c:forEach var="sm" items="${rateModuleList}">
                                            <div class="mod-item d-flex align-items-center px-2 py-2"
                                                 data-sm-id="${sm.getId()}" draggable="true" style="border-bottom:1px solid #eee;cursor:grab;">
                                                <i class="bi bi-grip-vertical text-muted me-2" style="font-size:0.9rem;"></i>
                                                <c:choose>
                                                    <c:when test="${not empty sm.getLos()}">
                                                        <span class="fw-semibold">${sm.getLos().getDescription()}</span>
                                                        <small class="text-muted ms-2">(${sm.getLos().getShortText()})</small>
                                                    </c:when>
                                                    <c:when test="${not empty sm.getEnhancement()}">
                                                        <span class="fw-semibold">${sm.getEnhancement().getDescription()}</span>
                                                        <small class="text-muted ms-2">(${sm.getEnhancement().getShortText()})</small>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="fw-semibold">${sm.getDescription()}</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:when>
                                <c:otherwise><div class="text-center text-muted py-3"><small>
                                    <c:choose>
                                        <c:when test="${not empty selectedRate}">No pricing rows yet — add items to the grid first</c:when>
                                        <c:otherwise>Select a rate to manage grid sort order</c:otherwise>
                                    </c:choose>
                                </small></div></c:otherwise>
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
                        <div class="locked-banner mb-2"><i class="bi bi-lock-fill me-1"></i><strong>Locked</strong> — pricing cannot be changed.
                            <a href="#" class="clone-link" onclick="if(confirm('Create an editable copy with the same pricing and agency assignments? The current rate will be archived.')){document.getElementById('cloneForm').submit();}return false;">Create an editable copy</a> to make a new version with the same pricing and agency assignments.
                            <form id="cloneForm" method="post" action="RateTableAction" style="display:none;">
                                <input type="hidden" name="action" value="cloneRate"/>
                                <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                            </form>
                        </div>
                    </c:if>
                    <div class="card">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span>
                                <c:if test="${isLocked}"><i class="bi bi-lock-fill me-1"></i></c:if>
                                <i class="bi bi-grid-3x3-gap me-1"></i>${selectedRate.getDescription()} Pricing Grid
                            </span>
                            <div class="d-flex gap-1 align-items-center">
                                <c:if test="${!isLocked}">
                                    <a href="#" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#editRateModal" title="Edit rate name"><i class="bi bi-pencil"></i></a>
                                </c:if>
                                <form method="post" action="RateTableAction" class="d-inline" onsubmit="return confirm('Suppress this rate? It will be hidden from lists but won\'t affect existing proposals.');">
                                    <input type="hidden" name="action" value="suppressRate"/>
                                    <input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
                                    <button type="submit" class="btn btn-sm btn-outline-light" title="Suppress rate"><i class="bi bi-eye-slash"></i></button>
                                </form>
                                <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#copyRateModal" title="Make new rate from this one"><i class="bi bi-copy me-1"></i>Make New From</button>
                                <c:if test="${!isLocked}">
                                    <button type="button" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addRateTableRowModal"><i class="bi bi-plus-lg"></i></button>
                                </c:if>
                            </div>
                        </div>
                        <div class="grid-scroll p-2">
                            <c:choose>
                                <c:when test="${not empty rateTableList}">
                                    <c:set var="currentModule" value=""/>
                                    <c:forEach var="rt" items="${rateTableList}">
                                        <c:if test="${rt.getModule().getId() != currentModule}">
                                            <c:set var="currentModule" value="${rt.getModule().getId()}"/>
                                            <%-- Show LOS or Enhancement name as header --%>
                                            <div class="module-header">
                                                <c:choose>
                                                    <c:when test="${not empty rt.getModule().getLos()}">
                                                        <i class="bi bi-briefcase me-1"></i>${rt.getModule().getLos().getDescription()}
                                                        <small class="opacity-75 ms-1">(${rt.getModule().getLos().getShortText()})</small>
                                                    </c:when>
                                                    <c:when test="${not empty rt.getModule().getEnhancement()}">
                                                        <i class="bi bi-puzzle me-1"></i>${rt.getModule().getEnhancement().getDescription()}
                                                        <small class="opacity-75 ms-1">(${rt.getModule().getEnhancement().getShortText()})</small>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <i class="bi bi-puzzle me-1"></i>${rt.getModule().getDescription()}
                                                        <small class="opacity-75 ms-1">(${rt.getModule().getShortText()})</small>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </c:if>
                                        <div class="rate-row d-flex justify-content-between align-items-center px-3">
                                            <span>${rt.getPriceItem().getDescription()}</span>
                                            <div class="d-flex align-items-center gap-2">
                                                <c:choose>
                                                    <c:when test="${!isLocked}">
                                                        <span class="price-display price-editable"
                                                              data-rate-id="${selectedRate.getId()}"
                                                              data-module-id="${rt.getModule().getId()}"
                                                              data-price-item-id="${rt.getPriceItem().getId()}"
                                                              data-price="${rt.getPrice()}"
                                                              onclick="editPrice(this)"
                                                              title="Click to edit"><fmt:formatNumber value="${rt.getPrice()}" type="currency"/></span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="price-display"><fmt:formatNumber value="${rt.getPrice()}" type="currency"/></span>
                                                    </c:otherwise>
                                                </c:choose>
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

<%-- Add Rate --%>
<div class="modal fade" id="addRateModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="RateTableAction"><input type="hidden" name="action" value="createRate"/>
        <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>New Rate</h6><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button></div>
        <div class="modal-body"><div class="mb-3"><label class="form-label fw-semibold">Rate Name</label><input type="text" name="description" class="form-control" required placeholder="e.g. Standard Rate"></div></div>
        <div class="modal-footer justify-content-center border-0"><button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Create</button><span class="ssa-action-sep">|</span><button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button></div>
    </form>
</div></div></div>

<%-- Edit Rate --%>
<c:if test="${not empty selectedRate && !isLocked}">
<div class="modal fade" id="editRateModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="RateTableAction"><input type="hidden" name="action" value="editRate"/><input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
        <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-pencil me-2"></i>Edit Rate</h6><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button></div>
        <div class="modal-body">
            <div class="mb-3"><label class="form-label fw-semibold">Rate Name</label><input type="text" name="description" class="form-control" required value="${selectedRate.getDescription()}"></div>
            <div class="form-check"><input type="checkbox" class="form-check-input" name="suppressed" id="editSuppressed" <c:if test="${selectedRate.isSuppressed()}">checked</c:if>><label class="form-check-label" for="editSuppressed">Suppressed</label></div>
        </div>
        <div class="modal-footer justify-content-center border-0"><button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Save</button><span class="ssa-action-sep">|</span><button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button></div>
    </form>
</div></div></div>
</c:if>

<%-- Add Pricing Row — LOS/Enhancement + Fee Type + Price --%>
<c:if test="${not empty selectedRate && !isLocked}">
<div class="modal fade" id="addRateTableRowModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="RateTableAction" id="addRowForm"><input type="hidden" name="action" value="addRateTableRow"/><input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
        <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>Add Pricing Row</h6><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button></div>
        <div class="modal-body">
            <div class="mb-3">
                <label class="form-label fw-semibold">Line of Service or Enhancement</label>
                <select class="form-select" id="entitySelect" onchange="onEntitySelect()" required>
                    <option value="">-- Select --</option>
                    <optgroup label="Lines of Service">
                        <c:forEach var="los" items="${losList}">
                            <option value="los_${los.getId()}" data-type="los" data-id="${los.getId()}">${los.getDescription()} (${los.getShortText()})</option>
                        </c:forEach>
                    </optgroup>
                    <optgroup label="Enhancements">
                        <c:forEach var="enh" items="${enhancementList}">
                            <option value="enh_${enh.getId()}" data-type="enh" data-id="${enh.getId()}">${enh.getDescription()} (${enh.getShortText()})</option>
                        </c:forEach>
                    </optgroup>
                </select>
                <%-- Hidden inputs populated by JS based on selection --%>
                <input type="hidden" name="losId" id="hiddenLosId" value=""/>
                <input type="hidden" name="enhId" id="hiddenEnhId" value=""/>
            </div>
            <div class="mb-3">
                <label class="form-label fw-semibold">Fee Type</label>
                <select name="priceItemId" class="form-select" id="modalFeeSelect" required>
                    <option value="">-- Select service first --</option>
                </select>
            </div>
            <div class="mb-3"><label class="form-label fw-semibold">Price ($)</label><input type="number" name="price" class="form-control" step="0.01" min="0" required placeholder="0.00"></div>
        </div>
        <div class="modal-footer justify-content-center border-0"><button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Add Row</button><span class="ssa-action-sep">|</span><button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button></div>
    </form>
</div></div></div>
</c:if>

<%-- Assign Agency --%>
<c:if test="${not empty selectedRate}">
<div class="modal fade" id="assignAgencyModal" tabindex="-1"><div class="modal-dialog modal-sm"><div class="modal-content">
    <form method="post" action="RateTableAction"><input type="hidden" name="action" value="assignAgencyToRate"/><input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
        <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-briefcase me-1"></i>Assign Agency</h6><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button></div>
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
        <div class="modal-footer justify-content-center border-0 py-1"><button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Assign</button></div>
    </form>
</div></div></div>
</c:if>

<%-- Add Fee Type --%>
<div class="modal fade" id="addPriceItemModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="RateTableAction"><input type="hidden" name="action" value="createPriceItem"/>
        <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-receipt me-2"></i>New Fee Type</h6><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button></div>
        <div class="modal-body"><div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text" name="description" class="form-control" required placeholder="e.g. Monthly Administration Fee"></div><small class="text-muted"><i class="bi bi-info-circle me-1"></i>Added at end. Drag to reorder.</small></div>
        <div class="modal-footer justify-content-center border-0"><button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Create</button><span class="ssa-action-sep">|</span><button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button></div>
    </form>
</div></div></div>

<%-- Copy Rate (Make New From) --%>
<c:if test="${not empty selectedRate}">
<div class="modal fade" id="copyRateModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="RateTableAction" id="copyRateForm"><input type="hidden" name="action" value="copyRate"/><input type="hidden" name="rateId" value="${selectedRate.getId()}"/>
        <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-copy me-2"></i>Make New Rate From: ${selectedRate.getDescription()}</h6><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button></div>
        <div class="modal-body">
            <p class="text-muted small">Creates a new editable rate with the same pricing grid. The original rate is not affected.</p>
            <div class="mb-3">
                <label class="form-label fw-semibold">New Rate Name</label>
                <input type="text" name="description" id="copyRateName" class="form-control" required placeholder="Enter a unique name">
                <div class="invalid-feedback" id="copyRateError">This name is already in use.</div>
            </div>
        </div>
        <div class="modal-footer justify-content-center border-0"><button type="submit" class="ssa-action save" id="copyRateSubmit"><i class="bi bi-copy me-1"></i>Create Copy</button><span class="ssa-action-sep">|</span><button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button></div>
    </form>
</div></div></div>
</c:if>

<script>
    // ── Tab-aware controls ────────────────────────────────────────────
    const addRefBtn = document.getElementById('addRefBtn');
    const toggleBtn = document.getElementById('toggleSuppressedBtn');
    document.getElementById('feeTab').addEventListener('shown.bs.tab', () => { addRefBtn.style.display = ''; toggleBtn.style.display = ''; });
    document.getElementById('modTab').addEventListener('shown.bs.tab', () => { addRefBtn.style.display = 'none'; toggleBtn.style.display = 'none'; });

    // ── Suppress toggle (fee types only now) ──────────────────────────
    let showFees = false;
    function toggleSuppressed() {
        showFees = !showFees;
        document.getElementById('toggleSuppressedIcon').className = showFees ? 'bi bi-eye' : 'bi bi-eye-slash';
        document.querySelectorAll('.fee-type-item').forEach(i => {
            if (i.dataset.isSuppressed === 'true') {
                showFees ? i.classList.remove('suppressed-hidden') : i.classList.add('suppressed-hidden');
                if (showFees) i.classList.add('suppressed-visible');
            }
        });
    }

    // ── Fee type suppress AJAX ────────────────────────────────────────
    document.querySelectorAll('.fee-suppress-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const id = this.dataset.piId, sup = this.dataset.suppressed === 'true';
            const item = this.closest('.fee-type-item'), desc = item.querySelector('.fee-desc').textContent;
            if (!confirm((sup ? 'Restore' : 'Suppress') + ' "' + desc + '"?')) return;
            fetch('PriceItemAction', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body:'action=toggleSuppress&priceItemId=' + id})
                .then(r => r.json()).then(d => {
                    if (d.status === 'ok') {
                        if (d.suppressed) {
                            item.dataset.isSuppressed = 'true'; item.classList.add('suppressed-visible');
                            if (!showFees) item.classList.add('suppressed-hidden');
                            this.className = 'btn btn-sm btn-outline-success fee-suppress-btn'; this.title = 'Restore';
                            this.innerHTML = '<i class="bi bi-eye"></i>'; this.dataset.suppressed = 'true';
                        } else {
                            item.dataset.isSuppressed = 'false'; item.classList.remove('suppressed-visible', 'suppressed-hidden');
                            this.className = 'btn btn-sm btn-outline-secondary fee-suppress-btn'; this.title = 'Suppress';
                            this.innerHTML = '<i class="bi bi-eye-slash"></i>'; this.dataset.suppressed = 'false';
                        }
                    }
                });
        });
    });

    // ── Drag-and-drop sorting ─────────────────────────────────────────
    function initDrag(listId, cls, endpoint, attr, extraParams) {
        const list = document.getElementById(listId); if (!list) return; let drag = null;
        list.addEventListener('dragstart', e => { drag = e.target.closest('.' + cls); if (drag) { drag.style.opacity = '0.4'; e.dataTransfer.effectAllowed = 'move'; } });
        list.addEventListener('dragend', () => { if (drag) drag.style.opacity = ''; drag = null; list.querySelectorAll('.' + cls).forEach(el => el.style.borderTop = ''); });
        list.addEventListener('dragover', e => { e.preventDefault(); const t = e.target.closest('.' + cls); if (!t || t === drag) return; list.querySelectorAll('.' + cls).forEach(el => el.style.borderTop = ''); const r = t.getBoundingClientRect(); if (e.clientY < r.top + r.height / 2) t.style.borderTop = '2px solid var(--ssa)'; else { const n = t.nextElementSibling; if (n) n.style.borderTop = '2px solid var(--ssa)'; } });
        list.addEventListener('drop', e => { e.preventDefault(); const t = e.target.closest('.' + cls); if (!t || !drag || t === drag) return; const r = t.getBoundingClientRect(); (e.clientY < r.top + r.height / 2) ? list.insertBefore(drag, t) : list.insertBefore(drag, t.nextElementSibling); const ids = Array.from(list.querySelectorAll('.' + cls)).map(el => el.dataset[attr]); let body = 'order=' + ids.join(','); if (extraParams) { for (const [k,v] of Object.entries(extraParams)) body += '&' + k + '=' + v; } else { body = 'action=reorder&' + body; } fetch(endpoint, {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body}); });
    }
    initDrag('feeTypeList', 'fee-type-item', 'PriceItemAction', 'piId');
    <c:if test="${not empty selectedRate}">
    initDrag('moduleList', 'mod-item', 'RateTableAction', 'smId', {action:'reorderModules', rateId:'${selectedRate.getId()}'});
    </c:if>

    // ── Add Pricing Row — smart entity/fee selection ──────────────────
    // Server-provided data
    const usedFees = ${not empty usedFeesJson ? usedFeesJson : '{}'};
    const entityModuleMap = ${not empty entityModuleMapJson ? entityModuleMapJson : '{}'};

    // All non-suppressed fee types for building the filtered list
    const allFeeTypes = [
        <c:forEach var="pi" items="${priceItemList}" varStatus="st">
            <c:if test="${!pi.isSuppressed()}">{id:${pi.getId()}, desc:'${fn:replace(pi.getDescription(), "'", "\\'")}'}<c:if test="${!st.last}">,</c:if></c:if>
        </c:forEach>
    ].filter(Boolean);

    function onEntitySelect() {
        const sel = document.getElementById('entitySelect');
        const opt = sel.options[sel.selectedIndex];
        const losInput = document.getElementById('hiddenLosId');
        const enhInput = document.getElementById('hiddenEnhId');
        const feeSelect = document.getElementById('modalFeeSelect');

        // Clear hidden inputs
        losInput.value = '';
        enhInput.value = '';

        if (!opt || !opt.value) {
            feeSelect.innerHTML = '<option value="">-- Select service first --</option>';
            return;
        }

        // Set the appropriate hidden input
        const type = opt.dataset.type;
        const id = opt.dataset.id;
        if (type === 'los') losInput.value = id;
        else if (type === 'enh') enhInput.value = id;

        // Find the moduleId for this entity (if one exists)
        const key = opt.value; // e.g. "los_5" or "enh_1"
        const moduleId = entityModuleMap[key];

        // Get already-used fee type IDs for this module in this rate
        const usedIds = moduleId ? (usedFees[moduleId] || []) : [];

        // Rebuild fee type dropdown with only unused fee types
        feeSelect.innerHTML = '<option value="">-- Select fee type --</option>';
        allFeeTypes.forEach(ft => {
            if (!usedIds.includes(ft.id)) {
                const o = document.createElement('option');
                o.value = ft.id;
                o.textContent = ft.desc;
                feeSelect.appendChild(o);
            }
        });

        // If all fees used, show a message
        if (feeSelect.options.length === 1) {
            feeSelect.innerHTML = '<option value="">All fee types already assigned</option>';
        }
    }

    // Reset modal state when opened
    const addRowModal = document.getElementById('addRateTableRowModal');
    if (addRowModal) {
        addRowModal.addEventListener('show.bs.modal', () => {
            document.getElementById('entitySelect').value = '';
            document.getElementById('hiddenLosId').value = '';
            document.getElementById('hiddenEnhId').value = '';
            document.getElementById('modalFeeSelect').innerHTML = '<option value="">-- Select service first --</option>';
            const priceInput = addRowModal.querySelector('input[name="price"]');
            if (priceInput) priceInput.value = '';
        });
    }

    // ── Inline price editing ──────────────────────────────────────────
    function editPrice(el) {
        if (el.querySelector('input')) return; // already editing
        const currentPrice = parseFloat(el.dataset.price);
        const rateId = el.dataset.rateId;
        const moduleId = el.dataset.moduleId;
        const priceItemId = el.dataset.priceItemId;
        const originalText = el.innerHTML;

        const input = document.createElement('input');
        input.type = 'number';
        input.step = '0.01';
        input.min = '0';
        input.value = currentPrice;
        input.className = 'price-input';

        el.innerHTML = '';
        el.appendChild(input);
        input.focus();
        input.select();

        function save() {
            const newPrice = parseFloat(input.value);
            if (isNaN(newPrice) || newPrice < 0) {
                el.innerHTML = originalText;
                return;
            }
            if (newPrice === currentPrice) {
                el.innerHTML = originalText;
                return;
            }
            // Save via POST
            fetch('RateTableAction', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: 'action=updatePrice&rateId=' + rateId + '&moduleId=' + moduleId + '&priceItemId=' + priceItemId + '&price=' + newPrice
            }).then(r => {
                // Update display
                el.dataset.price = newPrice;
                el.innerHTML = '$' + newPrice.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2});
            }).catch(() => {
                el.innerHTML = originalText;
            });
        }

        input.addEventListener('blur', save);
        input.addEventListener('keydown', e => {
            if (e.key === 'Enter') { e.preventDefault(); input.blur(); }
            if (e.key === 'Escape') { el.innerHTML = originalText; }
        });
    }

    // ── Copy Rate name validation ─────────────────────────────────────
    const existingRateNames = [
        <c:forEach var="rate" items="${rateList}" varStatus="st">
            '${fn:replace(rate.getDescription(), "'", "\\'")}'<c:if test="${!st.last}">,</c:if>
        </c:forEach>
    ].map(n => n.toLowerCase());

    const copyNameInput = document.getElementById('copyRateName');
    const copySubmitBtn = document.getElementById('copyRateSubmit');
    if (copyNameInput) {
        copyNameInput.addEventListener('input', function() {
            const name = this.value.trim().toLowerCase();
            const taken = existingRateNames.includes(name);
            this.classList.toggle('is-invalid', taken);
            if (copySubmitBtn) copySubmitBtn.disabled = taken;
        });
    }
</script>
</body>
</html>
