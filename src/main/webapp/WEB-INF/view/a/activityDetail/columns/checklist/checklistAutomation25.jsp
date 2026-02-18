<%@ page import="net.superiorstate.ams.model.activity.checklist.tasks.Task" %>
<%@ page import="java.util.List" %>
<%@ page import="net.superiorstate.ams.model.activity.checklist.tasks.ToDo" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:choose>
  <c:when test="${sessionScope.local.getCurrentActivity().getToDoList().get(0).isComplete()==false && sessionScope.local.getCurrentActivity().getToDoList().get(0).hasAutomation()==true}">
    <div class="row m-0 mb-2 p-0">
      <div class="col m-0 p-0">
        <div class="input-group input-group-sm">
          <div class="form-control bg-warning text-dark fw-bold fst-italic border-dark">
              ${sessionScope.local.getCurrentActivity().getToDoList().get(0).getAutomationText()}
          </div>
          <c:set var="sName" value="${sessionScope.local.getCurrentActivity().getToDoList().get(0).getTask().getServletName()}"></c:set>
          <c:set var="testName" value="SendAutoE"></c:set>
          <c:if test="${sessionScope.local.getCurrentActivity().getToDoList().get(0).getTask().hasAutomation()
                        && sessionScope.local.getCurrentActivity().getToDoList().get(0).getTask().getAutomation() != null}">
            <c:set var="autoId" value="${sessionScope.local.getCurrentActivity().getToDoList().get(0).getTask().getAutomation().id}" />
            <a href="PreviewAutomation?aeId=${autoId}"
               class="btn btn-outline-info border-info"
               title="Preview Email"
               target="_blank">
              <i class="bi bi-eye"></i>
            </a>
          </c:if>
          <c:choose>
            <c:when test="${sessionScope.local.getCurrentActivity().getToDoList().get(0).getTask().hasAutomation()==true && sessionScope.local.getCurrentActivity().getToDoList().get(0).getTask().getAutomation()==null}">
              <a class="btn btn-outline-warning text-dark border-dark pe-none"  href="#">
                <i class="bi bi-activity"></i>
              </a>
            </c:when>
            <c:otherwise>
              <a class="btn btn-outline-warning text-dark border-dark" href="${sessionScope.local.getCurrentActivity().getToDoList().get(0).getServletName()}">
                <i class="bi bi-power"></i>
              </a>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </div>
  </c:when>
  <c:otherwise>
    <div class="row m-0 mb-2 p-0">
      <div class="col m-0 p-0">
        <div class="input-group input-group-sm">
          <div class="form-control btn btn-outline-info fst-italic fw-light pe-none">
            current task is not automated
          </div>
        </div>
      </div>
    </div>
  </c:otherwise>
</c:choose>
