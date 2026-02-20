<%-- Admin navigation bar — include in all admin/sales pages --%>
<%-- Requires: Bootstrap 5 CSS/JS and Bootstrap Icons already loaded on the page --%>
<%-- Usage: <c:import url="/WEB-INF/view/sales/adminNav.jsp"/> --%>
<%-- Set 'adminCurrentPage' request attribute before import to highlight the active page --%>
<div class="d-flex gap-2 justify-content-end align-items-center">
    <div class="dropdown">
        <button class="btn btn-outline-ssa dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
            <i class="bi bi-gear me-1"></i>Admin
        </button>
        <ul class="dropdown-menu dropdown-menu-end">
            <li><a class="dropdown-item ${adminCurrentPage == 'serviceManager' ? 'active' : ''}" href="ServiceManagerHome"><i class="bi bi-diagram-3 me-2"></i>Service Manager</a></li>
            <li><a class="dropdown-item ${adminCurrentPage == 'rateManager' ? 'active' : ''}" href="PspAdminHome"><i class="bi bi-cash-coin me-2"></i>Rate Manager</a></li>
            <li><a class="dropdown-item ${adminCurrentPage == 'agencyManager' ? 'active' : ''}" href="PspAgencyHome"><i class="bi bi-people-fill me-2"></i>Agency Manager</a></li>
            <li><hr class="dropdown-divider"></li>
            <li><a class="dropdown-item ${adminCurrentPage == 'proposalBuilder' ? 'active' : ''}" href="ProposalBuilder"><i class="bi bi-file-earmark-plus me-2"></i>Proposal Builder</a></li>
            <li><a class="dropdown-item ${adminCurrentPage == 'applicationReview' ? 'active' : ''}" href="ReviewApplications"><i class="bi bi-clipboard-check me-2"></i>Application Review</a></li>
        </ul>
    </div>
    <a href="ViewHome25" class="btn btn-ssa"><i class="bi bi-house me-1"></i>Home</a>
</div>
