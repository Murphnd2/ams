<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<form method="post" action="RemoveLinkFromTask">
    <c:if test="${sessionScope.linkList.size()>0}">
        <h5>Associated Web/Document Links</h5>
    </c:if>
    <c:forEach var="link" items="${sessionScope.linkList}">
        <div class="row mb-1 mt-0">
            <div class="col">
                <div class="input-group">
                    <span class="input-group-text">
                        <c:choose>
                            <c:when test="${link.getLinkType().getId()==2}">Website</c:when>
                            <c:when test="${link.getLinkType().getId()==1}">Document</c:when>
                            <c:otherwise>Unknown</c:otherwise>
                        </c:choose>
                    </span>
                    <div class="form-control">
                        <c:choose>
                            <c:when test="${link.getLinkType().getId()==2}">
                                <a href="${link.getLinkPath()}" target="_blank">${link.getPlainText()}</a>
                            </c:when>
                            <c:when test="${link.getLinkType().getId()==1}">
                                <c:set var="fullPath" value="ViewFileUpload?doc=${link.getLinkPath()}"></c:set>
                                <a href="${fullPath}" target="_blank">${link.getPlainText()}</a>
                            </c:when>
                            <c:otherwise>
                                ${link.getPlainText()}
                            </c:otherwise>
                        </c:choose>

                    </div>
                </div>
            </div>
            <div class="col-lg-3">
                <button type="submit" class="btn btn-danger w-100" name="linkSelectButton" id="btnLink${link.getId()}" value="${link.getId()}">
                    Remove Link
                </button>
            </div>
        </div>
    </c:forEach>
</form>
