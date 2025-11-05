<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select m-0" aria-label="recurring freq type drop down" name="contactList" id="contactList">
  <c:if test="${sessionScope.contactList.size()==0}">
    <option value="0" selected class="text-danger fw-bold"><b><i>NO EMPLOYEES ASSIGNED</i></b></option>
  </c:if>
  <c:forEach var="employee" items="${sessionScope.contactList}">
    <option value="${employee.getId()}">${employee.getDropDownString()}</option>
  </c:forEach>
</select>
