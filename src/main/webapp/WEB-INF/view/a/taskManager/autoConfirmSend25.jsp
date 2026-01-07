<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8"/>
  <title>Sending...</title>
</head>
<body>
<c:url var="sendUrl" value="/SendAutoFinal25"/>

<form id="autoSendForm" method="post" action="${sendUrl}">
  <input type="hidden" name="csrf" value="${sessionScope.csrfToken}" />
  <input type="hidden" name="sendAutoEmail" value="1" />
</form>

<script>
  document.getElementById('autoSendForm').submit();
</script>
</body>
</html>
