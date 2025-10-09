<jsp:useBean id="toDo" scope="request" type=""/>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addLinkModal" role="dialog" tabindex="-1" aria-labelledby="addLinkModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Attach Link to Task</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <div class="row">
          <div class="row mb-3">
            <div class="col">
              <div class="input-group">
                <span class="input-group-text text-muted">URL:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
                <input type="url" class="form-control" name="linkPath" id="linkPath" placeholder="https://example.com" pattern="https://.*" >
              </div>
            </div>
          </div>
          <div class="row mb-3">
            <div class="col">
              <div class="input-group">
                <span class="input-group-text text-muted">Link Name</span>
                <input type="text" class="form-control" name="linkName" id="linkName" placeholder="Enter a name for your link" >
              </div>
            </div>
          </div>
          <div class="row">
            <div class="col">
              <button type="submit" class="btn btn-success w-100" name="btnToDo" value="L-${toDo.getId()}">Add</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
