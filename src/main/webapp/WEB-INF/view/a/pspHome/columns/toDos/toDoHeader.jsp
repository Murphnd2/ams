<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
