<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select" aria-label="recurring freq type drop down" name="ddTaskToAdd" id="ddTaskToAdd">
  <c:choose>
    <c:when test="${sessionScope.availableTasks.size()>0}">
      <c:forEach var="task" items="${sessionScope.availableTasks}">
        <option value="${task.getId()}">${task.getDescription()}</option>
      </c:forEach>
    </c:when>
    <c:otherwise>
      <option value="-1">** No Further Tasks Available **</option>
    </c:otherwise>
  </c:choose>
</select>
