<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="isPast" value="pe-none"/>
<c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete() == false}">
  <c:set var="isPast" value=""/>
</c:if>
<div class="card border-0 border-start border-3 mt-2 mb-2" style="border-color: var(--ssa-gray) !important;">
  <div class="card-body py-2 px-3">
    <div class="d-flex align-items-center justify-content-between mb-1">
      <span class="fw-semibold" style="color: var(--ssa); font-size: 0.85rem;">
        <i class="bi bi-folder2-open me-1"></i>Documents & Links
      </span>
      <div class="d-flex align-items-center gap-1">
        <button class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 d-none" id="btnExpandDocs"
                type="button" data-bs-toggle="modal" data-bs-target="#docsFullModal" title="View all">
          <i class="bi bi-arrows-fullscreen" style="font-size: 0.75rem;"></i>
        </button>
        <button type="button" class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 ${isPast}"
                data-bs-toggle="modal" data-bs-target="#addDocAct" title="Upload document">
          <i class="bi bi-file-earmark-arrow-up" style="font-size: 0.85rem;"></i>
        </button>
        <button type="button" class="btn btn-sm btn-outline-ssa border-0 p-0 px-1 ${isPast}"
                data-bs-toggle="modal" data-bs-target="#addUrlAct" title="Add link">
          <i class="bi bi-link-45deg" style="font-size: 0.85rem;"></i>
        </button>
      </div>
    </div>
    <c:choose>
      <c:when test="${empty sessionScope.local.getCurrentActivity().getActivity().getWebLinkList()}">
        <div class="text-muted fst-italic" style="font-size: 0.82rem;">No documents or links.</div>
      </c:when>
      <c:otherwise>
        <div class="overflow-auto" style="max-height: 120px;" id="docsContent">
          <c:forEach var="webLink" items="${sessionScope.local.getCurrentActivity().getActivity().getWebLinkList()}">
            <c:if test="${webLink.linkType.id == 1 || webLink.linkType.id == 2}">
              <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.82rem;">
                <c:choose>
                  <c:when test="${webLink.linkType.id == 1}">
                    <i class="bi bi-file-earmark-arrow-down me-2" style="color: var(--ssa);"></i>
                    <a class="text-decoration-none flex-grow-1 text-truncate" style="color: var(--ssa);"
                       href="${pageContext.request.contextPath}/ShowFileUpload?doc=${webLink.linkPath}" target="_blank">
                        ${webLink.plainText}
                    </a>
                  </c:when>
                  <c:when test="${webLink.linkType.id == 2}">
                    <i class="bi bi-link-45deg me-2" style="color: var(--ssa-alt);"></i>
                    <a class="text-decoration-none flex-grow-1 text-truncate" style="color: var(--ssa-alt);"
                       href="${webLink.linkPath}" target="_blank">
                        ${webLink.plainText}
                    </a>
                  </c:when>
                </c:choose>
              </div>
            </c:if>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<%-- Full list modal --%>
<div class="modal fade" id="docsFullModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-fullscreen-sm-down">
    <div class="modal-content">
      <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
        <h6 class="modal-title fw-semibold"><i class="bi bi-folder2-open me-2"></i>Documents & Links</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
      </div>
      <div class="modal-body">
        <c:forEach var="webLink" items="${sessionScope.local.getCurrentActivity().getActivity().getWebLinkList()}">
          <c:if test="${webLink.linkType.id == 1 || webLink.linkType.id == 2}">
            <div class="d-flex align-items-center py-1 border-bottom" style="font-size: 0.85rem;">
              <c:choose>
                <c:when test="${webLink.linkType.id == 1}">
                  <i class="bi bi-file-earmark-arrow-down me-2" style="color: var(--ssa);"></i>
                  <a class="text-decoration-none flex-grow-1" style="color: var(--ssa);"
                     href="${pageContext.request.contextPath}/ShowFileUpload?doc=${webLink.linkPath}" target="_blank">
                      ${webLink.plainText}
                  </a>
                </c:when>
                <c:when test="${webLink.linkType.id == 2}">
                  <i class="bi bi-link-45deg me-2" style="color: var(--ssa-alt);"></i>
                  <a class="text-decoration-none flex-grow-1" style="color: var(--ssa-alt);"
                     href="${webLink.linkPath}" target="_blank">
                      ${webLink.plainText}
                  </a>
                </c:when>
              </c:choose>
            </div>
          </c:if>
        </c:forEach>
      </div>
    </div>
  </div>
</div>

<%-- Add modals --%>
<c:import url="/WEB-INF/view/activity/addDocumentToActivityMod.jsp"></c:import>
<c:import url="/WEB-INF/view/activity/addUrlToActivityMod.jsp"></c:import>

<script>
  document.addEventListener('DOMContentLoaded', function() {
    var el = document.getElementById('docsContent');
    if (el && el.scrollHeight > el.clientHeight) {
      document.getElementById('btnExpandDocs').classList.remove('d-none');
    }
  });
</script>
