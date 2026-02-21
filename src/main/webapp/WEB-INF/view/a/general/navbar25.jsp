<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  Unified Navbar — replaces old navbar25.jsp and adminNav.jsp
  
  Usage: Each page sets pageTitle / pageIcon before importing:
    <c:set var="pageTitle" value="Service Manager" scope="request"/>
    <c:set var="pageIcon" value="bi-diagram-3" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
  
  If pageTitle is not set, only the PSP icon shows on the left.
--%>
<nav class="navbar navbar-expand-lg" style="background: #0d5681; padding: 0.35rem 0.75rem; margin-left: calc(-50vw + 50%); width: 100vw;">
  <div class="container-fluid">

    <%-- LEFT: PSP Icon + Page Title --%>
    <div class="d-flex align-items-center">
      <c:choose>
        <c:when test="${sessionScope.isAgent || sessionScope.isAgencyAdmin}">
          <a href="AgentHome" class="me-2">
            <img src="${pageContext.request.contextPath}/images/logoD.png" alt="Home" style="height:36px;">
          </a>
        </c:when>
        <c:when test="${sessionScope.local.isAuthenticated() == true}">
          <a href="ViewHome25" class="me-2">
            <img src="${pageContext.request.contextPath}/images/logoD.png" alt="Home" style="height:36px;">
          </a>
        </c:when>
        <c:otherwise>
          <span class="me-2">&nbsp;</span>
        </c:otherwise>
      </c:choose>
      <c:if test="${not empty pageTitle}">
        <span class="text-white fw-semibold" style="font-size: 1.15rem;">
          <c:if test="${not empty pageIcon}"><i class="bi ${pageIcon} me-1"></i></c:if>${pageTitle}
        </span>
      </c:if>
    </div>

    <%-- HAMBURGER (mobile) --%>
    <button class="navbar-toggler border-light" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav"
            aria-controls="mainNav" aria-expanded="false" aria-label="Toggle navigation">
      <i class="bi bi-list text-white" style="font-size:1.4rem;"></i>
    </button>

    <%-- RIGHT: Nav links --%>
    <div class="collapse navbar-collapse justify-content-end" id="mainNav">
      <div class="d-flex flex-column flex-lg-row gap-1 gap-lg-2 mt-2 mt-lg-0 align-items-lg-center">

        <%-- ═══ PSP USER / ADMIN LINKS ═══ --%>
        <c:if test="${sessionScope.isPspUser || sessionScope.isPspAdmin}">
          <a class="btn btn-sm btn-outline-light" href="ViewHome25">
            <i class="bi bi-house"></i><span class="d-lg-none d-xl-inline ms-1">Home</span>
          </a>
          <button class="btn btn-sm btn-outline-light" type="button" data-bs-toggle="modal" data-bs-target="#createTicketModal">
            <i class="bi bi-telephone-inbound-fill"></i><span class="d-lg-none d-xl-inline ms-1">Log</span>
          </button>
          <a class="btn btn-sm btn-outline-light" href="CreateEmail25">
            <i class="bi bi-send-fill"></i><span class="d-lg-none d-xl-inline ms-1">Email</span>
          </a>

          <%-- Sales Dropdown --%>
          <div class="dropdown">
            <button class="btn btn-sm btn-outline-light dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
              <i class="bi bi-briefcase"></i><span class="d-lg-none d-xl-inline ms-1">Sales</span>
            </button>
            <ul class="dropdown-menu dropdown-menu-end">
              <li><a class="dropdown-item" href="ProposalBuilder"><i class="bi bi-file-earmark-plus me-2"></i>Proposal Builder</a></li>
              <li><a class="dropdown-item" href="ReviewApplications"><i class="bi bi-clipboard-check me-2"></i>Application Review</a></li>
            </ul>
          </div>
        </c:if>

        <%-- ═══ PSP ADMIN ONLY — Admin Dropdown ═══ --%>
        <c:if test="${sessionScope.isPspAdmin}">
          <div class="dropdown">
            <button class="btn btn-sm btn-outline-warning dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
              <i class="bi bi-gear"></i><span class="d-lg-none d-xl-inline ms-1">Admin</span>
            </button>
            <ul class="dropdown-menu dropdown-menu-end">
              <li><a class="dropdown-item" href="ServiceManagerHome"><i class="bi bi-diagram-3 me-2"></i>Service Manager</a></li>
              <li><a class="dropdown-item" href="PspAdminHome"><i class="bi bi-cash-coin me-2"></i>Rate Manager</a></li>
              <li><a class="dropdown-item" href="PspAgencyHome"><i class="bi bi-people-fill me-2"></i>Agency Manager</a></li>
              <li><a class="dropdown-item" href="LibraryHome"><i class="bi bi-collection me-2"></i>Resource Library</a></li>
              <li><hr class="dropdown-divider"></li>
              <li><a class="dropdown-item" href="SequenceBuilder25"><i class="bi bi-list-check me-2"></i>Sequence Builder</a></li>
            </ul>
          </div>

          <%-- Billing (conditional) --%>
          <c:if test="${sessionScope.currentPerson.getId()==125}">
            <a class="btn btn-sm btn-outline-light" href="ResetBillingView">
              <i class="bi bi-currency-dollar"></i><span class="d-lg-none d-xl-inline ms-1">Billing</span>
            </a>
          </c:if>
        </c:if>

        <%-- ═══ AGENT / AGENCY MANAGER LINKS ═══ --%>
        <c:if test="${sessionScope.isAgent || sessionScope.isAgencyAdmin}">
          <a class="btn btn-sm btn-outline-light" href="AgentHome">
            <i class="bi bi-kanban"></i><span class="d-lg-none d-xl-inline ms-1">Pipeline</span>
          </a>
          <a class="btn btn-sm btn-outline-light" href="ProposalBuilder">
            <i class="bi bi-file-earmark-plus"></i><span class="d-lg-none d-xl-inline ms-1">New Proposal</span>
          </a>
        </c:if>

        <%-- ═══ UNAUTHENTICATED ═══ --%>
        <c:choose>
          <c:when test="${sessionScope.uninitialized != 1}">
            <a href="GoInitialize25" class="btn btn-sm btn-outline-warning">
              <i class="bi bi-lightning-charge-fill"></i><span class="ms-1">Initialize</span>
            </a>
          </c:when>
          <c:when test="${sessionScope.local.isAuthenticated() != true}">
            <button class="btn btn-sm btn-outline-light" type="button" data-bs-toggle="modal" data-bs-target="#loginModal">
              <i class="bi bi-door-closed-fill"></i><span class="ms-1">Login</span>
            </button>
          </c:when>
          <c:otherwise>
            <a href="LogOut" class="btn btn-sm btn-light">
              <i class="bi bi-door-open"></i><span class="d-lg-none d-xl-inline ms-1">Logout</span>
            </a>
          </c:otherwise>
        </c:choose>

      </div>
    </div>
  </div>
