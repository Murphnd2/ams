<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${questionnaire.name} — ${pspName}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <style>
        :root {
            --psp-primary: ${primaryColor};
            --psp-accent: ${accentColor};
        }
        body { background: #f8f9fa; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
        .app-header { background: var(--psp-primary); color: white; padding: 2rem 0; }
        .app-header h1 { font-size: 1.6rem; font-weight: 600; margin: 0; }
        .app-header .subtitle { opacity: 0.85; font-size: 0.95rem; }
        .accent-bar { height: 4px; background: var(--psp-accent); }
        .app-container { max-width: 800px; margin: 0 auto; padding: 2rem 1rem 4rem; }
        .section-card { background: white; border-radius: 8px; margin-bottom: 1.5rem; box-shadow: 0 1px 3px rgba(0,0,0,0.08); overflow: hidden; }
        .section-header { background: var(--psp-primary); color: white; padding: 0.75rem 1.25rem; font-weight: 600; font-size: 1.05rem; }
        .section-body { padding: 1.25rem; }
        .field-group { margin-bottom: 1rem; }
        .field-group label { font-weight: 500; margin-bottom: 0.25rem; display: block; }
        .field-group .help-text { font-size: 0.8rem; color: #6c757d; margin-top: 0.2rem; }
        .field-group .form-check { margin-bottom: 0.25rem; }
        .required-star { color: #dc3545; margin-left: 2px; }
        .btn-submit { background: var(--psp-accent); border-color: var(--psp-accent); color: white; font-size: 1.1rem; font-weight: 600; padding: 0.7rem 3rem; border-radius: 6px; }
        .btn-submit:hover { background: var(--psp-primary); border-color: var(--psp-primary); color: white; }
        .progress-bar-custom { height: 6px; background: #e9ecef; border-radius: 3px; margin-bottom: 2rem; }
        .progress-bar-fill { height: 100%; background: var(--psp-accent); border-radius: 3px; transition: width 0.3s; }
        .app-footer { text-align: center; color: #999; font-size: 0.85rem; padding: 2rem 0; border-top: 1px solid #e9ecef; }
        .submitted-banner { background: #d1ecf1; color: #0c5460; border: 1px solid #bee5eb; border-radius: 6px; padding: 1rem 1.25rem; margin-bottom: 1.5rem; }
    </style>
</head>
<body>

<%-- Header --%>
<div class="app-header">
    <div class="app-container" style="padding-top:0;padding-bottom:0;">
        <h1>${pspName}</h1>
        <div class="subtitle">${questionnaire.name} — ${activityName}</div>
    </div>
</div>
<div class="accent-bar"></div>

<div class="app-container">

    <%-- Submitted Banner --%>
    <c:if test="${readOnly}">
        <div class="submitted-banner">
            <i class="bi bi-check-circle-fill me-2"></i>
            <strong>This questionnaire has been submitted.</strong>
            <c:if test="${instance.dateSubmitted != null}">
                <span class="ms-1">Submitted on ${instance.dateSubmitted}</span>
            </c:if>
            <c:if test="${instance.submittedByName != null}">
                <span class="ms-1">by ${instance.submittedByName}</span>
            </c:if>
        </div>
    </c:if>

    <%-- Description --%>
    <c:if test="${questionnaire.description != null && not empty questionnaire.description}">
        <div class="section-card">
            <div class="section-body">
                <p class="text-muted mb-0" style="font-size: 0.95rem;">${questionnaire.description}</p>
            </div>
        </div>
    </c:if>

    <%-- Progress Bar (hidden when read-only) --%>
    <c:if test="${!readOnly}">
        <div class="progress-bar-custom">
            <div class="progress-bar-fill" id="progressFill" style="width: 0%"></div>
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/q/${instance.instanceGuid}" id="questionnaireForm">

        <%-- Submitter Info Section --%>
        <div class="section-card">
            <div class="section-header">
                <i class="bi bi-person-circle me-2"></i>Your Information
            </div>
            <div class="section-body">
                <div class="row">
                    <div class="col-md-6">
                        <div class="field-group">
                            <label for="_submitter_name">Your Name <span class="required-star">*</span></label>
                            <input type="text" class="form-control form-control-sm" id="_submitter_name"
                                   name="_submitter_name" required
                                   value="${defaults.containsKey('_submitter_name') ? defaults.get('_submitter_name') : ''}"
                                   ${readOnly ? 'disabled' : ''}>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="field-group">
                            <label for="_submitter_email">Your Email <span class="required-star">*</span></label>
                            <input type="email" class="form-control form-control-sm" id="_submitter_email"
                                   name="_submitter_email" required
                                   value="${defaults.containsKey('_submitter_email') ? defaults.get('_submitter_email') : ''}"
                                   ${readOnly ? 'disabled' : ''}>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <%-- Fields grouped by sectionName --%>
        <c:set var="currentSection" value="" />
        <c:set var="sectionOpen" value="false" />

        <c:forEach var="field" items="${fields}" varStatus="fieldStatus">
            <%-- Check if we need to start a new section --%>
            <c:set var="fieldSection" value="${field.sectionName != null ? field.sectionName : 'General'}" />

            <c:if test="${fieldSection != currentSection}">
                <%-- Close previous section if open --%>
                <c:if test="${sectionOpen}">
                    </div></div>
                </c:if>
                <%-- Open new section --%>
                <div class="section-card">
                    <div class="section-header">
                        <i class="bi bi-clipboard-check me-2"></i>${fieldSection}
                    </div>
                    <div class="section-body">
                <c:set var="currentSection" value="${fieldSection}" />
                <c:set var="sectionOpen" value="true" />
            </c:if>

            <div class="field-group">

                <%-- Label (skip for BOOLEAN — label is inline) --%>
                <c:if test="${field.fieldType != 'BOOLEAN'}">
                    <label for="${field.fieldKey}">
                        ${field.label}
                        <c:if test="${field.required}"><span class="required-star">*</span></c:if>
                    </label>
                </c:if>

                <%-- TEXT --%>
                <c:if test="${field.fieldType == 'TEXT'}">
                    <input type="text" class="form-control form-control-sm" id="${field.fieldKey}"
                           name="${field.fieldKey}" ${field.required ? 'required' : ''} ${readOnly ? 'disabled' : ''}
                           value="${defaults.containsKey(field.fieldKey) ? defaults.get(field.fieldKey) : ''}">
                </c:if>

                <%-- TEXTAREA --%>
                <c:if test="${field.fieldType == 'TEXTAREA'}">
                    <textarea class="form-control form-control-sm" id="${field.fieldKey}"
                              name="${field.fieldKey}" rows="3" ${field.required ? 'required' : ''} ${readOnly ? 'disabled' : ''}>${defaults.containsKey(field.fieldKey) ? defaults.get(field.fieldKey) : ''}</textarea>
                </c:if>

                <%-- NUMBER --%>
                <c:if test="${field.fieldType == 'NUMBER'}">
                    <input type="number" class="form-control form-control-sm" id="${field.fieldKey}"
                           name="${field.fieldKey}" ${field.required ? 'required' : ''} ${readOnly ? 'disabled' : ''}
                           value="${defaults.containsKey(field.fieldKey) ? defaults.get(field.fieldKey) : ''}">
                </c:if>

                <%-- DATE --%>
                <c:if test="${field.fieldType == 'DATE'}">
                    <input type="date" class="form-control form-control-sm" id="${field.fieldKey}"
                           name="${field.fieldKey}" ${field.required ? 'required' : ''} ${readOnly ? 'disabled' : ''}
                           value="${defaults.containsKey(field.fieldKey) ? defaults.get(field.fieldKey) : ''}">
                </c:if>

                <%-- SELECT --%>
                <c:if test="${field.fieldType == 'SELECT'}">
                    <select class="form-select form-select-sm" id="${field.fieldKey}"
                            name="${field.fieldKey}" ${field.required ? 'required' : ''} ${readOnly ? 'disabled' : ''}>
                        <option value="">— Select —</option>
                        <c:forEach var="opt" items="${field.selectOptionsList}">
                            <option value="${opt}" ${defaults.containsKey(field.fieldKey) && defaults.get(field.fieldKey) == opt ? 'selected' : ''}>${opt}</option>
                        </c:forEach>
                    </select>
                </c:if>

                <%-- RADIO --%>
                <c:if test="${field.fieldType == 'RADIO'}">
                    <div>
                        <c:forEach var="opt" items="${field.selectOptionsList}" varStatus="optStatus">
                            <div class="form-check form-check-inline">
                                <input class="form-check-input" type="radio"
                                       name="${field.fieldKey}" id="${field.fieldKey}_${optStatus.index}"
                                       value="${opt}" ${field.required ? 'required' : ''} ${readOnly ? 'disabled' : ''}
                                       ${defaults.containsKey(field.fieldKey) && defaults.get(field.fieldKey) == opt ? 'checked' : ''}>
                                <label class="form-check-label fw-normal" for="${field.fieldKey}_${optStatus.index}">${opt}</label>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>

                <%-- BOOLEAN --%>
                <c:if test="${field.fieldType == 'BOOLEAN'}">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox"
                               name="${field.fieldKey}" id="${field.fieldKey}" value="Yes" ${readOnly ? 'disabled' : ''}
                               ${defaults.containsKey(field.fieldKey) && defaults.get(field.fieldKey) == 'Yes' ? 'checked' : ''}>
                        <label class="form-check-label" for="${field.fieldKey}">
                            ${field.label}
                            <c:if test="${field.required}"><span class="required-star">*</span></c:if>
                        </label>
                    </div>
                </c:if>

                <%-- CHECKBOX (multi-select) --%>
                <c:if test="${field.fieldType == 'CHECKBOX'}">
                    <div class="row">
                        <c:forEach var="opt" items="${field.selectOptionsList}" varStatus="optStatus">
                            <div class="col-6">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox"
                                           name="${field.fieldKey}" id="${field.fieldKey}_${optStatus.index}"
                                           value="${opt}" ${readOnly ? 'disabled' : ''}
                                           ${defaults.containsKey(field.fieldKey) && fn:contains(defaults.get(field.fieldKey), opt) ? 'checked' : ''}>
                                    <label class="form-check-label fw-normal" for="${field.fieldKey}_${optStatus.index}">${opt}</label>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>

                <%-- Help Text --%>
                <c:if test="${field.helpText != null}">
                    <div class="help-text">${field.helpText}</div>
                </c:if>

            </div>
        </c:forEach>

        <%-- Close last section --%>
        <c:if test="${sectionOpen}">
            </div></div>
        </c:if>

        <%-- Submit Buttons (hidden when read-only) --%>
        <c:if test="${!readOnly}">
            <div class="text-center mt-4 mb-3">
                <button type="button" class="btn btn-outline-secondary me-3" id="btnSave" onclick="saveProgress()">
                    <i class="bi bi-save me-1"></i>Save Progress
                </button>
                <button type="submit" class="btn btn-submit" id="btnSubmit">
                    <i class="bi bi-send me-2"></i>Submit Questionnaire
                </button>
                <div id="saveStatus" class="text-muted mt-2" style="font-size:0.85rem;"></div>
            </div>
        </c:if>

    </form>

    <div class="app-footer">
        <p class="mb-0">&copy; ${pspName} &middot; Benefits Administration Services</p>
    </div>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    // =========================================================================
    // Progress bar
    // =========================================================================
    var form = document.getElementById('questionnaireForm');
    var progressFill = document.getElementById('progressFill');

    function updateProgress() {
        if (!progressFill) return;
        var inputs = form.querySelectorAll('input[type="text"], input[type="number"], input[type="date"], input[type="email"], select, textarea');
        var filled = 0;
        var total = 0;
        inputs.forEach(function(input) {
            var fg = input.closest('.field-group');
            if (fg && fg.style.display === 'none') return;
            total++;
            if (input.value && input.value.trim() !== '') filled++;
        });
        var radioGroups = {};
        form.querySelectorAll('input[type="radio"]').forEach(function(r) {
            var fg = r.closest('.field-group');
            if (fg && fg.style.display === 'none') return;
            radioGroups[r.name] = radioGroups[r.name] || false;
            if (r.checked) radioGroups[r.name] = true;
        });
        var radioNames = Object.keys(radioGroups);
        total += radioNames.length;
        filled += radioNames.filter(function(n) { return radioGroups[n]; }).length;
        var pct = total > 0 ? Math.round((filled / total) * 100) : 0;
        progressFill.style.width = pct + '%';
    }

    // =========================================================================
    // Event listeners
    // =========================================================================
    var isDirty = false;
    form.addEventListener('input', function() { updateProgress(); isDirty = true; });
    form.addEventListener('change', function() { updateProgress(); isDirty = true; });

    // Submit button loading state
    form.addEventListener('submit', function() {
        var btn = document.getElementById('btnSubmit');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Submitting...';
        isDirty = false;
    });

    // =========================================================================
    // Save Progress (manual + auto)
    // =========================================================================
    var instanceGuid = '${instance.instanceGuid}';

    function saveProgress() {
        var btn = document.getElementById('btnSave');
        var status = document.getElementById('saveStatus');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Saving...';
        var formData = new FormData(form);

        fetch('${pageContext.request.contextPath}/saveQuestionnaire?guid=' + encodeURIComponent(instanceGuid), {
            method: 'POST',
            body: formData
        })
            .then(function(resp) {
                if (!resp.ok) throw new Error('Server ' + resp.status);
                return resp.json();
            })
            .then(function(data) {
                isDirty = false;
                var now = new Date();
                var timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                status.innerHTML = '<i class="bi bi-check-circle text-success me-1"></i>Saved at ' + timeStr;
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-save me-1"></i>Save Progress';
            })
            .catch(function(err) {
                status.innerHTML = '<i class="bi bi-exclamation-circle text-danger me-1"></i>Save failed';
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-save me-1"></i>Save Progress';
            });
    }

    setInterval(function() {
        if (isDirty) saveProgress();
    }, 60000);

    window.addEventListener('beforeunload', function(e) {
        if (isDirty) { e.preventDefault(); e.returnValue = ''; }
    });

    // =========================================================================
    // Initialize
    // =========================================================================
    updateProgress();
</script>
</body>
</html>
