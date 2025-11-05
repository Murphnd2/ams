<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AgencyView">
      <c:forEach var="agency" items="${sessionScope.agencyList}">
        <div class="row mb-1">
          <div class="col">
            <div class="input-group input-group-sm">
              <c:choose>
                <c:when test="${agency.getId()==sessionScope.currentAgency.getId()}">
                  <button type="button" class="btn btn-danger" disabled name="agencySelectButton" id="btnAgency${agency.getId()}" value="${agency.getId()}">
                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                  </button>
                  <div class="form-control text-danger fw-bold">
                      ${agency.getName()}
                  </div>
                </c:when>
                <c:otherwise>
                  <button type="submit" class="btn btn-secondary" name="agencySelectButton" id="btnAgency${agency.getId()}" value="${agency.getId()}">
                    View
                  </button>
                  <div class="form-control">
                      ${agency.getName()}
                  </div>
                </c:otherwise>
              </c:choose>
            </div>
          </div>
        </div>
      </c:forEach>
</form>
