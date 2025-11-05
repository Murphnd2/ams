<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<script type="text/javascript">
  function showHide(){
    let eeList = document.getElementById('employeeList');
    let theInput = document.getElementById('pn1');
    if(eeList.value === "0"){
      theInput.style.display='block';
      theInput.required = true;
    } else {
      theInput.style.display='none';
      theInput.required = false;
    }
  }
</script>

<select class="form-select" oninput="showHide()" aria-label="recurring freq type drop down" name="employeeList" id="employeeList">
  <option value="-1" selected>S E L E C T&nbsp;&nbsp;&nbsp;P E R S O N</option>
  <option value="0" class="text-secondary fw-light fst-italic">E N T E R&nbsp;&nbsp;&nbsp;M A N U A L L Y</option>
  <c:forEach var="employee" items="${applicationScope.employeeList}">
    <option value="${employee.getId()}">
        ${employee.getLastName().toUpperCase().trim()}, ${employee.getFirstName().toUpperCase().trim()}--[${employee.getEr()}]
    </option>
  </c:forEach>
</select>
