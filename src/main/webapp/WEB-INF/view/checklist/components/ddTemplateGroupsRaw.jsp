<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select mb-3" aria-label="recurring freq type drop down" name="activityCategoryList" id="activityCategoryList">
  <c:forEach var="activityCategory" items="${sessionScope.activityCategoryList}">
    <option value="${activityCategory.getId()}">${activityCategory.getDescription()}</option>
  </c:forEach>
</select>
