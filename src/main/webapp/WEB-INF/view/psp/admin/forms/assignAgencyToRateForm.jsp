<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form action="AssignRate" method="post">
  <div class="row">
    <%-- ASSIGN AGENCY SECTION --%>
    <div class="input-group input-group-sm">
      <select class="form-select" aria-label="agencyList type drop down" name="agencyListDD" id="agencyListDD">
        <c:if test="${sessionScope.agencyList.size()<1}">
          <option value="0">MUST CREATE NEW AGENCY</option>
        </c:if>
        <c:forEach var="agency" items="${sessionScope.agencyList}">
          <option value="${agency.getId()}">${agency.getName()}</option>
        </c:forEach>
      </select>
      <c:if test="${sessionScope.agencyList.size()>0}">
        <button type="submit" class="btn btn-secondary" name="btnAddAgency" id="btnAddAgency">Assign&nbsp;</button>
      </c:if>
    </div>
  </div>
</form>
