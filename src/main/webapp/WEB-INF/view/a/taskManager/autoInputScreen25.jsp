<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>${applicationScope.global.getPsp().getFullName()}</title>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
  <div class="row">
    <div class="col"></div>
    <div class="col-lg-6 col-md-9 border border-secondary rounded-3">
      <h4 class="mt-3 mb-2">${sessionScope.a1autoName}</h4>
      <form method="post" action="SendAutoFinal25">
        <c:forEach var="input" items="${sessionScope.a1inputLabels}" varStatus="loop">
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
            &nbsp;
          </div>
          <div class="col-auto">
            <a class="btn btn-outline-primary" href="ViewActivity25">Cancel</a>
          </div>
          <div class="col-auto">
            <button type="submit" class="btn btn-primary">Send</button>
          </div>
        </div>
      </form>
    </div>
    <div class="col"></div>
  </div>

</div>
</body>
</html>
