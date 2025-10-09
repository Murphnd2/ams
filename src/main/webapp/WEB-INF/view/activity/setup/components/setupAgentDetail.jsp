<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="row mt-2">
  <div class="col">
    <div class="row mb-1">
      <div class="col text-info fw-bold fs-5">
        <i class="bi bi-incognito"></i> Sales Agent
      </div>
    </div>
    <div class="row me-1">
      <div class="col-auto">
        <i class="bi bi-arrow-right"></i>
      </div>
      <div class="col text-info text-capitalize">
        ${sessionScope.setupAgent.getFirstName().toLowerCase()} ${sessionScope.setupAgent.getLastName().toLowerCase()}
      </div>
      <div class="col-auto">
        <a class="text-info text-decoration-none" href="ViewEmailHistory?em=${sessionScope.setupAgent.getEmail()}" target="_blank">
          ${sessionScope.setupAgent.getEmail().toLowerCase()}
        </a>
      </div>
      <div class="col-auto text-info p-0">
        <a class="btn btn-sm btn-outline-info p-0 border-0" href="${sessionScope.proposalLink}" target="_blank">
          <i class="bi bi-journal-bookmark"></i>
        </a>
      </div>
    </div>
  </div>
</div>




