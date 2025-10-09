<%@ page import="net.superiorstate.ams.previous.data.misc.dbA" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
  String pathString = dbA.getWebPath() + "ShowFileUpload?doc=";
  request.getSession().setAttribute("webPath",pathString);
%>
<c:if test="${sessionScope.attachmentList.size()==0}">
  <div class="col-auto">
    <div class="form-control form-control-sm pe-none">
      EMPTY
    </div>
  </div>
</c:if>
<c:forEach var="attachment" items="${sessionScope.attachmentList}">
  <div class="col-auto">
    <div class="input-group input-group-sm">
                <span class="input-group-text">
                    ${attachment.getPlainText()}
                </span>
      <a class="btn btn-primary" href="${sessionScope.webPath}${attachment.getLinkPath()}" target="_blank">
        <i class="bi bi-download"></i>
      </a>
    </div>
  </div>
</c:forEach>
<div class="col"></div>
