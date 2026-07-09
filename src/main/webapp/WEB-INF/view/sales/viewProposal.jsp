<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Proposal — ${not empty agencyName ? agencyName : pspName}</title>
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
    .proposal-section {
      margin-bottom: 1.5rem;
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

    /* ── Print: each proposal section starts on a new page ── */
    @media print {
      .proposal-section {
        break-before: page;
        padding-top: 1.5rem;
      }
      .proposal-section:first-child {
        break-before: auto;
        padding-top: 0;
      }
      .los-card, .pricing-card, .greeting-card {
        break-inside: avoid;
      }
      .proposal-footer {
        break-before: auto;
      }
    }
  </style>
</head>
<body>

<%-- Header — suppressed when agency-branded (TITLE section card provides the brand) --%>
<c:if test="${empty agencyName}">
<div class="proposal-header">
  <div class="proposal-container" style="padding-top:0;padding-bottom:0;">
    <h1>${pspName}</h1>
    <div class="subtitle">Benefits Administration Proposal</div>
  </div>
</div>
<div class="accent-bar"></div>
</c:if>

<div class="proposal-container">

<c:choose>
  <%-- Section-based rendering (when ProposalSections exist for this PSP) --%>
  <c:when test="${not empty proposalSections}">
    <c:forEach var="section" items="${proposalSections}">
      <c:choose>
        <c:when test="${section.getSectionType() == 'TITLE'}">
          <div class="proposal-section title-section">
            ${sectionHtml[section.getId()]}
          </div>
        </c:when>
        <c:when test="${section.getSectionType() == 'FEATURES'}">
          <c:if test="${not empty features}">
          <div class="proposal-section features-section">
            <%@ include file="proposalFeatures.jsp" %>
          </div>
          </c:if>
        </c:when>
        <c:when test="${section.getSectionType() == 'PRICING'}">
          <div class="proposal-section pricing-section">
            <%@ include file="proposalPricing.jsp" %>
          </div>
        </c:when>
        <c:when test="${section.getSectionType() == 'CLOSING'}">
          <div class="proposal-section closing-section">
            ${sectionHtml[section.getId()]}
          </div>
        </c:when>
        <c:when test="${section.getSectionType() == 'CUSTOM'}">
          <div class="proposal-section custom-section">
            ${sectionHtml[section.getId()]}
          </div>
        </c:when>
      </c:choose>
    </c:forEach>
  </c:when>

  <%-- Fallback: legacy hardcoded layout (PSPs with no proposal_section rows) --%>
  <c:otherwise>

  <%-- Greeting --%>
  <div class="proposal-section">
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
  </div>

  <c:if test="${not empty features}">
  <div class="proposal-section">
    <%@ include file="proposalFeatures.jsp" %>
  </div>
  </c:if>
  <div class="proposal-section">
    <%@ include file="proposalPricing.jsp" %>
  </div>

  <%-- Apply Now --%>
  <div class="proposal-section">
    <div class="apply-section">
      <p class="text-muted mb-3">Ready to get started? Click below to begin your application.</p>
      <a href="${pageContext.request.contextPath}/apply/${proposal.getApplicationGUID()}" class="btn btn-apply">
        <i class="bi bi-pencil-square me-2"></i>Apply Now
      </a>
    </div>
  </div>

  </c:otherwise>
</c:choose>

  <%-- Footer --%>
  <div class="proposal-footer">
    <c:choose>
      <c:when test="${not empty agencyName}">
        <p class="mb-0">&copy; ${agencyName}</p>
      </c:when>
      <c:otherwise>
        <p class="mb-0">&copy; ${pspName} &middot; Benefits Administration Services</p>
      </c:otherwise>
    </c:choose>
  </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
