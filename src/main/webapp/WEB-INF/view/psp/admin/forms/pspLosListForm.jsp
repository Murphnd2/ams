<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="LosView">
  <c:forEach var="los" items="${sessionScope.losList}">
    <div class="row mb-1">
      <div class="col">
        <div class="input-group input-group-sm">
          <c:choose>
            <c:when test="${los.getId()==sessionScope.currentLos.getId()&&sessionScope.pspAdminHomeSender==2}">
              <button type="submit" class="btn btn-danger" disabled name="losSelectButton" id="btnLos${los.getId()}" value="${los.getId()}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </button>
              <div class="form-control text-danger fw-bold">
                  ${los.getDescription()}
              </div>
            </c:when>
            <c:otherwise>
              <button type="submit" class="btn btn-secondary" name="losSelectButton" id="btnLos${los.getId()}" value="${los.getId()}">
                View
              </button>
              <div class="form-control text-truncate">
                  ${los.getDescription()}
              </div>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </div>
  </c:forEach>
</form>
