<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="row m-2">
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
      <i class="bi bi-archive"></i> Past<span class="d-none d-sm-inline d-md-none d-xl-inline">&nbsp;${sessionScope.sVar.getCurrentActivity().getClass().getSimpleName()}s</span>
    </button>
  </div>
  <div class="col m-0 ms-1 p-0">
    <button type="button" class="btn btn-sm btn-outline-primary w-100" style="font-size: small" data-bs-toggle="modal" data-bs-target="#ownershipModal">
      <i class="bi bi-key"></i> Owner
    </button>
  </div>
</div>

<c:import url="/WEB-INF/view/activity/modals/pastActivityModal.jsp"></c:import>
<c:import url="/WEB-INF/view/a/general/activityToolbar/ownerAndDateChange/ownerModal.jsp"></c:import>
<c:import url="/WEB-INF/view/a/activityDetail/columns/modals/addContactToActivityMod.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/setup/addSetupItemMod.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/renew/addRenewalItemMod.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/addDocumentToActivityMod.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/addUrlToActivityMod.jsp"></c:import>
<c:import url="/WEB-INF/view/a/activityDetail/columns/modals/otherContactModal.jsp"></c:import>
