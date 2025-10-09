<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="penone" value=""></c:set>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==true}">
  <c:set var="penone" value="pe-none"></c:set>
</c:if>
<c:forEach var="contact" items="${sessionScope.local.getCurrentActivity().getAdditionalContacts()}">
  <div class="row mt-1 pt-0 me-1">
    <div class="col-auto">
      <i class="bi bi-arrow-right"></i>
    </div>
    <c:choose>
      <c:when test="${contact.getEmployee()!=null && contact.getEmployee().getEmail()!=null && !contact.getEmployee().getEmail().equals(\"\")}">
        <div class="col">
          <span class="text-altSsa fw-bolder text-uppercase">${contact.getEmployee().getFirstName().toUpperCase()} ${contact.getEmployee().getLastName().toUpperCase()}</span>
        </div>
        <div class="col-auto">
          <a class="text-altSsa text-decoration-none" href="ViewEmailHistory?em=${contact.getEmployee().getEmail()}" target="_blank">
              ${contact.getEmployee().getEmail().toLowerCase()}
          </a>
        </div>
      </c:when>
      <c:when test="${contact.getEmail()!=null && !contact.getEmail().equals(\"\")}">
        <div class="col">
          <span class="text-altSsa fw-bolder text-uppercase">${contact.getFirstName().toUpperCase()} ${contact.getLastName().toUpperCase()}</span>
        </div>
        <div class="col-auto">
          <a class="text-altSsa text-decoration-none" href="ViewEmailHistory?em=${contact.getEmail()}" target="_blank">
              ${contact.getEmail().toLowerCase()}
          </a>
        </div>
      </c:when>
      <c:otherwise>
        <div class="col">
          <span class="text-altSsa fw-bolder text-uppercase">${contact.getFirstName().toUpperCase()} ${contact.getLastName().toUpperCase()}</span>
        </div>
      </c:otherwise>
    </c:choose>

    <div class="col-auto p-0">
      <form action="RemoveContact25" method="post">
        <input type="text" hidden value="${contact.getId()}" name="contactIdToRemove" id="contactIdToRemove-${contact.getId()}">
        <button type="submit" class="btn btn-sm btn-outline-danger border-0 p-0 ${penone}">
          <i class="bi bi-trash"></i>
        </button>
      </form>
    </div>
  </div>
</c:forEach>
