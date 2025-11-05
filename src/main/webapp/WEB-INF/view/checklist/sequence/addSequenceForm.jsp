<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="AddSequence" >
  <div class="container">
    <div class="row mb-3">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text">Sequence Name&nbsp;&nbsp;</span>
          <input type="text" class="form-control" name="sequenceName" placeholder="Enter Sequence description here" required>
        </div>
      </div>
    </div>
    <div class="row mb-3">
      <div class="col">
        <div class="input-group">
          <span class="input-group-text">Sequence Purpose</span>
          <c:import url="/WEB-INF/view/checklist/components/ddTemplatePurposesRaw.jsp"></c:import>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <button type="submit" class="btn btn-success w-100" name="submitButton" value="0">Submit</button>
      </div>
    </div>
  </div>
</form>
