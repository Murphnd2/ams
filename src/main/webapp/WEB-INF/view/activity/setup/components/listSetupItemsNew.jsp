<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="accordion accordion-flush" id="benefitListAccordion">
  <div class="accordion-item">
    <div class="accordion-header" id="headingOne1">
      <div class="input-group">
        <div class="form-control bg-white text-secondary fw-bolder border border-secondary">
          Modules in Setup
        </div>
        <button type="button" class="btn btn-secondary " data-bs-toggle="collapse" data-bs-target="#collapseTwo1" aria-expanded="true" aria-controls="collapseOne">
          <i class="bi bi-journal-plus" ></i>&nbsp;&nbsp;Add Module
        </button>
      </div>
    </div>
    <div id="collapseTwo1" class="accordion-collapse collapse" aria-labelledby="headingOne1" data-bs-parent="#benefitListAccordion">
      <div class="accordion-body m-0 p-0 mb-1 pt-2">
        <c:import url="/WEB-INF/view/activity/setup/components/addModuleToSetupForm.jsp"></c:import>
      </div>
    </div>
  </div>
</div>
