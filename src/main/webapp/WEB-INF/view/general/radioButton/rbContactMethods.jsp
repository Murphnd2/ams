<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%String radioId; int countId = 0;%>
<c:forEach var="contactMethodList" items="${sessionScope.contactMethodList}" varStatus="loop">
  <div class="form-check form-check-inline mb-3">
    <% countId = countId + 1;%>
    <% radioId = "contactMethod" + countId; session.setAttribute("radioId",radioId);%>
    <input class="form-check-input" type="radio" name="contactMethodList" id="${sessionScope.radioId}" value="${contactMethodList.getId()}">
    <label class="form-check-label" for="${sessionScope.radioId}">${contactMethodList.getDescription()}</label>
  </div>
</c:forEach>