</nav>

<%-- ═══ MODAL IMPORTS ═══ --%>
<c:import url="/WEB-INF/view/a/navbar/createUserModal25.jsp"/>
<c:import url="/WEB-INF/view/a/general/updatePspMod25.jsp"/>
<c:import url="/WEB-INF/view/a/renew/createBlankRenewalMod25.jsp"/>
<c:import url="/WEB-INF/view/a/general/addInsertLinkModal25.jsp"/>
<c:import url="/WEB-INF/view/a/setup/generateSetupMod25.jsp"/>
<c:import url="/WEB-INF/view/authentication/loginFormModal.jsp"/>
<c:import url="/WEB-INF/view/general/admin/adminMenuOC.jsp"/>
<c:import url="/WEB-INF/view/a/todo/addReminder25.jsp"/>
<c:import url="/WEB-INF/view/a/todo/addChecklist25.jsp"/>
<c:import url="/WEB-INF/view/a/renew/upcomingRenewalsModal25.jsp"/>
<c:import url="/WEB-INF/view/a/navbar/createTicket25.jsp"/>
<c:import url="/WEB-INF/view/a/checklistDetail/makeRecurringModal25.jsp"/>

<%-- Chatbot — PSP users only --%>
<c:if test="${sessionScope.isPspUser || sessionScope.isPspAdmin}">
  <c:import url="/WEB-INF/view/a/general/chatAssistant25.jsp"/>
</c:if>
