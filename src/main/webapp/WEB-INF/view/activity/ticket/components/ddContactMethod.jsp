<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
  <c:forEach var="method" items="${sessionScope.contactMethods}">
  <div class="form-check form-check-inline">
    <c:choose>
      <c:when test="${method.getDescription().equals(\"Phone\")}">
        <input class="form-check-input" type="radio" name="contactMethodList" id="m${method.getId()}" value="${method.getId()}" checked>
        <label class="form-check-label" for="m${method.getId()}"><i class="bi bi-telephone-inbound"></i> ${method.getDescription()}</label>
      </c:when>
      <c:when test="${method.getDescription().equals(\"Email\")}">
        <input class="form-check-input" type="radio" name="contactMethodList" id="m${method.getId()}" value="${method.getId()}">
        <label class="form-check-label" for="m${method.getId()}"><i class="bi bi-envelope"></i> ${method.getDescription()}</label>
      </c:when>
      <c:when test="${method.getDescription().equals(\"Mail\")}">
        <input class="form-check-input" type="radio" name="contactMethodList" id="m${method.getId()}" value="${method.getId()}">
        <label class="form-check-label" for="m${method.getId()}"><i class="bi bi-mailbox"></i> ${method.getDescription()}</label>
      </c:when>
      <c:otherwise>
        <input class="form-check-input" type="radio" name="contactMethodList" id="m${method.getId()}" value="${method.getId()}">
        <label class="form-check-label" for="m${method.getId()}"><i class="bi bi-door-open"></i> ${method.getDescription()}</label>
      </c:otherwise>
    </c:choose>
  </div>
  </c:forEach>
