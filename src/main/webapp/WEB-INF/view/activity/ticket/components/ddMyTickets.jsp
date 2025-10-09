<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
  <c:when test="${sessionScope.myTicketList.size()>0}">
    <c:set var="buttonAction" value=""></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="buttonAction" value="disabled"></c:set>
  </c:otherwise>
</c:choose>
<div class="input-group">
  <button type="submit" class="btn btn-secondary" ${buttonAction}>Go</button>
  <select class="form-select" aria-label="recurring freq type drop down" name="myTicketList" id="myTicketList">
    <c:choose>
      <c:when test="${sessionScope.myTicketList.size()>0}">
        <c:forEach var="ticket" items="${sessionScope.myTicketList}">
          <option value="${ticket.getId()}">- - NEED TO ADD TICKET INFO HERE - -</option>
        </c:forEach>
      </c:when>
      <c:otherwise>
        <option>No Open Tickets</option>
      </c:otherwise>
    </c:choose>
  </select>
  <a href="CreateTicket" class="btn btn-info">New</a>
</div>

