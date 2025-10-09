<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="CreateBlankRenewal" class="mb-1">
  <div class="row">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Select Employer</span>
        <c:import url="/WEB-INF/view/activity/renew/components/ddEmployerList.jsp"></c:import>
        <button type="submit" class="btn btn-secondary">
          <i class="bi bi-recycle"></i>&nbsp;Create Blank Renewal
        </button>
      </div>
    </div>
  </div>
</form>
