<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%--
  Time Correction Request Modal
  Include this in pspHome25.jsp (or alongside the timeclock detail JSP).
  Populated dynamically via JavaScript when user clicks a completed stretch.

  tcOpenCorrection() now accepts two extra parameters:
    boundaryBefore - the out-time (HH:mm) of the previous stretch, or '' if none
    boundaryAfter  - the in-time (HH:mm) of the next stretch, or '' if none
--%>

<style>
  .tc-correction-modal .modal-header {
    background-color: var(--ssa); color: #fff; padding: 0.5rem 1rem;
  }
  .tc-correction-modal .modal-title { font-weight: 600; }
  .tc-correction-modal .btn-close { filter: brightness(0) invert(1); }
  .tc-correction-modal .form-label { font-size: 0.82rem; font-weight: 600; color: #495057; }
  .tc-correction-modal .tc-original-badge {
    display: inline-block; background: #e9ecef; border-radius: 6px;
    padding: 0.35rem 0.7rem; font-size: 0.88rem; font-weight: 600;
    color: #212529;
  }
  .tc-correction-modal .tc-arrow {
    color: #198754; font-weight: 700; font-size: 1.1rem; margin: 0 0.5rem;
    display: none;
  }
  .tc-correction-modal .tc-change-row {
    display: flex; align-items: center; gap: 0.5rem;
    padding: 0.6rem 0; border-bottom: 1px solid #f0f1f3;
  }
  .tc-correction-modal .tc-change-row:last-of-type { border-bottom: none; }
  .tc-correction-modal .form-check-input:checked { background-color: #0d5681; border-color: #0d5681; }
  .tc-correction-modal input[type="time"] { width: 140px; }
  .tc-correction-modal textarea { resize: vertical; min-height: 60px; }
  .tc-stretch-clickable {
    cursor: pointer; border-radius: 4px; padding: 0.4rem 0.25rem;
    transition: background 0.12s ease; margin: 0 -0.25rem;
  }
  .tc-stretch-clickable:hover { background: #e8f1f8; }
  .tc-validation-error {
    color: #dc3545; font-size: 0.82rem; font-weight: 600;
    margin-top: 0.5rem; display: none;
  }
  .tc-boundary-hint {
    font-size: 0.72rem; color: #6c757d; margin-top: 0.25rem;
  }
</style>

<%-- CORRECTION REQUEST MODAL --%>
<div class="modal fade tc-correction-modal" id="tcCorrectionModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">

      <div class="modal-header">
        <h6 class="modal-title fw-semibold"><i class="bi bi-pencil-square me-2"></i>Request Time Correction</h6>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>

      <form method="post" action="SubmitTimeCorrection" id="tcCorrectionForm">
        <div class="modal-body">

          <%-- Hidden fields --%>
          <input type="hidden" name="inLogId" id="tcInLogId">
          <input type="hidden" name="outLogId" id="tcOutLogId">
          <input type="hidden" name="originalDate" id="tcOriginalDate">
          <input type="hidden" name="originalInTime" id="tcOriginalInTime">
          <input type="hidden" name="originalOutTime" id="tcOriginalOutTime">
          <%-- Boundary times for overlap validation --%>
          <input type="hidden" id="tcBoundaryBefore" value="">
          <input type="hidden" id="tcBoundaryAfter" value="">

          <%-- Date display --%>
          <div class="mb-3">
            <label class="form-label">Date</label>
            <div id="tcDateDisplay" class="tc-original-badge"></div>
          </div>

          <%-- IN TIME correction --%>
          <div class="tc-change-row">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" id="tcChangeInCheck"
                     onchange="tcToggleField('In')">
              <input type="hidden" name="changeIn" id="tcChangeInHidden" value="false">
            </div>
            <div style="flex:1;">
              <label class="form-label mb-0">Clock In</label>
              <div class="d-flex align-items-center mt-1">
                <span class="tc-original-badge" id="tcOriginalInDisplay"></span>
                <span class="tc-arrow" id="tcArrowIn"><i class="bi bi-arrow-right"></i></span>
                <input type="time" class="form-control form-control-sm" name="newInTime"
                       id="tcNewInTime" style="display:none;" required disabled
                       onchange="tcUpdateSubmit()">
              </div>
              <div class="tc-boundary-hint" id="tcHintIn" style="display:none;"></div>
            </div>
          </div>

          <%-- OUT TIME correction --%>
          <div class="tc-change-row">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" id="tcChangeOutCheck"
                     onchange="tcToggleField('Out')">
              <input type="hidden" name="changeOut" id="tcChangeOutHidden" value="false">
            </div>
            <div style="flex:1;">
              <label class="form-label mb-0">Clock Out</label>
              <div class="d-flex align-items-center mt-1">
                <span class="tc-original-badge" id="tcOriginalOutDisplay"></span>
                <span class="tc-arrow" id="tcArrowOut"><i class="bi bi-arrow-right"></i></span>
                <input type="time" class="form-control form-control-sm" name="newOutTime"
                       id="tcNewOutTime" style="display:none;" required disabled
                       onchange="tcUpdateSubmit()">
              </div>
              <div class="tc-boundary-hint" id="tcHintOut" style="display:none;"></div>
            </div>
          </div>

          <%-- Validation error --%>
          <div id="tcValidationError" class="tc-validation-error"></div>

          <%-- Note --%>
          <div class="mt-3">
            <label class="form-label" for="tcNote">Reason for correction</label>
            <textarea class="form-control form-control-sm" name="correctionNote" id="tcNote"
                      maxlength="500" placeholder="e.g. Forgot to clock out for lunch..."></textarea>
          </div>

        </div>

        <div class="modal-footer justify-content-center border-0">
          <button type="submit" class="ssa-action save" id="tcSubmitBtn" disabled>
            <i class="bi bi-send me-1"></i>Submit Request
          </button>
          <span class="ssa-action-sep">|</span>
          <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
        </div>
      </form>

    </div>
  </div>
</div>

<script>
  /**
   * Opens the correction modal populated with the stretch data.
   *
   * @param inLogId       TimeLog.log_id for the IN punch
   * @param outLogId      TimeLog.log_id for the OUT punch
   * @param dateStr       date as "yyyy-MM-dd"
   * @param dateLabel     display date like "Friday, Feb 21"
   * @param inTime24      in time as "HH:mm"
   * @param outTime24     out time as "HH:mm"
   * @param inDisplay     in time display like "9:14 AM"
   * @param outDisplay    out time display like "12:01 PM"
   * @param boundaryBefore  out-time of previous stretch (HH:mm), or '' if first stretch
   * @param boundaryAfter   in-time of next stretch (HH:mm), or '' if last stretch
   */
  function tcOpenCorrection(inLogId, outLogId, dateStr, dateLabel,
                            inTime24, outTime24, inDisplay, outDisplay,
                            boundaryBefore, boundaryAfter) {
    document.getElementById('tcInLogId').value = inLogId;
    document.getElementById('tcOutLogId').value = outLogId;
    document.getElementById('tcOriginalDate').value = dateStr;
    document.getElementById('tcOriginalInTime').value = inTime24;
    document.getElementById('tcOriginalOutTime').value = outTime24;
    document.getElementById('tcBoundaryBefore').value = boundaryBefore || '';
    document.getElementById('tcBoundaryAfter').value = boundaryAfter || '';

    document.getElementById('tcDateDisplay').textContent = dateLabel;
    document.getElementById('tcOriginalInDisplay').textContent = inDisplay;
    document.getElementById('tcOriginalOutDisplay').textContent = outDisplay;

    document.getElementById('tcNewInTime').value = inTime24;
    document.getElementById('tcNewOutTime').value = outTime24;

    document.getElementById('tcChangeInCheck').checked = false;
    document.getElementById('tcChangeOutCheck').checked = false;
    tcToggleField('In');
    tcToggleField('Out');

    // Show boundary hints when adjacent stretches exist
    var hintIn = document.getElementById('tcHintIn');
    if (boundaryBefore) {
      hintIn.textContent = 'Previous stretch ends at ' + tcFormatDisplay(boundaryBefore);
      hintIn.style.display = 'block';
    } else {
      hintIn.style.display = 'none';
    }
    var hintOut = document.getElementById('tcHintOut');
    if (boundaryAfter) {
      hintOut.textContent = 'Next stretch starts at ' + tcFormatDisplay(boundaryAfter);
      hintOut.style.display = 'block';
    } else {
      hintOut.style.display = 'none';
    }

    document.getElementById('tcNote').value = '';
    document.getElementById('tcValidationError').style.display = 'none';
    document.getElementById('tcSubmitBtn').disabled = true;

    new bootstrap.Modal(document.getElementById('tcCorrectionModal')).show();
  }

  /** Convert "HH:mm" to "H:MM AM/PM" for display in hints */
  function tcFormatDisplay(time24) {
    if (!time24) return '';
    var parts = time24.split(':');
    var h = parseInt(parts[0]);
    var m = parts[1];
    var ampm = h >= 12 ? 'PM' : 'AM';
    h = h % 12;
    if (h === 0) h = 12;
    return h + ':' + m + ' ' + ampm;
  }

  /** Show/hide the new-time input when checkbox is toggled */
  function tcToggleField(which) {
    var checked = document.getElementById('tcChange' + which + 'Check').checked;
    document.getElementById('tcChange' + which + 'Hidden').value = checked ? 'true' : 'false';
    document.getElementById('tcArrow' + which).style.display = checked ? 'inline' : 'none';
    document.getElementById('tcNew' + which + 'Time').style.display = checked ? 'inline-block' : 'none';
    document.getElementById('tcNew' + which + 'Time').disabled = !checked;
    document.getElementById('tcNew' + which + 'Time').required = checked;
    tcUpdateSubmit();
  }

  /** Validate: in < out, no overlap with adjacent stretches */
  function tcValidateTimes() {
    var changeIn = document.getElementById('tcChangeInCheck').checked;
    var changeOut = document.getElementById('tcChangeOutCheck').checked;
    var errEl = document.getElementById('tcValidationError');

    var inTime = changeIn
        ? document.getElementById('tcNewInTime').value
        : document.getElementById('tcOriginalInTime').value;
    var outTime = changeOut
        ? document.getElementById('tcNewOutTime').value
        : document.getElementById('tcOriginalOutTime').value;

    var boundaryBefore = document.getElementById('tcBoundaryBefore').value;
    var boundaryAfter = document.getElementById('tcBoundaryAfter').value;

    // Need both times to validate
    if (!inTime || !outTime) {
      errEl.style.display = 'none';
      return true;
    }

    // Rule 1: in must be before out
    if (inTime >= outTime) {
      errEl.textContent = 'Clock in time must be before clock out time.';
      errEl.style.display = 'block';
      return false;
    }

    // Rule 2: new in-time can't be before the end of the previous stretch
    if (boundaryBefore && inTime < boundaryBefore) {
      errEl.textContent = 'Clock in time overlaps with the previous stretch (ends at ' + tcFormatDisplay(boundaryBefore) + ').';
      errEl.style.display = 'block';
      return false;
    }

    // Rule 3: new out-time can't be after the start of the next stretch
    if (boundaryAfter && outTime > boundaryAfter) {
      errEl.textContent = 'Clock out time overlaps with the next stretch (starts at ' + tcFormatDisplay(boundaryAfter) + ').';
      errEl.style.display = 'block';
      return false;
    }

    errEl.style.display = 'none';
    return true;
  }

  /** Enable submit only if at least one checkbox is checked AND times are valid */
  function tcUpdateSubmit() {
    var anyChecked = document.getElementById('tcChangeInCheck').checked ||
                     document.getElementById('tcChangeOutCheck').checked;
    var valid = tcValidateTimes();
    document.getElementById('tcSubmitBtn').disabled = !(anyChecked && valid);
  }
</script>
