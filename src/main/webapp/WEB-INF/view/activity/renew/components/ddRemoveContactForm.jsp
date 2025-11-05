<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="RemoveRenewalContact">
  <div class="input-group input-group-sm mb-1">
    <span class="input-group-text">Assigned</span>
    <c:import url="/WEB-INF/view/activity/renew/components/ddContactsAssigned.jsp"></c:import>
    <button type="submit" class="btn btn-success">Remove</button>
  </div>
</form>
