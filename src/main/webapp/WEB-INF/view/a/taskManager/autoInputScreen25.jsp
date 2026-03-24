<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
        <input type="hidden" name="csrf" value="${sessionScope.csrfToken}" />
        <input type="hidden" name="sendAutoEmail" value="1" />
        <c:forEach var="input" items="${sessionScope.a1inputLabels}" varStatus="loop">
          <div class="row mb-3">
            <div class="col">
              <div class="input-group">
                <span class="input-group-text">
                    ${fn:escapeXml(input.replaceAll('<[^>]*>', ''))}
                </span>
                <c:choose>
                  <c:when test="${sessionScope.a1inputTypes[loop.index] == 'TO'}">
                    <input type="email"
                           class="form-control"
                           name="aInput-${loop.index}"
                           id="aInput-${loop.index}"
                           placeholder="recipient@example.com"
                           required>
                  </c:when>
                  <c:when test="${sessionScope.a1inputTypes[loop.index] == 'LINK'}">
                    <input type="url"
                           class="form-control"
                           name="aInput-${loop.index}"
                           id="aInput-${loop.index}"
                           placeholder="https://..."
                           required>
                  </c:when>
                  <c:otherwise>
                    <input type="text"
                           class="form-control"
                           name="aInput-${loop.index}"
                           id="aInput-${loop.index}"
                           required>
                  </c:otherwise>
                </c:choose>
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
