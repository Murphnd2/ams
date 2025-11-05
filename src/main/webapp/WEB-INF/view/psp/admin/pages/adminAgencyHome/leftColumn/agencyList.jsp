<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="col">
  <div class="d-flex mb-1 justify-content-center">
    <h4 class="fw-bold p-2 ps-3 pe-3 position-relative">
        <label>&nbsp;Agency List&nbsp;</label>
       <button type="button" class="btn btn-outline-danger btn-sm position-absolute top-50 start-100 translate-middle p-1 pt-0 pb-0" data-bs-toggle="modal" data-bs-target="#addAgencyModal">+</button>
    </h4>
  </div>
  <c:import url="/WEB-INF/view/psp/admin/forms/pspAgencyListForm.jsp"></c:import>
</div>
