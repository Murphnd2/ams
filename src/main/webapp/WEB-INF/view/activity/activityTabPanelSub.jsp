<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set  var="cActive" value=""></c:set>
<c:set var="cShow" value=""></c:set>
<c:if test="${sessionScope.adminView==1 || sessionScope.adminView==2}">
  <c:set var="cActive" value="active"></c:set>
  <c:set var="cShow" value="show"></c:set>
</c:if>
<c:set var="lastTab" value="${sessionScope.lastTab}"></c:set>
<c:choose>
  <c:when test="${lastTab==1}">
    <c:set var="tContacts" value="active"></c:set>
    <c:set var="tBenefits" value=""></c:set>
    <c:set var="tIssue" value=""></c:set>
    <c:set var="tDocs" value=""></c:set>
    <c:set var="tOwner" value=""></c:set>
    <c:set var="tNote" value=""></c:set>

    <c:set var="cContacts" value="show"></c:set>
    <c:set var="cBenefits" value=""></c:set>
    <c:set var="cIssue" value=""></c:set>
    <c:set var="cDocs" value=""></c:set>
    <c:set var="cOwner" value=""></c:set>
    <c:set var="cNote" value=""></c:set>
  </c:when>
  <c:when test="${lastTab==2}">
    <c:set var="tContacts" value=""></c:set>
    <c:set var="tBenefits" value="active"></c:set>
    <c:set var="tIssue" value="active"></c:set>
    <c:set var="tDocs" value=""></c:set>
    <c:set var="tOwner" value=""></c:set>
    <c:set var="tNote" value=""></c:set>
    <c:set var="cContacts" value=""></c:set>
    <c:set var="cBenefits" value="show"></c:set>
    <c:set var="cIssue" value="show"></c:set>
    <c:set var="cDocs" value=""></c:set>
    <c:set var="cOwner" value=""></c:set>
    <c:set var="cNote" value=""></c:set>
  </c:when>
  <c:when test="${lastTab==3}">
    <c:set var="tContacts" value=""></c:set>
    <c:set var="tBenefits" value=""></c:set>
    <c:set var="tIssue" value=""></c:set>
    <c:set var="tDocs" value="active"></c:set>
    <c:set var="tOwner" value=""></c:set>
    <c:set var="tNote" value=""></c:set>

    <c:set var="cContacts" value=""></c:set>
    <c:set var="cBenefits" value=""></c:set>
    <c:set var="cIssue" value=""></c:set>
    <c:set var="cDocs" value="show"></c:set>
    <c:set var="cOwner" value=""></c:set>
    <c:set var="cNote" value=""></c:set>
  </c:when>
  <c:when test="${lastTab==4}">
    <c:set var="tContacts" value=""></c:set>
    <c:set var="tBenefits" value=""></c:set>
    <c:set var="tIssue" value=""></c:set>
    <c:set var="tDocs" value=""></c:set>
    <c:set var="tOwner" value="active"></c:set>
    <c:set var="tNote" value=""></c:set>
    <c:set var="cContacts" value=""></c:set>
    <c:set var="cBenefits" value=""></c:set>
    <c:set var="cIssue" value=""></c:set>
    <c:set var="cDocs" value=""></c:set>
    <c:set var="cOwner" value="show"></c:set>
    <c:set var="cNote" value=""></c:set>
  </c:when>
  <c:when test="${lastTab==5}">
    <c:set var="tContacts" value=""></c:set>
    <c:set var="tBenefits" value=""></c:set>
    <c:set var="tIssue" value=""></c:set>
    <c:set var="tDocs" value=""></c:set>
    <c:set var="tOwner" value=""></c:set>
    <c:set var="tNote" value="active"></c:set>
    <c:set var="cContacts" value=""></c:set>
    <c:set var="cBenefits" value=""></c:set>
    <c:set var="cIssue" value=""></c:set>
    <c:set var="cDocs" value=""></c:set>
    <c:set var="cOwner" value=""></c:set>
    <c:set var="cNote" value="show"></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="tContacts" value=""></c:set>
    <c:set var="tBenefits" value="active"></c:set>
    <c:set var="tIssue" value="active"></c:set>
    <c:set var="tDocs" value=""></c:set>
    <c:set var="tOwner" value=""></c:set>
    <c:set var="tNote" value=""></c:set>
    <c:set var="cContacts" value=""></c:set>
    <c:set var="cBenefits" value="show"></c:set>
    <c:set var="cIssue" value="show"></c:set>
    <c:set var="cDocs" value=""></c:set>
    <c:set var="cOwner" value=""></c:set>
    <c:set var="cNote" value=""></c:set>
  </c:otherwise>
