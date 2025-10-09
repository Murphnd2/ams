<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select mb-3" aria-label="insurance type drop down" name="insuranceTypeList" id="insuranceTypeList">
  <c:forEach var="insuranceTypeList" items="${sessionScope.insuranceTypeList}">
    <option value="${insuranceTypeList.getId()}">${insuranceTypeList.getDescription()}</option>
  </c:forEach>
</select>
