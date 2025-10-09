<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select" aria-label="recurring freq type drop down" name="requiredList" id="requiredList">
    <c:forEach var="reqList" items="${sessionScope.reqTaskList}">
        <option value="${reqList.getId()}">${reqList.getDescription()}</option>
    </c:forEach>
</select>
