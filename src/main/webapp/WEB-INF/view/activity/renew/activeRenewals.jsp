<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ActiveRenewalView">
  <c:forEach var="renewal" items="${sessionScope.renewalList}">
    <div class="row mb-1 mt-0">
      <div class="col">
        <div class="input-group input-group-sm">
          <c:choose>
            <c:when test="${sessionScope.currentRenewal.getId()==renewal.getId()}">
              <button type="button" class="btn btn-danger" disabled name="renewalSelectButton" id="btnRenewal${renewal.getId()}" value="${renewal.getId()}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </button>
              <c:set var="currentStyle" value="form-control text-danger fw-bold"> </c:set>
            </c:when>
            <c:otherwise>
              <button type="submit" class="btn btn-secondary" name="renewalSelectButton" id="btnRenewal${renewal.getId()}" value="${renewal.getId()}">
                View
              </button>
              <c:set var="currentStyle" value="form-control"> </c:set>
            </c:otherwise>
          </c:choose>
          <div class="${currentStyle}">
              ${renewal.getFullName()}
          </div>
        </div>
      </div>
    </div>
  </c:forEach>
</form>
