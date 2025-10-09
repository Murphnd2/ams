<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="CloseToDo" class="m-0 d-none d-sm-grid">
  <div class="input-group input-group-sm p-0 m-0">
    <button type="submit" class="btn btn-outline-success border-white border-0 m-0 p-0" name="btnToDo" id="btn${toDo.getId()}-${toDo.getId()}" value="${toDo.getId()}">
      <i class="bi bi-square" style="font-size: 1.4rem"></i>
    </button>
    <div class="form-control text-success fw-lighter p-0 pt-2 ps-2 border-white border-0"  style="text-decoration:line-through;font-style: italic;font-size:0.65rem">
      <c:choose>
        <c:when test="${toDo.getTask().hasGoTo()}">
          <a href="${toDo.getTask().getGoTo().getLinkPath()}" target="_blank">
              ${toDo.getTask().getDescription()}
          </a>
        </c:when>
        <c:otherwise>
          ${toDo.getTask().getDescription()}
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</form>
