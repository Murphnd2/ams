<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AddProposal">
  <div class="row">
    <div class="input-group mb-3">
      <span class="input-group-text">Rate</span>
      <c:import url="/WEB-INF/view/psp/admin/parts/ddAgencyRateList.jsp"></c:import>
    </div>
  </div>
  <div class="row">
        <c:forEach var="los" items="${sessionScope.losList}">
          <div class="col-12 mb-3">
            <input type="checkbox" class="btn-check w-100" id="chkLos${los.getId()}" name="chkLos${los.getId()}" autocomplete="off">
            <label class="btn btn-outline-secondary w-100" for="chkLos${los.getId()}">${los.getDescription()}</label>
          </div>
        </c:forEach>
  </div>
  <div class="row">
    <div class="col-12">
      <button type="submit" class="btn btn-primary w-100">Create Proposal</button>
    </div>
  </div>
</form>

