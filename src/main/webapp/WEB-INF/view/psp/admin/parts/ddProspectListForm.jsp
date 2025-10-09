<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form action="ProposalView" method="post">
  <div class="row">
    <div class="input-group input-group-sm">
      <c:if test="${sessionScope.prospectList.size()>0}">
        <button type="submit" class="btn btn-secondary" name="btnViewProspect" id="btnViewProspect">View&nbsp;</button>
      </c:if>
      <select class="form-select" aria-label="prospectList type drop down" name="prospectSelectButton" id="prospectListDD">
        <c:forEach var="prospect" items="${sessionScope.prospectList}">
          <c:if test="${sessionScope.prospectList.size()<1}">
            <option value="0">HAS NO PROSPECTS</option>
          </c:if>
          <option value="${prospect.getId()}" <c:if test="${sessionScope.currentProspect.getId()==prospect.getId()}">selected</c:if>>${prospect.getName()}</option>
        </c:forEach>
      </select>
    </div>
  </div>
</form>
