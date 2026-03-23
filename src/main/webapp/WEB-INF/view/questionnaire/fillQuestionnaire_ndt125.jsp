<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${questionnaire.name} — ${pspName}</title>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <style>
        :root {
            --psp-primary: ${primaryColor};
            --psp-accent: ${accentColor};
        }
        body { background: #f8f9fa; font-family: 'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
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
        .btn-accent { background: var(--psp-accent); border-color: var(--psp-accent); color: white; font-weight: 600; padding: 0.5rem 2rem; border-radius: 6px; }
        .btn-accent:hover { background: var(--psp-primary); border-color: var(--psp-primary); color: white; }
        .progress-bar-custom { height: 6px; background: #e9ecef; border-radius: 3px; margin-bottom: 0.75rem; }
        .progress-bar-fill { height: 100%; background: var(--psp-accent); border-radius: 3px; transition: width 0.3s; }
        .app-footer { text-align: center; color: #999; font-size: 0.85rem; padding: 2rem 0; border-top: 1px solid #e9ecef; }
        .submitted-banner { background: #d1ecf1; color: #0c5460; border: 1px solid #bee5eb; border-radius: 6px; padding: 1rem 1.25rem; margin-bottom: 1.5rem; }

        /* Wizard-specific styles */
        .wizard-page { display: none; }
        .wizard-page.active { display: block; }
        .wizard-nav { display: flex; justify-content: space-between; align-items: center; margin-top: 1.5rem; margin-bottom: 1rem; }
        .wizard-nav .btn { min-width: 130px; }
        .wizard-progress { margin-bottom: 1.5rem; }
        .wizard-progress .page-label { font-size: 0.9rem; color: #6c757d; margin-bottom: 0.4rem; font-weight: 500; }
        .wizard-progress .section-title-display { font-size: 1.1rem; font-weight: 600; color: var(--psp-primary); margin-bottom: 0.5rem; }
        .validation-error { color: #dc3545; font-size: 0.8rem; margin-top: 0.2rem; display: none; }
        .is-invalid + .validation-error,
        .field-group.has-error .validation-error { display: block; }
        .field-group.cond-hidden { display: none !important; }
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

    <%-- Wizard Progress (hidden when read-only) --%>
    <c:if test="${!readOnly}">
        <div class="wizard-progress">
            <div class="section-title-display" id="sectionTitleDisplay"></div>
            <div class="page-label" id="pageLabel"></div>
            <div class="progress-bar-custom">
                <div class="progress-bar-fill" id="progressFill" style="width: 0%"></div>
            </div>
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/q/${instance.instanceGuid}" id="questionnaireForm">

        <%-- ================================================================
             PAGE 0: Submitter Info (always visible, always first)
             ================================================================ --%>
        <div class="wizard-page" data-section="_submitter_info" data-section-title="Your Information">
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
                                <div class="validation-error">This field is required.</div>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="field-group">
                                <label for="_submitter_email">Your Email <span class="required-star">*</span></label>
                                <input type="email" class="form-control form-control-sm" id="_submitter_email"
                                       name="_submitter_email" required
                                       value="${defaults.containsKey('_submitter_email') ? defaults.get('_submitter_email') : ''}"
                                       ${readOnly ? 'disabled' : ''}>
                                <div class="validation-error">Please enter a valid email address.</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <%-- ================================================================
             Remaining pages: one page per unique sectionName
             Use JSTL to build ALL section pages into the DOM.
             JS will show/hide them.
             ================================================================ --%>
        <c:set var="currentSection" value="" />
        <c:set var="sectionOpen" value="false" />

        <c:forEach var="field" items="${fields}" varStatus="fieldStatus">
            <c:if test="${!field.suppressed}">
                <c:set var="fieldSection" value="${field.sectionName != null ? field.sectionName : 'General'}" />

                <%-- Start new section page --%>
                <c:if test="${fieldSection != currentSection}">
                    <c:if test="${sectionOpen}">
                        </div></div></div>
                    </c:if>
                    <div class="wizard-page" data-section="${fieldSection}" data-section-title="${fieldSection}">
                        <div class="section-card">
                            <div class="section-header">
                                <i class="bi bi-clipboard-check me-2"></i>${fieldSection}
                            </div>
                            <div class="section-body">
                    <c:set var="currentSection" value="${fieldSection}" />
                    <c:set var="sectionOpen" value="true" />
                </c:if>

                <%-- Field rendering --%>
                <div class="field-group" data-field-key="${field.fieldKey}" id="fg_${field.fieldKey}">

                    <%-- Label (skip for BOOLEAN) --%>
                    <c:if test="${field.fieldType != 'BOOLEAN'}">
                        <label for="${field.fieldKey}">
                            ${field.label}
                            <c:if test="${field.required}"><span class="required-star">*</span></c:if>
                        </label>
                    </c:if>

                    <%-- TEXT --%>
                    <c:if test="${field.fieldType == 'TEXT'}">
                        <input type="text" class="form-control form-control-sm" id="${field.fieldKey}"
                               name="${field.fieldKey}" ${field.required ? 'data-required="true"' : ''} ${readOnly ? 'disabled' : ''}
                               value="${defaults.containsKey(field.fieldKey) ? defaults.get(field.fieldKey) : ''}">
                    </c:if>

                    <%-- TEXTAREA --%>
                    <c:if test="${field.fieldType == 'TEXTAREA'}">
                        <textarea class="form-control form-control-sm" id="${field.fieldKey}"
                                  name="${field.fieldKey}" rows="3" ${field.required ? 'data-required="true"' : ''} ${readOnly ? 'disabled' : ''}>${defaults.containsKey(field.fieldKey) ? defaults.get(field.fieldKey) : ''}</textarea>
                    </c:if>

                    <%-- NUMBER --%>
                    <c:if test="${field.fieldType == 'NUMBER'}">
                        <input type="number" class="form-control form-control-sm" id="${field.fieldKey}"
                               name="${field.fieldKey}" ${field.required ? 'data-required="true"' : ''} ${readOnly ? 'disabled' : ''}
                               value="${defaults.containsKey(field.fieldKey) ? defaults.get(field.fieldKey) : ''}">
                    </c:if>

                    <%-- DATE --%>
                    <c:if test="${field.fieldType == 'DATE'}">
                        <input type="date" class="form-control form-control-sm" id="${field.fieldKey}"
                               name="${field.fieldKey}" ${field.required ? 'data-required="true"' : ''} ${readOnly ? 'disabled' : ''}
                               value="${defaults.containsKey(field.fieldKey) ? defaults.get(field.fieldKey) : ''}">
                    </c:if>

                    <%-- SELECT --%>
                    <c:if test="${field.fieldType == 'SELECT'}">
                        <select class="form-select form-select-sm" id="${field.fieldKey}"
                                name="${field.fieldKey}" ${field.required ? 'data-required="true"' : ''} ${readOnly ? 'disabled' : ''}>
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
                                           value="${opt}" ${field.required ? 'data-required="true"' : ''} ${readOnly ? 'disabled' : ''}
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

                    <div class="validation-error">This field is required.</div>
                </div>
            </c:if>
        </c:forEach>

        <%-- Close last section --%>
        <c:if test="${sectionOpen}">
            </div></div></div>
        </c:if>

        <%-- Wizard Navigation --%>
        <c:if test="${!readOnly}">
            <div class="wizard-nav">
                <button type="button" class="btn btn-outline-secondary" id="btnPrev" onclick="wizardPrev()">
                    <i class="bi bi-arrow-left me-1"></i>Previous
                </button>
                <div class="text-center">
                    <button type="button" class="btn btn-outline-secondary btn-sm" id="btnSave" onclick="saveProgress()">
                        <i class="bi bi-save me-1"></i>Save Progress
                    </button>
                    <div id="saveStatus" class="text-muted mt-1" style="font-size:0.8rem;"></div>
                </div>
                <button type="button" class="btn btn-accent" id="btnNext" onclick="wizardNext()">
                    Next<i class="bi bi-arrow-right ms-1"></i>
                </button>
                <button type="submit" class="btn btn-submit" id="btnSubmit" style="display:none;">
                    <i class="bi bi-send me-2"></i>Submit
                </button>
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
    // Core references
    // =========================================================================
    var form = document.getElementById('questionnaireForm');
    var progressFill = document.getElementById('progressFill');
    var instanceGuid = '${instance.instanceGuid}';
    var isReadOnly = ${readOnly};
    var isDirty = false;
    var currentPageIndex = 0;

    // =========================================================================
    // Build page list from DOM
    // =========================================================================
    var allPages = Array.from(document.querySelectorAll('.wizard-page'));

    // =========================================================================
    // Field-level conditional visibility rules
    // "target": { "source": "fieldKey", "condition": "value" | { "op": ..., "value": ... } }
    // =========================================================================
    var fieldConditions = {
        'b_non_covered_entities':           { source: 'b_controlled_group', equals: 'Yes' },
        'b_non_covered_entity_names':       { source: 'b_non_covered_entities', equals: 'Yes' },
        'b_non_covered_entity_employee_count': { source: 'b_non_covered_entities', equals: 'Yes' },
        'c_cba_good_faith':                 { source: 'c_cba_covered', equals: 'Yes' },
        'c_cba_employee_count':             { source: 'c_cba_covered', equals: 'Yes' },
        'c_cba_eligible':                   { source: 'c_cba_covered', equals: 'Yes' },
        'd_benefits_other_describe':        { source: 'd_benefits_offered', contains: 'Other' },
        'f_100_or_fewer':                   { source: 'f_simple_cafeteria', equalsAny: ['Yes', 'Unsure'] },
        'f_contribution_requirement':       { source: 'f_100_or_fewer', equals: 'Yes' },
        'f_eligibility_requirement':        { source: 'f_contribution_requirement', equals: 'Yes' },
        'e_waiting_period_length':          { source: 'e_waiting_period', equals: 'Yes' },
        'e_minimum_age_value':              { source: 'e_minimum_age', equals: 'Yes' },
        'e_hours_requirement_value':        { source: 'e_hours_requirement', equals: 'Yes' },
        'e_eligibility_varies_describe':    { source: 'e_eligibility_varies', equals: 'Yes' },
        'j_spouse_dependent_count':         { source: 'j_spouse_dependent_exists', equals: 'Yes' },
        'j_hci_non_covered_entities':       { source: 'b_non_covered_entities', equals: 'Yes' },
        'n_classification_description':     { source: 'n_available_to_all', equals: 'No' },
        'n_total_non_excludable':           { source: 'n_available_to_all', equals: 'No' },
        'n_non_hci_eligible':               { source: 'n_available_to_all', equals: 'No' },
        'o_benefits_differ_describe':       { source: 'o_same_benefits_all', equals: 'No' },
        'p_key_employee_reductions':        { source: 'p_key_participant_count', greaterThan: 0 },
        'p_key_employer_contributions':     { source: 'p_key_participant_count', greaterThan: 0 },
        'k_hci_non_covered_entities':       { source: 'b_non_covered_entities', equals: 'Yes' },
        'm_key_non_covered_entities':       { source: 'b_non_covered_entities', equals: 'Yes' },
        'l_hce_non_covered_entities':       { source: 'b_non_covered_entities', equals: 'Yes' },
        'r_expenses_differ_describe':       { source: 'r_same_expenses', equals: 'No' },
        'r_maximum_differ_describe':        { source: 'r_same_maximum', equals: 'No' },
        'r_cost_sharing_differ_describe':   { source: 'r_same_cost_sharing', equals: 'No' },
        'r_waiting_differ_describe':        { source: 'r_same_waiting_periods', equals: 'No' },
        'r_dependent_differ_describe':      { source: 'r_same_dependent_coverage', equals: 'No' },
        'r_benefit_changes_describe':       { source: 'r_benefit_changes', equals: 'Yes' },
        's_classification_description':     { source: 's_available_to_all', equals: 'No' },
        's_total_non_excludable':           { source: 's_available_to_all', equals: 'No' },
        's_non_hce_eligible':               { source: 's_available_to_all', equals: 'No' },
        't_maximum_differ_describe':        { source: 't_same_maximum', equals: 'No' },
        't_terms_differ_describe':          { source: 't_same_terms', equals: 'No' },
        't_nonelective_same_terms':         { source: 't_nonelective_contributions', equals: 'Yes' },
        't_nonelective_describe':           { source: 't_nonelective_contributions', equals: 'Yes' },
        'u_owner_election_total':           { source: 'u_owner_participant_count', greaterThan: 0 },
        'u_spouse_dependent_participate':   { source: 'u_owner_participant_count', greaterThan: 0 },
        'u_spouse_dependent_election_total':{ source: 'u_spouse_dependent_participate', equals: 'Yes' },
        'v_hce_election_total':             { source: 'v_hce_participant_count', greaterThan: 0 },
        'v_non_hce_participant_count':      { source: 'v_hce_participant_count', greaterThan: 0 },
        'v_non_hce_election_total':         { source: 'v_hce_participant_count', greaterThan: 0 }
    };

    // =========================================================================
    // Entity-type hide rules (Non-Profit / Government)
    // =========================================================================
    var entityTypeHiddenFields = [
        'j_owners_5pct', 'k_owners_10pct', 'm_owners_5pct',
        'm_owners_1pct_150k', 'l_owners_5pct'
    ];

    // =========================================================================
    // Helper: get field value (handles radio, checkbox, select, input)
    // =========================================================================
    function getFieldValue(fieldKey) {
        var el = document.getElementById(fieldKey);
        if (el) {
            if (el.tagName === 'SELECT' || el.tagName === 'TEXTAREA') return el.value;
            if (el.type === 'checkbox') return el.checked ? 'Yes' : '';
            if (el.type === 'text' || el.type === 'number' || el.type === 'date' || el.type === 'email') return el.value;
        }
        // Radio buttons
        var radios = form.querySelectorAll('input[type="radio"][name="' + fieldKey + '"]');
        if (radios.length > 0) {
            for (var i = 0; i < radios.length; i++) {
                if (radios[i].checked) return radios[i].value;
            }
            return '';
        }
        // Multi-checkbox: collect all checked values as comma-separated
        var checks = form.querySelectorAll('input[type="checkbox"][name="' + fieldKey + '"]');
        if (checks.length > 1) {
            var vals = [];
            checks.forEach(function(cb) { if (cb.checked) vals.push(cb.value); });
            return vals.join(',');
        }
        return '';
    }

    // =========================================================================
    // Evaluate a single field condition
    // =========================================================================
    function evalCondition(cond) {
        var val = getFieldValue(cond.source);
        if (cond.equals !== undefined) {
            return val === cond.equals;
        }
        if (cond.equalsAny !== undefined) {
            return cond.equalsAny.indexOf(val) >= 0;
        }
        if (cond.contains !== undefined) {
            return val.indexOf(cond.contains) >= 0;
        }
        if (cond.greaterThan !== undefined) {
            var num = parseFloat(val);
            return !isNaN(num) && num > cond.greaterThan;
        }
        return true;
    }

    // =========================================================================
    // Apply all field-level conditions
    // =========================================================================
    function applyFieldConditions() {
        for (var targetKey in fieldConditions) {
            var fg = document.getElementById('fg_' + targetKey);
            if (!fg) continue;
            var visible = evalCondition(fieldConditions[targetKey]);
            if (visible) {
                fg.classList.remove('cond-hidden');
            } else {
                fg.classList.add('cond-hidden');
            }
        }

        // Entity-type hide rules
        var entityType = getFieldValue('a_entity_type');
        var isNonProfitOrGov = (entityType === 'Non-Profit (501(c)(3))' || entityType === 'Government Entity');
        entityTypeHiddenFields.forEach(function(key) {
            var fg = document.getElementById('fg_' + key);
            if (!fg) return;
            if (isNonProfitOrGov) {
                fg.classList.add('cond-hidden');
            } else {
                fg.classList.remove('cond-hidden');
            }
        });
    }

    // =========================================================================
    // Page-level visibility rules
    // Returns a map: sectionName -> boolean (true = visible)
    // =========================================================================
    function getPageVisibility() {
        var vis = {};
        allPages.forEach(function(p) {
            vis[p.getAttribute('data-section')] = true;
        });

        // --- Simple Cafeteria Plan knockout ---
        var scVal = getFieldValue('f_simple_cafeteria');
        var sc100 = getFieldValue('f_100_or_fewer');
        var scContrib = getFieldValue('f_contribution_requirement');
        var scElig = getFieldValue('f_eligibility_requirement');
        var simpleCafeteriaKnockout = (scVal === 'Yes' && sc100 === 'Yes' && scContrib === 'Yes' && scElig === 'Yes');

        // Sections to hide on Simple Cafeteria knockout (letters E through V minus D and F)
        // The spec says hide pages for E, G, J, N, O, M, P, H, K, Q, R, I, L, S, T, U, V
        // We match by section name prefix letter
        var knockoutLetters = ['E', 'G', 'J', 'N', 'O', 'M', 'P', 'H', 'K', 'Q', 'R', 'I', 'L', 'S', 'T', 'U', 'V'];

        // Helper to get the leading letter from a section name (e.g. "E. Eligibility" -> "E")
        function sectionLetter(sectionName) {
            if (!sectionName) return '';
            var m = sectionName.match(/^([A-Z])/);
            return m ? m[1] : '';
        }

        // --- Benefits offered value for conditional page display ---
        var benefits = getFieldValue('d_benefits_offered');
        var hasHealthFSA = benefits.indexOf('Health FSA') >= 0;
        var hasLimitedFSA = benefits.indexOf('Limited Purpose FSA') >= 0;
        var hasDCFSA = benefits.indexOf('Dependent Care FSA') >= 0;
        var hasHSA = benefits.indexOf('HSA contributions') >= 0;
        var hasOther = benefits.indexOf('Other') >= 0;
        var hasNonPOP = hasHealthFSA || hasLimitedFSA || hasDCFSA || hasHSA || hasOther;
        var has105h = hasHealthFSA || hasLimitedFSA;
        var has129 = hasDCFSA;

        // POP-only knockout pages (O, M, P)
        var popOnlyLetters = ['O', 'M', 'P'];
        // 105(h) pages (H, K, Q, R)
        var s105hLetters = ['H', 'K', 'Q', 'R'];
        // 129 pages (I, L, S, T, U, V)
        var s129Letters = ['I', 'L', 'S', 'T', 'U', 'V'];

        allPages.forEach(function(p) {
            var sec = p.getAttribute('data-section');
            var letter = sectionLetter(sec);
            if (!letter) return;

            // Simple Cafeteria knockout
            if (simpleCafeteriaKnockout && knockoutLetters.indexOf(letter) >= 0) {
                vis[sec] = false;
                return;
            }

            // POP-only knockout: O, M, P only show if non-POP benefits are selected
            if (popOnlyLetters.indexOf(letter) >= 0 && !hasNonPOP) {
                vis[sec] = false;
                return;
            }

            // 105(h) trigger: H, K, Q, R only show if Health FSA or Limited Purpose FSA
            if (s105hLetters.indexOf(letter) >= 0 && !has105h) {
                vis[sec] = false;
                return;
            }

            // 129 trigger: I, L, S, T, U, V only show if Dependent Care FSA
            if (s129Letters.indexOf(letter) >= 0 && !has129) {
                vis[sec] = false;
                return;
            }
        });

        return vis;
    }

    // =========================================================================
    // Get visible pages list
    // =========================================================================
    function getVisiblePages() {
        var vis = getPageVisibility();
        return allPages.filter(function(p) {
            return vis[p.getAttribute('data-section')] !== false;
        });
    }

    // =========================================================================
    // Show a specific page
    // =========================================================================
    function showPage(index) {
        var visiblePages = getVisiblePages();
        if (index < 0 || index >= visiblePages.length) return;

        currentPageIndex = index;

        // Hide all pages
        allPages.forEach(function(p) { p.classList.remove('active'); });

        // Show target page
        visiblePages[index].classList.add('active');

        // Update progress label
        var totalVisible = visiblePages.length;
        var sectionTitle = visiblePages[index].getAttribute('data-section-title') || '';
        var pageLabelEl = document.getElementById('pageLabel');
        var sectionTitleEl = document.getElementById('sectionTitleDisplay');
        if (pageLabelEl) pageLabelEl.textContent = 'Section ' + (index + 1) + ' of ' + totalVisible;
        if (sectionTitleEl) sectionTitleEl.textContent = sectionTitle;

        // Update progress bar
        if (progressFill) {
            var pct = totalVisible > 1 ? Math.round(((index) / (totalVisible - 1)) * 100) : 100;
            progressFill.style.width = pct + '%';
        }

        // Update nav button visibility
        updateNavButtons(index, totalVisible);

        // Scroll to top
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    // =========================================================================
    // Update nav buttons
    // =========================================================================
    function updateNavButtons(index, total) {
        var btnPrev = document.getElementById('btnPrev');
        var btnNext = document.getElementById('btnNext');
        var btnSubmit = document.getElementById('btnSubmit');
        if (!btnPrev || !btnNext || !btnSubmit) return;

        btnPrev.style.display = (index > 0) ? '' : 'none';

        var isLast = (index === total - 1);
        btnNext.style.display = isLast ? 'none' : '';
        btnSubmit.style.display = isLast ? '' : 'none';
    }

    // =========================================================================
    // Validate current page
    // Returns true if valid
    // =========================================================================
    function validateCurrentPage() {
        var visiblePages = getVisiblePages();
        var page = visiblePages[currentPageIndex];
        if (!page) return true;

        var valid = true;

        // Clear previous errors
        page.querySelectorAll('.field-group').forEach(function(fg) {
            fg.classList.remove('has-error');
            fg.querySelectorAll('.is-invalid').forEach(function(el) { el.classList.remove('is-invalid'); });
        });

        // Check each visible, required field on this page
        page.querySelectorAll('.field-group').forEach(function(fg) {
            // Skip hidden fields
            if (fg.classList.contains('cond-hidden')) return;

            var fieldKey = fg.getAttribute('data-field-key');

            // Check inputs with data-required or the native required attribute
            var inputs = fg.querySelectorAll('[data-required="true"], [required]');
            if (inputs.length === 0) return;

            // For radio/checkbox groups, check if any is checked
            var radios = fg.querySelectorAll('input[type="radio"]');
            if (radios.length > 0) {
                var anyChecked = false;
                radios.forEach(function(r) { if (r.checked) anyChecked = true; });
                if (!anyChecked && radios[0].hasAttribute('data-required')) {
                    fg.classList.add('has-error');
                    valid = false;
                }
                return;
            }

            inputs.forEach(function(input) {
                if (input.type === 'checkbox') return; // handled separately
                var val = input.value ? input.value.trim() : '';
                if (val === '') {
                    input.classList.add('is-invalid');
                    fg.classList.add('has-error');
                    valid = false;
                }
                // Email validation
                if (input.type === 'email' && val !== '') {
                    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
                        input.classList.add('is-invalid');
                        fg.classList.add('has-error');
                        valid = false;
                    }
                }
            });
        });

        // Also validate submitter fields on page 0
        if (currentPageIndex === 0) {
            var subName = document.getElementById('_submitter_name');
            var subEmail = document.getElementById('_submitter_email');
            if (subName && !subName.value.trim()) {
                subName.classList.add('is-invalid');
                subName.closest('.field-group').classList.add('has-error');
                valid = false;
            }
            if (subEmail) {
                var emailVal = subEmail.value.trim();
                if (!emailVal || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailVal)) {
                    subEmail.classList.add('is-invalid');
                    subEmail.closest('.field-group').classList.add('has-error');
                    valid = false;
                }
            }
        }

        if (!valid) {
            // Scroll to first error
            var firstErr = page.querySelector('.has-error');
            if (firstErr) firstErr.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }

        return valid;
    }

    // =========================================================================
    // Wizard navigation
    // =========================================================================
    function wizardNext() {
        if (!validateCurrentPage()) return;
        var visiblePages = getVisiblePages();
        if (currentPageIndex < visiblePages.length - 1) {
            showPage(currentPageIndex + 1);
        }
    }

    function wizardPrev() {
        if (currentPageIndex > 0) {
            showPage(currentPageIndex - 1);
        }
    }

    // =========================================================================
    // Submit handler
    // =========================================================================
    form.addEventListener('submit', function(e) {
        if (!validateCurrentPage()) {
            e.preventDefault();
            return;
        }
        var btn = document.getElementById('btnSubmit');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Submitting...';
        }
        isDirty = false;
    });

    // =========================================================================
    // Change/input listeners for conditions and dirty tracking
    // =========================================================================
    form.addEventListener('input', function() {
        applyFieldConditions();
        isDirty = true;
    });
    form.addEventListener('change', function() {
        applyFieldConditions();
        isDirty = true;
        // Re-evaluate page visibility on every change
        // If current page is now hidden, jump to the first visible page
        var visiblePages = getVisiblePages();
        var currentPage = allPages.filter(function(p) { return p.classList.contains('active'); })[0];
        if (currentPage && visiblePages.indexOf(currentPage) < 0) {
            showPage(0);
        } else {
            // Refresh nav buttons in case visible page count changed
            var idx = visiblePages.indexOf(currentPage);
            if (idx >= 0) {
                currentPageIndex = idx;
                updateNavButtons(idx, visiblePages.length);
                var pageLabelEl = document.getElementById('pageLabel');
                if (pageLabelEl) pageLabelEl.textContent = 'Section ' + (idx + 1) + ' of ' + visiblePages.length;
                if (progressFill) {
                    var pct = visiblePages.length > 1 ? Math.round(((idx) / (visiblePages.length - 1)) * 100) : 100;
                    progressFill.style.width = pct + '%';
                }
            }
        }
    });

    // =========================================================================
    // Save Progress (manual + auto)
    // =========================================================================
    function saveProgress() {
        var btn = document.getElementById('btnSave');
        var status = document.getElementById('saveStatus');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Saving...';
        }
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
            if (status) status.innerHTML = '<i class="bi bi-check-circle text-success me-1"></i>Saved at ' + timeStr;
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-save me-1"></i>Save Progress';
            }
        })
        .catch(function(err) {
            if (status) status.innerHTML = '<i class="bi bi-exclamation-circle text-danger me-1"></i>Save failed';
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-save me-1"></i>Save Progress';
            }
        });
    }

    setInterval(function() {
        if (isDirty && !isReadOnly) saveProgress();
    }, 60000);

    window.addEventListener('beforeunload', function(e) {
        if (isDirty) { e.preventDefault(); e.returnValue = ''; }
    });

    // =========================================================================
    // Read-only mode: show all pages at once
    // =========================================================================
    function showAllPagesReadOnly() {
        allPages.forEach(function(p) { p.classList.add('active'); });
    }

    // =========================================================================
    // Initialize
    // =========================================================================
    applyFieldConditions();
    if (isReadOnly) {
        showAllPagesReadOnly();
    } else {
        showPage(0);
    }
</script>
</body>
</html>
