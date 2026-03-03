<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%-- Proposal Pricing: rate table. Included from viewProposal.jsp --%>

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
