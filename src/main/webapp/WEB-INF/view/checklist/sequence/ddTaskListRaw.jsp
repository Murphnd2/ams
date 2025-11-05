<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<select class="form-select fst-italic" aria-label="recurring freq type drop down" name="taskListDropDown" id="taskListDropDown">
    <c:set var="lastId" value="0"></c:set>
    <c:forEach var="task" items="${sessionScope.availableTasks}">
        <c:if test="${task.getId()!=lastId}">
            <option value="${task.getId()}">${task.getDescription()}</option>
        </c:if>
        <c:set var="lastId" value="${task.getId()}"></c:set>
    </c:forEach>
</select>
