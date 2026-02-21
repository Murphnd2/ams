<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:set var="activity" value="${sessionScope.local.getCurrentActivity().getActivity()}" />
<c:set var="activityType" value="${activity.getClass().getSimpleName()}" />

<div class="row m-2">
  <c:choose>
    <c:when test="${activityType == 'CheckList'}">
      <div class="col m-0 ms-1 p-0">
        <button type="button" class="btn btn-sm btn-outline-primary w-100" data-bs-toggle="modal" data-bs-target="#docLinksModal">
          <i class="bi bi-files"></i> Docs<span class="d-none d-sm-inline d-md-none d-xl-inline">&nbsp;& Links</span>
        </button>
      </div>

      <c:if test="${activity.getRecurringTaskList() != null}">
        <div class="col m-0 ms-1 p-0">
          <button type="button" class="btn btn-sm btn-outline-primary w-100" data-bs-toggle="modal" data-bs-target="#viewPastActivity">
            <i class="bi bi-archive"></i> Past<span class="d-none d-sm-inline d-md-none d-xl-inline">&nbsp;${activityType}s</span>
          </button>
        </div>
      </c:if>

      <c:if test="${sessionScope.isPspUser || sessionScope.isPspAdmin}">
        <div class="col m-0 ms-1 p-0">
          <button type="button" class="btn btn-sm btn-outline-primary w-100" data-bs-toggle="modal" data-bs-target="#ownershipModal">
            <i class="bi bi-key"></i> Owner
          </button>
        </div>
      </c:if>
    </c:when>

    <c:otherwise>
      <div class="col-auto m-0 p-0">
        <button type="button" class="btn btn-sm btn-outline-primary w-100" data-bs-toggle="modal" data-bs-target="#otherContactsModal">
          <i class="bi bi-people"></i> Other Contacts
        </button>
      </div>

      <div class="col m-0 ms-1 p-0">
        <button type="button" class="btn btn-sm btn-outline-primary w-100" data-bs-toggle="modal" data-bs-target="#docLinksModal">
          <i class="bi bi-files"></i> Docs<span class="d-none d-sm-inline d-md-none d-xl-inline">&nbsp;& Links</span>
        </button>
      </div>

      <div class="col m-0 ms-1 p-0">
        <button type="button" class="btn btn-sm btn-outline-primary w-100" data-bs-toggle="modal" data-bs-target="#viewPastActivity">
          <i class="bi bi-archive"></i> Past<span class="d-none d-sm-inline d-md-none d-xl-inline">&nbsp;${activityType}s</span>
        </button>
      </div>

      <c:if test="${sessionScope.isPspUser || sessionScope.isPspAdmin}">
        <div class="col m-0 ms-1 p-0">
          <button type="button" class="btn btn-sm btn-outline-primary w-100" data-bs-toggle="modal" data-bs-target="#ownershipModal">
            <i class="bi bi-key"></i> Owner
          </button>
        </div>
      </c:if>
    </c:otherwise>
  </c:choose>
</div>

<!-- Imports -->
<c:import url="/WEB-INF/view/a/activityDetail/webLinkListModal25.jsp" />
<c:import url="/WEB-INF/view/a/activityDetail/columns/modals/pastActivityModal25.jsp" />
<c:import url="/WEB-INF/view/a/general/activityToolbar/ownerAndDateChange/ownerModal25.jsp" />
<c:import url="/WEB-INF/view/activity/setup/addSetupItemMod.jsp" />
<c:import url="/WEB-INF/view/a/activityDetail/columns/detail/modals/addRenewalItemMod25.jsp" />
<c:import url="/WEB-INF/view/activity/addDocumentToActivityMod.jsp" />
<c:import url="/WEB-INF/view/activity/addUrlToActivityMod.jsp" />
<c:import url="/WEB-INF/view/a/activityDetail/columns/modals/otherContact25.jsp" />

