<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select form-select-sm w-100 text-center" name="daysInAdvance" id="daysInAdvance">
  <c:forEach var="num" begin="0" end="15" step="1">
    <c:choose>
      <c:when test="${sessionScope.currentChecklist.getRecurringTaskList().getDaysInAdvance()==num}">
        <option selected value="${num}">${num}</option>
      </c:when>
      <c:otherwise>
        <option value="${num}">${num}</option>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</select>
