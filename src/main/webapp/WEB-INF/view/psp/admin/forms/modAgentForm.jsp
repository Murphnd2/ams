<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ModAgent">
  <div class="row">
    <div class="col-lg-6">

      <c:import url="/WEB-INF/view/general/personBlockAgentMod.jsp"></c:import><%----%>
    </div>
    <div class="col-lg-6">
      <div class="row">
        <div class="col">
          <c:import url="/WEB-INF/view/general/addressBlockAgentMod.jsp"></c:import><%----%>
        </div>
      </div>
    </div>
    <c:choose>
      <c:when test="${sessionScope.formDisable2}">
      </c:when>
      <c:otherwise>
        <div class="col">
          <div class="row mb-3">
            <div class="col-lg-6">
              <button type="submit"  class="btn btn-primary w-100">Click to Save Changes</button>
            </div>
            <div class="col-lg-6">
              <a href="ToggleState"  class="btn btn-danger w-100">Exit</a>
            </div>
          </div>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</form>
<div class="row">
  <div class="col-lg-6">
    <form method="post" action="ResetAgentPassword">
      <button type="submit" class="btn btn-warning w-100">Reset Password</button>
    </form>
  </div>
  <div class="col-lg-6">
    <form method="post" action="SendLoginInstructions">
      <button type="submit" class="btn btn-success w-100">Send Login Instructions</button>
    </form>
  </div>
</div>