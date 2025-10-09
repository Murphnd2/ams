<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:url var="homePage" value="/index.jsp"></c:url>
<c:url var="logo1" value="logo.png"></c:url>
<c:url var="logo2" value="logo02.png"></c:url>
<c:url var="logo3" value="logo03.png"></c:url>
<nav class="navbar navbar-expand-lg navbar-light bg-light">
  <div class="container-fluid">
    <a class="navbar-brand" href="${homePage}">
      <c:choose>
        <c:when test="${sessionScope.isAuthenticated==true}">
          <img src="${logo1}" />
        </c:when>
        <c:otherwise>
          &nbsp;
        </c:otherwise>
      </c:choose>
    </a>
    <div class="visually-hidden">
      <img src="${logo2}" />
    </div>
  </div>
</nav>
