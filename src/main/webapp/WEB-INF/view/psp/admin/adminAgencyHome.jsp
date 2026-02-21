<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>Agency Home</title>
</head>
<body>
<div class="container-fluid">
  <%-- ****** N A V I G A T I O N   B A R ******************************** --%>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>ar.jsp"></c:import>
  <div class="row">
    <%-- ****  L E F T   C O L U M N ****** --%>
    <div class="col-lg-3">
      <c:import url="/WEB-INF/view/psp/admin/pages/adminAgencyHome/leftColumn.jsp"></c:import>
    </div>
    <%-- **** M A I N   S E C T I O N ****** --%>
    <div class ="col-lg-9">
      <c:import url="/WEB-INF/view/psp/admin/pages/adminAgencyHome/mainSection.jsp"></c:import>
    </div>
  </div>
</div>
<%-- **************** M O D A L   S E C T I O N  ***************************** --%>
<c:import url="/WEB-INF/view/psp/admin/modals/addProposalModal.jsp"></c:import>
<c:import url="/WEB-INF/view/psp/admin/modals/addProspectModal.jsp"></c:import>
<c:import url="/WEB-INF/view/psp/admin/modals/assignRemoveAgentModal.jsp"></c:import>
<c:import url="/WEB-INF/view/psp/admin/modals/addAgencyModal.jsp"></c:import>
<c:import url="/WEB-INF/view/psp/admin/modals/addPspAgentModal.jsp"></c:import>
</body>
</html>
