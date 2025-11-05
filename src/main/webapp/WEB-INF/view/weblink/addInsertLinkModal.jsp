<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addInsertLinkModal" role="dialog" tabindex="-1" aria-labelledby="addInsertLinkModal" aria-hidden="true">
  <div class="modal-dialog " role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel2">Create Insert Link</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/weblink/addInsertLinkForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>
