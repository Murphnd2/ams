<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addContactToActivity" role="dialog" tabindex="-1" aria-labelledby="addContactToActivity" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Add Contact</h5>
        <button type="button" class="btn-close" data-bs-toggle="modal" data-bs-target="#otherContactsModal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/activity/addContactToActivity.jsp"></c:import>
      </div>
    </div>
  </div>
</div>
