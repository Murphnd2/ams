<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form action="AddItemToModule" method="post">
  <c:import url="/WEB-INF/view/psp/admin/parts/ddServiceModulesRaw.jsp"></c:import>
  <c:import url="/WEB-INF/view/psp/admin/parts/ddServiceItemsRaw.jsp"></c:import>
  <div class="row">
    <div class="col">
      <button class="btn btn-primary form-control w-100" type="submit">Link Item to Group</button>
    </div>
  </div>
</form>
