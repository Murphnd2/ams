<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<form method="post" action="AddNoteToActivity">

  <div class="row mb-1">
    <div class="col">
      <div class="input-group input-group-sm">
        <span class="input-group-text">Status</span>
        <c:import url="/WEB-INF/view/activity/note/components/ddNoteStatus.jsp"></c:import>
        <span class="input-group-text">Reason</span>
        <c:import url="/WEB-INF/view/activity/note/components/ddReasons.jsp"></c:import>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group input-group-sm w-100">
        <textarea class="form-control w-75" name="noteText" required rows="6" type="text" placeholder="Add text here for the note"></textarea>
      </div>
    </div>
  </div>
  <div class="row">
    <div class="col"></div>
    <div class="col-auto">
      <button type="submit" name="btnAddNote1" value="Save" class="btn btn-danger"><i class="bi bi-clipboard-plus-fill"></i>&nbsp;&nbsp; Save Note</button>
    </div>
  </div>
</form>
