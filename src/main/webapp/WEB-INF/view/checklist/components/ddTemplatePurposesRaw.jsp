<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select" aria-label="recurring freq type drop down" name="serviceItemList" id="serviceItemList">
  <c:forEach var="serviceItem" items="${sessionScope.serviceItemList}">
    <option value="${serviceItem.getId()}">(${serviceItem.getTemplateGroup().getDescription()}) ${serviceItem.getDescription()}</option>
  </c:forEach>
</select>
