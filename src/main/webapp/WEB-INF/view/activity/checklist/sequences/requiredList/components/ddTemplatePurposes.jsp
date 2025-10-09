<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select" aria-label="recurring freq type drop down" name="templatePurposeList" id="templatePurposeList">
  <c:forEach var="templatePurpose" items="${sessionScope.templatePurposeList}">
    <option value="${templatePurpose.getId()}">(${templatePurpose.getTemplateGroup().getDescription()}) ${templatePurpose.getDescription()}</option>
  </c:forEach>
</select>
