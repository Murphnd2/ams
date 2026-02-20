<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Application — ${pspName}</title>
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
        .section-desc { color: #6c757d; font-size: 0.9rem; margin-bottom: 1rem; }
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
    </style>
</head>
<body>

<%-- Header --%>
<div class="app-header">
    <div class="app-container" style="padding-top:0;padding-bottom:0;">
        <h1>${pspName}</h1>
        <div class="subtitle">Benefits Administration Application — ${proposal.getProspect().getName()}</div>
    </div>
</div>
<div class="accent-bar"></div>

<div class="app-container">

    <%-- Progress Bar --%>
    <div class="progress-bar-custom">
        <div class="progress-bar-fill" id="progressFill" style="width: 0%"></div>
    </div>

    <form method="post" action="${pageContext.request.contextPath}/apply/${proposal.getApplicationGUID()}" id="applicationForm">

        <c:forEach var="section" items="${sections}" varStatus="secStatus">
            <div class="section-card">
                <div class="section-header">
                    <i class="bi bi-clipboard-check me-2"></i>${section.getName()}
                </div>
                <div class="section-body">
                    <c:if test="${section.getDescription() != null}">
                        <div class="section-desc">${section.getDescription()}</div>
                    </c:if>

                    <c:forEach var="field" items="${section.getFieldList()}">
                        <div class="field-group">

                                <%-- Label (skip for BOOLEAN — label is inline) --%>
                            <c:if test="${field.getFieldType() != 'BOOLEAN'}">
                                <label for="${field.getFieldKey()}">
                                        ${field.getLabel()}
                                    <c:if test="${field.isRequired()}"><span class="required-star">*</span></c:if>
                                </label>
                            </c:if>

                                <%-- TEXT --%>
                            <c:if test="${field.getFieldType() == 'TEXT'}">
                                <input type="text" class="form-control form-control-sm" id="${field.getFieldKey()}"
                                       name="${field.getFieldKey()}" ${field.isRequired() ? 'required' : ''}
                                       value="${defaults.containsKey(field.getFieldKey()) ? defaults.get(field.getFieldKey()) : ''}">
                            </c:if>

                                <%-- TEXTAREA --%>
                            <c:if test="${field.getFieldType() == 'TEXTAREA'}">
                <textarea class="form-control form-control-sm" id="${field.getFieldKey()}"
                          name="${field.getFieldKey()}" rows="3" ${field.isRequired() ? 'required' : ''}>${defaults.containsKey(field.getFieldKey()) ? defaults.get(field.getFieldKey()) : ''}</textarea>
                            </c:if>

                                <%-- NUMBER --%>
                            <c:if test="${field.getFieldType() == 'NUMBER'}">
                                <input type="number" class="form-control form-control-sm" id="${field.getFieldKey()}"
                                       name="${field.getFieldKey()}" ${field.isRequired() ? 'required' : ''}
                                       value="${defaults.containsKey(field.getFieldKey()) ? defaults.get(field.getFieldKey()) : ''}"
                                       <c:if test="${field.getFieldKey() == 'fsa_health_limit' && irsLimits.containsKey('FSA_HEALTH_MAX')}">placeholder="IRS max: ${irsLimits.get('FSA_HEALTH_MAX').getFormattedAmount()}"</c:if>
                                       <c:if test="${field.getFieldKey() == 'fsa_depcare_limit' && irsLimits.containsKey('FSA_DEPCARE_MAX')}">placeholder="IRS max: ${irsLimits.get('FSA_DEPCARE_MAX').getFormattedAmount()}"</c:if>
                                       <c:if test="${field.getFieldKey() == 'adopt_max_benefit' && irsLimits.containsKey('ADOPTION_MAX')}">placeholder="IRS max: ${irsLimits.get('ADOPTION_MAX').getFormattedAmount()}"</c:if>
                                >
                            </c:if>

                                <%-- DATE --%>
                            <c:if test="${field.getFieldType() == 'DATE'}">
                                <input type="date" class="form-control form-control-sm" id="${field.getFieldKey()}"
                                       name="${field.getFieldKey()}" ${field.isRequired() ? 'required' : ''}
                                       value="${defaults.containsKey(field.getFieldKey()) ? defaults.get(field.getFieldKey()) : ''}">
                            </c:if>

                                <%-- SELECT --%>
                            <c:if test="${field.getFieldType() == 'SELECT'}">
                                <select class="form-select form-select-sm" id="${field.getFieldKey()}"
                                        name="${field.getFieldKey()}" ${field.isRequired() ? 'required' : ''}>
                                    <option value="">— Select —</option>
                                    <c:forEach var="opt" items="${field.getSelectOptionsList()}">
                                        <c:choose>
                                            <c:when test="${field.getFieldKey() == 'fsa_health_rollover' && opt == 'Carryover' && irsLimits.containsKey('FSA_CARRYOVER')}">
                                                <option value="${irsLimits.get('FSA_CARRYOVER').getFormattedAmount()} Carryover"
                                                    ${defaults.containsKey(field.getFieldKey()) && fn:contains(defaults.get(field.getFieldKey()), 'Carryover') ? 'selected' : ''}>${irsLimits.get('FSA_CARRYOVER').getFormattedAmount()} Carryover (${irsLimits.get('FSA_CARRYOVER').getPlanYear()} IRS max)</option>
                                            </c:when>
                                            <c:otherwise>
                                                <option value="${opt}" ${defaults.containsKey(field.getFieldKey()) && defaults.get(field.getFieldKey()) == opt ? 'selected' : ''}>${opt}</option>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:forEach>
                                </select>
                            </c:if>

                                <%-- RADIO --%>
                            <c:if test="${field.getFieldType() == 'RADIO'}">
                                <div>
                                    <c:forEach var="opt" items="${field.getSelectOptionsList()}" varStatus="optStatus">
                                        <div class="form-check form-check-inline">
                                            <input class="form-check-input" type="radio"
                                                   name="${field.getFieldKey()}" id="${field.getFieldKey()}_${optStatus.index}"
                                                   value="${opt}" ${field.isRequired() ? 'required' : ''}
                                                ${defaults.containsKey(field.getFieldKey()) && defaults.get(field.getFieldKey()) == opt ? 'checked' : ''}>
                                            <label class="form-check-label fw-normal" for="${field.getFieldKey()}_${optStatus.index}">${opt}</label>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:if>

                                <%-- BOOLEAN --%>
                            <c:if test="${field.getFieldType() == 'BOOLEAN'}">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox"
                                           name="${field.getFieldKey()}" id="${field.getFieldKey()}" value="Yes"
                                        ${defaults.containsKey(field.getFieldKey()) && defaults.get(field.getFieldKey()) == 'Yes' ? 'checked' : ''}>
                                    <label class="form-check-label" for="${field.getFieldKey()}">
                                            ${field.getLabel()}
                                        <c:if test="${field.isRequired()}"><span class="required-star">*</span></c:if>
                                    </label>
                                </div>
                            </c:if>

                                <%-- CHECKBOX (multi-select) --%>
                            <c:if test="${field.getFieldType() == 'CHECKBOX'}">
                                <div class="row">
                                    <c:forEach var="opt" items="${field.getSelectOptionsList()}" varStatus="optStatus">
                                        <div class="col-6">
                                            <div class="form-check">
                                                <input class="form-check-input" type="checkbox"
                                                       name="${field.getFieldKey()}" id="${field.getFieldKey()}_${optStatus.index}"
                                                       value="${opt}"
                                                    ${defaults.containsKey(field.getFieldKey()) && fn:contains(defaults.get(field.getFieldKey()), opt) ? 'checked' : ''}>
                                                <label class="form-check-label fw-normal" for="${field.getFieldKey()}_${optStatus.index}">${opt}</label>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:if>

                                <%-- JSON (benefit plan builder) --%>
                            <c:if test="${field.getFieldType() == 'JSON' && field.getFieldKey() == 'bill_benefit_plans'}">
                                <div id="planBuilder">
                                    <div id="planList"></div>
                                    <button type="button" class="btn btn-sm btn-outline-success mt-2" onclick="addPlan()">
                                        <i class="bi bi-plus-circle me-1"></i>Add a Benefit Plan
                                    </button>
                                    <c:choose>
                                        <c:when test="${defaults.containsKey('bill_benefit_plans_escaped')}">
                                            <input type="hidden" name="bill_benefit_plans" id="bill_benefit_plans_json"
                                                   value="${defaults.get('bill_benefit_plans_escaped')}">
                                        </c:when>
                                        <c:otherwise>
                                            <input type="hidden" name="bill_benefit_plans" id="bill_benefit_plans_json" value="[]">
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </c:if>

                                <%-- Help Text --%>
                            <c:if test="${field.getHelpText() != null}">
                                <div class="help-text">${field.getHelpText()}</div>
                            </c:if>
                                <%-- Dynamic IRS limit help text --%>
                            <c:if test="${field.getFieldKey() == 'fsa_health_limit' && irsLimits.containsKey('FSA_HEALTH_MAX')}">
                                <div class="help-text">IRS maximum for ${irsLimits.get('FSA_HEALTH_MAX').getPlanYear()}: ${irsLimits.get('FSA_HEALTH_MAX').getFormattedAmount()}. Leave blank to use IRS max.</div>
                            </c:if>
                            <c:if test="${field.getFieldKey() == 'fsa_depcare_limit' && irsLimits.containsKey('FSA_DEPCARE_MAX')}">
                                <div class="help-text">IRS maximum for ${irsLimits.get('FSA_DEPCARE_MAX').getPlanYear()}: ${irsLimits.get('FSA_DEPCARE_MAX').getFormattedAmount()}. Leave blank to use IRS max.</div>
                            </c:if>
                            <c:if test="${field.getFieldKey() == 'adopt_max_benefit' && irsLimits.containsKey('ADOPTION_MAX')}">
                                <div class="help-text">IRS limit for ${irsLimits.get('ADOPTION_MAX').getPlanYear()}: ${irsLimits.get('ADOPTION_MAX').getFormattedAmount()}</div>
                            </c:if>
                            <c:if test="${field.getFieldKey() == 'transit_election_changes' && irsLimits.containsKey('TRANSIT_MONTHLY')}">
                                <div class="help-text">Current IRS monthly limit (${irsLimits.get('TRANSIT_MONTHLY').getPlanYear()}): ${irsLimits.get('TRANSIT_MONTHLY').getFormattedAmount()}</div>
                            </c:if>

                        </div>
                    </c:forEach>
                </div>
            </div>
        </c:forEach>

        <%-- Submit --%>
        <div class="text-center mt-4 mb-3">
            <button type="button" class="btn btn-outline-secondary me-3" id="btnSave" onclick="saveProgress()">
                <i class="bi bi-save me-1"></i>Save Progress
            </button>
            <button type="submit" class="btn btn-submit" id="btnSubmit">
                <i class="bi bi-send me-2"></i>Submit Application
            </button>
            <div id="saveStatus" class="text-muted mt-2" style="font-size:0.85rem;"></div>
        </div>

    </form>

    <div class="app-footer">
        <p class="mb-0">&copy; ${pspName} &middot; Benefits Administration Services</p>
    </div>

</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    // =========================================================================
    // Conditional show/hide rules
    // =========================================================================
    const conditionalRules = [
        { trigger: 'company_structure', showValue: 'Other', targets: ['company_structure_other'] },
        { trigger: 'elig_entry_frequency', showValue: 'Other', targets: ['elig_entry_freq_other'] },
        { trigger: 'elig_service_time', showValue: 'Other', targets: ['elig_service_time_other'] },
        { trigger: 'pay_has_second_cycle', showValue: 'Yes', targets: ['pay_period_2', 'pay_first_deduction_2'] },
        { trigger: 's125_flex_credits', showValue: 'checked', targets: ['s125_flex_credit_amount'] },
        { trigger: ['fsa_health', 'fsa_limited_purpose'], showValue: 'checked', mode: 'any', targets: ['fsa_health_limit', 'fsa_health_rollover'] },
        { trigger: 'fsa_dependent_care', showValue: 'checked', targets: ['fsa_depcare_limit', 'fsa_depcare_rollover'] },
        { trigger: 'pay_facilitate_payments', showValue: 'Yes', targets: ['pay_method_preference'] },
        { trigger: 'card_acknowledge_substantiation', showValue: 'checked', targets: [
                'card_company_name', 'card_contact_name', 'card_contact_email',
                'card_has_copay_medical', 'card_has_copay_dental', 'card_has_copay_vision'
            ]},
        { trigger: 'card_has_copay_medical', showValue: 'Yes', targets: ['card_med_copay_details'] },
        { trigger: 'card_has_copay_dental', showValue: 'Yes', targets: ['card_dental_copay_details'] },
        { trigger: 'card_has_copay_vision', showValue: 'Yes', targets: ['card_vision_copay_details'] },
        { trigger: 'hra_benefit_structure', showValue: 'Flat rate regardless of coverage level', targets: ['hra_flat_amount'] },
        { trigger: 'hra_benefit_structure', showValue: 'Varies by coverage level (Single, Family, etc.)', targets: ['hra_amount_by_tier'] },
        { trigger: 'hra_has_carryover', showValue: 'Yes', targets: ['hra_carryover_pct', 'hra_carryover_cap'] },
        { trigger: 'hra_has_spenddown', showValue: 'Yes', targets: ['hra_spenddown_events', 'hra_spenddown_months', 'hra_spenddown_pct'] },
        { trigger: 'hsa_funding_method', showValue: 'We wish to fund electronically via EFT/ACH', targets: ['hsa_bank_name', 'hsa_bank_account', 'hsa_bank_routing', 'hsa_bank_account_type'] }
    ];

    function getFieldGroup(fieldKey) {
        var el = document.getElementById(fieldKey);
        if (!el) {
            el = document.querySelector('[name="' + fieldKey + '"]');
        }
        if (!el) return null;
        return el.closest('.field-group');
    }

    function getTriggerValue(fieldKey) {
        var el = document.getElementById(fieldKey);
        if (!el) {
            var radios = document.querySelectorAll('input[name="' + fieldKey + '"]');
            for (var i = 0; i < radios.length; i++) {
                if (radios[i].checked) return radios[i].value;
            }
            return null;
        }
        if (el.type === 'checkbox') {
            return el.checked ? 'checked' : '';
        }
        return el.value;
    }

    function applyConditionalRules() {
        conditionalRules.forEach(function(rule) {
            var show = false;
            if (Array.isArray(rule.trigger)) {
                show = rule.trigger.some(function(t) {
                    return getTriggerValue(t) === rule.showValue;
                });
            } else {
                show = (getTriggerValue(rule.trigger) === rule.showValue);
            }
            var triggerKeys = Array.isArray(rule.trigger) ? rule.trigger : [rule.trigger];
            triggerKeys.forEach(function(tk) {
                var triggerFg = getFieldGroup(tk);
                if (triggerFg && triggerFg.style.display === 'none') {
                    show = false;
                }
            });
            rule.targets.forEach(function(targetKey) {
                var fg = getFieldGroup(targetKey);
                if (fg) {
                    fg.style.display = show ? '' : 'none';
                    if (!show) {
                        var inputs = fg.querySelectorAll('input, select, textarea');
                        inputs.forEach(function(input) {
                            if (input.type === 'checkbox' || input.type === 'radio') {
                                input.checked = false;
                            } else {
                                input.value = '';
                            }
                        });
                    }
                }
            });
        });
    }

    // =========================================================================
    // Progress bar
    // =========================================================================
    var form = document.getElementById('applicationForm');
    var progressFill = document.getElementById('progressFill');

    function updateProgress() {
        var inputs = form.querySelectorAll('input[type="text"], input[type="number"], input[type="date"], select, textarea');
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
    form.addEventListener('input', function() { applyConditionalRules(); updateProgress(); isDirty = true; });
    form.addEventListener('change', function() { applyConditionalRules(); updateProgress(); isDirty = true; });

    // Submit button loading state
    form.addEventListener('submit', function() {
        var btn = document.getElementById('btnSubmit');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Submitting...';
        isDirty = false;
    });

    // =========================================================================
    // Benefit Plan Builder
    // =========================================================================
    const benefitTypes = [
        <c:forEach var="bt" items="${benefitTypes}" varStatus="s">
        { id: ${bt.getId()}, name: '${bt.getName()}', defaultBillingTypeId: ${bt.getDefaultBillingType() != null ? bt.getDefaultBillingType().getId() : 'null'} }<c:if test="${!s.last}">,</c:if>
        </c:forEach>
    ];

    const billingTypes = [
        <c:forEach var="bt" items="${billingTypes}" varStatus="s">
        { id: ${bt.getId()}, name: '${bt.getName()}' }<c:if test="${!s.last}">,</c:if>
        </c:forEach>
    ];

    const tierPresets = [
        { label: 'Single / Family', tiers: ['Single', 'Family'] },
        { label: 'Single / Two Person / Family', tiers: ['Single', 'Two Person', 'Family'] },
        { label: 'Single / EE+Spouse / EE+Children / Family', tiers: ['Single', 'EE+Spouse', 'EE+Children', 'Family'] },
        { label: 'Custom', tiers: [] }
    ];

    let plans = [];
    try {
        var savedPlansEl = document.getElementById('bill_benefit_plans_json');
        if (savedPlansEl && savedPlansEl.value && savedPlansEl.value !== '[]') {
            var parsed = JSON.parse(savedPlansEl.value);
            if (Array.isArray(parsed)) {
                plans = parsed.map(function(p) {
                    return {
                        id: Date.now() + Math.random(),
                        planName: p.planName || '',
                        benefitTypeId: p.benefitTypeId || '',
                        benefitTypeName: p.benefitTypeName || '',
                        billingTypeId: p.billingTypeId || '',
                        billingTypeName: p.billingTypeName || '',
                        effectiveDate: p.effectiveDate || '',
                        renewalDate: p.renewalDate || '',
                        tierPreset: p.tierPreset || '',
                        tiers: p.tiers || [],
                        flatRate: p.flatRate || '',
                        notes: p.notes || '',
                        storageKey: p.storageKey || '',
                        fileName: p.fileName || ''
                    };
                });
            }
        }
    } catch(e) { }

    function addPlan() {
        plans.push({
            id: Date.now(),
            planName: '',
            benefitTypeId: '',
            benefitTypeName: '',
            billingTypeId: '',
            billingTypeName: '',
            effectiveDate: '',
            renewalDate: '',
            tierPreset: '',
            tiers: [],
            flatRate: '',
            notes: '',
            storageKey: '',
            fileName: ''
        });
        renderPlans();
    }

    function removePlan(planId) {
        plans = plans.filter(function(p) { return p.id !== planId; });
        renderPlans();
    }

    function renderPlans() {
        var html = '';
        plans.forEach(function(plan, idx) {
            var billingType = billingTypes.find(function(bt) { return bt.id == plan.billingTypeId; });
            var billingName = billingType ? billingType.name : '';
            var isTiered = billingName === 'Tiered Rates';
            var isFlat = billingName === 'Flat Rate';
            var isOther = plan.billingTypeId && !isTiered && !isFlat;

            html += '<div class="card mb-2 border-start border-3" style="border-left-color: var(--psp-accent) !important;">';
            html += '<div class="card-body px-3 py-2">';

            // Header row
            html += '<div class="d-flex justify-content-between align-items-center mb-2">';
            html += '<strong>Plan ' + (idx + 1) + '</strong>';
            html += '<div class="d-flex align-items-center">';
            if (plan.storageKey) {
                html += '<span class="badge bg-success me-2"><i class="bi bi-file-earmark-check me-1"></i>' + escHtml(plan.fileName) + '</span>';
                html += '<button type="button" class="btn btn-sm btn-outline-danger me-2" onclick="removeFile(' + plan.id + ')" title="Remove file"><i class="bi bi-file-x"></i></button>';
            } else {
                html += '<label class="btn btn-sm btn-outline-secondary me-2 mb-0">';
                html += '<i class="bi bi-upload me-1"></i>Upload Rates (optional)';
                html += '<input type="file" class="d-none" accept=".pdf,.xlsx,.xls,.csv" onchange="uploadRateSheet(' + plan.id + ',this)">';
                html += '</label>';
            }
            html += '<button type="button" class="btn btn-sm btn-outline-danger" onclick="removePlan(' + plan.id + ')" title="Remove plan"><i class="bi bi-trash"></i></button>';
            html += '</div></div>';

            // Row 1: Plan Name + Benefit Type
            html += '<div class="row mb-2">';
            html += '<div class="col-6"><label class="form-label form-label-sm mb-0 fw-normal">Plan Name</label>';
            html += '<input type="text" class="form-control form-control-sm" value="' + escHtml(plan.planName) + '" onchange="updatePlan(' + plan.id + ',\'planName\',this.value)"></div>';
            html += '<div class="col-6"><label class="form-label form-label-sm mb-0 fw-normal">Benefit Type</label>';
            html += '<select class="form-select form-select-sm" onchange="setBenefitType(' + plan.id + ',this.value)">';
            html += '<option value="">— Select —</option>';
            benefitTypes.forEach(function(bt) {
                html += '<option value="' + bt.id + '"' + (plan.benefitTypeId == bt.id ? ' selected' : '') + '>' + bt.name + '</option>';
            });
            html += '</select></div></div>';

            // Row 2: Billing Type + Effective + Renewal
            html += '<div class="row mb-2">';
            html += '<div class="col-4"><label class="form-label form-label-sm mb-0 fw-normal">Rate Structure</label>';
            html += '<select class="form-select form-select-sm" onchange="setBillingType(' + plan.id + ',this.value)">';
            html += '<option value="">— Select —</option>';
            billingTypes.forEach(function(bt) {
                html += '<option value="' + bt.id + '"' + (plan.billingTypeId == bt.id ? ' selected' : '') + '>' + bt.name + '</option>';
            });
            html += '</select></div>';
            html += '<div class="col-4"><label class="form-label form-label-sm mb-0 fw-normal">Rates Effective</label>';
            html += '<input type="date" class="form-control form-control-sm" value="' + plan.effectiveDate + '" onchange="updatePlan(' + plan.id + ',\'effectiveDate\',this.value)"></div>';
            html += '<div class="col-4"><label class="form-label form-label-sm mb-0 fw-normal">Rates Renew</label>';
            html += '<input type="date" class="form-control form-control-sm" value="' + plan.renewalDate + '" onchange="updatePlan(' + plan.id + ',\'renewalDate\',this.value)"></div>';
            html += '</div>';

            // Tiered rates
            if (isTiered) {
                html += '<div class="mb-2"><label class="form-label form-label-sm mb-0 fw-normal">Tier Template</label>';
                html += '<select class="form-select form-select-sm" onchange="setTierPreset(' + plan.id + ',this.value)">';
                html += '<option value="">— Select Tier Template —</option>';
                tierPresets.forEach(function(tp, tpIdx) {
                    html += '<option value="' + tpIdx + '"' + (plan.tierPreset === '' + tpIdx ? ' selected' : '') + '>' + tp.label + '</option>';
                });
                html += '</select></div>';
                if (plan.tiers.length > 0) {
                    html += '<div class="row mb-1">';
                    plan.tiers.forEach(function(tier, tIdx) {
                        html += '<div class="col-6 mb-1">';
                        html += '<div class="input-group input-group-sm">';
                        html += '<input type="text" class="form-control" value="' + escHtml(tier.name) + '" onchange="updateTier(' + plan.id + ',' + tIdx + ',\'name\',this.value)" placeholder="Tier name">';
                        html += '<span class="input-group-text">$</span>';
                        html += '<input type="number" class="form-control" value="' + tier.amount + '" onchange="updateTier(' + plan.id + ',' + tIdx + ',\'amount\',this.value)" placeholder="Rate" step="0.01">';
                        if (plan.tierPreset === '3') {
                            html += '<button type="button" class="btn btn-outline-danger btn-sm" onclick="removeTier(' + plan.id + ',' + tIdx + ')"><i class="bi bi-x"></i></button>';
                        }
                        html += '</div></div>';
                    });
                    html += '</div>';
                    if (plan.tierPreset === '3') {
                        html += '<button type="button" class="btn btn-sm btn-outline-secondary mt-1" onclick="addTier(' + plan.id + ')"><i class="bi bi-plus me-1"></i>Add Tier</button>';
                    }
                }
            }

            // Flat rate
            if (isFlat) {
                html += '<div class="row mb-2"><div class="col-4">';
                html += '<label class="form-label form-label-sm mb-0 fw-normal">Monthly Rate per Employee</label>';
                html += '<div class="input-group input-group-sm"><span class="input-group-text">$</span>';
                html += '<input type="number" class="form-control" value="' + plan.flatRate + '" onchange="updatePlan(' + plan.id + ',\'flatRate\',this.value)" step="0.01">';
                html += '</div></div></div>';
            }

            // Age-rated / other
            if (isOther) {
                html += '<div class="mb-2"><label class="form-label form-label-sm mb-0 fw-normal">Rate Details</label>';
                html += '<textarea class="form-control form-control-sm" rows="2" placeholder="Describe rate structure or note that a rate sheet will be provided" onchange="updatePlan(' + plan.id + ',\'notes\',this.value)">' + escHtml(plan.notes) + '</textarea></div>';
            }

            html += '</div></div>';
        });
        document.getElementById('planList').innerHTML = html;
        serializePlans();
        updateProgress();
    }

    function setBenefitType(planId, btId) {
        var plan = plans.find(function(p) { return p.id === planId; });
        var bt = benefitTypes.find(function(b) { return b.id == btId; });
        plan.benefitTypeId = btId;
        plan.benefitTypeName = bt ? bt.name : '';
        if (bt && bt.defaultBillingTypeId) {
            plan.billingTypeId = bt.defaultBillingTypeId;
            var blt = billingTypes.find(function(b) { return b.id == bt.defaultBillingTypeId; });
            plan.billingTypeName = blt ? blt.name : '';
            if (blt && blt.name === 'Tiered Rates') {
                plan.tierPreset = '0';
                plan.tiers = tierPresets[0].tiers.map(function(t) { return { name: t, amount: '' }; });
            } else {
                plan.tierPreset = '';
                plan.tiers = [];
            }
        }
        renderPlans();
    }

    function setBillingType(planId, btId) {
        var plan = plans.find(function(p) { return p.id === planId; });
        var bt = billingTypes.find(function(b) { return b.id == btId; });
        plan.billingTypeId = btId;
        plan.billingTypeName = bt ? bt.name : '';
        plan.tiers = [];
        plan.tierPreset = '';
        plan.flatRate = '';
        plan.notes = '';
        renderPlans();
    }

    function setTierPreset(planId, presetIdx) {
        var plan = plans.find(function(p) { return p.id === planId; });
        plan.tierPreset = '' + presetIdx;
        var preset = tierPresets[presetIdx];
        if (preset.tiers.length > 0) {
            plan.tiers = preset.tiers.map(function(t) { return { name: t, amount: '' }; });
        } else {
            plan.tiers = [{ name: '', amount: '' }];
        }
        renderPlans();
    }

    function addTier(planId) {
        var plan = plans.find(function(p) { return p.id === planId; });
        plan.tiers.push({ name: '', amount: '' });
        renderPlans();
    }

    function removeTier(planId, tierIdx) {
        var plan = plans.find(function(p) { return p.id === planId; });
        plan.tiers.splice(tierIdx, 1);
        renderPlans();
    }

    function updateTier(planId, tierIdx, field, value) {
        var plan = plans.find(function(p) { return p.id === planId; });
        plan.tiers[tierIdx][field] = value;
        serializePlans();
        isDirty = true;
    }

    function updatePlan(planId, field, value) {
        var plan = plans.find(function(p) { return p.id === planId; });
        plan[field] = value;
        serializePlans();
        isDirty = true;
    }

    function uploadRateSheet(planId, input) {
        if (!input.files || !input.files[0]) return;
        var file = input.files[0];
        var plan = plans.find(function(p) { return p.id === planId; });
        var label = input.parentElement;
        var origHtml = label.innerHTML;
        label.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Uploading...';
        var formData = new FormData();
        formData.append('rateSheet', file);
        fetch('${pageContext.request.contextPath}/uploadRateSheet', {
            method: 'POST',
            body: formData
        })
            .then(function(resp) {
                if (!resp.ok) {
                    return resp.text().then(function(t) { throw new Error('Server ' + resp.status + ': ' + t); });
                }
                return resp.json();
            })
            .then(function(data) {
                if (data.error) {
                    alert(data.error);
                    label.innerHTML = origHtml;
                } else {
                    plan.storageKey = data.storageKey;
                    plan.fileName = data.fileName;
                    renderPlans();
                    isDirty = true;
                }
            })
            .catch(function(err) {
                alert('Upload failed: ' + err.message);
                label.innerHTML = origHtml;
            });
    }

    function removeFile(planId) {
        var plan = plans.find(function(p) { return p.id === planId; });
        plan.storageKey = '';
        plan.fileName = '';
        renderPlans();
        isDirty = true;
    }

    function serializePlans() {
        var clean = plans.map(function(p) {
            var obj = {
                planName: p.planName,
                benefitTypeId: p.benefitTypeId,
                benefitTypeName: p.benefitTypeName,
                billingTypeId: p.billingTypeId,
                billingTypeName: p.billingTypeName,
                effectiveDate: p.effectiveDate,
                renewalDate: p.renewalDate
            };
            if (p.tiers.length > 0) obj.tiers = p.tiers;
            if (p.flatRate) obj.flatRate = p.flatRate;
            if (p.notes) obj.notes = p.notes;
            if (p.storageKey) { obj.storageKey = p.storageKey; obj.fileName = p.fileName; }
            return obj;
        });
        document.getElementById('bill_benefit_plans_json').value = JSON.stringify(clean);
    }

    function escHtml(str) {
        if (!str) return '';
        return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    // =========================================================================
    // Save Progress (manual + auto)
    // =========================================================================

    var applicationGuid = '${proposal.getApplicationGUID()}';

    function saveProgress() {
        var btn = document.getElementById('btnSave');
        var status = document.getElementById('saveStatus');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Saving...';
        var formData = new FormData(form);

        fetch('${pageContext.request.contextPath}/saveApplication?guid=' + encodeURIComponent(applicationGuid), {
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
    // Initialize — apply saved state
    // =========================================================================
    applyConditionalRules();
    updateProgress();
    if (plans.length > 0) renderPlans();
</script>
</body>
</html>
