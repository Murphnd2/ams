<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select mb-3" aria-label="preTaxOptionList drop down" name="preTaxOptionList" id="preTaxOptionList">
  <c:forEach var="preTaxOptionList" items="${sessionScope.preTaxOptionList}">
    <option value="${preTaxOptionList.getId()}">${preTaxOptionList.getDescription()}</option>
  </c:forEach>
</select>
