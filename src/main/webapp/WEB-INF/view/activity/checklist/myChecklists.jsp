<%@ page import="java.sql.Date" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="net.superiorstate.ams.previous.model.activity.checklist.CheckList" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:choose>
  <c:when test="${sessionScope.myChecklists.size()>0}">
    <c:forEach var="list" items="${sessionScope.myChecklists}">

      <c:choose>
        <c:when test="${list.dueDate <= Date.valueOf(LocalDate.now().minusDays(14))}">
          <c:set var="format" value="fw-bold text-uppercase bg-warning"></c:set>
          <c:set var="butt" value="btn-outline-warning text-dark fw-bold"></c:set>
        </c:when>
        <c:when test="${list.dueDate <= Date.valueOf(LocalDate.now().minusDays(1))}">
          <c:set var="format" value="text-danger bg-light fw-bolder"></c:set>
          <c:set var="butt" value=""></c:set>
        </c:when>
        <c:when test="${list.dueDate <= Date.valueOf(LocalDate.now())}">
          <c:set var="format" value=""></c:set>
          <c:set var="butt" value=""></c:set>
        </c:when>
        <c:otherwise>
          <c:set var="format" value="text-secondary fw-lighter text-lowercase"></c:set>
          <c:set var="butt" value=""></c:set>
        </c:otherwise>
      </c:choose>

      <c:choose>
        <c:when test="${list.isComplete()}">
            <form method="post" action="ReOpenChecklist" class="mb-1">
              <div class="input-group input-group-sm">
                <button type="submit" class="btn btn-outline-success"  name="btnCheckList" id="btn${list.getId()}" value="${list.getId()}">
                  <i class="bi bi-x-square"></i>
                </button>
                <div class="form-control text-success fw-lighter" style="text-decoration: line-through;font-style: italic">
                    ${list.getFullName()}
                </div>
              </div>
            </form>
        </c:when>
        <c:when test="${list.getToDoList().size()==1}">
          <c:forEach var="toDo" items="${list.getToDoList()}">
            <form method="post" action="CloseSingleItemChecklist" class="mb-1">
              <div class="accordion" id="a${list.getId()}">
                <div class="accordion-item">
                  <div class="accordion-header" id="h${list.getId()}">
                    <div class="input-group input-group-sm">
                      <c:choose>
                      <c:when test="${sessionScope.currentChecklist.getId()==list.getId()}">
                        <button type="submit" class="btn btn-dark pe-none" name="btnCheckList" id="btn2${list.getId()}" value="V-${list.getId()}">
                          <i class="bi bi-arrow-left-square"></i>
                        </button>
                        <div class="form-control ${format} bg-dark text-warning border border-dark">
                            ${toDo.getTask().getDescription()}
                        </div>
                        <button type="button" data-bs-toggle="collapse" data-bs-target="#collapse${list.getId()}" aria-expanded="true" aria-controls="collapse${list.getId()}" class="btn btn-warning">
                          <i class="bi bi-caret-down-square-fill"></i>
                        </button>
                      </c:when>
                      <c:otherwise>
                        <button type="submit" class="btn btn-outline-success ${butt}" name="btnCheckList" id="btn${list.getId()}-${toDo.getId()}" value="${list.getId()}">
                          <i class="bi bi-square"></i>
                        </button>
                        <button type="submit" class="btn btn-outline-success" name="btnCheckList" id="btn2${list.getId()}" value="V-${list.getId()}">
                          <i class="bi bi-eye-fill"></i>
                        </button>
                        <div class="form-control ${format}">
                            ${toDo.getTask().getDescription()}
                        </div>
                        <button type="button" data-bs-toggle="collapse" data-bs-target="#collapse${list.getId()}" aria-expanded="true" aria-controls="collapse${list.getId()}" class="btn btn-warning">
                          <i class="bi bi-caret-down-square-fill"></i>
                        </button>
                      </c:otherwise>
                      </c:choose>
                    </div>
                  </div>
                  <div id="collapse${list.getId()}" class="accordion-collapse collapse">
                    <div class="accordion-body p-0 m-0 mt-1">
                      <div class="input-group input-group-sm w-100 mb-1">
                        <c:import url="/WEB-INF/view/general/ddUserList.jsp"></c:import>
                        <button type="submit" class="btn btn-outline-success" name="btnCheckList" value="R-${list.getId()}">
                          <i class="bi bi-box-arrow-in-right"></i>
                        </button>
                      </div>
                      <div class="input-group input-group-sm w-100">
                        <div class="input-group-text">Due Date</div>
                        <input type="date" class="form-control" name="newDueDate" value="${list.getDueDate()}">
                        <button type="submit" class="btn btn-outline-success" name="btnCheckList" value="D-${list.getId()}"><i class="bi bi-calendar-check-fill"></i></button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </form>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <form method="post" action="ViewSelectedChecklist" class="mb-1">
            <div class="accordion" id="a${list.getId()}">
              <div class="accordion-item">
                <div class="accordion-header" id="h${list.getId()}">
                  <div class="input-group input-group-sm">
                    <c:choose>
                      <c:when test="${sessionScope.currentChecklist.getId()==list.getId()}">
                        <div type="submit" class="btn btn-dark pe-none" name="btnCheckList" id="btn${list.getId()}" value="${list.getId()}">
                          <i class="bi bi-arrow-left-square-fill"></i>
                        </div>
                        <div class="form-control ${format} bg-dark text-warning border border-dark">
                            ${list.getFullName()}
                        </div>
                        <button type="button" data-bs-toggle="collapse" data-bs-target="#collapse${list.getId()}" aria-expanded="true" aria-controls="collapse${list.getId()}" class="btn btn-warning">
                          <i class="bi bi-caret-down-square-fill"></i>
                        </button>
                      </c:when>
                      <c:otherwise>
                        <button type="submit" class="btn btn-outline-success" name="btnCheckList" id="btn${list.getId()}" value="${list.getId()}">
                          <i class="bi bi-eye"></i>
                        </button>
                        <div class="form-control ${format}">
                            ${list.getFullName()}
                        </div>
                        <button type="button" data-bs-toggle="collapse" data-bs-target="#collapse${list.getId()}" aria-expanded="true" aria-controls="collapse${list.getId()}" class="btn btn-warning">
                          <i class="bi bi-caret-down-square-fill"></i>
                        </button>
                      </c:otherwise>
                    </c:choose>
                  </div>
                </div>
                <div id="collapse${list.getId()}" class="accordion-collapse collapse">
                  <div class="accordion-body">
                    <div class="input-group input-group-sm">
                      <c:import url="/WEB-INF/view/general/ddUserList.jsp"></c:import>
                      <button type="submit" class="btn btn-outline-success" name="btnCheckList" value="R-${list.getId()}">
                        <i class="bi bi-box-arrow-in-right"></i>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </form>
        </c:otherwise>
      </c:choose>
    </c:forEach>
  </c:when>
  <c:otherwise>
    <div class="row">
      <div class="col">
        <div class="form-control text-primary fw-bold text-uppercase">
          Bubkis!
        </div>
      </div>
    </div>
  </c:otherwise>
</c:choose>
