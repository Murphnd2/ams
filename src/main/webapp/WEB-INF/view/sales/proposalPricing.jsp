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
    <c:set var="hasVisibleRows" value="false"/>
    <table class="table table-sm mb-0">
      <c:forEach var="rt" items="${pricing}">
        <%-- Skip $0.00 rows entirely --%>
        <c:if test="${rt.getPrice() > 0.001}">
          <c:if test="${rt.getModule().getId() != currentModule}">
            <c:set var="currentModule" value="${rt.getModule().getId()}"/>
            <%-- Only show module header if at least one row in this module has price > $0.00 --%>
            <c:set var="moduleHasVisible" value="false"/>
            <c:forEach var="check" items="${pricing}">
              <c:if test="${check.getModule().getId() == currentModule && check.getPrice() > 0.001}">
                <c:set var="moduleHasVisible" value="true"/>
              </c:if>
            </c:forEach>
            <c:if test="${moduleHasVisible == 'true'}">
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
          </c:if>
          <tr class="pricing-row">
            <td class="ps-4">${rt.getPriceItem().getDescription()}</td>
            <td class="text-end pe-4">
              <c:choose>
                <c:when test="${rt.getPrice() < 0.02}">Included</c:when>
                <c:otherwise><fmt:formatNumber value="${rt.getPrice()}" type="currency"/></c:otherwise>
              </c:choose>
            </td>
          </tr>
          <c:set var="hasVisibleRows" value="true"/>
        </c:if>
      </c:forEach>
      <c:if test="${empty pricing || hasVisibleRows == 'false'}">
        <tr><td class="text-muted p-3" colspan="2">Pricing details will be provided separately.</td></tr>
      </c:if>
    </table>
  </div>
</div>
