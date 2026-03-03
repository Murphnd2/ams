<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- Proposal Features: LOS cards + Enhancement cards. Included from viewProposal.jsp --%>

<%-- Lines of Service with Features --%>
<c:forEach var="los" items="${proposal.getLosList()}">
  <div class="los-card">
    <div class="los-card-header">
      <i class="bi bi-check-circle me-2"></i>${los.getDescription()}
    </div>
    <div class="los-card-body">
      <c:set var="hasFeatures" value="false"/>
      <c:forEach var="module" items="${los.getServiceModuleList()}">
        <c:forEach var="feature" items="${features}">
          <c:if test="${feature.getServiceModule().getId() == module.getId()}">
            <c:set var="hasFeatures" value="true"/>
            <div class="feature-item">
              <i class="bi bi-check2"></i>
              <div>
                <c:if test="${not empty feature.getHeadline()}">
                  <div class="fw-semibold">${feature.getHeadline()}</div>
                </c:if>
                <span class="${not empty feature.getHeadline() ? 'text-muted' : ''}">${renderedFeatures[feature.getId()]}</span>
              </div>
            </div>
          </c:if>
        </c:forEach>
      </c:forEach>
      <c:if test="${hasFeatures == 'false'}">
        <p class="text-muted mb-0">Full-service administration included.</p>
      </c:if>
    </div>
  </div>
</c:forEach>

<%-- Enhancement Feature Cards --%>
<c:set var="shownEnhIds" value=","/>
<c:forEach var="rt" items="${pricing}">
  <c:if test="${not empty rt.getModule().getEnhancement()}">
    <c:set var="enhId" value="${rt.getModule().getEnhancement().getId()}"/>
    <c:if test="${!shownEnhIds.contains(','.concat(String.valueOf(enhId)).concat(','))}">
      <c:set var="shownEnhIds" value="${shownEnhIds}${enhId},"/>
      <c:set var="enhModuleId" value="${rt.getModule().getId()}"/>
      <c:set var="hasEnhFeatures" value="false"/>
      <c:forEach var="feature" items="${features}">
        <c:if test="${feature.getServiceModule().getId() == enhModuleId}">
          <c:set var="hasEnhFeatures" value="true"/>
        </c:if>
      </c:forEach>
      <c:if test="${hasEnhFeatures == 'true'}">
        <div class="los-card">
          <div class="los-card-header" style="background: var(--psp-accent);">
            <i class="bi bi-puzzle me-2"></i>${rt.getModule().getEnhancement().getDescription()}
          </div>
          <div class="los-card-body">
            <c:forEach var="feature" items="${features}">
              <c:if test="${feature.getServiceModule().getId() == enhModuleId}">
                <div class="feature-item">
                  <i class="bi bi-check2"></i>
                  <div>
                    <c:if test="${not empty feature.getHeadline()}">
                      <div class="fw-semibold">${feature.getHeadline()}</div>
                    </c:if>
                    <span class="${not empty feature.getHeadline() ? 'text-muted' : ''}">${renderedFeatures[feature.getId()]}</span>
                  </div>
                </div>
              </c:if>
            </c:forEach>
          </div>
        </div>
      </c:if>
    </c:if>
  </c:if>
</c:forEach>
