<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form action="ProposalView" method="post">
  <div class="row">
    <div class="btn-group">
      <button type="button" class="btn btn-outline-dark dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
        Prospects of
        <span class="fst-italic">
          <c:choose>
            <c:when test="${sessionScope.currentAgent.getFullName().length()>20}">
              ${sessionScope.currentAgent().getFullName().substring(0,19)}...
            </c:when>
            <c:otherwise>
              ${sessionScope.currentAgent.getFullName()}
            </c:otherwise>
          </c:choose>
        </span>
      </button>
      <ul class="dropdown-menu">
        <c:forEach var="prospect" items="${sessionScope.prospectList}">
          <c:if test="${sessionScope.prospectList.size()<1}">
            <li><button type="button" class="dropdown-item" data-bs-toggle="modal" data-bs-target="#addProspectModal">Add New Prospect</button>
          </c:if>
          <li><button class="dropdown-item" type="submit" name="prospectSelectButton" value="${prospect.getId()}">${prospect.getName()}</button></li>
        </c:forEach>
        <li><hr class="dropdown-divider"></li>
        <li>
          <button type="button" class="dropdown-item" data-bs-toggle="modal" data-bs-target="#addProspectModal">Add Prospect</button>
        </li>
      </ul>
    </div>
  </div>
</form>

