<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
  <head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Login Help</title>
  </head>
  <body>
    <div class="container-fluid">
      <%--****************** NAVIGATION BAR ***************************** --%>
        <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
        <div class="row">
          <div class="col offset-lg-4">
            <%--****************** LOGIN HELP HERE ******************************* --%>
              <c:import url="/WEB-INF/view/authentication/loginHelpForm.jsp"></c:import>
          </div>
          <div class="col-lg-4">
          </div>
        </div>

      <%--****************** NEXT LEVEL HERE ******************************* --%>

    </div>
  </body>
</html>
