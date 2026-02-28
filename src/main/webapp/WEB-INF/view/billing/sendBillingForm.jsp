<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <script src="https://cdn.ckeditor.com/ckeditor5/36.0.1/classic/ckeditor.js"></script>
    <title>Send Billing - ${sessionScope.currentBillingEmployer.getEmployerName()}</title>
    <style>
        body { background-color: #f5f7fa; }
        .er-brand {
            background: linear-gradient(135deg, #0d5681 0%, #0a4469 100%);
            color: white;
            padding: 1.25rem 1.5rem;
            border-radius: 0 0 8px 8px;
        }
        .er-brand h3 { margin-bottom: 0.25rem; }
        .er-brand .subtitle { opacity: 0.8; font-size: 0.85rem; }
        .ck-editor__editable_inline {
            max-width: none;
            width: 100%;
            margin: 0 auto;
            box-sizing: border-box;
            min-height: 200px;
        }
        .recipient-chip {
            display: inline-flex;
            align-items: center;
            background: #e9ecef;
            border-radius: 20px;
            padding: 0.25rem 0.7rem;
            margin: 0.15rem;
            font-size: 0.82rem;
            gap: 0.3rem;
        }
        .recipient-chip.checked { background: #d4edda; }
        .recipient-chip input[type="checkbox"] {
            margin: 0;
            width: 14px;
            height: 14px;
            cursor: pointer;
        }
        .recipient-chip label {
            cursor: pointer;
            margin: 0;
            line-height: 1.3;
        }
        .extra-section { display: none; }
    </style>
</head>
<body>
<div class="container-fluid" style="max-width: 760px;">

    <%-- Branded header --%>
    <div class="er-brand mb-3">
        <h3 class="fw-bold"><i class="bi bi-envelope-at me-2"></i>Send Billing Detail</h3>
        <span class="subtitle">${sessionScope.currentBillingEmployer.getEmployerName()}</span>
    </div>

    <form method="post" id="billingEmailForm" action="SendEmployerBillingDetail">

        <%-- ===== RECIPIENTS CARD ===== --%>
        <div class="card border-0 shadow-sm mb-3">
            <div class="hdr-bar d-flex align-items-center gap-2">
                <i class="bi bi-people-fill"></i>
                <span class="fw-bold" style="font-size: 0.9rem;">Recipients</span>
            </div>
            <div class="card-body">

                <%-- Primary contacts as chips --%>
                <div class="d-flex flex-wrap align-items-center" style="min-height: 38px;">
                    <c:choose>
                        <c:when test="${not empty sessionScope.employerContactList}">
                            <c:forEach var="contact" items="${sessionScope.employerContactList}" varStatus="loop">
                                <span class="recipient-chip checked" id="chip_e${loop.count}">
                                    <input type="checkbox" checked value="${contact.getId()}"
                                           name="eCheck${loop.count}" id="eCheck${loop.count}"
                                           onchange="toggleChip(this, 'chip_e${loop.count}')">
                                    <label for="eCheck${loop.count}">
                                        ${contact.getFirstName()} ${contact.getLastName()}
                                        <span class="text-muted" style="font-size:0.75rem;">${contact.getEmail()}</span>
                                    </label>
                                </span>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <span class="text-muted fst-italic" style="font-size: 0.85rem;">
                                No contacts found from recent billing activity
                            </span>
                        </c:otherwise>
                    </c:choose>
                </div>

                <%-- Expandable additional employees --%>
                <c:if test="${not empty sessionScope.remainingContacts}">
                    <div class="mt-2">
                        <button type="button" class="btn btn-sm btn-outline-ssa" onclick="toggleExtras()">
                            <i class="bi bi-person-plus me-1"></i>Show ${sessionScope.remainingContacts.size()} more employee(s)
                        </button>
                    </div>
                    <div class="extra-section mt-2" id="extraContacts">
                        <div class="d-flex flex-wrap">
                            <c:forEach var="extra" items="${sessionScope.remainingContacts}" varStatus="xloop">
                                <span class="recipient-chip" id="chip_x${xloop.count}">
                                    <input type="checkbox" value="${extra.getId()}"
                                           name="xCheck${xloop.count}" id="xCheck${xloop.count}"
                                           onchange="toggleChip(this, 'chip_x${xloop.count}')">
                                    <label for="xCheck${xloop.count}">
                                        ${extra.getFirstName()} ${extra.getLastName()}
                                        <span class="text-muted" style="font-size:0.75rem;">${extra.getEmail()}</span>
                                    </label>
                                </span>
                            </c:forEach>
                        </div>
                    </div>
                </c:if>

                <%-- Manual email entry --%>
                <div class="mt-2">
                    <div class="input-group input-group-sm">
                        <span class="input-group-text" style="font-size:0.8rem;"><i class="bi bi-at"></i></span>
                        <input type="text" name="additionalEmails" class="form-control form-control-sm"
                               placeholder="Additional emails separated by semi-colon" style="font-size:0.82rem;">
                    </div>
                </div>
            </div>
        </div>

        <%-- ===== MESSAGE CARD ===== --%>
        <div class="card border-0 shadow-sm mb-3">
            <div class="hdr-bar d-flex align-items-center gap-2">
                <i class="bi bi-envelope"></i>
                <span class="fw-bold" style="font-size: 0.9rem;">Message</span>
            </div>
            <div class="card-body">
                <textarea id="emailBody" name="emailBody"><p>Good afternoon,</p><p>Below is a link to your monthly billing detail for review. If you have any questions or notice any discrepancies, please don't hesitate to reach out.</p><p><a href="${sessionScope.bcLink}">View Billing Detail</a></p><p>Thank you,</p></textarea>
            </div>
        </div>

        <%-- ===== ACTION BUTTONS ===== --%>
        <div class="d-flex justify-content-end gap-2 mb-4">
            <a class="btn btn-outline-ssa" href="BillingAction?action=drillDown">
                <i class="bi bi-x-lg me-1"></i>Cancel
            </a>
            <button type="submit" class="btn btn-ssa" id="sendBtn">
                <i class="bi bi-send me-1"></i>Send
                <span class="spinner-border spinner-border-sm d-none ms-1" role="status" id="sendSpinner"></span>
            </button>
        </div>
    </form>

    <%-- Footer --%>
    <div class="text-center text-muted mt-4 mb-3" style="font-size: 0.75rem;">
        Superior State Administration
    </div>
</div>

<script>
    /* --- CKEditor --- */
    var editorInstance;
    document.addEventListener('DOMContentLoaded', function () {
        ClassicEditor
            .create(document.querySelector('#emailBody'), {
                toolbar: {
                    items: ['bold', 'italic', 'link', '|', 'bulletedList', 'numberedList', '|', 'undo', 'redo'],
                    shouldNotGroupWhenFull: true
                }
            })
            .then(function(editor) {
                editorInstance = editor;
            })
            .catch(function(error) {
                console.error(error);
            });

        /* --- Form submission: sync CKEditor then submit --- */
        document.getElementById('billingEmailForm').addEventListener('submit', function (e) {
            e.preventDefault();
            if (editorInstance) {
                document.getElementById('emailBody').value = editorInstance.getData();
            }
            var btn = document.getElementById('sendBtn');
            document.getElementById('sendSpinner').classList.remove('d-none');
            btn.disabled = true;
            this.submit();
        });
    });

    /* --- Chip toggle --- */
    function toggleChip(cb, chipId) {
        var chip = document.getElementById(chipId);
        if (cb.checked) {
            chip.classList.add('checked');
        } else {
            chip.classList.remove('checked');
        }
    }

    /* --- Expand extra contacts --- */
    function toggleExtras() {
        var section = document.getElementById('extraContacts');
        section.style.display = section.style.display === 'none' ? 'block' : 'none';
    }
</script>
</body>
</html>
