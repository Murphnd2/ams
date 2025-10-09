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
    <div class="row mbt-1">
        <div class="col"></div>
        <div class="col-lg-10 border border-secondary rounded-3">
            <form method="post" action="SendPreviewEmail">
                <c:choose>
                    <c:when test="${sessionScope.aPreviewView==1 && sessionScope.aNumInputs>0}">
                        <c:forEach var="input" items="${sessionScope.aInputLabels}" varStatus="loop">
                            <div class="row mb-3">
                                <div class="col">
                                    <div class="input-group">
                                        <span class="input-group-text"><c:out value="${input}" /></span>
                                        <input type="text" class="form-control" name="aInput-${loop.index}" id="aInput-${loop.index}" required >
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                        <div class="row mb-3">
                            <div class="col">
                            </div>
                            <div class="col-auto">
                                <a class="btn btn-outline-primary" href="GoAdminHome">Cancel</a>
                            </div>
                            <div class="col-auto">
                                <button type="submit" class="btn btn-primary" value="STEP1">Next</button>
                            </div>
                        </div>
                    </c:when>
                    <c:otherwise>

                    </c:otherwise>
                </c:choose>
            </form>
        </div>
        <div class="col"></div>
    </div>
</div>
</body>
</html>
