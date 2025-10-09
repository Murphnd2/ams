<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select mb-3" aria-label="recurring freq type drop down" name="recurringFrequencyList" id="recurringFrequencyList">
  <c:forEach var="recurringFrequencyList" items="${sessionScope.recurringFrequencyList}">
    <option value="${recurringFrequencyList.getId()}">${recurringFrequencyList.getDescription()}</option>
  </c:forEach>
</select>
