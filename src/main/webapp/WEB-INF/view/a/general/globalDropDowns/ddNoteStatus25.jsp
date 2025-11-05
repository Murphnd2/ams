<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select" aria-label="recurring freq type drop down" name="noteStatus" id="noteStatus">
  <c:forEach var="status" items="${applicationScope.global.getActivityStatuses()}">
    <c:choose>
      <c:when test="${status.getId()==2}">
        <option selected value="${status.getId()}">${status.getDescription()}</option></c:when>
      <c:otherwise>
        <option value="${status.getId()}">${status.getDescription()}</option>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</select>
