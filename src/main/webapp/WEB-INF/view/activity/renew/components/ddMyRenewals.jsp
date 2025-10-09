<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
  <c:when test="${sessionScope.myRenewalList.size()>0}">
    <c:set var="buttonAction" value=""></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="buttonAction" value="disabled"></c:set>
  </c:otherwise>
</c:choose>
<div class="input-group">
  <button type="submit" class="btn btn-secondary" ${buttonAction}>Go</button>
  <select class="form-select" aria-label="recurring freq type drop down" name="myRenewalList" id="myRenewalList">
    <c:choose>
      <c:when test="${sessionScope.myRenewalList.size()>0}">
        <c:forEach var="renewal" items="${sessionScope.myRenewalList}">
          <option value="${renewal.getId()}">${renewal.getEmployer().getEmployerName()}</option>
        </c:forEach>
      </c:when>
      <c:otherwise>
        <option>No Open Renewals</option>
      </c:otherwise>
    </c:choose>
  </select>
</div>

