<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <script src="https://code.jquery.com/jquery-3.5.1.min.js"></script>
  <title>View Email</title>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/navbar.jsp"></c:import>
  <div class="row mt-3">
    <div class="col offset-xl-1">
        <%-- ************************ TO WHO LIST ********************************* --%>
        <div class="row mb-2">
            <div class="input-group">
                <span class="input-group-text me-0"><i class="bi bi-send"></i>&nbsp;&nbsp;Sent By &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
                <div class="form-control">
                        ${sessionScope.currentEmail.getCreatedBy().getFullName()} on <fmt:formatDate value="${sessionScope.currentEmail.getDateCreated()}" pattern="MM/dd/yy @ hh:mm aa"></fmt:formatDate>
                </div>
            </div>
        </div>
        <%-- ************************ TO WHO LIST ********************************* --%>
        <div class="row mb-2">
          <div class="input-group">
            <span class="input-group-text me-0"><i class="bi bi-people-fill"></i>&nbsp;&nbsp;Recipient(s)&nbsp;&nbsp;&nbsp;</span>
            <div class="form-control m-0 p-0">
              <div class="d-flex row m-1 p-0">
                <c:import url="/WEB-INF/view/general/email/lists/toWhoList2.jsp"></c:import>
              </div>
            </div>
          </div>
        </div>
        <%-- ************************ SUBJECT LINE ********************************* --%>
        <div class="row mb-2">
          <div class="input-group">
            <span class="input-group-text me-0"><i class="bi bi-lightbulb"></i>&nbsp;&nbsp;Subject&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
            <input type="text" class="form-control form-control" name="eSubject" placeholder="Subject of message . . ." value="${sessionScope.currentEmail.getSubject()}">
          </div>
        </div>
        <%-- ************************ ATTACHMENT LINE ********************************* --%>
            <div class="row mb-2">
                <div class="input-group">
                    <span class="input-group-text me-0"><i class="bi bi-paperclip"></i>&nbsp;&nbsp;Attachment(s)</span>
                    <div class="form-control m-0 p-0">
                        <div class="d-flex row m-1 p-0">
                            <c:import url="/WEB-INF/view/general/email/lists/attachmentList2.jsp"></c:import>
                        </div>
                    </div>
                </div>
            </div>
        <%-- ************************ MESSAGE BODY ********************************* --%>
        <div class="row mb-2">
          <div class="col">
              ${sessionScope.currentEmail.getDetail()}
          </div>
        </div>
    </div>
    <div class="col-xl-1"></div>
  </div>
</div>
