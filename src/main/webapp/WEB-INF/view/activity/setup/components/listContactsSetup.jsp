<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="accordion accordion-flush pb-2" id="contactListAccordion">
  <div class="accordion-item">
    <div class="accordion-header" id="headingOne">
      <div class="input-group">
        <div class="form-control bg-white text-primary fw-bold border border-primary">
          ${sessionScope.setupContact.getFirstName()} ${sessionScope.setupContact.getLastName()}
            &nbsp;
            <a class="text-secondary fw-light text-muted fst-italic" href="ViewEmailHistory?em=${sessionScope.setupContact.getEmail()}" target="_blank">
              ${sessionScope.setupContact.getEmail()}
            </a>
        </div>
        <button type="button" data-bs-toggle="collapse" data-bs-target="#collapseOne" aria-expanded="true" aria-controls="collapseOne" class="btn btn-primary d-none d-md-inline">
          <i class="bi bi-person-plus"></i> Add Additional
        </button>
      </div>
    </div>
    <div id="collapseOne" class="accordion-collapse collapse" aria-labelledby="headingOne" data-bs-parent="#contactListAccordion">
      <div class="accordion-body m-0 p-0 mb-1 pt-2">
        <form method="post" action="AddSetupContact">
          <div class="row mb-1">
            <div class="col">
              <div class="input-group input-group-sm">
                <span class="input-group-text">Name</span>
                <input type="text" required class="form-control" name="newFirst" placeholder="First Name">
                <input type="text" required class="form-control" name="newLast" placeholder="Last Name">
              </div>
            </div>
          </div>
          <div class="row mb-1">
            <div class="col">
              <div class="input-group input-group-sm">
                <span class="input-group-text">Email&nbsp;</span>
                <input type="email" required class="form-control" name="newEmail" placeholder="Enter email here">
                <button type="submit" class="btn btn-outline-success">
                  <i class="bi bi-person-plus"></i> Add
                </button>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>
<form method="post" action="RemoveSetupContact" class="mt-0 mb-2 form-control d-none d-md-grid" >
  <c:forEach var="contact" items="${sessionScope.setupContactList}">
    <div class="row mb-1">
      <div class="col-auto me-0 pe-0">
        <button type="submit" class="btn btn-danger border-white border-0 m-0 p-0 ps-2 pe-2" id="btn${contact.getId()}" name="setupContactList" value="${contact.getId()}">
          <i class="bi bi-person-dash" style="font-size: 1.4em"></i>
        </button>
      </div>
      <div class="col ms-1 ps-0">
        <div class="form-control form-control-sm pe-none border-0">
          <div class="row m-0 c-0 g-0">
            <div class="col m-0 p-0 g-0" style="font-size: 1rem">
                ${contact.getFirstName()} ${contact.getLastName()}
            </div>
            <div class="col-auto m-0 p-0 g-0 text-muted fst-italic">
              <a class="pe-auto" href="ViewEmailHistory?em=${contact.getEmail()}" target="_blank">${contact.getEmail()}</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </c:forEach>
</form>
