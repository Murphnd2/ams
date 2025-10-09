<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>${sessionScope.theEmail}</title>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
    <div class="row">
        <div class="col">
            <div class="row m-1 p-1 bg-dark text-white border-bottom-0">
                <div class="col-2">
                    Date
                </div>
                <div class="col-2 ">From</div>
                <div class="col-7">
                    Subject
                </div>
                <div class="col-1">
                </div>

            </div>
        </div>
    </div>
    <div class="row overflow-auto" style="height: 690px">
        <div class="col">
            <c:forEach var="email" items="${sessionScope.emailList}" varStatus="theCount">
                <c:set var="bgColor" value="bg-light"></c:set>
                <c:if test="${theCount.count%2==0}">
                    <c:set var="bgColor" value=""></c:set>
                </c:if>
                <div class="row ${bgColor} border-bottom-0 m-1 p-1">
                    <div class="col-2">
                        <fmt:formatDate value="${email.getDateGenerated()}" pattern="MMM dd, yyyy"></fmt:formatDate>
                    </div>
                    <div class="col-2 ">
                        ${email.getCreatedBy().getFirstName()}&nbsp;${email.getCreatedBy().getLastName()}
                    </div>
                    <div class="col-7">
                        ${email.getSubject()}
                    </div>
                    <div class="col-1">
                        <a class="btn btn-sm btn-outline-primary" href="ViewEmail?id=${email.getId()}" target="_blank">View</a>
                    </div>
                </div>
            </c:forEach>
        </div>
    </div>

</body>
</html>
