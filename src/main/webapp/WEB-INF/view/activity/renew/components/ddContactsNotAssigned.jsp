<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select m-0" aria-label="recurring freq type drop down" name="addEmployeeList" id="addEmployeeList">
  <c:if test="${sessionScope.remainingEmployees.size()==0}">
    <option value="0">NO EMPLOYEES REMAIN TO ASSIGN</option>
  </c:if>
  <c:forEach var="employee" items="${sessionScope.remainingEmployees}">
    <option value="${employee.getId()}">${employee.getDropDownString()}</option>
  </c:forEach><%----%>
</select>
