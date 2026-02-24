<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:set var="activityType" value="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName()}" />

<%-- Setup module modal (triggered from detailSetup25.jsp plus-circle button) --%>
<c:if test="${activityType == 'Setup'}">
  <c:import url="/WEB-INF/view/activity/setup/addSetupItemMod.jsp"></c:import>
</c:if>

<%-- Renewal item modal (triggered from detailRenewal25.jsp plus-circle button) --%>
<c:if test="${activityType == 'Renewal'}">
  <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/modals/addRenewalItemMod25.jsp"></c:import>
</c:if>

<%-- Past activities modal (triggered from archive icon in detail header) --%>
<c:import url="/WEB-INF/view/a/activityDetail/columns/modals/pastActivityModal25.jsp"></c:import>