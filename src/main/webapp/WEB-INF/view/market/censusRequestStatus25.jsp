<%@ page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- S47-C -- one muted status line for the Summit setup panel's step 3, rendered by
     CensusRequestStatusServlet (include-only). Same size and class as SummitSetupStatusServlet's
     fragment. Renders nothing when the servlet set no line. --%>
<c:if test="${not empty censusStatusLine}"><div style="font-size: 0.72rem;" class="text-muted"><c:out value="${censusStatusLine}"/><c:if test="${not empty censusReviewUrl}"> · <a href="${censusReviewUrl}">Review</a></c:if></div></c:if>
