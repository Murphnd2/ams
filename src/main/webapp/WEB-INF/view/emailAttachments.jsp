<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Secure File Download</title>
</head>
<body>
<div class="container-fluid">
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
    <div class="row">
        <div class="col">
            Download ${sessionScope.currentWebLink.getPlainText()} Below
        </div></div><div class="row">
        <div class="col">
            ${sessionScope.currentWebLink.getInternalAnchorTag()}
        </div>
    </div>
</div>
</body>
</html>
