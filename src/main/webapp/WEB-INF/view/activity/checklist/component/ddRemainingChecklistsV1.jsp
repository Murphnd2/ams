<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<select class="form-select" aria-label="recurring freq type drop down" name="btnCheckList" id="btnCheckList">
    <c:choose>
        <c:when test="${sessionScope.myFutureItems.size()==0}">
            <option value="-1">NO FUTURE CHECKLISTS RIGHT NOW</option>
        </c:when>
        <c:otherwise>
            <c:forEach var="checklist" items="${sessionScope.myFutureItems}">
                <c:choose>
                    <c:when test="${checklist.getId()==sessionScope.currentChecklist.getId()}">
                        <option value="${checklist.getId()}" selected>${checklist.getName()} (<fmt:formatDate value="${checklist.getDueDate()}" pattern="MM/dd/yy"></fmt:formatDate>)</option>
                    </c:when>
                    <c:otherwise>
                        <option value="${checklist.getId()}">${checklist.getName()} (<fmt:formatDate value="${checklist.getDueDate()}" pattern="MM/dd/yy"></fmt:formatDate>)</option>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</select>
