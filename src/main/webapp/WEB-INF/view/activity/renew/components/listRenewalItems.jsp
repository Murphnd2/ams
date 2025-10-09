<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="accordion accordion-flush d-none d-md-grid" id="contactListAccordion">
  <div class="accordion-item">
    <div class="accordion-header" id="headingOne">
      <div class="input-group">
        <div class="form-control bg-white text-secondary fw-bolder border border-secondary">
          Benefits In Renewal
        </div>
        <c:if test="${sessionScope.currentPerson.getId()==104}">
          <a href="AddHsaQuick" class="btn btn-warning">
            <i class="bi bi-hammer"></i>
          </a>
        </c:if>
        <button type="button" class="btn btn-secondary " data-bs-toggle="collapse" data-bs-target="#collapseTwo" aria-expanded="true" aria-controls="collapseOne">
          <i class="bi bi-journal-plus" ></i>&nbsp;&nbsp;Add Benefit
        </button>
      </div>
    </div>
    <div id="collapseTwo" class="accordion-collapse collapse" aria-labelledby="headingOne" data-bs-parent="#contactListAccordion">
      <div class="accordion-body m-0 p-0 mb-1 pt-2">
        <c:import url="/WEB-INF/view/activity/renew/components/ddBensNotInRenewalForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>
<c:import url="/WEB-INF/view/activity/renew/components/itemsInRenewalForm.jsp"></c:import>