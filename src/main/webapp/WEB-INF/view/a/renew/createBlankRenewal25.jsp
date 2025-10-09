<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="CreateBlankRenewal25" class="mb-1">
  <div class="row">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Select Employer</span>
        <select class="form-select m-0" aria-label="recurring freq type drop down" name="employerId" id="employerId2">
          <c:if test="${applicationScope.global.getEmployers().size()==0}">
            <option value="0">NO EMPLOYERS EXIST</option>
          </c:if>
          <c:forEach var="employer" items="${applicationScope.global.getEmployers()}">
            <option value="${employer.getId()}">${employer.getEmployerName().toUpperCase().trim()}</option>
          </c:forEach><%----%>
        </select>
        <button type="submit" class="btn btn-secondary">
          <i class="bi bi-recycle"></i>&nbsp;Create Blank Renewal
        </button>
      </div>
    </div>
  </div>
</form>
