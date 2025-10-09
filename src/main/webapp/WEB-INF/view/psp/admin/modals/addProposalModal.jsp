<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="addProposalModal" role="dialog" tabindex="-1" aria-labelledby="addProposalLabel" aria-hidden="true">
  <div class="modal-dialog modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="addProposalLabel">Select (Click) Items Below to Include</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:import url="/WEB-INF/view/agency/addProposalForm.jsp"></c:import>
      </div>
      <div class="modal-footer">
        <i>New Proposal for ${sessionScope.currentProspect.getName()}</i>
      </div>
    </div>
  </div>
</div>
