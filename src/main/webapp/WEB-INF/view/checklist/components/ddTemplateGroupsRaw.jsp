<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select mb-3" aria-label="recurring freq type drop down" name="templateGroupList" id="templateGroupList">
  <c:forEach var="templateGroup" items="${sessionScope.templateGroupList}">
    <option value="${templateGroup.getId()}">${templateGroup.getDescription()}</option>
  </c:forEach>
</select>
