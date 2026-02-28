<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>Send Billing</title>
</head>
<body>
<div class="container-fluid">
  <div class="row mt-5">
    <div class="col"></div>
    <div class="col-lg-6 col-md-9 col-12">
      <form method="post" action="SendEmployerBillingDetail">
        <div class="row border border-dark rounded bg-dark text-white mb-2">
          <div class="col">
            <h4>Send Billing Detail to ${sessionScope.currentBillingEmployer.getEmployerName()}</h4>
          </div>
        </div>
        <div class="row border border-danger rounded text-danger">
          <div class="col">
            <h5>Distribution List</h5>
          </div>
        </div>
        <c:forEach var="contact" items="${sessionScope.employerContactList}" varStatus="loop">
          <div class="row">
            <div class="col">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" checked value="${contact.getId()}" name="eCheck${loop.count}" id="eCheck${loop.count}">
                <label class="form-check-label" for="eCheck${loop.count}">${contact.getFirstName()} ${contact.getLastName()} [${contact.getEmail()}]</label>
              </div>
            </div>
          </div>
        </c:forEach>
        <div class="row">
          <div class="col">
            <input type="text" name="additionalEmails" class="form-control" placeholder="Enter additional emails separated by semi-colon">
          </div>
        </div>
        <div class="row border border-danger rounded text-danger mt-3">
          <div class="col">
            <h5>Message Content</h5>
          </div>
        </div>
        <div class="row">
          <div class="col">
            <textarea name="preLinkText" class="form-control" placeholder="text to include before link">Please find below, a link to the detail of your current monthly billing.  If you have any questions or concerns, please let me know.
            </textarea>
          </div>
        </div>
        <div class="row mt-2 mb-2">
          <div class="col">
            <a href="${sessionScope.bcLink}" target="_blank">BILLING DETAIL LINK</a>
          </div>
        </div>
        <div class="row">
          <div class="col">
            <textarea name="postLinkArea" class="form-control" placeholder="text to include after link"></textarea>
          </div>
        </div>
        <div class="row mt-1">
          <div class="col"></div>
          <div class="col-auto">
            <a class="btn btn-outline-success" href="BillingAction?action=drillDown">Cancel</a>
          </div>

          <div class="col-auto">
            <button type="submit" class="btn btn-success" id="sendBtn"
                    onclick="this.querySelector('.spinner-border').classList.remove('d-none'); this.disabled=true; this.form.submit();">
              <i class="bi bi-send"></i> Send
              <span class="spinner-border spinner-border-sm d-none ms-1" role="status"></span>
            </button>
          </div>
        </div>
      </form>
    </div>
    <div class="col"></div>
  </div>
</div>
</body>
</html>
