<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%-- Proposal ICHRA Illustration (build-plan item 6). Included from viewProposal.jsp.

     PUBLIC page, same price-leak discipline as proposalPricing.jsp: renders only from
     ${ichraSnapshot} / ${ichraBands} — the immutable snapshot written at proposal
     creation. Never references ${pricing}, never a basePrice or markup value (this
     section has no pricing concept of its own — it is a market illustration, not a
     quote). No affordability figure, no PTC status, no income, no carrier name, no
     plan list, no ranking or recommendation. No SSA chrome — white-labelling is
     controlled entirely by viewProposal.jsp's header suppression; this fragment does
     not reference agencyName or reintroduce branding. --%>

<div class="los-card">
  <div class="los-card-header">
    <i class="bi bi-heart-pulse me-2"></i>ICHRA Market Illustration
  </div>
  <div class="los-card-body">

    <p class="text-muted mb-2" style="font-size: 0.85rem;">
      This is an illustration based on cached market rates, not a quote and not a compliance determination.
      Actual premiums depend on individual enrollee details, and ICHRA affordability must be determined separately.
    </p>
    <p class="text-muted mb-3" style="font-size: 0.85rem;">
      <strong>Off-exchange plans only.</strong> These figures cover the off-exchange individual market.
      On-exchange plans are not included in the figures shown.
    </p>

    <c:choose>
      <c:when test="${ichraSnapshot.mode == 'AGE_BAND'}">
        <table class="table table-sm mb-3">
          <thead>
            <tr>
              <th>Age</th>
              <th>Lives</th>
              <th class="text-end">Net / Employee</th>
              <th class="text-end">Total</th>
            </tr>
          </thead>
          <tbody>
            <c:forEach var="band" items="${ichraBands}">
              <tr>
                <td>${band.age}</td>
                <td>${band.lives}</td>
                <td class="text-end"><fmt:formatNumber value="${band.netPerEmployee}" type="currency"/></td>
                <td class="text-end"><fmt:formatNumber value="${band.bandNet}" type="currency"/></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
        <p class="mb-1">
          <strong>Group Monthly Net Cost:</strong>
          <fmt:formatNumber value="${ichraSnapshot.groupNetTotal}" type="currency"/>
        </p>
        <p class="text-muted mb-0" style="font-size: 0.85rem;">
          After the employer's monthly contribution
          (<fmt:formatNumber value="${ichraSnapshot.contribution}" type="currency"/> per employee).
        </p>
      </c:when>
      <c:otherwise>
        <p class="mb-1">
          <strong>Estimated Monthly Group Premium at the Bronze Floor:</strong>
          <c:choose>
            <c:when test="${not empty ichraSnapshot.groupMonthlyLow and not empty ichraSnapshot.groupMonthlyHigh}">
              <fmt:formatNumber value="${ichraSnapshot.groupMonthlyLow}" type="currency"/> &ndash; <fmt:formatNumber value="${ichraSnapshot.groupMonthlyHigh}" type="currency"/>
            </c:when>
            <c:otherwise>&mdash;</c:otherwise>
          </c:choose>
        </p>
        <p class="text-muted mb-0" style="font-size: 0.85rem;">
          For ${ichraSnapshot.headcount} eligible employees. This spread reflects age mix across the group, not plan choice.
        </p>
      </c:otherwise>
    </c:choose>

    <p class="text-muted mt-3 mb-0" style="font-size: 0.8rem;">
      <c:out value="${ichraSnapshot.countyName}"/>, <c:out value="${ichraSnapshot.state}"/> &middot;
      Plan Year ${ichraSnapshot.planYear} &middot;
      <c:choose>
        <c:when test="${not empty ichraSnapshot.ratesFetchedAtDisplay}">
          Rates as of <c:out value="${ichraSnapshot.ratesFetchedAtDisplay}"/>
        </c:when>
        <c:otherwise>Cache freshness unavailable</c:otherwise>
      </c:choose>
    </p>

  </div>
</div>
