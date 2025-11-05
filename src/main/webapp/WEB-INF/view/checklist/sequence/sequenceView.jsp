<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="UpdateSequenceName" >
  <div class="row mb-3">
    <div class="col">
      <div class="input-group">
        <input type="text" class="form-control text-primary fw-bold" name="sequenceName" required value="${currentTaskSequence.getDescription()}">
        <select class="form-select" aria-label="recurring freq type drop down" name="templatePurposeList" id="templatePurposeList">
          <c:forEach var="templatePurpose" items="${sessionScope.templatePurposeList}">
            <c:choose>
              <c:when test="${templatePurpose.getId()==currentTaskSequence.getTemplatePurpose().getId()}">
                <option value="${templatePurpose.getId()}" selected>(${templatePurpose.getTemplateGroup().getDescription()}) ${templatePurpose.getDescription()}</option>
              </c:when>
              <c:otherwise>
                <option value="${templatePurpose.getId()}">(${templatePurpose.getTemplateGroup().getDescription()}) ${templatePurpose.getDescription()}</option>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </select>
        <button type="submit" class="btn btn-primary col-2" name="submitButton" value="0">Update</button>
      </div>
    </div>
  </div>

</form>
