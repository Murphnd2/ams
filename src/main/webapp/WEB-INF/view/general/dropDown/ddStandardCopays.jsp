<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select mb-3" aria-label="standardCopayList type drop down" name="standardCopayList" id="standardCopayList">
  <c:forEach var="standardCopayList" items="${sessionScope.standardCopayList}">
    <option value="${standardCopayList.getId()}">${standardCopayList.getDescription()}</option>
  </c:forEach>
</select>
