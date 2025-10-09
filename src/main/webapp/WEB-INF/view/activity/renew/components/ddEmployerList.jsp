<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select m-0" aria-label="recurring freq type drop down" name="employerId" id="employerId2">
  <c:if test="${sessionScope.pspEmployerList.size()==0}">
    <option value="0">NO EMPLOYERS EXIST</option>
  </c:if>
  <c:forEach var="employer" items="${sessionScope.pspEmployerList}">
    <option value="${employer.getId()}">${employer.getEmployerName().toUpperCase().trim()}</option>
  </c:forEach><%----%>
</select>
