<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="modal fade" id="viewPastActivity" role="dialog" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-sm modal-fullscreen-sm-down" role="document">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold">
          <i class="bi bi-archive me-2"></i>Past ${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName()}s
        </h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body p-2">
        <c:set var="hasPast" value="false"/>
        <c:forEach var="act" items="${sessionScope.local.getCurrentActivity().getPastActivities()}">
          <c:if test="${!act.getId().equals(sessionScope.local.getCurrentActivity().getActivity().getId())}">
            <c:set var="hasPast" value="true"/>
            <form action="ViewPastActivity25" method="post" class="m-0">
              <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.82rem;">
                <c:choose>
                  <c:when test="${act.isComplete()}">
                    <span class="badge bg-secondary me-2" style="font-size: 0.68rem;">Closed</span>
                  </c:when>
                  <c:otherwise>
                    <span class="badge bg-success me-2" style="font-size: 0.68rem;">Open</span>
                  </c:otherwise>
                </c:choose>
                <span class="text-muted flex-grow-1" style="font-size: 0.78rem;">
                  <fmt:formatDate value="${act.getDateCreated()}" pattern="MM/dd/yyyy"/>
                </span>
                <button type="submit" class="btn btn-sm btn-outline-ssa border-0 p-0 px-1"
                        name="pastActivityId" value="${act.getId()}" title="View">
                  <i class="bi bi-box-arrow-up-right" style="font-size: 0.78rem;"></i>
                </button>
              </div>
            </form>
          </c:if>
        </c:forEach>
        <c:if test="${hasPast == 'false'}">
          <div class="text-muted fst-italic text-center py-2" style="font-size: 0.82rem;">No past activities.</div>
        </c:if>
      </div>
    </div>
  </div>
</div>