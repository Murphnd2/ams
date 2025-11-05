<%@ page import="net.superiorstate.ams.previous.model.activity.checklist.tasks.Task" %>
<%@ page import="java.util.List" %>
<%@ page import="net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:choose>
  <c:when test="${sessionScope.currentToDoList.get(0).isComplete()==false && sessionScope.currentToDoList.get(0).getTask().isAutomated()}">
        <div class="row m-0 mb-2 p-0">
          <div class="col m-0 p-0">
            <div class="input-group input-group-sm">
              <div class="form-control bg-warning text-dark fw-bold fst-italic border-dark">
                  ${sessionScope.currentToDoList.get(0).getTask().getAutomationText()}
              </div>
              <c:set var="sName" value="${sessionScope.currentToDoList.get(0).getTask().getServletName()}"></c:set>
              <c:set var="testName" value="SendAutoE"></c:set>
              <c:if test="${sName.substring(0,9) == testName}">
                <c:set var="newLink" value="PreviewServlet"></c:set>
                <c:set var="newPath" value="${newLink}${sName.substring(13)}"></c:set>
                <a class="btn btn-outline-warning text-primary border-primary" href="${newPath}">
                  <i class="bi bi-eyeglasses"></i>
                </a>
              </c:if>
              <a class="btn btn-outline-warning text-dark border-dark" href="${sessionScope.currentToDoList.get(0).getTask().getServletName()}">
                <i class="bi bi-power"></i>
              </a>
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
