<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select" aria-label="recurring freq type drop down" name="contactMethodList" id="contactMethodList" tabindex="-1">
  <c:forEach var="method" items="${sessionScope.contactMethods}">
    <c:choose>
      <c:when test="${method.getDescription().equals(\"Email\")}">
        <option value="${method.getId()}" selected>${method.getDescription()}</option>
      </c:when>
      <c:otherwise>
        <option value="${method.getId()}">${method.getDescription()}</option>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</select>
