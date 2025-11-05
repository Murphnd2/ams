<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="RemoveAgencyFromRate">
  <c:if test="${sessionScope.agenciesAssigned.size() < 1}">
    <div class="row mb-1">
      <div class="col">
        <div class="input-group input-group-sm">
          <div class="form-control">
            No Agencies Assigned to This Rate
          </div>
        </div>
      </div>
    </div>
  </c:if>
  <c:forEach var="agency" items="${sessionScope.agenciesAssigned}">
  <div class="row mb-1">
    <div class="col">
      <div class="input-group input-group-sm">
        <div class="form-control">
            ${agency.getName()}
        </div>
        <button type="submit" class="btn btn-secondary" name="btnRemoveAgency" id="btnRemAg${agency.getId()}" value="${agency.getId()}">
          Remove
        </button>
      </div>
    </div>
  </div>
  </c:forEach>
</form>
