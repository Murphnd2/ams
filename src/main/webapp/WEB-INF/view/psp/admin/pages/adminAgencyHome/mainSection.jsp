<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${sessionScope.hasCurrentAgency==true}">
  <div class="row">
    <c:import url="/WEB-INF/view/psp/admin/accordion/agencyMainAccord.jsp"></c:import>
  </div>
</c:if>

