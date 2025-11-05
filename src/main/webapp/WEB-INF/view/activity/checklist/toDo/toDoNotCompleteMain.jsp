<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
  <c:when test="${toDo.getTask().isSourced()}">

  </c:when>
  <c:otherwise>


  </c:otherwise>
</c:choose>
