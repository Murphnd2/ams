<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<c:url var="logo1" value="logo.png"></c:url>
<c:url var="logo2" value="logo02.png"></c:url>
<c:url var="logo3" value="logo03.png"></c:url>
<c:url var="logoA" value="/images/logoA.png"></c:url>
<c:url var="logoC" value="/images/logoC.png"></c:url>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>
        <c:choose>
            <c:when test="${sessionScope.local.isAuthenticated()==true}">
                ${sessionScope.psp.getFullName()}
            </c:when>
            <c:otherwise>
                AMS
            </c:otherwise>
        </c:choose>
        </title>
    <style>
        .input-group-text{
            background-color: #06357a;
            color: white;
        }
        .input-group-sm{
            margin-bottom: 10px;
        }
    </style>
</head>
<body>
    <div class="container-fluid">
        <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>

                <div class="row mt-5">
                    <div class="col">
                        <div class="row pt-1 mt-1">
                            <div class="col"></div>
                            <div class="col-auto">
                                <img src="${pageContext.request.contextPath}/images/logoC.png" class="img-fluid" alt="Administrator Logo" style="height: 650px">
                            </div>
                            <div class="col"></div>
                        </div>

                    </div>
                </div>
    </div>
</body>
</html>