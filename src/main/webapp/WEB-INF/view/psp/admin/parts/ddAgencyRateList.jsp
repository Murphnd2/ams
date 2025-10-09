<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select" aria-label="rateListdd type drop down" name="ddRateList" id="ddRateList">
    <c:if test="${sessionScope.agencyRateList.size()<1}">
        <option value="0">MUST CREATE NEW RATE</option>
    </c:if>
    <c:forEach var="rate" items="${sessionScope.agencyRateList}">
        <option value="${rate.getId()}">${rate.getDescription()}</option>
    </c:forEach>
</select>
