<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="get" action="DeleteServiceItem">
  <c:forEach var="moduleItem" items="${sessionScope.currentModule.getServiceItemList()}">
    <div class="row mb-1">
      <div class="col">
        <div class="input-group input-group-sm">
          <c:if test="${sessionScope.hasCurrentModule == true}">
            <div class="form-control text-truncate">
                ${moduleItem.getBulletPoint()}
            </div>
            <button type="submit" class="btn btn-outline-secondary" name="itemSelectButton" id="btn1Mod${moduleItem.getId()}" value="${moduleItem.getId()}">
              Del
            </button>
          </c:if>
        </div>
      </div>
    </div>
  </c:forEach>
</form>
