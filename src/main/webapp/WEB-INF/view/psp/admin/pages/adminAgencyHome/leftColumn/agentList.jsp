<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="col">
  <div class="row mb-3">
    <div class="col">
      <h5 class="fw-bolder">
        Agents
      </h5>
    </div>
    <div class="col-auto">
      <div class="btn-group">
        <button type="button" class="btn btn-outline-primary btn-sm dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
          Agent Actions
        </button>
        <ul class="dropdown-menu">
          <li><button type="button" class="dropdown-item" data-bs-toggle="modal" data-bs-target="#addAgentModal">Add New Agent</button></li>
          <li><button type="button" class="dropdown-item" data-bs-toggle="modal" data-bs-target="#assignRemoveAgentModal">Assign/Remove an Agent</button></li>
        </ul>
      </div>
    </div>
  </div>
  <c:import url="/WEB-INF/view/psp/admin/forms/agencyAgentListForm.jsp"></c:import>
</div>
