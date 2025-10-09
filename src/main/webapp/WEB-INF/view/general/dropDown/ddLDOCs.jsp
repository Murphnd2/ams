<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select mb-3" aria-label="ldoc type drop down" name="ldocList" id="ldocList">
  <c:forEach var="ldocList" items="${sessionScope.ldocList}">
    <option value="${ldocList.getId()}">${ldocList.getDescription()}</option>
  </c:forEach>
</select>
