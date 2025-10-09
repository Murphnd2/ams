<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>Recurring Items</title>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
  <div class="row">
    <%-- ******* L E F T    C O L U M N   **************** --%>
    <div class="col-lg-3">
      <h3>Recurring Items</h3>

    </div>
    <%-- ******* C E N T E R    C O L U M N   **************** --%>
    <div class="col">
      <c:choose>
        <c:when test="${sessionScope.recurView == 1}">
          <h3>Recurring Item Builder</h3>

        </c:when>
        <c:when test="${sessionScope.recurView == 2}">
          <h3>Sequence Management</h3>
        </c:when>
        <c:otherwise>
          <h3>Default View</h3>
        </c:otherwise>
      </c:choose>

    </div>
    <%-- ******* R I G H T    C O L U M N   **************** --%>
    <div class="col-lg-3">
      <h3>Sequence List</h3>
    </div>
  </div>
</div>
<%-- ******* M O D A L S  A R E   B E L O W   **************** --%>
</body>
</html>
