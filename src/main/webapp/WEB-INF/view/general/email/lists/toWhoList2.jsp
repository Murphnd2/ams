<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${sessionScope.recipientList2.size()==0}">
  <div class="col-auto">
    <div class="form-control form-control-sm pe-none">
      EMPTY
    </div>
  </div>
</c:if>
<c:forEach var="recipient" items="${sessionScope.recipientList2}">
  <div class="col-auto">
    <div class="input-group input-group-sm">
                <span class="input-group-text">
                  ${recipient.getFirstName()} ${recipient.getLastName()} (${recipient.getEmail()})
                </span>
    </div>
  </div>
</c:forEach>
<div class="col"></div>
