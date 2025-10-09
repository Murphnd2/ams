<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>${sessionScope.psp.getFullName()}</title>
  <script src="https://cdn.ckeditor.com/ckeditor5/36.0.1/classic/ckeditor.js"></script>
  <style>
    .ck-editor__editable_inline {
      max-width: 800px;
      width: 100%;
      margin: 0 auto;
      box-sizing: border-box;
      height:300px;
    }
    .ck.ck-toolbar {
      width: 100%;                /* Allow full width */
      max-width: 800px;            /* Disable maximum width restriction */
      margin: 0;                  /* Remove default margin */
      display: flex;              /* Use flexbox layout */
      flex-wrap: wrap;            /* Allow wrapping */
      box-sizing: border-box;     /* Include padding and border in width */
    }
  </style>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
  <c:choose>
    <c:when test="${sessionScope.isPspUser || sessionScope.isPspAdmin}">
      <div class="row">
        <div class="col-12 col-md-5 col-lg-5 col-xl-3 order-2 order-xl-first">
          <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/checklistHeader.jsp"></c:import>
          <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
            <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/checklistAutomation25.jsp"></c:import>
          </c:if>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/checklistBasic25.jsp"></c:import>
          <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
            <c:import url="/WEB-INF/view/a/activityDetail/columns/checklist/checklistFooter25.jsp"></c:import>
          </c:if>
          <c:if test="${sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"CheckList\")}">
            <c:import url="/WEB-INF/view/a/checklistDetail/modifyRecurring25.jsp"></c:import>
          </c:if>
        </div>
        <div class="col-12 col-md-7 col-lg-7 col-xl-5 order-first order-xl-2">
          <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailHeader25.jsp"></c:import>
          <c:if test="${!sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName().equals(\"CheckList\")}">
            <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailPrimaryContact25.jsp"></c:import>
          </c:if>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailDetail25.jsp"></c:import>
          <c:if test="${sessionScope.local.getCurrentActivity().getActivity().isComplete()==false}">
            <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailAddNote25.jsp"></c:import>
          </c:if>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/detail/detailFooter25.jsp"></c:import><%-- --%>
        </div>
        <div class="col-12 col-md-7 col-xl-4 order-last">
          <c:import url="/WEB-INF/view/a/activityDetail/columns/history/historyHeader.jsp"></c:import>
          <c:import url="/WEB-INF/view/a/activityDetail/columns/history/historyDetail25.jsp"></c:import>
        </div>
      </div>
    </c:when>
  </c:choose>
</div>
</body>
</html>
