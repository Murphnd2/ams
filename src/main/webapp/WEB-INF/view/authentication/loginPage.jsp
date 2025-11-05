<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>Home</title>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
  <div class="row m-3">
    <div class="col">
      <c:import url="loginForm.jsp"></c:import>
    </div>
  </div>
</div>
</body>
</html>
