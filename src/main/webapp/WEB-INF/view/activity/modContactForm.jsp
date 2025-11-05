<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form action="ModifyContactForm" method="post">
  <input type="text" hidden name="pcId" value="${sessionScope.currentPrimaryContact.getId()}">
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Name</span>
        <input type="text" class="form-control" name="contactFirst" value="${sessionScope.currentPrimaryContact.getFirstName().toUpperCase()}">
        <input type="text" class="form-control" name="contactLast" value="${sessionScope.currentPrimaryContact.getLastName().toUpperCase()}">
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Email</span>
        <input type="email" class="form-control" name="contactEmail" value="${sessionScope.currentPrimaryContact.getEmail().toLowerCase()}">
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col"></div>
    <div class="col-auto">
      <button type="submit" value="1" name="btnModContact" id="btnModContact" class="btn btn-outline-secondary">
        <i class="bi bi-upload"></i> Update
      </button>
    </div>
  </div>
</form>
