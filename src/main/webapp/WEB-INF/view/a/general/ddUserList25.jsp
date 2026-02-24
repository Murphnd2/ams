<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select m-0" aria-label="user list" name="userList" id="userList">
  <c:choose>
    <c:when test="${sessionScope.isBpo || sessionScope.isBpoAdmin || sessionScope.isBpoUser}">
      <c:forEach var="user" items="${applicationScope.global.getBpoUsers()}">
        <c:choose>
          <c:when test="${user.getId().equals(sessionScope.local.getCurrentPerson().getId())}">
            <option selected value="${user.getId()}"><b>${user.getFullName().toUpperCase()}</b></option>
          </c:when>
          <c:otherwise>
            <option value="${user.getId()}">${user.getFullName().toLowerCase()}</option>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </c:when>
    <c:otherwise>
      <c:forEach var="user" items="${applicationScope.global.getUsers()}">
        <c:choose>
          <c:when test="${user.getId().equals(sessionScope.local.getCurrentPerson().getId())}">
            <option selected value="${user.getId()}"><b>${user.getFullName().toUpperCase()}</b></option>
          </c:when>
          <c:otherwise>
            <option value="${user.getId()}">${user.getFullName().toLowerCase()}</option>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </c:otherwise>
  </c:choose>
</select>
