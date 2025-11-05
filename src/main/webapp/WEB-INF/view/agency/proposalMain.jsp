<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<c:url var="logo3" value="logo03.png"></c:url>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Proposal #${sessionScope.proposal.getId()}</title>
</head>
<body>
  <div class="container">
    <section id="coverPage" class="row page-break-after">
      <div class="col">
        <div class="row">
          <div class="col mt-5 text-center fs-1 text-wrap fw-bold text-ssa">
            Service Proposal For ${sessionScope.proposal.getProspect().getName()}
          </div>
        </div>
        <div class="row">
          <div class="col mt-5 text-center fs-4 text-wrap text-altSsa">
            Prepared By
          </div>
        </div>
        <div class="row">
          <div class="col mt-1 text-center fs-4 text-wrap fw-bolder">
            ${sessionScope.propAgent.getFirstName()} ${sessionScope.propAgent.getLastName()}
          </div>
        </div>
        <div class="row">
          <div class="col mt-1 text-center fs-4 text-wrap">
            ${sessionScope.propAgency.getName()}
          </div>
        </div>
        <div class="row">
          <div class="col mt-1 text-center fs-4 text-wrap">
            ${sessionScope.propAgency.getAddress().getAddress1()}, ${sessionScope.propAgency.getAddress().getAddress2()}
          </div>
        </div>
        <div class="row">
          <div class="col mt-1 text-center fs-4 text-wrap">
            ${sessionScope.propAgency.getAddress().getCity()}, ${sessionScope.propAgency.getAddress().getState()} ${sessionScope.propAgency.getAddress().getZipCode()}
          </div>
        </div>
        <div class="row">
          <div class="col mt-1 text-center fs-5 fw-lighter text-wrap">
            ${sessionScope.propAgent.getEmail()}
          </div>
        </div>
        <div class="row">
          <div class="col mt-1 text-center fs-5 fw-lighter text-wrap">
            ${sessionScope.propAgent.getPhone()}
          </div>
        </div>
        <div class="row">
          <div class="col mt-5 text-center fs-4 text-wrap fw-bolder">
            <img src="${logo3}" class="img-fluid" alt="Administrator Logo" style="height: 100px">
          </div>
        </div>
        <div class="row">
          <div class="col mt-5 text-center fs-4 text-wrap text-altSsa">
            Services Provided By
          </div>
        </div>
        <div class="row">
          <div class="col mt-1 text-center fs-4 text-wrap">
            ${sessionScope.propPsp.getFullName()}
          </div>
        </div>
        <div class="row">
          <div class="col mt-1 text-center fs-4 text-wrap">
            ${sessionScope.propPsp.getAddress().getAddress1()} ${sessionScope.propPsp.getAddress().getAddress2()}
          </div>
        </div>
        <div class="row mb-5">
          <div class="col mt-1 text-center fs-4 text-wrap">
            ${sessionScope.propPsp.getAddress().getCity()}, ${sessionScope.propPsp.getAddress().getState()} ${sessionScope.propPsp.getAddress().getZipCode()}
          </div>
        </div>
      </div>
    </section>
    <section id="page2" class="row page-break-after">
      <div class="row">
        <div class="col">
          <div class="row">
            <div class="col">
              <div class="text-center fs-2 text-ssa fw-bold">
                Services Included In Proposal
              </div>
            </div>
          </div>
          <c:forEach var="los" items="${sessionScope.proposal.getLosList()}">
            <div class="row mt-1">
              <div class="col">
                <div class="text-center fs-4">
                    ${los.getDescription()}
                </div>
              </div>
            </div>
          </c:forEach>
          <div class="row mt-3">
            <div class="col">
              <div class="text-center fs-2 fw-bold text-altSsa">
                Pricing
              </div>
            </div>
          </div>
          <div class="row">
            <div class="col"></div>
            <div class="col-auto">
              <c:forEach var="price" items="${sessionScope.propPricing}">
                <c:if test="${price.getModule().getDescription() != hVal}">
                  <div class="row mt-1">
                    <div class="col">
                      <div class="text-left fs-5 fw-bold">
                          ${price.getModule().getDescription()}
                      </div>
                    </div>
                  </div>
                  <c:set var="hVal" value="${price.getModule().getDescription()}"></c:set>
                </c:if>
                <div class="row mt-1">
                  <div class="col">
                    <div class="text-left">
                        <i class="bi bi-dot"></i>&nbsp;
                        ${price.getPriceItem().getDescription()}
                    </div>
                  </div>
                  <div class="col-auto text-right">
                    <fmt:formatNumber value="${price.getPrice()}" type="CURRENCY"></fmt:formatNumber>
                  </div>
                </div>
              </c:forEach>
              <div class="row mt-5 mb-5 d-print-none">
                <div class="col">
                  <div class="input-group w-100">
                    <input class="form-control pe-none text-ssa fw-bold" readonly value="Ready To Apply?">
                    <a class="btn btn-ssa" href="${sessionScope.hrefString}" target="_blank">Apply Now</a>
                  </div>
                </div>
              </div>
            </div>
            <div class="col"></div>
          </div>
        </div>
      </div>
    </section>
  </div>
</body>
</html>
