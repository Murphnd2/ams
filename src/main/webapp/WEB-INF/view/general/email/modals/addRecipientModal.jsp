<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addRecipientModal" role="dialog" tabindex="-1" aria-labelledby="addRecipientModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Add Email Recipient</h5>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/general/email/forms/addRecipientForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>
