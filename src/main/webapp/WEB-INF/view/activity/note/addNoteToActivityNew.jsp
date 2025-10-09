<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="row m-1">
  <div class="col">
    <div class="row">
      <div class="col">
        <span class="text-danger fw-bolder fs-5">Add Notes To ${sessionScope.currentActivity.getClass().getSimpleName()}</span>
      </div>

      <div class="col-auto"></div>
    </div>
    <form method="post" action="AddNoteToActivity">
      <div class="row mb-0 pb-0">
        <div class="col mb-0 pb-0">
          <div class="input-group input-group-sm w-100 rounded-0 rounded-top">
            <textarea class="form-control rounded-0 rounded-top" name="noteText" required rows="6" type="text" placeholder="Add text here for the note"></textarea>
          </div>
        </div>
      </div>
      <c:set var="isVis1" value=""> </c:set>
      <c:if test="${sessionScope.adminView!=4}">
        <c:set var="isVis1" value="d-none"> </c:set>
      </c:if>
      <div class="row mt-1">
        <div class="col mt-0 pt-0">

            <c:choose>
              <c:when test="${sessionScope.adminView!=4}">
                <div class="input-group input-group-sm rounded-0 d rounded-bottom">
                  <span class="input-group-text d-none d-sm-inline d-md-none d-lg-inline">Reason</span>
                  <c:import url="/WEB-INF/view/activity/note/components/ddReasons.jsp"> </c:import>
                  <span class="input-group-text d-none d-sm-inline d-md-none d-lg-inline">Status</span>
                  <c:import url="/WEB-INF/view/activity/note/components/ddNoteStatus.jsp"> </c:import>
                  <button type="submit" name="btnAddNote1" value="Save" class="btn btn-danger">
                    <i class="bi bi-journal-plus"></i> Save
                  </button>
                </div>
              </c:when>
              <c:otherwise>
                <div class="d-none">
                  <span class="input-group-text d-none d-sm-inline d-md-none d-lg-inline">Reason</span>
                  <c:import url="/WEB-INF/view/activity/note/components/ddReasons.jsp"> </c:import>
                  <span class="input-group-text d-none d-sm-inline d-md-none d-lg-inline">Status</span>
                  <c:import url="/WEB-INF/view/activity/note/components/ddNoteStatus.jsp"> </c:import>
                </div>
                <button type="submit" name="btnAddNote1" value="Save" class="btn btn-sm btn-danger w-100">
                  <i class="bi bi-journal-plus"></i> Save
                </button>
              </c:otherwise>
            </c:choose>

        </div>
      </div>
    </form>
  </div>
</div>

