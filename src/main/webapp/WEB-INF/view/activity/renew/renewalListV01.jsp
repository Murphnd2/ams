<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="EmployerRenewalDetailView">
  <c:forEach var="re" items="${sessionScope.employerRenewalListAlt}">
    <c:set var="emphasis" value="text-muted fst-italic"> </c:set>
    <c:set var="vis1" value="d-none"> </c:set>
    <c:choose>
      <c:when test="${re.getStage()==0}">
        <c:set var="emphasis" value="text-danger fw-bold"> </c:set>
        <c:set var="vis1" value=""> </c:set>
      </c:when>
      <c:when test="${re.getStage()==1}">
        <c:set var="emphasis" value="fw-bold"> </c:set>
        <c:set var="vis1" value=""> </c:set>
      </c:when>
      <c:when test="${re.getStage()==2}">
        <c:set var="emphasis" value=""> </c:set>
        <c:set var="vis1" value=""> </c:set>
      </c:when>
    </c:choose>
    <div class="row">
      <div class="col-12 mt-1">
        <div class="input-group input-group-sm">
          <button type="submit" class="btn btn-primary" name="employerRenewalSelectButton" id="btnEmployerRenewal${re.getEmployer().getId()}" value="${re.getEmployer().getId()}">
            <i class="bi bi-repeat"></i>&nbsp;View
          </button>
          <div class="form-control pe-none ${emphasis}">
              ${re.getEmployer().getEmployerName()}
          </div>
          <a href="#" class="btn btn-secondary ${vis1}">
            <i class="bi bi-plus-circle-fill"></i> Start
          </a>
        </div>
      </div>
    </div>
  </c:forEach>
</form>
