<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select form-select-sm w-100 text-center" aria-label="recurring freq type drop down" name="userList" id="userList">
  <c:forEach var="user" items="${sessionScope.pspUserList}">
    <c:choose>
      <c:when test="${sessionScope.currentChecklist.getRecurringTaskList().getAssignee().getId()==user.getId()}">
        <option selected value="${user.getId()}">${user.getFullNameFirstLast()}</option>
      </c:when>
      <c:otherwise>
        <option value="${user.getId()}">${user.getFullNameFirstLast()}</option>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</select>
