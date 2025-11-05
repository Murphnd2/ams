<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
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
    <div class="row">
        <%-- ************************ T I M E C L O C K   C O L U M N ********************************************************* --%>
        <div class="col-12 col-lg-7 col-xl-3 order-last">
            <c:import url="/WEB-INF/view/a/pspHome/columns/timeClock/timeClockHeader.jsp"></c:import>
            <c:import url="/WEB-INF/view/a/pspHome/columns/timeClock/timeClockDetail25.jsp"></c:import>
        </div>
        <%-- ************************ A C T I V I T Y   C O L U M N ********************************************************* --%>
        <div class="col-12 col-lg-7 col-xl-6 order-first order-xl-2 ">
            <c:import url="/WEB-INF/view/activity/renew/addRenewalForm.jsp"></c:import>
        </div>
        <%-- ************************ T O D O   C O L U M N ********************************************************* --%>
        <div class="col-12 col-lg-5 col-xl-3 order-2 order-xl-first">
            <c:import url="/WEB-INF/view/a/pspHome/columns/toDos/toDoHeader.jsp"></c:import>
            <c:import url="/WEB-INF/view/a/pspHome/columns/toDos/toDoCurrentList25.jsp"></c:import>
            <c:import url="/WEB-INF/view/a/pspHome/columns/toDos/toDoFutureList25.jsp"></c:import>
        </div>
    </div>
</div>
</body>
</html>