<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="TaskDetailView">
  <c:set var="lastId" value="0"></c:set>
  <c:forEach var="task" items="${sessionScope.PspTaskList}">
    <c:if test="${task.getId()!=lastId}">
      <div class="row mb-1 mt-0">
        <div class="col">
          <div class="input-group input-group-sm">
            <c:choose>
              <c:when test="${sessionScope.currentTask.getId()==task.getId()}">
                <button type="button" class="btn btn-danger" disabled name="taskSelectButton" id="btnTask${task.getId()}" value="${task.getId()}">
                  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                </button>
                <c:set var="currentStyle" value="form-control text-danger fw-bold"></c:set>
              </c:when>
              <c:otherwise>
                <button type="submit" class="btn btn-secondary" name="taskSelectButton" id="btnTask${task.getId()}" value="${task.getId()}">
                  View
                </button>
                <c:set var="currentStyle" value="form-control"></c:set>
              </c:otherwise>
            </c:choose>
            <div class="${currentStyle}">
              <c:choose>
                <c:when test="${task.getWebLinkList().size()==0}"><%-- **************************************** NO WEBLINKS TIED TO TASK --%>
                  ${task.getDescription()}
                </c:when>
                <c:when test="${task.getWebLinkList().size()==1}"><%-- **************************************** ONLY 1 LINK TIED TO TASK --%>
                  <c:choose>
                    <c:when test="${task.getWebLinkList().get(0).getLinkType().getId()==2}"><%-- --------link ia a hyperlink ------------- --%>
                      <a href="${task.getWebLinkList().get(0).getLinkPath()}" target="_blank">${task.getDescription()} (Link)</a>
                    </c:when>
                    <c:otherwise><%-- -------------------------------------------------------------------link is a file upload ------------ --%>
                      <a href="ViewFileUpload?doc=${task.getWebLinkList().get(0).getLinkPath()}" target="_blank">${task.getDescription()} (File)</a>
                    </c:otherwise>
                  </c:choose>
                </c:when>
                <c:otherwise><%-- ******************************************************************************** MULITPLE LINKS TIED TO TASK --%>
                  <a href="#" target="_blank">${task.getDescription()} (2+)</a>
                </c:otherwise>
              </c:choose>
            </div>
          </div>
        </div>
      </div>
    </c:if>
    <c:set var="lastId" value="${task.getId()}"></c:set>

  </c:forEach>
</form>


