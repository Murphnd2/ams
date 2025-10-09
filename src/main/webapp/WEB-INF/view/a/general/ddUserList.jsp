<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select m-0" aria-label="recurring freq type drop down" name="userList" id="userList">
  <c:forEach var="user" items="${sessionScope.pspUserList}">
    <c:choose>
      <c:when test="${user.getId().equals(sessionScope.sVar.getCurrentPerson().getId())}">
        <option selected value="${user.getId()}">${user.getFullName()}</option>
      </c:when>
      <c:otherwise>
        <option value="${user.getId()}">${user.getFullName()}</option>
      </c:otherwise>
    </c:choose>

  </c:forEach>
</select>
