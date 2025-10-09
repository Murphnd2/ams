<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<input class="form-control" list="datalistOptions1" id="ticketSubCategoryList" autocomplete="off" name="ticketSubCategoryList" required placeholder="Type to search...">
<datalist id="datalistOptions1">
  <c:forEach var="cat" items="${sessionScope.ticketSubCategories}">
    <option value="${cat.getNoteCategory().getShortText()}-${cat.getDescription()} [-${cat.getId()}-]"></option>
  </c:forEach>
</datalist>
