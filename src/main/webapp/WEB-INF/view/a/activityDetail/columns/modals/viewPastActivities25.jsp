<%@ page import="net.superiorstate.ams.data.dao.AppConstantDAO" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row mt-3 mb-2 overflow-auto" style="height: 400px">
  <div class="col">
    <c:forEach var="act" items="${sessionScope.local.getCurrentActivity().getPastActivities()}">
      <c:set var="isOpen" value="OPEN"></c:set>
      <c:if test="${act.isComplete()}">
        <c:set var="isOpen" value="CLOSED"></c:set>
      </c:if>
      <c:if test="${!act.getId().equals(sessionScope.local.getCurrentActivity().getActivity().getId())}">
        <form action="ViewPastActivity25" method="post">
          <div class="row mb-1">
            <div class="col">
              <div class="input-group input-group-sm">
                <span class="input-group-text">${isOpen}</span>
                <div class="form-control">
                  Created <fmt:formatDate value="${act.getDateCreated()}" pattern="MM/dd/yyyy"></fmt:formatDate>
                </div>
                <button type="submit" class="btn btn-sm btn-warning" name="pastActivityId" id="paId" value="${act.getId()}">View</button>
              </div>
            </div>
          </div>
        </form>
      </c:if>
    </c:forEach>
  </div>
</div>
