<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form action="AssignPendingAgent" method="post">
  <div class="row">
    <%-- ASSIGN AGENT SECTION --%>
    <div class="input-group input-group-sm">
      <select class="form-select" aria-label="pendingAgentList type drop down" name="pendingAgentList" id="pendingAgentList">
        <c:if test="${sessionScope.pendingAgentList.size()<1}">
          <option value="0">NO PENDING AGENTS</option>
        </c:if>
        <c:forEach var="agent" items="${sessionScope.pendingAgentList}">
          <option value="${agent.getId()}">${agent.toString()}</option>
        </c:forEach>
      </select>
      <c:if test="${sessionScope.pendingAgentList.size()>0}">
        <button type="submit" class="btn btn-secondary" name="btnAddAgent" id="btnAddAgent">Assign&nbsp;</button>
      </c:if>
    </div>
  </div>
</form>
