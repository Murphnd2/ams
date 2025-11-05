<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AddPspAgent">
  <div class="row">
    <div class="col-lg-6">
      <div class="row mb-3">
        <div class="col">
          <div class="input-group input-group-sm">
            <label class="input-group-text bg-dark text-light">Primary Contact</label>
          </div>
        </div>
      </div>
      <c:import url="/WEB-INF/view/general/personBlock.jsp"></c:import><%----%>
    </div>
    <div class="col-lg-6">
      <div class="row mb-3">
        <div class="col">
          <div class="input-group input-group-sm">
            &nbsp;
          </div>
        </div>
      </div>
      <div class="row">
        <div class="col">
          <c:import url="/WEB-INF/view/general/addressBlock.jsp"></c:import><%----%>
        </div>
      </div>
    </div>
  </div>
  <hr/>
  <div class="row justify-content-end">
    <div class="col">
      <c:choose>
        <c:when test="${sessionScope.pspAdminHomeSender==2}">
          <button type="submit" class="btn btn-primary">Update</button>
        </c:when>
        <c:otherwise>
          <button type="submit" class="btn btn-primary">Add</button>
        </c:otherwise>
      </c:choose>
      <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
    </div>
  </div>
</form>
