<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:url var="homePage" value="/index.jsp"></c:url>
<c:url var="logo1" value="logo.png"></c:url>
<c:url var="logo2" value="logo02.png"></c:url>
<c:url var="logo3" value="logo03.png"></c:url>
<c:url var="logoA" value="/images/logoA.png"></c:url>
<nav class="navbar navbar-expand-lg navbar-light bg-light">
  <div class="container-fluid">
    <a class="navbar-brand" href="${homePage}">
      <c:choose>
        <c:when test="${sessionScope.local.isAuthenticated()==true}">
          <img src="${pageContext.request.contextPath}/images/logoD.png" class="img-fluid" alt="Administrator Logo"  style="height:65px">

        </c:when>
        <c:otherwise>
          &nbsp;
        </c:otherwise>
      </c:choose>
    </a>
    <div class="visually-hidden">
      <img src="${logo2}" />
    </div>
    <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
      <span class="navbar-toggler-icon"></span>
    </button>
    <div class="collapse navbar-collapse" id="navbarSupportedContent">
      <div class="col"></div>
      <div class="d-grid gap-2 d-lg-block">
        <c:if test="${sessionScope.isPspUser}">
          <a class="btn btn-outline-primary" href="ViewHome25">
            <i class="bi bi-house"></i> Home
          </a>
          <button class="btn btn-outline-secondary" type="button" data-bs-toggle="modal" data-bs-target="#createTicketModal">
            <i class="bi bi-telephone-inbound-fill"></i>&nbsp;&nbsp;Log
          </button>
          <a class="btn btn-outline-secondary" href="CreateEmail25">
            <i class="bi bi-send-fill"></i> Email
          </a>
        </c:if>
        <c:choose>
          <c:when test="${sessionScope.uninitialized!=1}">
            <a href="GoInitialize25" class="btn btn-outline-secondary">
              <i class="bi bi-lightning-charge-fill"></i> Initialize Database
            </a>
          </c:when>
          <c:when test="${sessionScope.local.isAuthenticated()!=true}">
            <button class="btn btn-outline-secondary" type="button" data-bs-toggle="modal" data-bs-target="#loginModal">
              <i class="bi bi-door-closed-fill"></i> Login
            </button>
          </c:when>
          <c:otherwise>
            <a href="LogOut" class="btn btn-outline-secondary">
              <i class="bi bi-door-open"></i> Logout
            </a>
          </c:otherwise>
        </c:choose>

        <c:if test="${sessionScope.currentPerson.getId()==125}">
          <a class="btn btn-outline-secondary" href="ResetBillingView">
            <i class="bi bi-currency-dollar"></i> Billing
          </a>
        </c:if>
        <c:if test="${sessionScope.isPspAdmin}">
          <button class="btn btn-outline-danger" type="button" data-bs-toggle="offcanvas" data-bs-target="#ocAdminMenu">
            Admin
          </button>
        </c:if>
      </div>
    </div>
  </div>
</nav>
<c:import url="/WEB-INF/view/a/navbar/createUserModal25.jsp"></c:import>
<c:import url="/WEB-INF/view/a/general/updatePspMod25.jsp"></c:import>
<c:import url="/WEB-INF/view/a/renew/createBlankRenewalMod25.jsp"></c:import>
<c:import url="/WEB-INF/view/a/general/addInsertLinkModal25.jsp"></c:import>
<c:import url="/WEB-INF/view/a/setup/generateSetupMod25.jsp"></c:import>
<c:import url="/WEB-INF/view/authentication/loginFormModal.jsp"></c:import>
<c:import url="/WEB-INF/view/general/admin/adminMenuOC.jsp"></c:import>
<c:import url="/WEB-INF/view/a/todo/addReminder25.jsp"></c:import>
<c:import url="/WEB-INF/view/a/todo/addChecklist25.jsp"></c:import>
<c:import url="/WEB-INF/view/a/renew/upcomingRenewalsModal25.jsp"></c:import>
<c:import url="/WEB-INF/view/a/navbar/createTicket25.jsp"></c:import>
<c:import url="/WEB-INF/view/a/checklistDetail/makeRecurringModal25.jsp"></c:import><%----%>
