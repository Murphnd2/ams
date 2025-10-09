<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="CreateAutoCategory">
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Select Category</span>
        <select class="form-select mb-3" aria-label="contactMethodList type drop down" name="tCategory" id="category">
          <option value="-1">SELECT A CATEGORY</option>
          <c:forEach var="category" items="${sessionScope.ticketCategories}">
            <option value="${category.getId()}">${category.getDescription()}</option>
          </c:forEach>
        </select>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text">Description</span>
        <input type="text" name="automationName" class="form-control" required>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
    </div>
    <div class="col-auto">
      <a href="SequenceHome" class="btn btn-outline-secondary">Cancel</a>
    </div>
    <div class="col-auto">
      <button type="submit" class="btn btn-primary">Submit</button>
    </div>
  </div>
</form>
