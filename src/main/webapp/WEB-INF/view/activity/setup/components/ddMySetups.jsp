<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
  <c:when test="${sessionScope.mySetupList.size()>0}">
    <c:set var="buttonAction" value=""></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="buttonAction" value="disabled"></c:set>
  </c:otherwise>
</c:choose>
<div class="input-group">
  <button type="submit" class="btn btn-secondary" ${buttonAction}>Go</button>
  <select class="form-select" aria-label="recurring freq type drop down" name="mySetupList" id="mySetupList">
    <c:choose>
      <c:when test="${sessionScope.mySetupList.size()>0}">
        <c:forEach var="setup" items="${sessionScope.mySetupList}">
          <option value="${setup.getId()}">${setup.getProposal().getProspect().getName()}</option>
        </c:forEach>
      </c:when>
      <c:otherwise>
        <option>No Open Setups</option>
      </c:otherwise>
    </c:choose>
  </select>
</div>
