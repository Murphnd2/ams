<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
            <button type="submit" class="btn btn-danger" name="btnSubmit" id="btnA${attachment.getId()}" value="RA-${attachment.getId()}">X</button>
        </div>
    </div>
</c:forEach>
<div class="col"></div>
