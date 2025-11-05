<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addSequenceModal" role="dialog" tabindex="-1" aria-labelledby="addSequenceModal" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="loginLabel">Create a Sequence of Tasks</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
          <c:import url="/WEB-INF/view/checklist/sequence/addSequenceForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>
