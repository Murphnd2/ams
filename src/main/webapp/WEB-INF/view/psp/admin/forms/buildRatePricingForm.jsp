<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form action="BuildRate" method="post">
  <c:import url="/WEB-INF/view/psp/admin/parts/ddServiceModulesRaw.jsp"></c:import>
  <c:import url="/WEB-INF/view/psp/admin/parts/ddPriceItemsRaw.jsp"></c:import>

  <div class="row mb-1">
    <div class="col">
      <div class="input-group input-group-sm mb-3">
        <div class="input-group-prepend">
          <span class="input-group-text" id="basic-addon1">$</span>
        </div>
        <input class="form-control" type="text" required id="price" name="price"/>
        <button class="btn btn-primary form-control" type="submit">Add to Rate Table</button>
      </div>
    </div>
  </div>
</form>
