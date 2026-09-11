<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%-- Summit setup panel (S41-G). Replaces the Census Upload card and the Summit export block formerly
     inline in detailSetup25.jsp. Carries its own PSP-admin gate, a verbatim copy of the caller's, so it
     cannot leak if included elsewhere. Live controls are the pre-existing URLs, copied verbatim. Dashed
     controls are placeholders ("Not built yet"). Preview is a placeholder because every download writes
     a summit_file_export row. --%>
<c:if test="${sessionScope.local.isPspAdmin()
              and not empty sessionScope.local.getCurrentActivity().getActivity().getApplication()
              and not empty sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal()}">
  <div class="detail-section-card">
    <div class="detail-section-header">
      <i class="bi bi-diagram-3"></i>
      Summit setup
      <span class="section-end">
        <button class="btn btn-sm btn-outline-ssa border-0 p-0 px-1" type="button"
                data-bs-toggle="collapse" data-bs-target="#summitSetupBody"
                aria-expanded="true" aria-controls="summitSetupBody" title="Collapse or expand">
          <i class="bi bi-chevron-down" style="font-size: 0.85rem;"></i>
        </button>
      </span>
    </div>
    <div id="summitSetupBody" class="collapse show">
      <div class="detail-section-body" style="font-size: 0.85rem;">

        <%-- 1. Employer --%>
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <span class="badge rounded-pill text-bg-light border" style="min-width: 1.5rem;">1</span>
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Employer</div>
            <div class="text-muted" style="font-size: 0.72rem;">File 1 · creates or updates the employer</div>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="employer"/></jsp:include>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Preview — not built yet"><i class="bi bi-eye"></i></button>
            <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=employer" class="btn btn-outline-ssa" title="Download file 1"><i class="bi bi-download"></i></a>
            <button type="submit" form="summitPush-employer" class="btn btn-outline-ssa" title="Push to DataPath" data-summit-push="employer" onclick="return confirm('Push the Employer file to DataPath? Summit processes it automatically within about 15 minutes. There is no undo.');"><i class="bi bi-cloud-upload"></i></button>
            <a href="${pageContext.request.contextPath}/SummitResponse?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&step=employer" class="btn btn-outline-ssa" title="Check response"><i class="bi bi-arrow-repeat"></i></a>
            <button type="submit" form="summitDone-employer" class="btn btn-outline-ssa" title="Mark done (manual)" onclick="return confirm('Mark Employer done without a response review? Use this for a group already set up in Summit or entered by hand.');"><i class="bi bi-check2-circle"></i></button>
          </div>
        </div>

        <%-- 2. Plans (CDH), with the contribution-schedule checkpoint --%>
        <div class="d-flex align-items-center gap-2 py-1">
          <span class="badge rounded-pill text-bg-light border" style="min-width: 1.5rem;">2</span>
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Plans (CDH)</div>
            <div class="text-muted" style="font-size: 0.72rem;">File 2 · CDH plans elected on the application</div>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="cdhplan"/></jsp:include>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Preview — not built yet"><i class="bi bi-eye"></i></button>
            <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=cdhplan" class="btn btn-outline-ssa" title="Download file 2"><i class="bi bi-download"></i></a>
            <button type="submit" form="summitPush-cdhplan" class="btn btn-outline-ssa" title="Push to DataPath" data-summit-push="cdhplan" onclick="return confirm('Push the Plans (CDH) file to DataPath? Summit processes it automatically within about 15 minutes. There is no undo.');"><i class="bi bi-cloud-upload"></i></button>
            <a href="${pageContext.request.contextPath}/SummitResponse?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&step=cdhplan" class="btn btn-outline-ssa" title="Check response"><i class="bi bi-arrow-repeat"></i></a>
            <button type="submit" form="summitDone-cdhplan" class="btn btn-outline-ssa" title="Mark done (manual)" onclick="return confirm('Mark Plans (CDH) done without a response review? Use this for a group already set up in Summit or entered by hand.');"><i class="bi bi-check2-circle"></i></button>
          </div>
        </div>
        <div class="d-flex align-items-center gap-2 pb-1 border-bottom" style="padding-left: 2rem;">
          <div class="flex-grow-1 lh-sm">
            <div>Contribution schedules</div>
            <div class="text-muted" style="font-size: 0.72rem;">Created by hand in Summit before any election file</div>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="submit" form="summitDone-schedules" class="btn btn-outline-ssa" title="Mark done (manual)" onclick="return confirm('Mark Contribution schedules done without a response review? Use this for a group already set up in Summit or entered by hand.');"><i class="bi bi-check2-circle"></i></button>
          </div>
        </div>

        <%-- 3. Request census --%>
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <span class="badge rounded-pill text-bg-light border" style="min-width: 1.5rem;">3</span>
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Request census</div>
            <div class="text-muted" style="font-size: 0.72rem;">Secure link for the client · upload held for review</div>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Send census request — not built yet"><i class="bi bi-envelope"></i></button>
          </div>
        </div>

        <%-- 4. Census --%>
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <span class="badge rounded-pill text-bg-light border" style="min-width: 1.5rem;">4</span>
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Census</div>
            <div class="text-muted" style="font-size: 0.72rem;">Upload and review the roster</div>
          </div>
          <div class="btn-group btn-group-sm">
            <a href="${pageContext.request.contextPath}/CensusUpload?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}" class="btn btn-outline-ssa" title="Census upload"><i class="bi bi-people"></i></a>
          </div>
        </div>

        <%-- 5. Demographics --%>
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <span class="badge rounded-pill text-bg-light border" style="min-width: 1.5rem;">5</span>
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Demographics</div>
            <div class="text-muted" style="font-size: 0.72rem;">File 4 · creates or updates participants</div>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="demographics"/></jsp:include>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Preview — not built yet"><i class="bi bi-eye"></i></button>
            <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=demographics" class="btn btn-outline-ssa" title="Download file 4"><i class="bi bi-download"></i></a>
            <button type="submit" form="summitPush-demographics" class="btn btn-outline-ssa" title="Push to DataPath" data-summit-push="demographics" onclick="return confirm('Push the Demographics file to DataPath? Summit processes it automatically within about 15 minutes. There is no undo.');"><i class="bi bi-cloud-upload"></i></button>
            <a href="${pageContext.request.contextPath}/SummitResponse?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&step=demographics" class="btn btn-outline-ssa" title="Check response"><i class="bi bi-arrow-repeat"></i></a>
            <button type="submit" form="summitDone-demographics" class="btn btn-outline-ssa" title="Mark done (manual)" onclick="return confirm('Mark Demographics done without a response review? Use this for a group already set up in Summit or entered by hand.');"><i class="bi bi-check2-circle"></i></button>
          </div>
        </div>

        <%-- 6. Enrollment: one row per import template --%>
        <div class="d-flex align-items-center gap-2 pt-1">
          <span class="badge rounded-pill text-bg-light border" style="min-width: 1.5rem;">6</span>
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Enrollment</div>
          </div>
        </div>
        <div class="d-flex align-items-center gap-2 py-1" style="padding-left: 2rem;">
          <div class="flex-grow-1 lh-sm">
            <div>125 PI Elections</div>
            <div class="text-muted" style="font-size: 0.72rem;">Section 125 plans: premium, FSA, DCA, PRA</div>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Preview — not built yet"><i class="bi bi-eye"></i></button>
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Download — not built yet"><i class="bi bi-download"></i></button>
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Push to DataPath — not built yet"><i class="bi bi-cloud-upload"></i></button>
          </div>
        </div>
        <div class="d-flex align-items-center gap-2 py-1" style="padding-left: 2rem;">
          <div class="flex-grow-1 lh-sm">
            <div>HRA Enrollment</div>
            <div class="text-muted" style="font-size: 0.72rem;">HRA plans: ICHRA, QSEHRA, HRA, MERP · requires T201 confirm</div>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Preview — not built yet"><i class="bi bi-eye"></i></button>
            <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=enrollment" class="btn btn-outline-ssa" title="Download HRA Enrollment (refuses without T201 confirm)"><i class="bi bi-download"></i></a>
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Push to DataPath — not built yet"><i class="bi bi-cloud-upload"></i></button>
          </div>
        </div>

      </div>
    </div>

    <%-- S42-B -- hidden push forms, one per pushable type. The visible push buttons above use
         form="summitPush-{type}" to submit these; each carries no visible content of its own. --%>
    <form id="summitPush-employer" method="post" target="_blank"
          action="${pageContext.request.contextPath}/SummitExport">
      <input type="hidden" name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
      <input type="hidden" name="type" value="employer"/>
    </form>
    <form id="summitPush-cdhplan" method="post" target="_blank"
          action="${pageContext.request.contextPath}/SummitExport">
      <input type="hidden" name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
      <input type="hidden" name="type" value="cdhplan"/>
    </form>
    <form id="summitPush-demographics" method="post" target="_blank"
          action="${pageContext.request.contextPath}/SummitExport">
      <input type="hidden" name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
      <input type="hidden" name="type" value="demographics"/>
    </form>

    <%-- S45-B -- hidden manual "Mark done" forms, one per T230 phase-1 step. The visible dashed
         Mark done buttons above use form="summitDone-{step}" to submit these. Each records basis
         MANUAL with no exportId -- the panel itself never reviews a response; that happens on
         /SummitResponse, reached via the "Check response" links above. --%>
    <form id="summitDone-employer" method="post" target="_blank"
          action="${pageContext.request.contextPath}/SummitResponse">
      <input type="hidden" name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
      <input type="hidden" name="step" value="employer"/>
      <input type="hidden" name="action" value="markdone"/>
    </form>
    <form id="summitDone-cdhplan" method="post" target="_blank"
          action="${pageContext.request.contextPath}/SummitResponse">
      <input type="hidden" name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
      <input type="hidden" name="step" value="cdhplan"/>
      <input type="hidden" name="action" value="markdone"/>
    </form>
    <form id="summitDone-schedules" method="post" target="_blank"
          action="${pageContext.request.contextPath}/SummitResponse">
      <input type="hidden" name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
      <input type="hidden" name="step" value="schedules"/>
      <input type="hidden" name="action" value="markdone"/>
    </form>
    <form id="summitDone-demographics" method="post" target="_blank"
          action="${pageContext.request.contextPath}/SummitResponse">
      <input type="hidden" name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
      <input type="hidden" name="step" value="demographics"/>
      <input type="hidden" name="action" value="markdone"/>
    </form>
  </div>
</c:if>
