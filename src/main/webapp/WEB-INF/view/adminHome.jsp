<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>${sessionScope.psp.getFullName()}</title>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
  <c:choose>
    <c:when test="${sessionScope.isPspUser || sessionScope.isPspAdmin}">
      <c:choose>
        <%-- ******************** SETUP DETAIL VIEW ********************************************************************* --%>
        <c:when test="${sessionScope.adminView==1}">
          <div class="row">
            <%-- ---------------------------------LEFT COLUMN-------------------------------------- --%>
            <div class="col-12 col-md-5 col-lg-5 col-xl-3 order-2 order-xl-first">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                <i class="bi bi-check2-square"></i>
                Checklist</div>
              <c:import url="/WEB-INF/view/activity/checklist/toDoListFormV01.jsp"></c:import>
            </div>
                <%-- ---------------------------------CENTER COLUMN-------------------------------------- --%>
            <div class="col-12 col-md-7 col-lg-7 col-xl-5 order-first order-xl-2">
              <div class="p-2 pt-0 border border-secondary bg-white rounded w-100 mt-2 mb-2 fw-bold fs-3 align-items-center text-center position-relative">
                <div class="row m-0 p-0 align-items-center align-middle ">
                  <div class="col-auto m-0 p-0">
                    <a class="btn btn-secondary btn-sm btn-sm pe-auto position-absolute start-0 top-50 translate-middle" href="ResetAdminView">
                      <i class="bi bi-arrow-return-left"></i>
                    </a>
                  </div>
                  <div class="col m-0 p-0 pt-1 fw-bold fs-3 text-secondary text-truncate">
                    <i class="bi bi-building"></i> ${sessionScope.currentSetup.getFullName()}
                  </div>
                </div>
              </div>
              <c:import url="/WEB-INF/view/activity/activityTabPanel.jsp"></c:import>
            </div>
                <%-- ---------------------------------RIGHT COLUMN-------------------------------------- --%>
            <div class="col-12 col-md-7 col-xl-4 order-last">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                <i class="bi bi-stack-overflow"></i>
                History</div>
              <c:import url="/WEB-INF/view/activity/note/activityHistory.jsp"></c:import>
            </div>
          </div>
        </c:when>
        <%-- ******************** RENEWAL DETAIL VIEW ********************************************************************* --%>
        <c:when test="${sessionScope.adminView==2}">
          <div class="row">
            <div class="col-12 col-md-5 col-lg-5 col-xl-3 order-2 order-xl-first">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                <i class="bi bi-check2-square"></i>
                Checklist</div>
                <c:import url="/WEB-INF/view/activity/checklist/toDoListFormV01.jsp"></c:import>
            </div>
            <div class="col-12 col-md-7 col-lg-7 col-xl-5 order-first order-xl-2">
              <div class="p-2 pt-0 border border-primary bg-white rounded w-100 mt-2 mb-2 fw-bold fs-3 align-items-center text-center position-relative">
                <div class="row m-0 p-0 align-items-center align-middle ">
                  <div class="col-auto m-0 p-0">
                    <a class="btn btn-primary btn-sm btn-sm pe-auto position-absolute start-0 top-50 translate-middle" href="ResetAdminView">
                      <i class="bi bi-arrow-return-left"></i>
                    </a>
                  </div>
                  <div class="col m-0 p-0 pt-1 ms-4 fw-bold fs-3 text-primary text-truncate">
                     <i class="bi bi-repeat"></i> ${sessionScope.currentRenewal.getFullName()}
                  </div>
                </div>
              </div>
              <c:import url="/WEB-INF/view/activity/activityTabPanel.jsp"></c:import>
            </div>
            <div class="col-12 col-md-7 col-xl-4 order-last">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                <i class="bi bi-stack-overflow"></i>
                History</div>
              <c:import url="/WEB-INF/view/activity/note/activityHistory.jsp"></c:import>
            </div>
          </div>
        </c:when>
        <%-- ******************** TICKET DETAIL VIEW ********************************************************************* --%>
        <c:when test="${sessionScope.adminView==3}">
          <div class="row">
            <div class="col-12 col-md-5 col-lg-5 col-xl-3 order-2 order-xl-first">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                <i class="bi bi-check2-square"></i>
                Checklist</div>
              <c:import url="/WEB-INF/view/activity/checklist/toDoListFormV01.jsp"></c:import>
            </div>
            <div class="col-12 col-md-7 col-lg-7 col-xl-5 order-first order-xl-2">
              <div class="p-2 pt-0 border border-info bg-white rounded w-100 mt-2 mb-2 fw-bold fs-3 align-items-center text-center position-relative">
                <div class="row m-0 p-0 align-items-center align-middle">
                  <div class="col-auto m-0 p-0">
                    <a class="btn btn-info btn-sm pe-auto position-absolute start-0 top-50 translate-middle" href="ResetAdminView">
                      <i class="bi bi-arrow-return-left"></i>
                    </a>
                  </div>
                  <div class="col m-0 p-0 pt-1 fw-bold fs-4 text-info text-truncate">
                    <i class="bi bi-ticket-detailed"></i>
                      <c:choose>
                        <c:when test="${sessionScope.tIsEmployee==1}">
                          ${sessionScope.currentEmployer3.toUpperCase()}
                        </c:when>
                        <c:when test="${sessionScope.currentPrimaryContact.getFirstName()!=null}">
                          ${sessionScope.currentPrimaryContact.getFirstName().toUpperCase()}&nbsp;${sessionScope.currentPrimaryContact.getLastName().toUpperCase()}
                        </c:when>
                        <c:otherwise>
                          ${sessionScope.currentTicket.getFullName().toUpperCase()}
                        </c:otherwise>
                      </c:choose>
                  </div>
                </div>
              </div>
              <c:import url="/WEB-INF/view/activity/activityTabPanel.jsp"></c:import>
            </div>
            <div class="col-12 col-md-7 col-xl-4 order-last">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                <i class="bi bi-stack-overflow"></i>
                History</div>
              <c:import url="/WEB-INF/view/activity/note/activityHistory.jsp"></c:import>
            </div>
          </div>
        </c:when>
        <%-- ******************** CHECKLIST DETAIL VIEW ********************************************************************* --%>
        <c:when test="${sessionScope.adminView==4}">
          <div class="row">
            <div class="col-12 col-md-5 col-lg-5 col-xl-3 order-2 order-xl-first">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                <i class="bi bi-check2-square"></i>
                Checklist</div>
              <c:import url="/WEB-INF/view/activity/checklist/toDoListFormV01.jsp"></c:import>
              <c:import url="/WEB-INF/view/activity/checklist/modifyRecurringItem.jsp"></c:import>
            </div>
            <div class="col-12 col-md-7 col-lg-7 col-xl-5 order-first order-xl-2">
              <div class="p-2 pt-0 border border-warning bg-dark rounded w-100 mt-2 mb-2 fw-bold fs-3 align-items-center text-center position-relative">
                <div class="row m-0 p-0 align-items-center align-middle">
                  <div class="col-auto m-0 p-0">
                    <a class="btn btn-warning btn-sm pe-auto position-absolute start-0 top-50 translate-middle" href="ResetAdminView">
                      <i class="bi bi-arrow-return-left"></i>
                    </a>
                  </div>
                  <div class="col m-0 p-0 pt-1 fw-bold fs-3 text-warning ">
                      ${sessionScope.currentChecklist.getFullName()}
                  </div>
                </div>
              </div>
              <c:import url="/WEB-INF/view/activity/activityTabPanel.jsp"></c:import>
            </div>
            <div class="col-12 col-md-7 col-xl-4 order-last">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                <i class="bi bi-stack-overflow"></i>
                History</div>
              <c:import url="/WEB-INF/view/activity/note/activityHistory.jsp"></c:import>
            </div>
          </div>
        </c:when>
        <%-- ******************** RENEWAL GENERATOR DETAIL VIEW ********************************************************************* --%>
        <c:when test="${sessionScope.adminView==5}">
          <div class="row">
            <div class="col-lg-3 order-2 order-md-first">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">My Time</div>
              <c:import url="/WEB-INF/view/authentication/timeclock/punchClock.jsp"></c:import>
            </div>
            <div class="col-lg-5">
              <c:import url="/WEB-INF/view/activity/renew/addRenewalForm.jsp"></c:import>
            </div>
            <div class="col-lg-4">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
                <div class="row">
                  <div class="col-auto">
                    <button class="btn btn-sm btn-warning pe-auto fs-6 border border-dark" type="button" data-bs-toggle="modal" data-bs-target="#addReminderModal">
                      <i class="bi bi-bell"></i>
                    </button>
                  </div>
                  <div class="col">
                    <i class="bi bi-check-all"></i>
                    My Checklist
                  </div>
                  <div class="col-auto">
                    <button class="btn btn-sm btn-warning pe-auto fs-6 border border-dark" type="button" data-bs-toggle="modal" data-bs-target="#addSimpleChecklistModal">
                      <i class="bi bi-journal-check"></i>
                    </button>
                  </div>
                </div>
              </div>
              <c:import url="/WEB-INF/view/activity/checklist/myChecklistsNew.jsp"></c:import>
            </div>
          </div>
        </c:when>
        <c:otherwise>
          <div class="row">
            <div class="col-12 col-lg-7 col-xl-3 order-last">
              <div class="btn btn-dark w-100 mt-2 mb-2 fw-bold fs-3 pe-none">
               <i class="bi bi-clock-history"></i> My Time</div>
              <c:import url="/WEB-INF/view/authentication/timeclock/punchClock.jsp"></c:import>
            </div>
            <div class="col-12 col-lg-7 col-xl-6 order-first order-xl-2 ">
              <c:import url="/WEB-INF/view/activity/adminHomeHeader3.jsp"></c:import>
            </div>
            <div class="col-12 col-lg-5 col-xl-3 order-2 order-xl-first">
              <div class="p-2 pt-1 border border-dark rounded w-100 mt-2 mb-2 text-light bg-dark fw-bold fs-3 align-items-center text-center">
                <div class="row">
                  <div class="col-auto">
                    <button class="btn btn-sm btn-outline-warning pe-auto fs-6 border border-warning" type="button" data-bs-toggle="modal" data-bs-target="#addReminderModal">
                      <i class="bi bi-bell"></i>
                    </button>
                  </div>
                  <div class="col">
                    <i class="bi bi-check-all"></i>
                    ToDos
                  </div>
                  <div class="col-auto">
                    <button class="btn btn-sm btn-outline-warning pe-auto fs-6 border border-warning" type="button" data-bs-toggle="modal" data-bs-target="#addSimpleChecklistModal">
                      <i class="bi bi-journal-check"></i>
                    </button>
                  </div>
                </div>
              </div>
              <c:import url="/WEB-INF/view/activity/checklist/myChecklistsV1.jsp"></c:import>

              <c:import url="/WEB-INF/view/activity/checklist/component/viewRemainingChecklistForm.jsp"></c:import>
            </div>
          </div>
        </c:otherwise>
      </c:choose>
    </c:when>
    <c:otherwise>
      <h5>Not Expected</h5>
    </c:otherwise>
  </c:choose>
</div>
</body>
</html>
