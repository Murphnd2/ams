<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" data-bs-backdrop="static" data-bs-keyboard="false" id="addNewPriceItem" tabindex="-1" aria-labelledby="addNewPriceItem" aria-hidden="true">
  <div class="modal-dialog modal-fullscreen-sm-down modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="addNewPriceItemLabel">Add a New Price Item</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/psp/admin/forms/addPriceItemForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>
