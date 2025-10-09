<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select form-select-sm text-center" aria-label="recurring freq type drop down" name="frequencyList" id="frequencyList">
  <c:forEach var="taskFrequency" items="${applicationScope.global.getTaskFrequencies()}">
    <c:choose>
      <c:when test="${sessionScope.local.getCurrentChecklist().getCheckList().getRecurringTaskList().getTaskFrequency().getId()==taskFrequency.getId()}">
        <option selected value="${taskFrequency.getId()}">${taskFrequency.getDescription()}</option>
      </c:when>
      <c:otherwise>
        <option value="${taskFrequency.getId()}">${taskFrequency.getDescription()}</option>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</select>