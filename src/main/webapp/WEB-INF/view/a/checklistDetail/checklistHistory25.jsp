<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="hdr-bar mt-2 mb-0 d-flex align-items-center justify-content-between">
  <span><i class="bi bi-clock-history me-2"></i>Past Completions</span>
  <span id="historyCount" class="badge bg-light text-secondary" style="font-size:0.7rem;">—</span>
</div>

<div id="historyPanel" class="overflow-auto flex-grow-1" style="min-height:0; padding: 0.5rem 0.25rem;">
  <div id="historyLoading" class="text-center text-muted fst-italic py-3" style="font-size:0.85rem;">
    <i class="bi bi-arrow-repeat me-1"></i>Loading history...
  </div>
  <div id="historyEmpty" class="text-center text-muted py-3" style="font-size:0.85rem; display:none;">
    <i class="bi bi-check2-all me-1"></i>No past completions yet.
  </div>
  <div id="historyAccordion" class="accordion accordion-flush" style="display:none;"></div>
</div>

<script>
(function() {
  const checklistId = '${sessionScope.local.getCurrentActivity().getActivity().getId()}';

  fetch('ViewRecurringHistory25?checklistId=' + checklistId)
    .then(r => r.json())
    .then(data => {
      document.getElementById('historyLoading').style.display = 'none';
      const accordion = document.getElementById('historyAccordion');
      const badge = document.getElementById('historyCount');
      badge.textContent = data.length;

      if (data.length === 0) {
        document.getElementById('historyEmpty').style.display = 'block';
        return;
      }

      accordion.style.display = 'block';

      data.forEach(function(run, idx) {
        const collapseId = 'histRun' + idx;
        const dueLabel = run.dueDate ? run.dueDate : '\u2014';
        const completedLabel = run.completedBy ? ' \u00b7 ' + run.completedBy : '';

        // Count tasks with notes for badge
        let noteCount = 0;
        (run.tasks || []).forEach(function(t) { noteCount += (t.notes || []).length; });
        const noteBadge = noteCount > 0
          ? '<span class="badge ms-2" style="background:#0d5681;font-size:0.65rem;">' + noteCount + ' note' + (noteCount > 1 ? 's' : '') + '</span>'
          : '';

        let tasksHtml = '';
        (run.tasks || []).forEach(function(t) {
          const icon = t.isComplete
            ? '<i class="bi bi-check-circle-fill text-success me-2" style="font-size:0.8rem;"></i>'
            : '<i class="bi bi-circle text-muted me-2" style="font-size:0.8rem;"></i>';
          const nameStyle = t.isComplete ? 'text-decoration:line-through;color:#6c757d;' : '';
          tasksHtml += '<div class="d-flex align-items-start py-1 px-2" style="border-bottom:1px solid #f0f0f0;">';
          tasksHtml += '<span class="mt-1">' + icon + '</span>';
          tasksHtml += '<div style="flex:1;min-width:0;">';
          tasksHtml += '<div style="font-size:0.8rem;' + nameStyle + '">' + escHtml(t.taskName) + '</div>';

          (t.notes || []).forEach(function(n) {
            const badgeColor = n.sourceType === 'BPO' ? '#0d5681' : '#6c757d';
            const badgeLabel = n.sourceType === 'BPO' ? 'BPO' : 'PSP';
            tasksHtml += '<div class="mt-1 p-1 rounded" style="background:#f8f9fa;font-size:0.75rem;">';
            tasksHtml += '<span class="badge me-1" style="background:' + badgeColor + ';font-size:0.65rem;">' + badgeLabel + '</span>';
            tasksHtml += '<span style="color:#555;">' + escHtml(n.author) + '</span>';
            if (n.date) tasksHtml += '<span class="text-muted ms-1" style="font-size:0.7rem;">' + n.date + '</span>';
            tasksHtml += '<div class="mt-1" style="color:#333;">' + escHtml(n.text) + '</div>';
            if (n.attachments && n.attachments.length > 0) {
              tasksHtml += '<div style="margin-top:0.25rem;">';
              n.attachments.forEach(function(att) {
                tasksHtml += '<a href="' + att.url + '" target="_blank" '
                  + 'style="display:inline-block;font-size:0.7rem;padding:0.1rem 0.4rem;'
                  + 'background:#e8f4f8;color:#0d5681;border-radius:12px;text-decoration:none;'
                  + 'margin-right:0.3rem;margin-bottom:0.2rem;">'
                  + '<i class="bi bi-paperclip"></i> ' + escHtml(att.name) + '</a>';
              });
              tasksHtml += '</div>';
            }
            tasksHtml += '</div>';
          });

          tasksHtml += '</div></div>';
        });

        const item = document.createElement('div');
        item.className = 'accordion-item';
        item.style.border = '1px solid #dee2e6';
        item.style.marginBottom = '0.35rem';
        item.style.borderRadius = '4px';
        item.innerHTML =
          '<h2 class="accordion-header">' +
            '<button class="accordion-button collapsed py-2 px-3" type="button" ' +
              'data-bs-toggle="collapse" data-bs-target="#' + collapseId + '" ' +
              'style="font-size:0.82rem;background:#f8f9fa;">' +
              '<i class="bi bi-calendar-check me-2 text-muted"></i>' +
              '<span>Due: <strong>' + dueLabel + '</strong>' + completedLabel + '</span>' +
              noteBadge +
            '</button>' +
          '</h2>' +
          '<div id="' + collapseId + '" class="accordion-collapse collapse">' +
            '<div class="accordion-body p-0">' + tasksHtml + '</div>' +
          '</div>';
        accordion.appendChild(item);
      });
    })
    .catch(function() {
      document.getElementById('historyLoading').style.display = 'none';
      document.getElementById('historyEmpty').style.display = 'block';
      document.getElementById('historyEmpty').innerHTML =
        '<i class="bi bi-exclamation-circle me-1 text-muted"></i>Could not load history.';
    });

  function escHtml(s) {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }
})();
</script>
