<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  Unified Navbar — replaces old navbar25.jsp and adminNav.jsp

  Usage: Each page sets pageTitle / pageIcon before importing:
    <c:set var="pageTitle" value="Service Manager" scope="request"/>
    <c:set var="pageIcon" value="bi-diagram-3" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

  If pageTitle is not set, only the PSP icon shows on the left.
--%>
<style>
  .nav-ghost {
    color: rgba(255,255,255,0.8);
    background: none;
    border: none;
    padding: 0.35rem 0.7rem;
    font-size: 0.82rem;
    font-weight: 500;
    border-radius: 6px;
    transition: background 0.15s, color 0.15s;
    text-decoration: none;
    display: inline-flex;
    align-items: center;
    cursor: pointer;
    line-height: 1.3;
  }
  .nav-ghost:hover, .nav-ghost:focus {
    background: rgba(255,255,255,0.13);
    color: #fff;
    text-decoration: none;
  }
  .nav-ghost-warn {
    color: rgba(255,213,79,0.85);
  }
  .nav-ghost-warn:hover, .nav-ghost-warn:focus {
    background: rgba(255,213,79,0.13);
    color: #ffd54f;
  }
  .nav-ghost-logout {
    color: rgba(255,255,255,0.55);
  }
  .nav-ghost-logout:hover {
    background: rgba(255,255,255,0.1);
    color: rgba(255,255,255,0.9);
  }
  .nav-ghost-init {
    color: rgba(255,213,79,0.9);
    border: 1px solid rgba(255,213,79,0.4);
  }
  .nav-ghost-init:hover {
    background: rgba(255,213,79,0.15);
    color: #ffd54f;
    border-color: rgba(255,213,79,0.6);
  }
  /* Dropdown toggle caret spacing */
  .nav-ghost.dropdown-toggle::after { margin-left: 0.35rem; }
  /* Vertical divider between groups */
  .nav-divider {
    width: 1px;
    height: 1.1rem;
    background: rgba(255,255,255,0.2);
    align-self: center;
    margin: 0 0.15rem;
  }
  .navbar .dropdown-menu {
    font-size: 0.82rem;
    padding: 0.35rem 0;
  }
  .navbar .dropdown-item {
    padding: 0.3rem 0.85rem;
    font-size: 0.82rem;
  }
  .navbar .dropdown-divider {
    margin: 0.25rem 0;
  }
  .nav-ghost-unauth {
    color: #0d5681;
    background: none;
    border: 1px solid rgba(13,86,129,0.25);
    padding: 0.35rem 0.85rem;
    font-size: 0.85rem;
    font-weight: 500;
    border-radius: 6px;
    transition: background 0.15s, color 0.15s;
    text-decoration: none;
    display: inline-flex;
    align-items: center;
    cursor: pointer;
  }
  .nav-ghost-unauth:hover {
    background: rgba(13,86,129,0.08);
    color: #0d5681;
    text-decoration: none;
  }
</style>
<c:choose>
  <c:when test="${sessionScope.local.isAuthenticated() == true}">