</c:choose>
<div class="row">
  <div class="col">
    <div class="row mb-1">
      <div class="col">
        <ul class="nav nav-pills mb-1">
          <li class="nav-item">
            <button class="nav-link ${tContacts}" id="contacts-tab" data-bs-toggle="tab" data-bs-target="#contacts-tab-pane" type="button" role="tab">
              Contacts
            </button>
          </li>
          <c:choose>
            <c:when test="${sessionScope.adminView==1 || sessionScope.adminView==2}">
              <li class="nav-item ${tBenefits}">
                <button class="nav-link" id="benefits-tab" data-bs-toggle="tab" data-bs-target="#benefits-tab-pane" type="button" role="tab">
                  Bens
                </button>
              </li>
            </c:when>
            <c:otherwise>
              <li class="nav-item ${tIssue}">
                <button class="nav-link" id="issues-tab" data-bs-toggle="tab" data-bs-target="#issues-tab-pane" type="button" role="tab">
                  Issue
                </button>
              </li>
            </c:otherwise>
          </c:choose>
          <li class="nav-item ${tDocs}">
            <button class="nav-link" id="documents-tab" data-bs-toggle="tab" data-bs-target="#documents-tab-pane" type="button" role="tab">
              Docs
            </button>
          </li>
          <li class="nav-item ${tOwner}">
            <button class="nav-link" id="owners-tab" data-bs-toggle="tab" data-bs-target="#owners-tab-pane" type="button" role="tab">
              Owner
            </button>
          </li>
          <li class="nav-item ${tNote}">
            <button class="nav-link" id="notes-tab" data-bs-toggle="tab" data-bs-target="#notes-tab-pane" type="button" role="tab">
              Note+
            </button>
          </li>
          <li class="nav-item ${tNote}">
            <button class="nav-link" id="past-tab" data-bs-toggle="tab" data-bs-target="#past-tab-pane" type="button" role="tab">
              Past
            </button>
          </li>
        </ul>
        <div class="tab-content" id="theTabContent">
          <div class="tab-pane fade ${cContacts} ${tContacts}" id="contacts-tab-pane" role="tabpanel" tabindex="0">
            <c:import url="/WEB-INF/view/activity/activityContactManager.jsp"></c:import>
            <c:if test="${sessionScope.adminView==1}">
              <c:import url="/WEB-INF/view/activity/setup/components/setupAgentDetail.jsp"></c:import>
            </c:if>
          </div>
          <c:choose>
            <c:when test="${sessionScope.adminView==1 || sessionScope.adminView==2}">
              <div class="tab-pane fade ${tBenefits} ${cBenefits}" id="benefits-tab-pane" role="tabpanel" tabindex="0">
                <c:choose>
                  <c:when test="${sessionScope.adminView==1}">
                    <c:import url="/WEB-INF/view/activity/setup/components/listSetupItems.jsp"></c:import>
                    <c:import url="/WEB-INF/view/activity/setup/components/itemsInSetupForm.jsp"></c:import>
                  </c:when>
                  <c:otherwise>
                    <c:import url="/WEB-INF/view/activity/renew/components/listRenewalItems.jsp"></c:import>
                  </c:otherwise>
                </c:choose>
              </div>
            </c:when>
            <c:otherwise>
              <div class="tab-pane fade ${tIssue} ${cIssue}" id="issues-tab-pane" role="tabpanel" tabindex="0">
                <c:import url="/WEB-INF/view/activity/ticket/ticketDetail.jsp"></c:import>
              </div>
            </c:otherwise>
          </c:choose>
          <div class="tab-pane fade ${tDocs} ${cDocs}" id="documents-tab-pane" role="tabpanel" tabindex="0">
            <c:import url="/WEB-INF/view/activity/activityWebLinkList.jsp"></c:import>
            <c:import url="/WEB-INF/view/activity/addDocumentToActivityMod.jsp"></c:import>
            <c:import url="/WEB-INF/view/activity/addUrlToActivityMod.jsp"></c:import>
          </div>
          <div class="tab-pane fade ${tOwner} ${cOwner}" id="owners-tab-pane" role="tabpanel" tabindex="0">
            <c:import url="/WEB-INF/view/activity/renew/changeOwnerForm.jsp"></c:import>
            <c:import url="/WEB-INF/view/activity/changeActivityDueDate.jsp"></c:import>
          </div>
          <div class="tab-pane fade ${tNote} ${cNote}" id="notes-tab-pane" role="tabpanel" tabindex="0">
            moved
          </div>
          <div class="tab-pane fade" id="past-tab-pane" role="tabpanel" tabindex="0">
            <c:import url="/WEB-INF/view/activity/viewPastActivities.jsp"></c:import>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>