<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:choose>
  <c:when test="${empty sessionScope.local.getCurrentActivity().getNotes()}">
    <div class="text-muted fst-italic text-center py-3" style="font-size: 0.82rem;">No notes yet.</div>
  </c:when>
  <c:otherwise>
    <c:forEach var="note" items="${sessionScope.local.getCurrentActivity().getNotes()}">
      <div class="border-bottom py-2 px-2">
        <div class="d-flex align-items-center justify-content-between" style="font-size: 0.78rem;">
          <span class="fw-semibold" style="color: var(--ssa);">
            <c:choose>
              <c:when test="${note.getClass().getSimpleName() == 'Email'}">
                <a href="ViewEmail?id=${note.getId()}" target="_blank" class="text-decoration-none" style="color: var(--ssa);">
                  <i class="bi bi-envelope me-1"></i>${note.getReasonCreated().getDescription()}
                </a>
              </c:when>
              <c:otherwise>
                ${note.getReasonCreated().getDescription()}
              </c:otherwise>
            </c:choose>
          </span>
          <span class="badge bg-light text-dark border" style="font-size: 0.68rem;">
              ${note.getStatus().getDescription()}
          </span>
        </div>
        <div class="mt-1" style="font-size: 0.8rem;">
            ${note.getDetail()}
        </div>
        <c:if test="${not empty note.getWebLinkList()}">
          <div class="mt-1" style="font-size: 0.78rem;">
            <i class="bi bi-paperclip me-1 text-muted"></i>
            <c:forEach var="att" items="${note.getWebLinkList()}">
              <c:if test="${att.isActive()}">
                <c:choose>
                  <c:when test="${att.getLinkType().getId() == 1}">
                    <a href="${pageContext.request.contextPath}/ShowFileUpload?doc=${att.getLinkPath()}" target="_blank"
                       class="me-2 text-decoration-none" style="color: var(--ssa);">
                      <i class="bi bi-file-earmark-arrow-down me-1"></i>${att.getPlainText()}
                    </a>
                  </c:when>
                  <c:when test="${att.getLinkType().getId() == 2}">
                    <a href="${att.getLinkPath()}" target="_blank"
                       class="me-2 text-decoration-none" style="color: var(--ssa);">
                      <i class="bi bi-link-45deg me-1"></i>${att.getPlainText()}
                    </a>
                  </c:when>
                </c:choose>
              </c:if>
            </c:forEach>
          </div>
        </c:if>
        <div class="text-muted fst-italic mt-1" style="font-size: 0.68rem;">
            ${note.getCreatedBy().getFullName()} &mdash;
          <c:choose>
            <c:when test="${note.getDateCreated() != null}">
              <fmt:formatDate value="${note.getDateCreated()}" pattern="MM/dd/yy @ hh:mm aa"/>
            </c:when>
            <c:otherwise>
              <fmt:formatDate value="${note.getDateGenerated()}" pattern="MM/dd/yy"/>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </c:forEach>
  </c:otherwise>
</c:choose>
