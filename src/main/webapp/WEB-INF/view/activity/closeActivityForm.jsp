<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
  <div class="row mb-2">
    <div class="col">
      All tasks have been completed.  Are you sure you want to close this ${sessionScope.currentActivity.getClass().getSimpleName()}?
    </div>
  </div>
  <div class="row">
    <div class="col">
      <button type="submit" class="btn btn-danger w-100" name="btnCheckList" value="${sessionScope.currentToDoList.get(0).getCheckList().getId()}">Close ${sessionScope.currentActivity.getClass().getSimpleName()}</button>
    </div>
  </div>
