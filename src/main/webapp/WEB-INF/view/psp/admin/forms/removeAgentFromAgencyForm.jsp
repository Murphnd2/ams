<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form action="RemoveAgent" method="post">
  <div class="row">
    <%-- REMOVE AGENT SECTION --%>
    <div class="input-group input-group-sm">
      <select class="form-select" aria-label="activeAgentList type drop down" name="activeAgentList" id="activeAgentList">
        <c:if test="${sessionScope.agencyAgentList.size()<1}">
          <option value="0">NO AGENTS ASSIGNED</option>
        </c:if>
        <c:forEach var="agent" items="${sessionScope.agencyAgentList}">
          <option value="${agent.getId()}">${agent.toString()}</option>
        </c:forEach>
      </select>
      <c:if test="${sessionScope.agencyAgentList.size()>0}">
        <button type="submit" class="btn btn-secondary" name="btnRemoveAgent" id="btnRemoveAgent">Remove&nbsp;</button>
      </c:if>
    </div>
  </div>
</form>
