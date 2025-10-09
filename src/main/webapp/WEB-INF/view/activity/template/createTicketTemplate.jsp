<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>Create Template</title>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
  <form method="post" action="CreateTicketTemplate">
    <div class="row mb-1">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text">Template Name</span>
          <input type="text" required class="form-control" name="templateName" placeholder="Enter Name" value="${sessionScope.templateName}">
        </div>
      </div>
    </div>
    <div class="row mb-1">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text">Ticket Category</span>
          <select class="form-select" aria-label="recurring freq type drop down" name="categoryList" id="categoryList">
            <c:choose>
              <c:when test="${sessionScope.ticketCategories.size()==0}">
                <option value="-1">NO CATEGORIES RIGHT NOW</option>
              </c:when>
              <c:otherwise>
                <c:forEach var="category" items="${sessionScope.ticketCategories}">
                  <c:choose>
                    <c:when test="${category.getId()==sessionScope.templateCategoryId}">
                      <option value="${category.getId()}" selected>${category.getDescription()}</option>
                    </c:when>
                    <c:otherwise>
                      <option value="${category.getId()}">${category.getDescription()}</option>
                    </c:otherwise>
                  </c:choose>
                </c:forEach>
              </c:otherwise>
            </c:choose>
          </select>
        </div>
      </div>
    </div>
    <c:forEach var="task" items="${sessionScope.taskListBuilder}" varStatus="taskStat">
      <c:set var="theValue" value=""></c:set>
      <c:if test="${sessionScope.taskListStat==1}">
        <c:set var="theValue" value="${task}"></c:set>
      </c:if>
      <div class="row mb-1">
        <div class="col">
          <div class="input-group">
            <span class="input-group-text">Task # ${taskStat.count}</span>
            <input class="form-control" name="taskDesc${taskStat.index}" id="taskDesc${taskStat.index}" value="${theValue}" placeholder="Enter Task Here" required>
          </div>
        </div>
        <div class="col-auto">
          <button type="submit" class="btn btn-primary" id="addButton${taskStat.index}" name="submitButton" value="A-${taskStat.index}">
            <i class="bi bi-plus"></i>
          </button>
        </div>
        <div class="col-auto">
          <button type="submit" class="btn btn-outline-danger" id="delButton${taskStat.index}" name="submitButton" value="D-${taskStat.index}">
            <i class="bi bi-trash"></i>
          </button>
        </div>
      </div>
    </c:forEach>
    <div class="row">
      <div class="col"></div>
      <div class="col-auto">
        <a class="btn btn-outline-secondary" href="GoAdminHome">
          Cancel
        </a>
      </div>
      <div class="col-auto">
        <a class="btn btn-outline-danger" href="GoTicketTemplate">
          Reset
        </a>
      </div>
      <div class="col-auto">
        <button type="submit" name="submitButton" value="S-submit" class="btn btn-primary">Create</button>
      </div>
    </div>
  </form>
</div>
</body>
</html>
