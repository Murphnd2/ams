<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Proposal — ${pspName}</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
  <style>
    :root {
      --psp-primary: ${primaryColor};
      --psp-accent: ${accentColor};
    }
    body {
      background: #f8f9fa;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }
    .proposal-header {
      background: var(--psp-primary);
      color: white;
      padding: 2rem 0;
    }
    .proposal-header h1 {
      font-size: 1.6rem;
      font-weight: 600;
      margin: 0;
    }
    .proposal-header .subtitle {
      opacity: 0.85;
      font-size: 0.95rem;
    }
    .accent-bar {
      height: 4px;
      background: var(--psp-accent);
    }
    .proposal-container {
      max-width: 900px;
      margin: 0 auto;
      padding: 2rem 1rem 4rem;
    }
    .greeting-card {
      background: white;
      border-radius: 8px;
      padding: 2rem;
      margin-bottom: 1.5rem;
      box-shadow: 0 1px 3px rgba(0,0,0,0.08);
    }
    .los-card {
      background: white;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      box-shadow: 0 1px 3px rgba(0,0,0,0.08);
      overflow: hidden;
    }
    .los-card-header {
      background: var(--psp-primary);
      color: white;
      padding: 0.75rem 1.25rem;
      font-weight: 600;
      font-size: 1.05rem;
    }
    .los-card-body {
      padding: 1.25rem;
    }
    .feature-item {
      padding: 0.4rem 0;
      display: flex;
      align-items: flex-start;
      gap: 0.5rem;
    }
    .feature-item i {
      color: var(--psp-accent);
      margin-top: 0.15rem;
      flex-shrink: 0;
    }
    .pricing-card {
      background: white;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      box-shadow: 0 1px 3px rgba(0,0,0,0.08);
      overflow: hidden;
    }
    .pricing-card .card-header {
      background: white;
      border-bottom: 2px solid var(--psp-accent);
      padding: 1rem 1.25rem;
    }
    .pricing-module-header td {
      background: #f8f9fa;
      font-weight: 600;
      padding: 0.6rem 1.25rem !important;
      border-top: 1px solid #dee2e6;
    }
    .pricing-row td {
      padding: 0.5rem 1.25rem !important;
    }
    .apply-section {
      text-align: center;
      padding: 2rem 0;
    }
    .btn-apply {
      background: var(--psp-accent);
      border-color: var(--psp-accent);
      color: white;
      font-size: 1.15rem;
      font-weight: 600;
      padding: 0.75rem 3rem;
      border-radius: 6px;
      transition: all 0.2s;
    }
    .btn-apply:hover {
      background: var(--psp-primary);
      border-color: var(--psp-primary);
      color: white;
    }
    .proposal-footer {
      text-align: center;
      color: #999;
      font-size: 0.85rem;
      padding: 2rem 0;
      border-top: 1px solid #e9ecef;
    }
  </style>
</head>
<body>

<%-- Header --%>
<div class="proposal-header">
  <div class="proposal-container" style="padding-top:0;padding-bottom:0;">
    <h1>${pspName}</h1>
    <div class="subtitle">Benefits Administration Proposal</div>
  </div>
</div>
<div class="accent-bar"></div>

<div class="proposal-container">

  <%-- Greeting --%>
  <div class="greeting-card">
    <h4 class="mb-2" style="color: var(--psp-primary);">
      Hello<c:if test="${proposal.getProspect().getContact().getFirstName() != null}">, ${proposal.getProspect().getContact().getFirstName()}</c:if>!
    </h4>
    <p class="mb-1">
      We've prepared a customized benefits administration proposal for
      <strong>${proposal.getProspect().getName()}</strong>.
    </p>
    <p class="text-muted mb-0">
      Below you'll find the services we recommend, along with features and pricing details.
    </p>
  </div>

  <%-- Lines of Service with Features --%>
  <c:forEach var="los" items="${proposal.getLosList()}">
    <div class="los-card">
      <div class="los-card-header">
        <i class="bi bi-check-circle me-2"></i>${los.getDescription()}
      </div>
      <div class="los-card-body">
          <%-- Features for modules in this LOS --%>
        <c:set var="hasFeatures" value="false"/>
        <c:forEach var="module" items="${los.getServiceModuleList()}">
          <c:forEach var="feature" items="${features}">
            <c:if test="${feature.getServiceModule().getId() == module.getId()}">
              <c:set var="hasFeatures" value="true"/>
              <div class="feature-item">
                <i class="bi bi-check2"></i>
                <span>${feature.getDescription()}</span>
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
                    <span>${feature.getDescription()}</span>
                  </div>
                </c:if>
              </c:forEach>
            </div>
          </div>
        </c:if>
      </c:if>
    </c:if>
  </c:forEach>

  <%-- Pricing Table --%>
  <div class="pricing-card">
    <div class="card-header">
      <h5 class="mb-0 fw-semibold" style="color: var(--psp-primary);">
        <i class="bi bi-tag me-2"></i>Pricing
      </h5>
    </div>
    <div class="card-body p-0">
      <c:set var="currentModule" value=""/>
      <table class="table table-sm mb-0">
        <c:forEach var="rt" items="${pricing}">
          <c:if test="${rt.getModule().getId() != currentModule}">
            <c:set var="currentModule" value="${rt.getModule().getId()}"/>
            <tr class="pricing-module-header">
              <td colspan="2">
                <c:choose>
                  <c:when test="${not empty rt.getModule().getLos()}">${rt.getModule().getLos().getDescription()}</c:when>
                  <c:when test="${not empty rt.getModule().getEnhancement()}">${rt.getModule().getEnhancement().getDescription()}</c:when>
                  <c:otherwise>${rt.getModule().getDescription()}</c:otherwise>
                </c:choose>
              </td>
            </tr>
          </c:if>
          <tr class="pricing-row">
            <td class="ps-4">${rt.getPriceItem().getDescription()}</td>
            <td class="text-end pe-4">
              <fmt:formatNumber value="${rt.getPrice()}" type="currency"/>
            </td>
          </tr>
        </c:forEach>
        <c:if test="${empty pricing}">
          <tr><td class="text-muted p-3" colspan="2">Pricing details will be provided separately.</td></tr>
        </c:if>
      </table>
    </div>
  </div>

  <%-- Apply Now --%>
  <div class="apply-section">
    <p class="text-muted mb-3">Ready to get started? Click below to begin your application.</p>
    <a href="${pageContext.request.contextPath}/apply/${proposal.getApplicationGUID()}" class="btn btn-apply">
      <i class="bi bi-pencil-square me-2"></i>Apply Now
    </a>
  </div>

  <%-- Footer --%>
  <div class="proposal-footer">
    <p class="mb-0">&copy; ${pspName} &middot; Benefits Administration Services</p>
  </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
