<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select m-0" aria-label="recurring freq type drop down" name="userList" id="userList">
    <c:forEach var="user" items="${sessionScope.pspUserList}">
        <option value="${user.getPerson().getId()}">${user.getPerson().getFullName()}</option>
    </c:forEach>
</select>
