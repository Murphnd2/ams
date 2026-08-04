<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%-- Proposal Market page (S11-H). Included from viewProposal.jsp.

     PUBLIC page, same discipline as proposalIchra.jsp: renders only from the market*
     request attributes ViewProposal.resolveMarketPage set while the EM was open. Issues
     no query of its own and never references ${pricing}, a basePrice or a markup value.

     REACHED ONLY WHEN THE GUARD PASSED -- entitled agency, plus-tier LOS on the proposal,
     and RateCacheDAO.check() == PRODUCTION_OK. So it carries no empty state, but every
     value is still null-guarded: a cache row can be missing an individual age or figure
     even when the county as a whole is production-sourced.

     COMPLIANCE (LA-17 / LA-04 / LA-05). Aggregate counts and the bronze floor only. No
     plan named, no carrier named, nothing ordered, ranked, defaulted or recommended, and
     no per-employee affordability figure. Off-exchange only, and it says so -- any plan
     display AMS can build is definitionally incomplete. Not a quote.

     PRINT (S11-C). The customer's own browser produces the PDF, and browsers drop
     background colours by default, so anything with a background carries
     print-color-adjust: exact and its -webkit- variant. Without it this prints unreadable. --%>

<div class="los-card">
  <div class="los-card-header">
    <i class="bi bi-graph-up me-2"></i>Individual Market Overview
  </div>
  <div class="los-card-body">

    <p class="text-muted mb-2" style="font-size: 0.85rem;">
      A summary of the individual market in
      <c:choose>
        <c:when test="${not empty marketCountyName}"><c:out value="${marketCountyName}"/></c:when>
        <c:otherwise>the selected county</c:otherwise>
      </c:choose>
      for plan year ${marketPlanYear}. These are the lowest available monthly premiums, shown at
      three representative ages. <strong>They are not a quote</strong> &mdash; actual premiums
      depend on individual enrollee details.
    </p>
    <p class="text-muted mb-3" style="font-size: 0.85rem;">
      <strong>Off-exchange plans only.</strong> On-exchange plans are not included, so this is not a
      complete view of the market. No plan or carrier is named, ranked or recommended here.
    </p>

    <div class="row g-3 mb-3">
      <c:if test="${not empty marketPlanCount}">
        <div class="col-6 col-md-3">
          <div style="background:#f8f9fa; -webkit-print-color-adjust:exact; print-color-adjust:exact;
                      border-radius:6px; padding:0.85rem 1rem;">
            <div style="font-size:1.5rem; font-weight:600; line-height:1.1;">${marketPlanCount}</div>
            <div class="text-muted" style="font-size:0.8rem;">plans available</div>
          </div>
        </div>
      </c:if>
      <c:if test="${not empty marketCarrierCount}">
        <div class="col-6 col-md-3">
          <div style="background:#f8f9fa; -webkit-print-color-adjust:exact; print-color-adjust:exact;
                      border-radius:6px; padding:0.85rem 1rem;">
            <div style="font-size:1.5rem; font-weight:600; line-height:1.1;">${marketCarrierCount}</div>
            <div class="text-muted" style="font-size:0.8rem;">carriers</div>
          </div>
        </div>
      </c:if>
    </div>

    <c:if test="${not empty marketFloor21 or not empty marketFloor40 or not empty marketFloor64}">
      <table class="table table-sm mb-3">
        <thead>
          <tr>
            <th>Age</th>
            <th class="text-end">Lowest Available Monthly Premium</th>
          </tr>
        </thead>
        <tbody>
          <c:if test="${not empty marketFloor21}">
            <tr>
              <td>21</td>
              <td class="text-end"><fmt:formatNumber value="${marketFloor21}" type="currency"/></td>
            </tr>
          </c:if>
          <c:if test="${not empty marketFloor40}">
            <tr>
              <td>40</td>
              <td class="text-end"><fmt:formatNumber value="${marketFloor40}" type="currency"/></td>
            </tr>
          </c:if>
          <c:if test="${not empty marketFloor64}">
            <tr>
              <td>64</td>
              <td class="text-end"><fmt:formatNumber value="${marketFloor64}" type="currency"/></td>
            </tr>
          </c:if>
        </tbody>
      </table>
      <p class="text-muted mb-0" style="font-size: 0.85rem;">
        Premiums shown are the lowest available at each age and reflect age only &mdash; not plan
        choice, tobacco use, or any individual circumstance.
      </p>
    </c:if>

    <p class="text-muted mt-3 mb-0" style="font-size: 0.8rem;">
      <c:if test="${not empty marketCountyName}">
        <c:out value="${marketCountyName}"/><c:if test="${not empty marketState}">, <c:out value="${marketState}"/></c:if> &middot;
      </c:if>
      Plan Year ${marketPlanYear}
      <c:if test="${not empty marketRatesAsOf}">
        &middot; Rates as of <c:out value="${marketRatesAsOf}"/>
      </c:if>
    </p>

  </div>
</div>
