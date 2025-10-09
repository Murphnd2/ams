<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="ReOpenToDo" class="m-0 d-none d-sm-grid">
  <div class="input-group input-group-sm p-0 m-0">
    <button type="submit" class="btn btn-outline-success border-white border-0 m-0 p-0" name="btnToDo" id="btn${toDo.getId()}-${toDo.getId()}" value="${toDo.getId()}">
      <i class="bi bi-x-square" style="font-size: 1.4rem"></i>
    </button>
    <div class="form-control text-success fw-lighter p-0 pt-2 ps-2 border-white border-0"  style="text-decoration:line-through;font-style: italic;font-size:0.65rem">
      ${toDo.getTask().getDescription()}
    </div>
  </div>
</form>