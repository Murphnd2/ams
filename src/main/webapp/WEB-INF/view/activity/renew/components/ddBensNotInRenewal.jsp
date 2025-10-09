<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select m-0" aria-label="recurring freq type drop down" name="addBenefitList" id="addBenefitList">
  <c:if test="${sessionScope.benefitsNotInRenewal.size()==0}">
    <option value="0">ALL BENEFITS ARE IN RENEWAL</option>
  </c:if>
  <c:forEach var="benefit" items="${sessionScope.benefitsNotInRenewal}">
    <option value="${benefit.getId()}">${benefit.getPlanDescription()} - Next Due ${benefit.getNextRenewalDue()}</option>
  </c:forEach><%----%>
</select>
