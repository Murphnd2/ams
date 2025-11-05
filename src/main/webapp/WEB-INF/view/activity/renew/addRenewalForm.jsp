<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AddRenewal25">
    <div class="p-2 pt-0 border border-primary bg-light rounded w-100 mt-2 mb-2 fw-bold fs-3 align-items-center text-center">
        <div class="row m-0 p-0 align-items-center align-middle">
            <div class="col-auto m-0 p-0">
                <a class="btn btn-primary btn-sm pe-auto" href="ViewHome25">
                    <i class="bi bi-arrow-return-left"></i>
                </a>
            </div>
            <div class="col m-0 p-0 pt-1 fw-bold fs-3 text-primary text-truncate">
                ${sessionScope.currentEmployer.getEmployerName()}
            </div>
        </div>
    </div>
    <div class="row mb-1">
      <div class="col-9">
        <h5>Benefit Name</h5>
      </div>
      <div class="col-3">
        <h5>Due</h5>
      </div>
    </div>
    <div class="row mb-3">
    <c:forEach var="benefit" items="${sessionScope.benefitsForRenewalList}">
     <c:set var="checked" value=""></c:set>
      <c:set var="highlight" value=""></c:set>
         <c:if test="${benefit.flagBenefitForRenewal()}">
          <c:set var="checked" value="checked"></c:set>
          <c:set var="highlight" value="style=\"color:red; font-weight: bold;\""></c:set>
        </c:if>
          <div class="col-9">
            <div class="form-check">
              <input class="form-check-input" ${checked} type="checkbox" value="${benefit.getId()}" name="btnBen${benefit.getId()}" id="btnBen${benefit.getId()}">
              <label class="form-check-label" ${highlight} for="btnBen${benefit.getId()}">
                  (${benefit.getPlanType().getCode()}) ${benefit.getPlanDescription()}
              </label>
            </div>
          </div>
          <div class="col-3">
              ${benefit.getNextRenewalDue()}
          </div>
    </c:forEach>
    </div>
    <div class="row mb-3">
      <div class="col-12">
        <button type="submit" class="btn btn-primary">Start This Renewal For These Selected Benefits</button>
      </div>
    </div>
</form>
