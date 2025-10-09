<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:forEach var="contact" items="${sessionScope.otherContactList}">
  <div class="row mt-1 pt-0 me-1">
    <div class="col-auto">
      <i class="bi bi-arrow-right"></i>
    </div>
    <div class="col">
      <span class="text-altSsa fw-bolder text-uppercase">${contact.getFirstName().toUpperCase()} ${contact.getLastName().toUpperCase()}</span>
    </div>
    <div class="col-auto">
      <c:choose>
        <c:when test="${contact.getEmail()==null}">
          &nbsp;
        </c:when>
        <c:otherwise>
          <a class="text-altSsa text-decoration-none" href="ViewEmailHistory?em=${contact.getEmail()}" target="_blank">
              ${contact.getEmail().toLowerCase()}
          </a>
        </c:otherwise>
      </c:choose>
    </div>
    <div class="col-auto p-0">
      <form action="RemoveContactFromOther" method="post">
        <input type="text" hidden value="${contact.getId()}" name="contactIdToRemove" id="contactIdToRemove-${contact.getId()}">
        <button type="submit" class="btn btn-sm btn-outline-danger border-0 p-0">
          <i class="bi bi-trash"></i>
        </button>
      </form>
    </div>
  </div>
</c:forEach>


