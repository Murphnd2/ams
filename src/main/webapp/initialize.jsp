<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<c:url var="logo1" value="logo.png"></c:url>
<c:url var="logo2" value="logo02.png"></c:url>
<c:url var="logo3" value="logo03.png"></c:url>
<c:url var="logoA" value="/images/logoA.png"></c:url>
<c:url var="logoC" value="/images/logoC.png"></c:url>
<!DOCTYPE html>
<html>
<head>
  <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
  <title>
    <c:choose>
      <c:when test="${sessionScope.uninitialized!=1}">
        ${sessionScope.psp.getFullName()}
      </c:when>
      <c:otherwise>
        AMS
      </c:otherwise>
    </c:choose>
  </title>
  <style>
    .input-group-text{
      background-color: #06357a;
      color: white;
    }
    .input-group-sm{
      margin-bottom: 10px;
    }
  </style>
</head>
<body>
<div class="container-fluid">
  <c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>
  <c:choose>
    <c:when test="${sessionScope.uninitialized!=1}">
      <div class="row">
        <div class="col">
          <form method="post" action="InitializeDataBase">
            <div class="input-group input-group-sm">
              <label for="deploymentKey" class="input-group-text">Deployment Key</label>
              <input type="password" class="form-control" name="deploymentKey" id="deploymentKey" required>
            </div>
            <div class="row">
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="pspName" class="input-group-text dk" style="min-width: 25%">PSP Name</label>
                  <input type="text" class="form-control" name="pspName" id="pspName" required>
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="firstName" class="input-group-text" style="min-width: 25%">Primary Contact</label>
                  <input type="text" class="form-control" name="firstName" id="firstName" placeholder="First Name" required>
                  <input type="text" class="form-control" name="lastName" id="lastName" placeholder="Last Name" required>
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="email" class="input-group-text" style="min-width: 25%">Primary Email</label>
                  <input type="email" class="form-control" name="email" placeholder="This will also be your UserName" id="email" required>
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="password" class="input-group-text" style="min-width: 25%">Desired Password</label>
                  <input type="text" class="form-control" name="password" id="password" minlength="8" required>
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="address" class="input-group-text" style="min-width: 25%">Address</label>
                  <input type="text" class="form-control" name="address" id="address" required>
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="city" class="input-group-text" style="min-width: 25%">City, State, Zip</label>
                  <input type="text" class="form-control" name="city" id="city" placeholder="City" required>
                  <input type="text" class="form-control" name="state" id="state" placeholder="2 Character State" required minlength="2" maxlength="2">
                  <input type="number" class="form-control" name="zipCode" id="zipCode" placeholder="5 Digit Zip Code" required minlength="5" maxlength="5">
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="domain" class="input-group-text" style="min-width: 25%">Domain Name</label>
                  <input type="url" class="form-control" name="domain" id="domain" placeholder="Example - https://yourDomainName.com" required>
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="smtpServer" class="input-group-text" style="min-width: 25%">SMTP Server/Port</label>
                  <input type="text" class="form-control" name="smtpServer" id="smtpServer" placeholder="Example - smtp.gmail.com" required>
                  <input type="text" class="form-control" name="smtpPort" id="smtpPort" placeholder="Port #" required>
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="smtpUser" class="input-group-text" style="min-width: 25%">SMTP User/Pwd</label>
                  <input type="text" class="form-control" name="smtpUser" placeholder="SMTP Username" id="smtpUser" required>
                  <input type="text" class="form-control" name="smtpPwd" placeholder="SMTP Password" id="smtpPwd" required>
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="phone" class="input-group-text" style="min-width: 25%">Phone #</label>
                  <input type="tel" class="form-control" name="phone" id="phone" placeholder="###-###-#### format" required>
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="taxId" class="input-group-text" style="min-width: 25%">Tax Id</label>
                  <input type="text" class="form-control" name="taxId" id="taxId" placeholder="##-####### format" required>
                </div>
              </div>
              <div class="col-12">
                <div class="input-group input-group-sm">
                  <label for="summit" class="input-group-text" style="min-width: 25%">Summit Admin Path</label>
                  <input type="url" class="form-control" name="summit" id="summit" placeholder="Example - yourDomain.summitWith.us" required>
                </div>
              </div>
              <div class="col-12">
                <button type="submit" class="btn btn-secondary btn-sm w-100">
                  <i class="bi bi-exclamation-triangle"></i>&nbsp;Initialize&nbsp;Database
                </button>
              </div>
            </div>

          </form>
        </div>
      </div>
    </c:when>

    <c:otherwise>
      <div class="row mt-5">
        <div class="col">
          <div class="row pt-1 mt-1">
            <div class="col"></div>
            <div class="col-auto">
              <img src="${pageContext.request.contextPath}/images/logoC.png" class="img-fluid" alt="Administrator Logo" style="height: 650px">
            </div>
            <div class="col"></div>
          </div>

        </div>
      </div>
    </c:otherwise>
  </c:choose>


</div>
</body>
</html>
