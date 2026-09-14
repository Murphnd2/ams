<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
        <%-- S59-P2 -- the section title is the Summit link when one is available. mode=url asks
             the same servlet/resolver/availability chain the status line below already uses for a
             bare URL instead of markup; on any failure it yields nothing and the title stays plain
             text. Two lookups per render (url + status) -- see technical_assumptions.md. --%>
        <c:import var="employerSummitUrl" url="/SummitEmployerLink">
          <c:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
          <c:param name="mode" value="url"/>
        </c:import>
        <c:set var="employerSummitUrl" value="${fn:trim(employerSummitUrl)}"/>
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <div class="flex-grow-1 lh-sm">
            <c:choose>
              <c:when test="${not empty employerSummitUrl}">
                <div class="fw-semibold"><a href="${employerSummitUrl}" target="_blank" rel="noopener">Employer <i class="bi bi-box-arrow-up-right" style="font-size: 0.7em;"></i></a><button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-employer" aria-expanded="false" aria-controls="summitHelp-employer" aria-label="Show details for Employer" title="Show details for Employer"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
              </c:when>
              <c:otherwise>
                <div class="fw-semibold">Employer<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-employer" aria-expanded="false" aria-controls="summitHelp-employer" aria-label="Show details for Employer" title="Show details for Employer"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
              </c:otherwise>
            </c:choose>
            <div class="collapse" id="summitHelp-employer">
            <div class="text-muted" style="font-size: 0.72rem;">File 1 · creates or updates the employer</div>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="employer"/></jsp:include>
            <jsp:include page="/SummitEmployerLink"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="mode" value="status"/></jsp:include>
            </div>
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
        <%-- S59-P2 -- same title-as-link treatment as Employer above, tab=BenefitPlans. The old
             "Open Benefit Plans ↗" anchor-only include (mode=null, tabMode) emitted nothing on any
             failure and only a bare anchor on success -- entirely superseded by the title link, so
             it is removed rather than kept alongside it. --%>
        <c:import var="plansSummitUrl" url="/SummitEmployerLink">
          <c:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
          <c:param name="tab" value="BenefitPlans"/>
          <c:param name="mode" value="url"/>
        </c:import>
        <c:set var="plansSummitUrl" value="${fn:trim(plansSummitUrl)}"/>
        <div class="d-flex align-items-center gap-2 py-1">
          <div class="flex-grow-1 lh-sm">
            <c:choose>
              <c:when test="${not empty plansSummitUrl}">
                <div class="fw-semibold"><a href="${plansSummitUrl}" target="_blank" rel="noopener">Plans (CDH) <i class="bi bi-box-arrow-up-right" style="font-size: 0.7em;"></i></a><button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-cdhplan" aria-expanded="false" aria-controls="summitHelp-cdhplan" aria-label="Show details for Plans (CDH)" title="Show details for Plans (CDH)"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
              </c:when>
              <c:otherwise>
                <div class="fw-semibold">Plans (CDH)<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-cdhplan" aria-expanded="false" aria-controls="summitHelp-cdhplan" aria-label="Show details for Plans (CDH)" title="Show details for Plans (CDH)"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
              </c:otherwise>
            </c:choose>
            <div class="collapse" id="summitHelp-cdhplan">
            <div class="text-muted" style="font-size: 0.72rem;">File 2 · CDH plans elected on the application</div>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="cdhplan"/></jsp:include>
            </div>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Preview — not built yet"><i class="bi bi-eye"></i></button>
            <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=cdhplan" class="btn btn-outline-ssa" title="Download file 2"><i class="bi bi-download"></i></a>
            <button type="submit" form="summitPush-cdhplan" class="btn btn-outline-ssa" title="Push to DataPath" data-summit-push="cdhplan" onclick="return confirm('Push the Plans (CDH) file to DataPath? Summit processes it automatically within about 15 minutes. There is no undo.');"><i class="bi bi-cloud-upload"></i></button>
            <a href="${pageContext.request.contextPath}/SummitResponse?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&step=cdhplan" class="btn btn-outline-ssa" title="Check response"><i class="bi bi-arrow-repeat"></i></a>
            <button type="submit" form="summitDone-cdhplan" class="btn btn-outline-ssa" title="Mark done (manual)" onclick="return confirm('Mark Plans (CDH) done without a response review? Use this for a group already set up in Summit or entered by hand.');"><i class="bi bi-check2-circle"></i></button>
          </div>
        </div>
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <div class="flex-grow-1 lh-sm">
            <div>Contribution schedules<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-contribsched" aria-expanded="false" aria-controls="summitHelp-contribsched" aria-label="Show details for Contribution schedules" title="Show details for Contribution schedules"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
            <div class="collapse" id="summitHelp-contribsched">
            <div class="text-muted" style="font-size: 0.72rem;">Created by hand in Summit before any election file</div>
            </div>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="submit" form="summitDone-schedules" class="btn btn-outline-ssa" title="Mark done (manual)" onclick="return confirm('Mark Contribution schedules done without a response review? Use this for a group already set up in Summit or entered by hand.');"><i class="bi bi-check2-circle"></i></button>
          </div>
        </div>

        <%-- 3. Census (S59-P6rev) -- merges the former Request census / Census / Demographics
             rows into one state-driven row. CensusLifecycleService returns one of eight tokens,
             gating which of the fixed-order controls below render (see the S59-P7 comment). The
             three description lines and both status includes are unconditional -- they always
             execute and always render inside the toggle, regardless of state. --%>
        <c:import var="censusState" url="/CensusLifecycle">
          <c:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
        </c:import>
        <c:set var="censusState" value="${fn:trim(censusState)}"/>
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Census<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-census" aria-expanded="false" aria-controls="summitHelp-census" aria-label="Show details for Census" title="Show details for Census"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
            <div class="collapse" id="summitHelp-census">
            <div class="text-muted" style="font-size: 0.72rem;">Secure link for the client · upload held for review</div>
            <div class="text-muted" style="font-size: 0.72rem;">Upload and review the roster</div>
            <div class="text-muted" style="font-size: 0.72rem;">File 4 · creates or updates participants</div>
            <jsp:include page="/CensusRequestStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/></jsp:include>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="demographics"/></jsp:include>
            </div>
          </div>
          <%-- S59-P7 -- fixed control sequence, each control carrying its own visibility
               condition, replacing the eight independent per-state lists above (each of which
               could drift, and three of which silently dropped the envelope). Order can no
               longer diverge because there is only one order; a control can vanish only if its
               own condition excludes it. Only the envelope's title varies, selected once here. --%>
          <c:choose>
            <c:when test="${censusState == 'AWAITING_CLIENT'}">
              <c:set var="censusEnvelopeTitle" value="Manage census request — revoke or renew"/>
            </c:when>
            <c:when test="${censusState == 'LINK_EXPIRED'}">
              <c:set var="censusEnvelopeTitle" value="Renew the census request"/>
            </c:when>
            <c:when test="${censusState == 'REVOKED'}">
              <c:set var="censusEnvelopeTitle" value="Request census again"/>
            </c:when>
            <c:when test="${censusState == 'AWAITING_REVIEW' or censusState == 'ROSTER_LOADED' or censusState == 'TERMINAL'}">
              <c:set var="censusEnvelopeTitle" value="Request an updated census"/>
            </c:when>
            <c:otherwise>
              <c:set var="censusEnvelopeTitle" value="Request census from client"/>
            </c:otherwise>
          </c:choose>
          <div class="btn-group btn-group-sm">
            <a href="${pageContext.request.contextPath}/CensusRequest?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}" class="btn btn-outline-ssa" title="${censusEnvelopeTitle}"><i class="bi bi-envelope"></i></a>
            <a href="${pageContext.request.contextPath}/CensusUpload?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}" class="btn btn-outline-ssa" title="Census upload"><i class="bi bi-people"></i></a>
            <c:if test="${censusState == 'ROSTER_LOADED' or censusState == 'INDETERMINATE'}">
              <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=demographics" class="btn btn-outline-ssa" title="Download file 4"><i class="bi bi-download"></i></a>
            </c:if>
            <c:if test="${censusState == 'ROSTER_LOADED' or censusState == 'TERMINAL' or censusState == 'INDETERMINATE'}">
              <button type="submit" form="summitPush-demographics" class="btn btn-outline-ssa" title="Push to DataPath" data-summit-push="demographics" onclick="return confirm('Push the Demographics file to DataPath? Summit processes it automatically within about 15 minutes. There is no undo.');"><i class="bi bi-cloud-upload"></i></button>
              <a href="${pageContext.request.contextPath}/SummitResponse?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&step=demographics" class="btn btn-outline-ssa" title="Check response"><i class="bi bi-arrow-repeat"></i></a>
            </c:if>
            <c:if test="${censusState == 'ROSTER_LOADED' or censusState == 'INDETERMINATE'}">
              <button type="submit" form="summitDone-demographics" class="btn btn-outline-ssa" title="Mark done (manual)" onclick="return confirm('Mark Demographics done without a response review? Use this for a group already set up in Summit or entered by hand.');"><i class="bi bi-check2-circle"></i></button>
            </c:if>
          </div>
        </div>

        <%-- V104 -- the $1 card-issuer seed election. No file number (T196's precedent): it is not
             one of the four core setup files, and the setup-sequence numbering above has no slot
             for it. Both controls always lead to a confirmation screen with an editable effective
             date (T201 shape, widened) -- the JSP itself never carries confirm or effectiveDate. --%>
        <%-- S59-P3rev -- the row renders only when a card-issuer template association is elected
             on this setup, or when that cannot be determined (fail open -- see
             CardIssuerAvailabilityService). mode=absent means nothing: this fragment has only one
             output shape, unlike the S59-P2 title-link fragments. --%>
        <%-- S59-P9 -- moved above the Enrollment line: a $1 seed election precedes real
             elections, so it belongs before them, not after. --%>
        <c:import var="cardIssuerAvailable" url="/CardIssuerAvailability">
          <c:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
        </c:import>
        <c:set var="cardIssuerAvailable" value="${fn:trim(cardIssuerAvailable)}"/>
        <c:if test="${not empty cardIssuerAvailable}">
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Card Issuance<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-cardseed" aria-expanded="false" aria-controls="summitHelp-cardseed" aria-label="Show details for Card Issuance" title="Show details for Card Issuance"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
            <div class="collapse" id="summitHelp-cardseed">
            <div class="text-muted" style="font-size: 0.72rem;">125 PI Elections · $1.00 annual to the Card Issuer plan · requires confirm</div>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="cardseed"/></jsp:include>
            </div>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Preview — not built yet"><i class="bi bi-eye"></i></button>
            <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=cardseed" class="btn btn-outline-ssa" title="Generate (opens a confirmation screen)"><i class="bi bi-download"></i></a>
            <button type="submit" form="summitPush-cardseed" class="btn btn-outline-ssa" title="Push to DataPath (opens a confirmation screen)"><i class="bi bi-cloud-upload"></i></button>
          </div>
        </div>
        </c:if>
        <%-- 6. Enrollment (S59-P8) -- consolidates 125 PI Elections and HRA Enrollment onto one
             line; they differ only by file-type parameter (type=elections/enrollment,
             step=elections/enrollment), so Download/Check response/Push each become a chooser
             offering both, rather than each file owning a row. Also relocates the Enrollment
             Matrix / Copy matrix link / Open agent view controls in from detailSetup25.jsp, now
             the first three elements on the line -- their prior isPspAdmin-or-isPspUser gate is
             dropped on arrival in favor of this panel's own local.isPspAdmin() gate (Kevin's
             decision: PSP user loses these three controls for now; opening the whole panel to
             PSP user is separate future work).
             The chooser offers both file types unconditionally and does not pre-filter which
             actually applies -- that determination (import_file_type routing, the
             unassigned-route refusal, the TIER-on-125 refusal) is computed inside
             SummitExportServlet, which this run does not modify and does not reproduce; an
             inapplicable choice returns the refusal the exporter already returns.
             Push sits last, after a Bootstrap .vr divider and spacing, not adjacent to the
             read-only Download/Check-response choosers -- positioning only, not a distinct
             confirm surface. Both hidden push forms and their confirm tokens
             (ELECT-ALL-P{proposalId} / ENROLL-ALL-P{proposalId}) are unmoved and unedited. --%>
        <div class="d-flex align-items-center gap-2 py-1">
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Enrollment<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-enrollment" aria-expanded="false" aria-controls="summitHelp-enrollment" aria-label="Show details for Enrollment" title="Show details for Enrollment"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
            <div class="collapse" id="summitHelp-enrollment">
            <div class="text-muted" style="font-size: 0.72rem;">Section 125 plans: PremiumPath, FSA, DCA · from the Enrollment Matrix · requires confirm</div>
            <div class="text-muted" style="font-size: 0.72rem;">HRA plans: ICHRA, QSEHRA, HRA, MERP · from the Enrollment Matrix · requires confirm</div>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="elections"/></jsp:include>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="enrollment"/></jsp:include>
            </div>
          </div>
          <div class="btn-group btn-group-sm">
            <a class="btn btn-sm btn-outline-ssa" href="${pageContext.request.contextPath}/EnrollmentMatrix?setupId=${sessionScope.local.getCurrentActivity().getActivity().getId()}" title="Open the enrollment matrix" aria-label="Open the enrollment matrix"><i class="bi bi-grid-3x3-gap"></i></a>
            <button type="button" class="btn btn-sm btn-outline-ssa" id="btnCopyMatrixLink" onclick="ammCopyMatrixLink(${sessionScope.local.getCurrentActivity().getActivity().getId()})" title="Copy a link an agent can open to view this matrix" aria-label="Copy a link an agent can open to view this matrix"><i class="bi bi-link-45deg"></i></button>
            <button type="button" class="btn btn-sm btn-outline-ssa" id="btnOpenAgentView" onclick="ammOpenAgentView(${sessionScope.local.getCurrentActivity().getActivity().getId()})" title="Open the agent view of this matrix in a new tab" aria-label="Open the agent view of this matrix in a new tab"><i class="bi bi-box-arrow-up-right"></i></button>
            <script>
              function ammIssueMatrixLink(setupId) {
                return fetch('${pageContext.request.contextPath}/EnrollmentMatrix', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                  body: 'action=issueLink&setupId=' + encodeURIComponent(setupId)
                }).then(function (r) { return r.json(); }).then(function (data) {
                  if (!data || !data.url) { throw new Error(data && data.error ? data.error : 'The link could not be issued.'); }
                  return data.url;
                });
              }
              function ammCopyMatrixLink(setupId) {
                var btn = document.getElementById('btnCopyMatrixLink');
                if (btn) btn.disabled = true;
                ammIssueMatrixLink(setupId).then(function (url) {
                  if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(url).then(function () {
                      alert('Matrix link copied:\n' + url);
                    }, function () { window.prompt('Copy this matrix link:', url); });
                  } else {
                    window.prompt('Copy this matrix link:', url);
                  }
                }).catch(function (e) {
                  alert(e && e.message ? e.message : 'The link could not be issued.');
                }).finally(function () { if (btn) btn.disabled = false; });
              }
              function ammOpenAgentView(setupId) {
                var btn = document.getElementById('btnOpenAgentView');
                if (btn) btn.disabled = true;
                var tab = window.open('', '_blank');
                ammIssueMatrixLink(setupId).then(function (url) {
                  if (tab) { tab.location = url; } else { window.location = url; }
                }).catch(function (e) {
                  if (tab) tab.close();
                  alert(e && e.message ? e.message : 'The link could not be issued.');
                }).finally(function () { if (btn) btn.disabled = false; });
              }
            </script>
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Preview — not built yet"><i class="bi bi-eye"></i></button>
            <div class="btn-group btn-group-sm">
              <button type="button" class="btn btn-outline-ssa dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false" title="Download"><i class="bi bi-download"></i></button>
              <ul class="dropdown-menu dropdown-menu-end">
                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=elections" title="Download 125 PI Elections (refuses without confirm)">125 PI Elections</a></li>
                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=enrollment" title="Download HRA Enrollment (refuses without confirm)">HRA Enrollment</a></li>
              </ul>
            </div>
            <div class="btn-group btn-group-sm">
              <button type="button" class="btn btn-outline-ssa dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false" title="Check response"><i class="bi bi-arrow-repeat"></i></button>
              <ul class="dropdown-menu dropdown-menu-end">
                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/SummitResponse?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&step=elections" title="Check response">125 PI Elections</a></li>
                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/SummitResponse?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&step=enrollment" title="Check response">HRA Enrollment</a></li>
              </ul>
            </div>
            <div class="vr mx-1"></div>
            <div class="btn-group btn-group-sm">
              <button type="button" class="btn btn-outline-ssa dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false" title="Push to DataPath"><i class="bi bi-cloud-upload"></i></button>
              <ul class="dropdown-menu dropdown-menu-end">
                <li><button type="submit" form="summitPush-elections" class="dropdown-item" data-summit-push="elections" onclick="return confirm('Push the 125 PI Elections file to DataPath? This sends every non-declined Section 125 election recorded on the Enrollment Matrix. Summit processes it automatically within about 15 minutes. There is no undo.');">125 PI Elections</button></li>
                <li><button type="submit" form="summitPush-enrollment" class="dropdown-item" data-summit-push="enrollment" onclick="return confirm('Push the HRA Enrollment file to DataPath? This sends every non-declined HRA election recorded on the Enrollment Matrix. Summit processes it automatically within about 15 minutes. There is no undo.');">HRA Enrollment</button></li>
              </ul>
            </div>
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
    <%-- V104 -- carries no confirm/effectiveDate; the servlet always renders the confirmation
         screen first, which carries its own onclick confirm() and posts back here with both. --%>
    <%-- S59-P3rev -- a hidden form whose only submit button no longer renders is dead markup;
         gated on the same cardIssuerAvailable capture as the row above. --%>
    <c:if test="${not empty cardIssuerAvailable}">
    <form id="summitPush-cardseed" method="post" target="_blank"
          action="${pageContext.request.contextPath}/SummitExport">
      <input type="hidden" name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
      <input type="hidden" name="type" value="cardseed"/>
    </form>
    </c:if>
    <%-- S56-C -- the two matrix-sourced enrollment files pre-fill their confirm tokens: there is
         no HTML confirm screen for either and no URL a POST lets the operator retype, so the
         token is supplied here, gated by each button's own onclick confirm(). Keyed on
         proposalId like every sibling token. --%>
    <form id="summitPush-elections" method="post" target="_blank"
          action="${pageContext.request.contextPath}/SummitExport">
      <input type="hidden" name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
      <input type="hidden" name="type" value="elections"/>
      <input type="hidden" name="confirm" value="ELECT-ALL-P${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
    </form>
    <form id="summitPush-enrollment" method="post" target="_blank"
          action="${pageContext.request.contextPath}/SummitExport">
      <input type="hidden" name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
      <input type="hidden" name="type" value="enrollment"/>
      <input type="hidden" name="confirm" value="ENROLL-ALL-P${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
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
