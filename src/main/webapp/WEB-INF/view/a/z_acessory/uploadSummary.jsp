<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>${sessionScope.psp.getFullName()}</title>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/navbar.jsp"></c:import>

    <div class="mt-4">
        <h2>📤 Upload Summary</h2>

        <div class="alert alert-success mt-3">
            <strong>Successfully uploaded files:</strong> ${uploadCount}
        </div>

        <c:if test="${not empty matchedFiles}">
            <h4 class="mt-4">✅ Matched & Renamed Files</h4>
            <ul class="list-group mb-4">
                <c:forEach var="item" items="${matchedFiles}">
                    <li class="list-group-item list-group-item-success">${item}</li>
                </c:forEach>
            </ul>
        </c:if>

        <c:if test="${not empty unmatchedFiles}">
            <h4 class="mt-4">❌ Unmatched or Rejected Files</h4>
            <ul class="list-group">
                <c:forEach var="item" items="${unmatchedFiles}">
                    <li class="list-group-item list-group-item-danger">${item}</li>
                </c:forEach>
            </ul>
        </c:if>

        <div class="mt-4">
            <a href="ShowUploadPage" class="btn btn-secondary">⬅️ Back to Upload Page</a>
        </div>
    </div>
</div>
</body>
</html>


