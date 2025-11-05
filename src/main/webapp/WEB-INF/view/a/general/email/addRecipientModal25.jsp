<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="readOnly" value=""></c:set>
<c:set var="reqd" value=""></c:set>
<c:set var="visible" value="invisible"></c:set>
<c:if test="${sessionScope.personNotFound==true}">
  <c:set var="readOnly" value="readonly"></c:set>
  <c:set var="reqd" value="required"></c:set>
  <c:set var="visible" value=""></c:set>
</c:if>
<c:if test="${sessionScope.emailNotFound}==true">
  <c:set var="readOnly" value=""></c:set>
  <c:set var="reqd" value=""></c:set>
  <c:set var="visible" value="invisible"></c:set>
</c:if>
<div class="modal fade" id="addRecipientModal" role="dialog" tabindex="-1" aria-labelledby="addRecipientModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Add Email Recipient</h5>
      </div>
      <div class="modal-body">
        <div class="row mb-2">
          <div class="col">
            <input type="email" class="form-control" ${readOnly} name="emailName" id="emailName" value="${sessionScope.local.getCurrentEmail().getEmailToAdd()}">
          </div>
        </div>
        <div class="row ${visible} mb-2">
          <div class="col">
            <div class="input-group">
              <span class="input-group-text">Enter Name</span>
              <input class="form-control" type="text" name="firstName" ${reqd} placeholder="First">
              <input class="form-control" type="text" name="lastName" ${reqd} placeholder="Last">
            </div>
          </div>
        </div>
        <div class="row mb-2">
          <div class="col"></div>
          <div class="col-auto">
            <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Close</button>
          </div>
          <div class="col-auto">
            <button type="submit" class="btn btn-primary" name="action" value="AR">
              <i class="bi bi-plus"></i> Add Recipient
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
