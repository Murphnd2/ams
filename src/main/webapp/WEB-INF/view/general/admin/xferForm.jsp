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
  <div class="row">
    <div class="col">
      <form action="CreateNewEmployer" method="post">
        <h1>Employer Must Be Created in Summit</h1>
        <div class="input-group w-50">
          <span class="input-group-text">Summit Org ID</span>
          <input class="form-control" type="number" name="oid" required>
        </div>
        <div class="input-group w-50">
          <span class="input-group-text">Summit Employer ID</span>
          <input class="form-control" type="number" name="eid" required>
        </div>
        <div class="input-group w-50">
          <span class="input-group-text">DPI ER_KEY</span>
          <input class="form-control" type="number" name="eky" required>
        </div>
        <button type="submit" class="btn btn-primary w-50">Submit</button>
      </form>
    </div>
  </div>

</body>
</html>
