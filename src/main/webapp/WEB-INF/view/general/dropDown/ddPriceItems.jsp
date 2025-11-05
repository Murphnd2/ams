<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select mb-3" aria-label="priceItemList type drop down" name="priceItemList" id="priceItemList">
  <c:forEach var="priceItemList" items="${sessionScope.priceItemList}">
    <option value="${priceItemList.getId()}">${priceItemList.getDescription()}</option>
  </c:forEach>
</select>
