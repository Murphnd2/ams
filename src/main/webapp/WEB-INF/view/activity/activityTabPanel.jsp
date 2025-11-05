<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row m-1">
  <div class="col">
    <div class="row">
      <div class="col-auto me-0 pe-0">
        <span class="text-ssa fw-bold fs-5">Primary Contact</span>
      </div>
      <div class="col ms-0 ps-0">
        <button class="btn btn-sm" name="showModConForm1" id="btnShowModConForm12" type="button" data-bs-target="#modContactModal1" data-bs-toggle="modal">
          [edit]
        </button>
      </div>
      <div class="col-auto"></div>
    </div>
    <c:import url="/WEB-INF/view/activity/modContactModal.jsp"></c:import>
    <div class="row">
      <div class="col-auto">
        <i class="bi bi-arrow-right"></i>
      </div>
      <div class="col">
        <span class="text-altSsa fw-bolder text-uppercase">${sessionScope.currentPrimaryContact.getFullName().toLowerCase()}</span>
      </div>
      <div class="col-auto">
        <c:choose>
          <c:when test="${sessionScope.currentPrimaryContact.getEmail()==null}">
            &nbsp;
          </c:when>
          <c:otherwise>
            <a class="text-altSsa text-decoration-none" href="ViewEmailHistory?em=${sessionScope.currentPrimaryContact.getEmail()}" target="_blank">
                ${sessionScope.currentPrimaryContact.getEmail().toLowerCase()}
            </a>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</div>
<div class="row">
  <div class="col">
    <c:choose>
      <c:when test="${sessionScope.adminView==1}">
        <c:import url="/WEB-INF/view/activity/setup/components/itemsInSetupFormNew.jsp"></c:import>
      </c:when>
      <c:when test="${sessionScope.adminView==2}">
        <c:import url="/WEB-INF/view/activity/renew/components/listRenewalItemsNew.jsp"></c:import>
      </c:when>
      <c:when test="${sessionScope.adminView==4}">
        <c:import url="/WEB-INF/view/activity/checklist/checklistDetailV1.jsp"></c:import>
      </c:when>
      <c:otherwise>
        <c:import url="/WEB-INF/view/activity/ticket/ticketDetailNew.jsp"></c:import>
      </c:otherwise>
    </c:choose>
  </div>
</div>
<c:import url="/WEB-INF/view/activity/note/addNoteToActivityNew.jsp"></c:import>

  <div class="row m-2">
  <c:if test="${sessionScope.adminView!=4}">
    <div class="col-auto m-0 p-0">
      <button type="button" class="btn btn-sm btn-outline-primary w-100" style="font-size: small" data-bs-toggle="modal" data-bs-target="#otherContactsModal">
        <i class="bi bi-people"></i> Other Contacts
      </button>
    </div>
    <div class="col m-0 ms-1 p-0">
      <button type="button" class="btn btn-sm btn-outline-primary w-100" style="font-size: small" data-bs-toggle="modal" data-bs-target="#docLinksModal">
        <i class="bi bi-files"></i> Docs<span class="d-none d-sm-inline d-md-none d-xl-inline">&nbsp;& Links</span>
      </button>
    </div>
    <div class="col m-0 ms-1 p-0">
      <button type="button" class="btn btn-sm btn-outline-primary w-100" style="font-size: small" data-bs-toggle="modal" data-bs-target="#viewPastActivity">
        <i class="bi bi-archive"></i> Past<span class="d-none d-sm-inline d-md-none d-xl-inline">&nbsp;${sessionScope.currentActivity.getClass().getSimpleName()}s</span>
      </button>
    </div>
  </c:if>
    <div class="col m-0 ms-1 p-0">
      <button type="button" class="btn btn-sm btn-outline-primary w-100" style="font-size: small" data-bs-toggle="modal" data-bs-target="#ownershipModal">
        <i class="bi bi-key"></i> Owner
      </button>
    </div>
  </div>

<c:import url="/WEB-INF/view/activity/modals/pastActivityModal.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/modals/ownershipModal.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/modals/otherContactsModal.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/setup/addSetupItemMod.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/renew/addRenewalItemMod.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/addDocumentToActivityMod.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/addUrlToActivityMod.jsp"></c:import>
