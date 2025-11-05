<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row">
  <div class="col">
    <form action="AddContactToActivity" method="post">
      <div class="row">
        <div class="col">
          <h5>Add By Email</h5>
        </div>
      </div>
      <div class="row mb-3">
        <div class="col">
          <div class="input-group">
            <span class="input-group-text">Email</span>
            <input type="email" class="form-control" name="contactEmail">
            <button type="submit" value="1" name="btnAddContact" id="btnAddContact-1" class="btn btn-outline-secondary">Add</button>
          </div>
        </div>
      </div>
      <c:if test="${(sessionScope.adminView==2 || sessionScope.adminView==3) && sessionScope.currentActivityEmployees.size()>0}">
        <div class="row">
          <div class="col">
            <h5>Add Current Employee</h5>
          </div>
        </div>
        <div class="row mb-3">
          <div class="col">
            <div class="input-group">
              <span class="input-group-text">Select Employee</span>
              <select class="form-select" name="addEmployeeList" id="addEmployeeList">
                <c:forEach var="employee" items="${sessionScope.currentActivityEmployees}">
                  <option value="${employee.getId()}">${employee.getLastName()}, ${employee.getFirstName()}</option>
                </c:forEach>
              </select>
              <button type="submit" class="btn btn-outline-secondary" value="2" name="btnAddContact" id="btnAddContact-2">Add</button>
            </div>
          </div>
        </div>
      </c:if>
      <div class="row">
        <div class="col">
          <input type="checkbox" class="form-check-input" name="makePrimaryCheck" id="makePrimaryCheck" value="1">
          <label type="text" class="form-check-label">Make This Contact Primary?</label>
        </div>
      </div>
    </form>
  </div>
</div>
