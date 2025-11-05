<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${sessionScope.recipientList.size()==0}">
    <div class="col-auto">
        <div class="form-control form-control-sm pe-none">
            EMPTY
        </div>
    </div>
</c:if>
<c:forEach var="recipient" items="${sessionScope.recipientList}">
    <div class="col-auto">
        <div class="input-group input-group-sm">
                <span class="input-group-text">
                  ${recipient.getFirstName()} ${recipient.getLastName()} (${recipient.getEmail()})
                </span>
            <button type="submit" class="btn btn-danger" name="btnSubmit" id="btnT${recipient.getId()}" value="RT-${recipient.getId()}">X</button>
        </div>
    </div>
</c:forEach>
<div class="col"></div>

