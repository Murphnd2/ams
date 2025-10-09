<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select mb-3" aria-label="contactMethodList type drop down" name="contactMethodList" id="contactMethodList">
  <c:forEach var="contactMethodList" items="${sessionScope.contactMethodList}">
    <option value="${contactMethodList.getId()}">${contactMethodList.getDescription()}</option>
  </c:forEach>
</select>
