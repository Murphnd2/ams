<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="modContactModal1" role="dialog" tabindex="-1" aria-labelledby="modContactModal1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Modify Contact</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <form action="modContactForm" method="post">
          <input type="text" hidden name="pcId" value="${sessionScope.local.getCurrentActivity().getPrimaryContact().getId()}">
          <div class="row mb-1">
            <div class="col">
              <div class="input-group">
                <span class="input-group-text">Name</span>
                <input type="text" class="form-control" name="contactFirst" value="${sessionScope.local.getCurrentActivity().getPrimaryContact().getFirstName().toUpperCase()}">
                <input type="text" class="form-control" name="contactLast" value="${sessionScope.local.getCurrentActivity().getPrimaryContact().getLastName().toUpperCase()}">
              </div>
            </div>
          </div>
          <div class="row mb-1">
            <div class="col">
              <div class="input-group">
                <span class="input-group-text">Email</span>
                <input type="email" class="form-control" name="contactEmail" value="${sessionScope.local.getCurrentActivity().getPrimaryContact().getEmail().toLowerCase()}">
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
      </div>
    </div>
  </div>
</div>
