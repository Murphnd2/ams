<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="modal fade" id="assignRemoveAgentModal" role="dialog" tabindex="-1" aria-labelledby="assignRemoveAgentModal" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" id="assignRemoveAgentModalLabel">Modify Agent Assignments</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:if test="${sessionScope.hasCurrentAgency==true}">
          <%-- ACTION TO ASSIGN AGENT TO SELECTED AGENCY --%>
          <div class="row mb-3">
            <div class="col">
              <div class="d-flex mb-1">
                <h5 class="p-1 mb-0">
                  <label >Assign Agent&nbsp;&nbsp;&nbsp;</label>
                  </h5>
              </div>
              <c:import url="/WEB-INF/view/psp/admin/forms/assignAgentToAgencyForm.jsp"></c:import>
            </div>
          </div>
          <div class="row mb-3">
            <div class="col">
              <div class="d-flex mb-1">
                <h5 class="p-1 mb-0 position-relative">
                  <label>Unlink Agent</label>
                </h5>
              </div>
              <c:import url="/WEB-INF/view/psp/admin/forms/removeAgentFromAgencyForm.jsp"></c:import>
            </div>
          </div>
        </c:if>
      </div>
    </div>
  </div>
</div>
