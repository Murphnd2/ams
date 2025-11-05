<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<input class="form-control" list="datalistOptions" id="employeeList" name="employeeList" required autocomplete="off"  placeholder="Enter name last, first to search or create . . .">
<datalist id="datalistOptions">
    <c:if test="${applicationScope.employeeList.size()==0}">
        <option value="NO EMPLOYEES LOADED [] (0)"></option>
    </c:if>
    <c:forEach var="employee" items="${applicationScope.employeeList}">
        <option value="${employee.getLastName()}, ${employee.getFirstName()} [${employee.getEr()}] (${employee.getId()})"></option>
    </c:forEach>
</datalist>
