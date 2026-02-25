<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<div class="hdr-bar d-flex align-items-center justify-content-between mt-2" style="border-radius: 8px 8px 0 0; flex-shrink:0;">
  <span><i class="bi bi-check2-square me-1"></i>ToDos</span>
  <div class="d-flex gap-1">
    <button class="btn btn-sm btn-outline-light" style="font-size: 0.7rem; padding: 0.15rem 0.45rem;" type="button" data-bs-toggle="modal" data-bs-target="#addReminderModal" title="New Reminder">
      <i class="bi bi-bell"></i>
    </button>
    <button class="btn btn-sm btn-outline-light" style="font-size: 0.7rem; padding: 0.15rem 0.45rem;" type="button" data-bs-toggle="modal" data-bs-target="#addSimpleChecklistModal" title="New Checklist">
      <i class="bi bi-journal-plus"></i>
    </button>
  </div>
</div>
