<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select" aria-label="recurring freq type drop down" name="reasonList" id="noteStatus">
  <c:forEach var="reason" items="${applicationScope.global.getReasonsCreated()}">
    <c:choose>
      <c:when test="${reason.getId()==1}">
        <option selected value="${reason.getId()}">${reason.getDescription()}</option>
      </c:when>
      <c:when test="${reason.getId()==8}"><%-- STAYS EMPTY FOR QUICK ACTION --%>
      </c:when>
      <c:otherwise>
        <option value="${reason.getId()}">${reason.getDescription()}</option>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</select>
