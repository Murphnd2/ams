<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AddProspect">
  <div class="row">
    <div class="col-lg-6">
      <div class="container">
        <div class="row mb-3">
          <div class="col">
            <div class="input-group input-group-sm">
              <label for="prospectName" class="col-3 input-group-text">Prospect Name</label>
              <input type="text" class="form-control" id="prospectName" name = "prospectName" required/>
            </div>
          </div>
        </div>
      </div>
      <c:import url="/WEB-INF/view/general/addressBlock.jsp"></c:import><%----%>
    </div>
    <div class="col-lg-6">
      <div class="container">
        <div class="row mb-3">
          <div class="col">
            <div class="input-group input-group-sm">
              Primary Contact Info
            </div>
          </div>
        </div>
      </div>
      <c:import url="/WEB-INF/view/general/personBlock.jsp"></c:import><%----%>
    </div>
  </div>
  <hr/>
  <div class="row justify-content-end">
    <div class="col">
          <button type="submit" class="btn btn-primary">Add</button>
    </div>
  </div>
</form>
