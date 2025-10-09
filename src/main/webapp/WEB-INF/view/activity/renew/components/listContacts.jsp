<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="accordion accordion-flush d-none d-md-grid" id="contactListAccordion">
  <div class="accordion-item">
    <div class="accordion-header" id="headingOne">
      <div class="input-group">
        <div class="form-control bg-white text-primary fw-bold border border-primary">
          Contact List
        </div>
        <button type="button" data-bs-toggle="collapse" data-bs-target="#collapseOne" aria-expanded="true" aria-controls="collapseOne" class="btn btn-primary">
          <i class="bi bi-person-plus"></i> Add Contact
        </button>
      </div>
    </div>
    <div id="collapseOne" class="accordion-collapse collapse" aria-labelledby="headingOne" data-bs-parent="#contactListAccordion">
      <div class="accordion-body m-0 p-0 mb-1 pt-2">
        <c:import url="/WEB-INF/view/activity/renew/components/ddAssignContactForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>
<c:import url="/WEB-INF/view/activity/renew/components/updateContactModal.jsp"></c:import>
<form method="post" action="RemoveRenewalContact" class="mt-0 pt-2 mb-1 form-control form-control-sm" >
<c:forEach var="employee" items="${sessionScope.contactList}">
  <div class="row mb-1">
    <c:if test="${sessionScope.contactList.size()==1}">
      <div class="col-auto me-0 pe-0">
        <button type="button" class="btn btn-warning btn-sm" id="updateContactButton" name="updateContactButton" data-bs-target="#updateRenewalContact" data-bs-toggle="modal">
          <i class="bi bi-recycle"></i>
        </button>
      </div>
    </c:if>
    <div class="col-auto me-0 pe-0">
      <button type="submit" class="btn btn-danger btn-sm border-white border-0 m-0 p-0 ps-2 pe-2" id="btn${employee.getId()}" name="contactList" value="${employee.getId()}">
        <i class="bi bi-person-dash" style="font-size: 1.4em"></i>
      </button>
    </div>
    <div class="col ms-1 ps-0">
      <div class="form-control form-control-sm pe-none border-0">
        <div class="row m-0 c-0 g-0">
          <div class="col m-0 p-0 g-0" style="font-size: 1rem">
            ${employee.getFirstName()} ${employee.getLastName()}
          </div>
          <a class="col-auto m-0 p-0 g-0 text-muted fst-italic pe-auto" href="ViewEmailHistory?em=${employee.getEmail()}" target="_blank">
              ${employee.getEmail()}
          </a>
        </div>
      </div>
    </div>
  </div>
</c:forEach>
</form>
