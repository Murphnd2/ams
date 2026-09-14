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

        <%-- 3. Request census --%>
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Request census<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-censusrequest" aria-expanded="false" aria-controls="summitHelp-censusrequest" aria-label="Show details for Request census" title="Show details for Request census"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
            <div class="collapse" id="summitHelp-censusrequest">
            <div class="text-muted" style="font-size: 0.72rem;">Secure link for the client · upload held for review</div>
            <jsp:include page="/CensusRequestStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/></jsp:include>
            </div>
          </div>
          <div class="btn-group btn-group-sm">
            <a href="${pageContext.request.contextPath}/CensusRequest?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}" class="btn btn-outline-ssa" title="Request census from client"><i class="bi bi-envelope"></i></a>
          </div>
        </div>

        <%-- 4. Census --%>
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Census<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-census" aria-expanded="false" aria-controls="summitHelp-census" aria-label="Show details for Census" title="Show details for Census"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
            <div class="collapse" id="summitHelp-census">
            <div class="text-muted" style="font-size: 0.72rem;">Upload and review the roster</div>
            </div>
          </div>
          <div class="btn-group btn-group-sm">
            <a href="${pageContext.request.contextPath}/CensusUpload?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}" class="btn btn-outline-ssa" title="Census upload"><i class="bi bi-people"></i></a>
          </div>
        </div>

        <%-- 5. Demographics --%>
        <div class="d-flex align-items-center gap-2 py-1 border-bottom">
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Demographics<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-demographics" aria-expanded="false" aria-controls="summitHelp-demographics" aria-label="Show details for Demographics" title="Show details for Demographics"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
            <div class="collapse" id="summitHelp-demographics">
            <div class="text-muted" style="font-size: 0.72rem;">File 4 · creates or updates participants</div>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="demographics"/></jsp:include>
            </div>
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
          <div class="flex-grow-1 lh-sm">
            <div class="fw-semibold">Enrollment</div>
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
        <c:import var="cardIssuerAvailable" url="/CardIssuerAvailability">
          <c:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/>
        </c:import>
        <c:set var="cardIssuerAvailable" value="${fn:trim(cardIssuerAvailable)}"/>
        <c:if test="${not empty cardIssuerAvailable}">
        <div class="d-flex align-items-center gap-2 py-1">
          <div class="flex-grow-1 lh-sm">
            <div>$1 card-issuer seed election<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-cardseed" aria-expanded="false" aria-controls="summitHelp-cardseed" aria-label="Show details for $1 card-issuer seed election" title="Show details for $1 card-issuer seed election"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
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
        <%-- S56-C -- the two matrix-sourced enrollment files. Both read the Enrollment Matrix
             (V106) and route each plan's rows by its 'Enrollment import file' (V108): HRA
             Enrollment for ICHRA/HRA/MERP, 125 PI Elections for PremiumPath/FSA/DCA. The servlet
             refuses either file while the matrix is incomplete (every cell an election or a
             waiver -- one matrix, one state, both files or neither) or while any plan with
             elections has no import file assigned; both refusals name what to fix. Confirm
             tokens are ENROLL-ALL-P{proposalId} / ELECT-ALL-P{proposalId}: the download links
             carry none (first click shows the plain-text refusal naming the retry URL, the
             established T201 friction); the push forms pre-fill them, since a POST has no URL to
             retype, gated by the buttons' own onclick confirm(). --%>
        <div class="d-flex align-items-center gap-2 py-1" style="padding-left: 2rem;">
          <div class="flex-grow-1 lh-sm">
            <div>125 PI Elections<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-elections" aria-expanded="false" aria-controls="summitHelp-elections" aria-label="Show details for 125 PI Elections" title="Show details for 125 PI Elections"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
            <div class="collapse" id="summitHelp-elections">
            <div class="text-muted" style="font-size: 0.72rem;">Section 125 plans: PremiumPath, FSA, DCA · from the Enrollment Matrix · requires confirm</div>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="elections"/></jsp:include>
            </div>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Preview — not built yet"><i class="bi bi-eye"></i></button>
            <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=elections" class="btn btn-outline-ssa" title="Download 125 PI Elections (refuses without confirm)"><i class="bi bi-download"></i></a>
            <button type="submit" form="summitPush-elections" class="btn btn-outline-ssa" title="Push to DataPath" data-summit-push="elections" onclick="return confirm('Push the 125 PI Elections file to DataPath? This sends every non-declined Section 125 election recorded on the Enrollment Matrix. Summit processes it automatically within about 15 minutes. There is no undo.');"><i class="bi bi-cloud-upload"></i></button>
            <a href="${pageContext.request.contextPath}/SummitResponse?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&step=elections" class="btn btn-outline-ssa" title="Check response"><i class="bi bi-arrow-repeat"></i></a>
          </div>
        </div>
        <div class="d-flex align-items-center gap-2 py-1" style="padding-left: 2rem;">
          <div class="flex-grow-1 lh-sm">
            <div>HRA Enrollment<button type="button" class="btn btn-sm btn-link text-muted p-0 ms-1" style="line-height: 1;" data-bs-toggle="collapse" data-bs-target="#summitHelp-enrollment" aria-expanded="false" aria-controls="summitHelp-enrollment" aria-label="Show details for HRA Enrollment" title="Show details for HRA Enrollment"><i class="bi bi-info-circle" style="font-size: 0.75rem;"></i></button></div>
            <div class="collapse" id="summitHelp-enrollment">
            <div class="text-muted" style="font-size: 0.72rem;">HRA plans: ICHRA, QSEHRA, HRA, MERP · from the Enrollment Matrix · requires confirm</div>
            <jsp:include page="/SummitSetupStatus"><jsp:param name="proposalId" value="${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}"/><jsp:param name="step" value="enrollment"/></jsp:include>
            </div>
          </div>
          <div class="btn-group btn-group-sm">
            <button type="button" class="btn btn-outline-secondary opacity-50" style="border-style: dashed; cursor: not-allowed;" aria-disabled="true" title="Preview — not built yet"><i class="bi bi-eye"></i></button>
            <a href="${pageContext.request.contextPath}/SummitExport?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&type=enrollment" class="btn btn-outline-ssa" title="Download HRA Enrollment (refuses without confirm)"><i class="bi bi-download"></i></a>
            <button type="submit" form="summitPush-enrollment" class="btn btn-outline-ssa" title="Push to DataPath" data-summit-push="enrollment" onclick="return confirm('Push the HRA Enrollment file to DataPath? This sends every non-declined HRA election recorded on the Enrollment Matrix. Summit processes it automatically within about 15 minutes. There is no undo.');"><i class="bi bi-cloud-upload"></i></button>
            <a href="${pageContext.request.contextPath}/SummitResponse?proposalId=${sessionScope.local.getCurrentActivity().getActivity().getApplication().getProposal().id}&step=enrollment" class="btn btn-outline-ssa" title="Check response"><i class="bi bi-arrow-repeat"></i></a>
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