<nav class="navbar navbar-expand-lg" style="background: #0d5681; padding: 0.55rem 0.75rem; border-radius: 0 0 8px 8px;">
  <div class="container-fluid">

    <%-- LEFT: PSP Icon + Page Title --%>
    <div class="d-flex align-items-center">
      <c:choose>
        <c:when test="${sessionScope.isBpo || sessionScope.isBpoAdmin || sessionScope.isBpoUser}">
          <a href="BpoHome" class="me-2">
            <img src="${pageContext.request.contextPath}${not empty applicationScope.global.logoNavbar ? applicationScope.global.logoNavbar : '/images/logoA.png'}" alt="Home" style="height:36px;">
          </a>
        </c:when>
        <c:when test="${sessionScope.isAgent || sessionScope.isAgencyAdmin}">
          <a href="AgentHome" class="me-2">
            <img src="${pageContext.request.contextPath}${not empty applicationScope.global.logoNavbar ? applicationScope.global.logoNavbar : '/images/logoA.png'}" alt="Home" style="height:36px;">
          </a>
        </c:when>
        <c:when test="${sessionScope.local.isAuthenticated() == true}">
          <a href="ViewHome25" class="me-2">
            <img src="${pageContext.request.contextPath}${not empty applicationScope.global.logoNavbar ? applicationScope.global.logoNavbar : '/images/logoA.png'}" alt="Home" style="height:36px;">
          </a>
        </c:when>
        <c:otherwise>
          <span class="me-2">&nbsp;</span>
        </c:otherwise>
      </c:choose>
      <span class="me-2 text-light">&nbsp;|</span>
      <c:if test="${not empty pageTitle}">
        <span class="text-white fw-semibold" style="font-size: 1.1rem; letter-spacing: 0.01em;">
          <c:if test="${not empty pageIcon}"><i class="bi ${pageIcon} me-1"></i></c:if>${pageTitle}
        </span>
      </c:if>
    </div>

    <%-- HAMBURGER (mobile) --%>
    <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav"
            aria-controls="mainNav" aria-expanded="false" aria-label="Toggle navigation">
      <i class="bi bi-list text-white" style="font-size:1.4rem;"></i>
    </button>

    <%-- RIGHT: Nav links --%>
    <div class="collapse navbar-collapse justify-content-end" id="mainNav">
      <div class="d-flex flex-column flex-lg-row gap-1 gap-lg-1 mt-2 mt-lg-0 align-items-lg-center">

        <%-- ═══ PSP USER / ADMIN LINKS ═══ --%>
        <c:if test="${sessionScope.isPspUser || sessionScope.isPspAdmin}">
          <a class="nav-ghost" href="ViewHome25">
            <i class="bi bi-house"></i><span class="d-lg-none d-xl-inline ms-1">Home</span>
          </a>
          <button class="nav-ghost" type="button" data-bs-toggle="modal" data-bs-target="#createTicketModal">
            <i class="bi bi-telephone-inbound-fill"></i><span class="d-lg-none d-xl-inline ms-1">Log</span>
          </button>
          <a class="nav-ghost" href="CreateEmail25">
            <i class="bi bi-send-fill"></i><span class="d-lg-none d-xl-inline ms-1">Email</span>
          </a>

          <%-- Sales Dropdown --%>
          <div class="dropdown">
            <button class="nav-ghost dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
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
          <div class="nav-divider d-none d-lg-block"></div>
          <div class="dropdown">
            <button class="nav-ghost nav-ghost-warn dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
              <i class="bi bi-gear"></i><span class="d-lg-none d-xl-inline ms-1">Admin</span>
            </button>
            <ul class="dropdown-menu dropdown-menu-end">
              <li><a class="dropdown-item" href="ServiceManagerHome"><i class="bi bi-diagram-3 me-2"></i>Service Manager</a></li>
              <li><a class="dropdown-item" href="PspAdminHome"><i class="bi bi-cash-coin me-2"></i>Rate Manager</a></li>
              <li><a class="dropdown-item" href="PspAgencyHome"><i class="bi bi-people-fill me-2"></i>Agency Manager</a></li>
              <li><a class="dropdown-item" href="LibraryHome"><i class="bi bi-collection me-2"></i>Resource Library</a></li>
              <li><hr class="dropdown-divider"></li>
              <li><a class="dropdown-item" href="SequenceBuilder25"><i class="bi bi-list-check me-2"></i>Sequence Builder</a></li>
              <li><a class="dropdown-item" href="ReviewTimeCorrections"><i class="bi bi-clock-history me-2"></i>Time Corrections</a></li>
              <li><hr class="dropdown-divider"></li>
              <li><button class="dropdown-item" type="button" data-bs-toggle="modal" data-bs-target="#createUserModal"><i class="bi bi-person-plus me-2"></i>Create User</button></li>
              <li><a class="dropdown-item" href="UploadPspBranding"><i class="bi bi-palette me-2"></i>Branding</a></li>
            </ul>
          </div>

          <%-- Billing (conditional) --%>
          <c:if test="${sessionScope.currentPerson.getId()==125}">
            <a class="nav-ghost" href="ResetBillingView">
              <i class="bi bi-currency-dollar"></i><span class="d-lg-none d-xl-inline ms-1">Billing</span>
            </a>
          </c:if>
        </c:if>

            <%-- ═══ AGENT / AGENCY MANAGER LINKS ═══ --%>
          <c:if test="${sessionScope.isAgent || sessionScope.isAgencyAdmin}">
            <a class="nav-ghost" href="AgentHome">
              <i class="bi bi-kanban"></i><span class="d-lg-none d-xl-inline ms-1">Pipeline</span>
            </a>
            <a class="nav-ghost" href="ProposalBuilder">
              <i class="bi bi-file-earmark-plus"></i><span class="d-lg-none d-xl-inline ms-1">New Proposal</span>
            </a>
            <c:if test="${sessionScope.isAgencyAdmin}">
              <button class="nav-ghost" type="button" data-bs-toggle="modal" data-bs-target="#createUserModal">
                <i class="bi bi-person-plus"></i><span class="d-lg-none d-xl-inline ms-1">Add Agent</span>
              </button>
            </c:if>
          </c:if>

            <%-- ═══ BPO USER LINKS ═══ --%>
          <c:if test="${sessionScope.isBpo || sessionScope.isBpoAdmin || sessionScope.isBpoUser}">
            <a class="nav-ghost" href="BpoHome">
              <i class="bi bi-house"></i><span class="d-lg-none d-xl-inline ms-1">Home</span>
            </a>
            <c:if test="${sessionScope.isBpoAdmin}">
              <div class="nav-divider d-none d-lg-block"></div>
              <div class="dropdown">
                <button class="nav-ghost nav-ghost-warn dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                  <i class="bi bi-gear"></i><span class="d-lg-none d-xl-inline ms-1">Admin</span>
                </button>
                <ul class="dropdown-menu dropdown-menu-end">
                  <li><button class="dropdown-item" type="button" data-bs-toggle="modal" data-bs-target="#createUserModal"><i class="bi bi-person-plus me-2"></i>Create User</button></li>
                  <li><a class="dropdown-item" href="#"><i class="bi bi-building me-2"></i>PSP Connections</a></li>
                </ul>
              </div>
            </c:if>
          </c:if>

        <%-- ═══ UNAUTHENTICATED / LOGOUT ═══ --%>
        <div class="nav-divider d-none d-lg-block"></div>
        <c:choose>
          <c:when test="${sessionScope.uninitialized != null && sessionScope.uninitialized != 1}">
            <a href="GoInitialize25" class="nav-ghost nav-ghost-init">
              <i class="bi bi-lightning-charge-fill"></i><span class="ms-1">Initialize</span>
            </a>
          </c:when>
          <c:when test="${sessionScope.local.isAuthenticated() != true}">
            <button class="nav-ghost" type="button" data-bs-toggle="modal" data-bs-target="#loginModal">
              <i class="bi bi-door-closed-fill"></i><span class="ms-1">Login</span>
            </button>
          </c:when>
          <c:otherwise>
            <a href="LogOut" class="nav-ghost nav-ghost-logout">
              <i class="bi bi-door-open"></i><span class="d-lg-none d-xl-inline ms-1">Logout</span>
            </a>
          </c:otherwise>
        </c:choose>

      </div>
    </div>
  </div>
</nav>
  </c:when>
  <c:otherwise>
    <%-- ═══ UNAUTHENTICATED — minimal bar ═══ --%>
    <nav class="navbar" style="background: transparent; padding: 0.55rem 0.75rem;">
      <div class="container-fluid">
        <div class="d-flex align-items-center">
          <img src="${pageContext.request.contextPath}/images/logoD.png" alt="Home" style="height:36px;" class="me-2">
        </div>
        <div class="d-flex align-items-center gap-1">
          <c:choose>
            <c:when test="${sessionScope.uninitialized != null && sessionScope.uninitialized != 1}">
              <a href="GoInitialize25" class="nav-ghost-unauth">
                <i class="bi bi-lightning-charge-fill"></i><span class="ms-1">Initialize</span>
              </a>
            </c:when>
            <c:otherwise>
              <button class="nav-ghost-unauth" type="button" data-bs-toggle="modal" data-bs-target="#loginModal">
                <i class="bi bi-door-closed-fill"></i><span class="ms-1">Login</span>
              </button>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </nav>
  </c:otherwise>
</c:choose>
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
