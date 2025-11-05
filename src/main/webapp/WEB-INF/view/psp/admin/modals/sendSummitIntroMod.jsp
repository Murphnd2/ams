<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="sendSummitModal" role="dialog" tabindex="-1" aria-labelledby="sendSummitLabel" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="sendSummitLabel">Send Intro</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <div class="input-group mb-2">
          <span class="input-group-text">Contact Username</span>
          <input class="form-control" id="contactUserName" name="contactUserName">
        </div>
        <div class="input-group mb-2">
          <span class="input-group-text">Contact Password</span>
          <input class="form-control" id="contactPassword" name="contactPassword">
        </div>
        <input class="invisible" name="tId" value="4976">
        <button type="submit" id="S-btn${toDo.getId()}-${toDo.getId()}" name="btnToDo" value="${toDo.getId()}" class="btn btn-primary">Send</button>
      </div>
    </div>
  </div>
</div>
