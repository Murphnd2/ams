<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Superior State</title>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
    <c:choose>
        <c:when test="${sessionScope.isAuthenticated==true}">
            Index Page When Authenticated
        </c:when>
        <c:otherwise>
            <c:import url="WEB-INF/view/authentication/loginForm.jsp"></c:import>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
