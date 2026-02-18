<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="container-fluid m-0 p-0">
  <div class="row m-0 p-0 overflow-auto" style="max-height:575px">
    <div class="col m-0 p-0">
      <c:set var="isPast" value="pe-none"></c:set>
      <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
        <c:set var="isPast" value=""></c:set>
      </c:if>
      <c:forEach var="toDo" items="${sessionScope.local.getCurrentActivity().getToDoList()}" varStatus="tds">
        <c:if test="${toDo.getTask().getId()!=153}">
          <div class="row m-0 p-0 ">
            <div class="col m-0 p-0">
              <form method="post" action="${toDo.getFormServlet()}">
                <div class="input-group input-group-sm p-0 m-0">
                  <button type="submit" class="btn btn-outline-cb border-white border-0 m-0 p-0 ${toDo.getPointerEvents()} ${isPast}" name="btnToDo" id="btn${toDo.getToDo().getId()}" value="${toDo.getToDo().getId()}">
                    <i class="bi bi-${toDo.getBtnIcon()}" style="font-size: 1.4rem"></i>
                  </button>
                  <c:choose>
                    <c:when test="${toDo.isComplete()==false && toDo.hasGoto()==true && toDo.getGotoLink()!=null}">
                      <div class="form-control ${toDo.getRowCssClass()} border-white border-0">
                        <a href="${toDo.getGotoLink().getLinkPath()}" target="_blank" style="font-size:0.65em">${toDo.getDescription()}</a>
                      </div>
                    </c:when>
                    <c:otherwise>
                      <div class="form-control ${toDo.getRowCssClass()} border-white border-0">
                      <span style="font-size: 0.65em; ${toDo.getRowStyle()}">
                          ${toDo.getDescription()}
                      </span>
                      </div>
                    </c:otherwise>
                  </c:choose>
                </div>
              </form>
            </div>
            <c:if test="${toDo.isComplete()==false && toDo.hasInfo()==true && toDo.getInfoLink()!=null}">
              <div class="col-auto m-0 p-0 me-1">
                <a class="btn btn-outline-qm m-0 p-0 mt-1 ps-1 pe-1" href="${toDo.getInfoLink().getLinkPath()}" target="_blank">
                  <i class="bi bi-question-lg"></i>
                </a>
              </div>
            </c:if>
            <div class="col-auto m-0 p-0">
              <form method="post" action="ManageTask25" id="fm${toDo.getToDo().getId()}">
                <input type="text" name="toDoId" id="tdId${toDo.getToDo().getId()}" value="${toDo.getToDo().getId()}" hidden>
                <button type="submit" class="btn btn-outline-auto m-0 p-0 ps-1 pe-1 ${toDo.getPointerEvents()} mt-1 ${isPast}">
                  <i class="bi bi-tools"></i>
                </button>
              </form>
            </div>
          </div>
        </c:if>
      </c:forEach>
    </div>
  </div>
</div>
<!-- Auto-save on page unload -->
<form id="autoSaveForm" method="post" action="PersistChecklist25" style="display:none;">
  <input type="hidden" name="autoSave" value="true">
</form>