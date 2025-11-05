<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="get" action="ModuleView">
  <c:forEach var="losModule" items="${sessionScope.currentLos.getServiceModuleList()}">
    <div class="row mb-1">
      <div class="col">
        <div class="input-group input-group-sm">
          <c:if test="${sessionScope.pspAdminHomeSender==2}">
                <c:choose>
                  <c:when test="${losModule.getId()==sessionScope.currentModule.getId()}">
                    <button type="submit" class="btn btn-warning disabled"  name="moduleSelectButton" id="btnMod${losModule.getId()}" value="0-${losModule.getId()}">
                      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                    </button>
                      <div class="form-control text-truncate fw-bold btn-outline-warning disabled">
                              ${losModule.getDescription()}
                      </div>
                    <button type="submit"  class="btn btn-outline-warning" name="moduleSelectButton" id="btn1Mod${losModule.getId()}" value="1-${losModule.getId()}">
                      Unassign
                    </button>
                  </c:when>
                  <c:otherwise>
                    <button type="submit" class="btn btn-dark" name="moduleSelectButton" id="btnMod${losModule.getId()}" value="0-${losModule.getId()}">
                      View
                    </button>
                      <div class="form-control text-truncate">
                              ${losModule.getDescription()}
                      </div>
                    <button type="submit"  class="btn btn-outline-dark" name="moduleSelectButton" id="btn1Mod${losModule.getId()}" value="1-${losModule.getId()}">
                      Unassign
                    </button>
                  </c:otherwise>
                </c:choose>
          </c:if>
        </div>
      </div>
    </div>
  </c:forEach>
</form>
