<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="isPast" value="pe-none"/>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete() == false}">
    <c:set var="isPast" value=""/>
</c:if>
<c:set var="contacts" value="${sessionScope.local.getCurrentActivity().getAdditionalContacts()}"/>
<div class="card border-0 border-start border-3 mt-2 mb-2" style="border-color: var(--ssa) !important;">
    <div class="card-body py-2 px-3">
        <div class="d-flex align-items-center justify-content-between mb-1">
      <span class="fw-semibold" style="color: var(--ssa); font-size: 0.85rem;">
        <i class="bi bi-people me-1"></i>Additional Contacts
      </span>
            <button type="button" class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 ${isPast}"
                    data-bs-target="#addContactToActivity" data-bs-toggle="modal" title="Add contact">
                <i class="bi bi-plus-circle" style="font-size: 0.85rem;"></i>
            </button>
        </div>
        <c:choose>
            <c:when test="${contacts == null || contacts.size() == 0}">
                <div class="text-muted fst-italic" style="font-size: 0.82rem;">No additional contacts.</div>
            </c:when>
            <c:otherwise>
                <div class="overflow-auto" style="max-height: 100px;">
                    <c:forEach var="contact" items="${contacts}">
                        <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.85rem;">
              <span class="fw-semibold text-dark text-capitalize flex-grow-1">
                ${contact.getFirstName()} ${contact.getLastName()}
              </span>
                            <c:if test="${contact.getEmail() != null && !contact.getEmail().equals('')}">
                                <a class="text-muted text-decoration-none me-2" style="font-size: 0.75rem;"
                                   href="ViewEmailHistory?em=${contact.getEmail()}" target="_blank">
                                    <i class="bi bi-envelope me-1"></i>${contact.getEmail().toLowerCase()}
                                </a>
                            </c:if>
                            <form action="RemoveContact25" method="post" class="m-0 p-0">
                                <input type="hidden" value="${contact.getId()}" name="contactIdToRemove">
                                <button type="submit" class="btn btn-sm text-danger p-0 ${isPast}" title="Remove">
                                    <i class="bi bi-x-circle" style="font-size: 0.75rem;"></i>
                                </button>
                            </form>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>
<c:import url="/WEB-INF/view/a/activityDetail/columns/modals/addContactToActivity25.jsp"></c:import>