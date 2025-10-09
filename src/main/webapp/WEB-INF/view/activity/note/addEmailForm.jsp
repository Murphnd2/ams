<%@ page import="net.superiorstate.ams.previous.model.general.Person" %>
<%@ page import="java.util.List" %>
<%@ page import="net.superiorstate.ams.previous.model.summit.archive.Employee" %>
<%@ page import="net.superiorstate.ams.previous.model.activity.ticket.tEmployee" %>
<%@ page import="net.superiorstate.ams.previous.model.activity.ticket.Ticket" %>
<%@ page import="net.superiorstate.ams.previous.data.misc.dbEmail" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%  StringBuilder emailString = new StringBuilder("value=\"");%>
<c:choose>
  <c:when test="${sessionScope.adminView==1}">
    <c:set var="isReadOnly" value="readonly"></c:set>
  </c:when>
  <c:when test="${sessionScope.adminView==2}">
    <%
      List<tEmployee> contactList = (List<tEmployee>) session.getAttribute("contactList");
        int i = 0;
        session.setAttribute("isReadO","");
        while (i < contactList.size()) {
          session.setAttribute("isReadO","readonly");
          if(i>0) {
            emailString.append(", ");
          }
          emailString.append(contactList.get(i).getEmail());
          i++;
        }

    %>
    <c:set var="isReadOnly" value="${sessionScope.isReadO}"></c:set>
  </c:when>
  <c:when test="${sessionScope.adminView==3}">
    <%
      Ticket t = (Ticket) session.getAttribute("currentActivity");
      String eString = t.getContact().getEmail();
      session.setAttribute("isReadO","");
      try{
        if(dbEmail.isValidEmail(eString)){
          emailString.append(eString);
          session.setAttribute("isReadO","readonly");
        }
      } catch (Exception e){
        e.printStackTrace();
      }
    %>
    <c:set var="isReadOnly" value="${sessionScope.isReadO}"></c:set>
  </c:when>
  <c:otherwise>
    <c:set var="isReadOnly" value=""></c:set>
  </c:otherwise>
</c:choose>
<% emailString.append("\""); %>
<form method="post" action="AddEmail"enctype="multipart/form-data" >
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text" style="width:80px">To</span>
        <input type="email" multiple class="form-control" name="toEmail" required ${isReadOnly} <%=emailString.toString()%>>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text" style="width:80px">Cc</span>
        <input type="email" class="form-control" name="ccEmail">
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <span class="input-group-text" style="width:80px">Subject</span>
        <input type="text" class="form-control" required name="subject">
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <input type="file" class="form-control"  name="emailFile1">
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col">
      <div class="input-group">
        <textarea type="text" class="form-control" rows="5" name="emailBody" placeholder="Enter your message here..." required></textarea>
      </div>
    </div>
  </div>
  <div class="row mb-1">
    <div class="col"></div>
    <div class="col-auto">
      <button class="btn btn-success" type="submit">
        <i class="bi bi-send"></i>&nbsp;&nbsp;&nbsp;Send
      </button>
    </div>
  </div>
</form>
