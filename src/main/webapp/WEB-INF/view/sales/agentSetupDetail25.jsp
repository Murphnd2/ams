<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- ─────────────────────────────────────────────────────────────────────
     V062: Agent-flavored Setup detail view.
     Shown to users whose session roles are agent-only (isAgent or
     isAgencyAdmin, without any PSP-side role). Hides checklist, docs, and
     PSP-only note controls. Filters notes by agent visibility rules.
     ───────────────────────────────────────────────────────────────────── --%>
<c:set var="setup"       value="${sessionScope.local.getCurrentActivity().getActivity()}"/>
<c:set var="application" value="${setup.getApplication()}"/>
<c:set var="proposal"    value="${application != null ? application.getProposal() : null}"/>
<c:set var="prospect"    value="${proposal != null ? proposal.getProspect() : null}"/>
<c:set var="sellingAgent" value="${prospect != null ? prospect.getAgent() : null}"/>
<c:set var="pspNoteDefault" value="${applicationScope.global.notesAgentVisibleDefault}"/>
<c:set var="pspName"      value="${applicationScope.global.getPsp().getFullName()}"/>

<%-- Compute a simple "waiting" status from the latest note's status id.
     status.id == 3 → Waiting on Us (from PSP's view) → "Waiting on PSP" for the agent.
     status.id == 1 → Waiting on Them (from PSP's view) → "Waiting on You".
     else           → "On Track". --%>
<c:set var="latestStatusId" value="0"/>
<c:forEach var="n" items="${sessionScope.local.getCurrentActivity().getNotes()}" varStatus="st">
  <c:if test="${st.first}">
    <c:if test="${n.getStatus() != null}">
      <c:set var="latestStatusId" value="${n.getStatus().getId()}"/>
    </c:if>
  </c:if>
</c:forEach>

<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>${fn:escapeXml(setup.getFullName())} — Setup</title>
    <style>
        :root { --ssa: #0d5681; --ssa-accent: #87a948; --ssa-soft: #f5f8fb; --ink: #1f2d3d; }

        .audit-wrap { display: flex; flex-direction: column; height: calc(100vh - 64px); }

        .toolbar {
            background: #fff; border-bottom: 1px solid #dee2e6;
            padding: 10px 18px;
            display: flex; align-items: center; gap: 14px; flex-wrap: wrap;
        }
        .toolbar .back {
            color: var(--ssa); text-decoration: none; border: 1px solid #dee2e6;
            padding: 3px 10px; border-radius: 4px; font-size: 0.78rem;
        }
        .toolbar .back:hover { background: var(--ssa-soft); }
        .toolbar h1 {
            font-size: 1.05rem; font-weight: 700; color: var(--ssa); margin: 0;
            display: flex; align-items: center; gap: 6px;
        }
        .toolbar .sub { font-size: 0.78rem; color: #6c757d; margin-top: 2px; }
        .toolbar .title-block { line-height: 1.2; }
        .toolbar .status-pill { font-size: 0.72rem; font-weight: 700; padding: 4px 10px; border-radius: 14px; }
        .pill-on-track { background: #d4edda; color: #155724; }
        .pill-wait-psp { background: #fff3cd; color: #856404; }
        .pill-wait-you { background: #f8d7da; color: #721c24; }

        .body-grid {
            flex: 1; overflow: hidden;
            display: grid; grid-template-columns: 3fr 2fr; gap: 12px;
            padding: 12px 16px; background: #eef1f5;
        }
        .col { background: #fff; border: 1px solid #dee2e6; border-radius: 8px;
               display: flex; flex-direction: column; min-height: 0; }
        .col-hdr { background: linear-gradient(135deg, var(--ssa) 0%, #0a4468 100%);
                   color: white; font-weight: 600; font-size: 0.88rem;
                   padding: 8px 14px; border-radius: 8px 8px 0 0;
                   display: flex; align-items: center; gap: 8px; }
        .col-body { padding: 14px 16px; overflow-y: auto; flex: 1; }

        .section { margin-bottom: 18px; }
        .section-title { font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.06em;
                         color: #6c757d; font-weight: 700; margin: 0 0 6px;
                         border-bottom: 1px solid #dee2e6; padding-bottom: 3px; }
        .kv-grid { display: grid; grid-template-columns: 150px 1fr; gap: 4px 14px; font-size: 0.82rem; }
        .kv-grid .k { color: #6c757d; font-weight: 500; }
        .kv-grid .v { color: var(--ink); font-weight: 400; }

        .plan-row {
            display: grid; grid-template-columns: 36px 1fr auto;
            gap: 10px; align-items: center;
            padding: 8px 10px; border: 1px solid #dee2e6; border-radius: 6px;
            margin-bottom: 6px; background: var(--ssa-soft);
        }
        .plan-row .icon { width: 32px; height: 32px; border-radius: 50%;
                          background: var(--ssa); color: white;
                          display: flex; align-items: center; justify-content: center; font-size: 0.85rem; }
        .plan-row .name { font-weight: 600; font-size: 0.88rem; }
        .plan-row .detail { font-size: 0.72rem; color: #6c757d; }
        .plan-row .status-chip { font-size: 0.68rem; font-weight: 700; padding: 2px 8px; border-radius: 10px;
                                 background: #e2e3e5; color: #383d41; }

        .contact-row { display: grid; grid-template-columns: 1fr 1fr auto;
                       gap: 6px; padding: 6px 8px; border-bottom: 1px dashed #dee2e6; font-size: 0.8rem; }
        .contact-row:last-child { border-bottom: 0; }
        .contact-row .role-chip { font-size: 0.66rem; background: #e9ecef; color: #495057;
                                  padding: 1px 7px; border-radius: 10px; }

        /* Composer (nudge PSP) */
        .composer { border: 1px solid #e5d5b0; border-radius: 6px;
                    padding: 10px 12px; margin-bottom: 14px; background: #fff9ec; }
        .composer label { font-size: 0.76rem; font-weight: 700; color: #856404; margin-bottom: 4px; display: block; }
        .composer textarea { width: 100%; border: 1px solid #e5d5b0; border-radius: 4px;
                             padding: 6px 8px; font-size: 0.82rem; resize: vertical;
                             min-height: 54px; max-height: 200px; font-family: inherit; }
        .composer .sub { font-size: 0.68rem; color: #6c757d; margin-top: 4px; }
        .composer .actions { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
        .btn-nudge { background: var(--ssa); color: white; border: 0;
                     padding: 4px 14px; border-radius: 4px; font-size: 0.8rem; font-weight: 600; }
        .btn-nudge:hover { background: #0a4468; }
        .btn-nudge:disabled { opacity: 0.6; cursor: not-allowed; }

        /* Notes timeline */
        .timeline-hint { text-align: center; font-size: 0.7rem; color: #6c757d;
                         margin: 4px 0 10px; letter-spacing: 0.08em; text-transform: uppercase; }
        .note-bubble { padding: 9px 12px; border-radius: 8px; font-size: 0.84rem;
                       margin-bottom: 10px; border: 1px solid #dee2e6; }
        .note-bubble.from-psp   { background: #fff; border-left: 3px solid var(--ssa); }
        .note-bubble.from-agent { background: #f0f7e8; border-left: 3px solid var(--ssa-accent); margin-left: 28px; }
        .note-meta { font-size: 0.7rem; color: #6c757d;
                     display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 4px; }
        .note-author { font-weight: 700; color: var(--ink); }
        .note-chip { font-size: 0.65rem; font-weight: 600; padding: 1px 7px; border-radius: 8px; }
        .chip-wait-psp { background: #fff3cd; color: #856404; }
        .chip-wait-you { background: #d1ecf1; color: #0c5460; }
        .chip-info     { background: #e9ecef; color: #495057; }
        .chip-you-sent { background: #d4edda; color: #155724; }
        .note-body { color: var(--ink); line-height: 1.4; }

        .empty-notes { text-align: center; padding: 20px 14px; color: #6c757d; font-size: 0.82rem;
                       background: #fafbfc; border: 1px dashed #dee2e6; border-radius: 6px; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="audit-wrap">

    <%-- ═══ TOOLBAR ═══ --%>
    <div class="toolbar">
        <a href="AgentSetupList" class="back"><i class="bi bi-arrow-left"></i> My Setups</a>
        <div class="title-block">
            <h1><i class="bi bi-clipboard-check"></i>
                <c:out value="${setup.getFullName()}"/>
            </h1>
            <div class="sub">
                <c:if test="${prospect != null}">
                    Prospect: <b><c:out value="${prospect.getName()}"/></b>
                </c:if>
                <c:if test="${setup.getDueDate() != null}">
                    &middot; Target date: <b><fmt:formatDate value="${setup.getDueDate()}" pattern="MMM d, yyyy"/></b>
                </c:if>
                <c:if test="${sellingAgent != null}">
                    &middot; Selling agent: <c:out value="${sellingAgent.getFullName()}"/>
                </c:if>
            </div>
        </div>
        <div style="flex:1"></div>
        <c:choose>
            <c:when test="${setup.isComplete()}">
                <span class="status-pill pill-on-track"><i class="bi bi-check-circle me-1"></i>Setup complete</span>
            </c:when>
            <c:when test="${latestStatusId == 3}">
                <span class="status-pill pill-wait-psp"><i class="bi bi-hourglass-split me-1"></i>Waiting on <c:out value="${pspName}"/></span>
            </c:when>
            <c:when test="${latestStatusId == 1}">
                <span class="status-pill pill-wait-you"><i class="bi bi-person-raised-hand me-1"></i>Waiting on You</span>
            </c:when>
            <c:otherwise>
                <span class="status-pill pill-on-track"><i class="bi bi-arrow-right-circle me-1"></i>In progress</span>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- ═══ BODY ═══ --%>
    <div class="body-grid">

        <%-- ═════ LEFT: Application snapshot ═════ --%>
        <div class="col">
            <div class="col-hdr"><i class="bi bi-file-earmark-text"></i> Application Snapshot</div>
            <div class="col-body">

                <%-- Employer basics --%>
                <div class="section">
                    <h3 class="section-title">Employer</h3>
                    <div class="kv-grid">
                        <div class="k">Name</div>
                        <div class="v"><c:out value="${setup.getFullName()}"/></div>
                        <c:if test="${prospect != null}">
                            <div class="k">Prospect record</div>
                            <div class="v"><c:out value="${prospect.getName()}"/></div>
                        </c:if>
                        <c:if test="${setup.getDueDate() != null}">
                            <div class="k">Target date</div>
                            <div class="v"><fmt:formatDate value="${setup.getDueDate()}" pattern="MMMM d, yyyy"/></div>
                        </c:if>
                        <c:if test="${sellingAgent != null}">
                            <div class="k">Selling agent</div>
                            <div class="v">
                                <c:out value="${sellingAgent.getFullName()}"/>
                                <c:if test="${sellingAgent.getEmail() != null}">
                                    &middot; <c:out value="${sellingAgent.getEmail()}"/>
                                </c:if>
                            </div>
                        </c:if>
                    </div>
                </div>

                <%-- Key contacts --%>
                <c:if test="${setup.getPrimaryContactSetup() != null || not empty setup.getContactList()}">
                    <div class="section">
                        <h3 class="section-title">Key Contacts</h3>
                        <c:if test="${setup.getPrimaryContactSetup() != null}">
                            <div class="contact-row">
                                <div>
                                    <b><c:out value="${setup.getPrimaryContactSetup().getFullName()}"/></b>
                                </div>
                                <div>
                                    <c:out value="${setup.getPrimaryContactSetup().getEmail()}"/>
                                </div>
                                <div><span class="role-chip">Primary</span></div>
                            </div>
                        </c:if>
                        <c:forEach var="contact" items="${setup.getContactList()}">
                            <c:if test="${setup.getPrimaryContactSetup() == null || contact.getId() != setup.getPrimaryContactSetup().getId()}">
                                <div class="contact-row">
                                    <div>
                                        <b><c:out value="${contact.getFullName()}"/></b>
                                    </div>
                                    <div>
                                        <c:out value="${contact.getEmail()}"/>
                                    </div>
                                    <div><span class="role-chip">Contact</span></div>
                                </div>
                            </c:if>
                        </c:forEach>
                    </div>
                </c:if>

                <%-- Services selected (from Application module list OR selected LOS/Enhancement) --%>
                <c:if test="${not empty agentAppLosNames || not empty agentAppEnhNames
                              || (application != null && not empty application.getApplicationModuleList())}">
                    <div class="section">
                        <h3 class="section-title">Services Selected</h3>

                        <c:forEach var="svcName" items="${agentAppLosNames}">
                            <div class="plan-row">
                                <div class="icon"><i class="bi bi-gear-fill"></i></div>
                                <div><div class="name"><c:out value="${svcName}"/></div></div>
                                <span class="status-chip">Core service</span>
                            </div>
                        </c:forEach>

                        <c:forEach var="enhName" items="${agentAppEnhNames}">
                            <div class="plan-row">
                                <div class="icon" style="background:#87a948;"><i class="bi bi-plus-lg"></i></div>
                                <div><div class="name"><c:out value="${enhName}"/></div></div>
                                <span class="status-chip">Enhancement</span>
                            </div>
                        </c:forEach>

                        <%-- Fall back to module list if LOS/Enhancement names empty --%>
                        <c:if test="${empty agentAppLosNames && empty agentAppEnhNames
                                       && application != null && not empty application.getApplicationModuleList()}">
                            <c:forEach var="svcModule" items="${application.getApplicationModuleList()}">
                                <div class="plan-row">
                                    <div class="icon"><i class="bi bi-gear-fill"></i></div>
                                    <div><div class="name"><c:out value="${svcModule.getServiceItem().getDescription()}"/></div></div>
                                    <span class="status-chip">In setup</span>
                                </div>
                            </c:forEach>
                        </c:if>
                    </div>
                </c:if>

                <%-- Application meta --%>
                <c:if test="${application != null}">
                    <div class="section">
                        <h3 class="section-title">Application</h3>
                        <div class="kv-grid">
                            <c:if test="${application.getDateSubmitted() != null}">
                                <div class="k">Submitted</div>
                                <div class="v"><fmt:formatDate value="${application.getDateSubmitted()}" pattern="MMM d, yyyy"/></div>
                            </c:if>
                            <c:if test="${application.getDateReviewed() != null}">
                                <div class="k">Reviewed</div>
                                <div class="v"><fmt:formatDate value="${application.getDateReviewed()}" pattern="MMM d, yyyy"/></div>
                            </c:if>
                            <c:if test="${application.getStatus() != null}">
                                <div class="k">Status</div>
                                <div class="v"><c:out value="${application.getStatus()}"/></div>
                            </c:if>
                        </div>
                    </div>
                </c:if>

                <%-- ═══ Application field values, grouped by section ═══ --%>
                <c:forEach var="sec" items="${agentAppSections}">
                    <%-- Precompute "has any visible value" to skip empty sections --%>
                    <c:set var="secHasAny" value="${false}"/>
                    <c:forEach var="field" items="${sec.getFieldList()}">
                        <c:set var="v" value="${agentAppValueMap[field.getFieldKey()]}"/>
                        <c:if test="${field.getFieldKey() != 'bill_benefit_plans' && v != null && not empty v}">
                            <c:set var="secHasAny" value="${true}"/>
                        </c:if>
                    </c:forEach>

                    <c:if test="${secHasAny}">
                        <div class="section">
                            <h3 class="section-title"><c:out value="${sec.getName()}"/></h3>
                            <div class="kv-grid">
                                <c:forEach var="field" items="${sec.getFieldList()}">
                                    <c:set var="v" value="${agentAppValueMap[field.getFieldKey()]}"/>
                                    <c:if test="${field.getFieldKey() != 'bill_benefit_plans' && v != null && not empty v}">
                                        <div class="k"><c:out value="${field.getLabel()}"/></div>
                                        <div class="v">
                                            <c:choose>
                                                <c:when test="${field.getFieldType() == 'BOOLEAN'}">
                                                    <c:choose>
                                                        <c:when test="${v == 'true' || v == '1' || v == 'Yes' || v == 'checked'}">
                                                            <span style="color:#198754;"><i class="bi bi-check-circle me-1"></i>Yes</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span style="color:#6c757d;"><i class="bi bi-x-circle me-1"></i>No</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:when>
                                                <c:when test="${field.getFieldType() == 'CHECKBOX'}">
                                                    <c:forEach var="cb" items="${fn:split(v, '|')}">
                                                        <span style="display:inline-block; background:#e9ecef; color:#495057; padding:1px 7px; border-radius:10px; font-size:0.72rem; margin:0 3px 3px 0;">
                                                            <c:out value="${cb}"/>
                                                        </span>
                                                    </c:forEach>
                                                </c:when>
                                                <c:when test="${field.getFieldType() == 'TEXTAREA'}">
                                                    <div style="white-space: pre-wrap; background:#f8f9fb; border:1px solid #e9ecef; border-radius:4px; padding:5px 8px; font-size:0.78rem;">
                                                        <c:out value="${v}"/>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:out value="${v}"/>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </c:if>
                                </c:forEach>
                            </div>
                        </div>
                    </c:if>
                </c:forEach>

            </div>
        </div>

        <%-- ═════ RIGHT: Messages + my tasks ═════ --%>
        <div class="col">
            <div class="col-hdr"><i class="bi bi-chat-left-dots"></i> Messages &amp; Tasks</div>
            <div class="col-body">

                <%-- My Tasks on this Setup. Defensive: only iterate if the session
                     ToDoOut25 list is populated. In some paths (fresh ViewById load
                     before the setup's checklist lazy-loads) the list can be null,
                     and a c:forEach over null used to silently bail and abort the
                     rest of the column body. --%>
                <c:set var="todoList" value="${sessionScope.local.getCurrentActivity().getToDoList()}"/>
                <c:if test="${not empty todoList}">
                    <c:set var="myOpenTodoCount" value="0"/>
                    <c:forEach var="td" items="${todoList}">
                        <c:if test="${!td.isComplete() && (td.isMyTask() || td.allowsNonOwner())}">
                            <c:set var="myOpenTodoCount" value="${myOpenTodoCount + 1}"/>
                        </c:if>
                    </c:forEach>

                    <c:if test="${myOpenTodoCount > 0}">
                        <div style="border: 1px solid #dee2e6; border-radius: 6px; background: #fff; margin-bottom: 14px;">
                            <div style="padding: 7px 12px; font-size: 0.78rem; font-weight: 700; color: var(--ssa); border-bottom: 1px solid #dee2e6; display:flex; align-items:center; gap:6px;">
                                <i class="bi bi-check2-square"></i>
                                My Tasks on This Setup
                                <span style="margin-left:auto; background:#fff3cd; color:#856404; font-size:0.7rem; font-weight:700; padding:1px 8px; border-radius:10px;">
                                    ${myOpenTodoCount} open
                                </span>
                            </div>
                            <div>
                                <c:forEach var="td" items="${todoList}">
                                    <c:if test="${!td.isComplete() && (td.isMyTask() || td.allowsNonOwner())}">
                                        <div style="display:flex; align-items:center; gap:10px; padding: 8px 12px; border-bottom:1px solid #f0f2f5;">
                                            <form method="post" action="CloseToDo25" style="margin:0;">
                                                <input type="hidden" name="btnToDo" value="${td.getToDo().getId()}"/>
                                                <button type="submit"
                                                        title="Mark complete"
                                                        style="border:0; background:transparent; padding:0; font-size:1.1rem; color:#198754; cursor:pointer; line-height:1;">
                                                    <i class="bi bi-circle"></i>
                                                </button>
                                            </form>
                                            <div style="flex:1; font-size:0.82rem;">
                                                <c:out value="${td.getDescription()}"/>
                                                <c:if test="${td.allowsNonOwner() && !td.isMyTask()}">
                                                    <span style="font-size:0.66rem; background:#e9ecef; color:#495057; padding:1px 6px; border-radius:8px; margin-left:6px;">shared</span>
                                                </c:if>
                                            </div>
                                            <form method="post" action="CloseToDo25" style="margin:0;">
                                                <input type="hidden" name="btnToDo" value="${td.getToDo().getId()}"/>
                                                <button type="submit"
                                                        style="border:1px solid var(--ssa-accent); background:#f0f7e8; color:#3f6b1f; padding:2px 10px; border-radius:4px; font-size:0.72rem; font-weight:600; cursor:pointer;">
                                                    <i class="bi bi-check2 me-1"></i>Done
                                                </button>
                                            </form>
                                        </div>
                                    </c:if>
                                </c:forEach>
                            </div>
                        </div>
                    </c:if>
                </c:if>

                <%-- Nudge composer: posts to AddNoteToActivity25 with hidden defaults.
                     reasonList=7 = "Sent Email Message" (outbound).
                     noteStatus=3 = "Waiting on Us" (flips the activity waitingOnUs flag,
                                     so PSP sees the nudge in their attention filter).
                     Agent-authored notes are auto-flagged agent_visible=TRUE in the
                     servlet, regardless of PSP default. --%>
                <form method="post" action="AddNoteToActivity25" id="nudgeForm">
                    <div class="composer">
                        <label><i class="bi bi-send me-1"></i>Send a message to <c:out value="${pspName}"/></label>
                        <textarea name="noteText" id="nudgeBody"
                                  placeholder="Ask a question or share an update. Your message sets the setup to &quot;Waiting on ${fn:escapeXml(pspName)}&quot;."
                                  required></textarea>
                        <input type="hidden" name="reasonList" value="7"/>
                        <input type="hidden" name="noteStatus" value="3"/>
                        <div class="actions">
                            <div class="sub">
                                <i class="bi bi-info-circle me-1"></i>Visible to the <c:out value="${pspName}"/> team and you.
                            </div>
                            <button type="submit" class="btn-nudge" id="nudgeSubmit">
                                <i class="bi bi-send me-1"></i>Send
                            </button>
                        </div>
                    </div>
                </form>

                <%-- Activity timeline: only agent-visible notes. Agent-authored bubbles
                     are indented right (from-agent); everything else is flat (from-psp). --%>
                <c:set var="visibleNotes" value="0"/>
                <c:set var="notesList" value="${sessionScope.local.getCurrentActivity().getNotes()}"/>
                <c:forEach var="note" items="${notesList}" varStatus="ns">
                    <c:set var="noteVisible" value="${pspNoteDefault}"/>
                    <c:if test="${note.getAgentVisible() != null}">
                        <c:set var="noteVisible" value="${note.getAgentVisible()}"/>
                    </c:if>
                    <c:if test="${noteVisible}">
                        <c:set var="visibleNotes" value="${visibleNotes + 1}"/>

                        <%-- "From agent" = authored by the current viewer OR by the
                             originating selling agent. Covers the usual cases
                             (viewer themself + selling-agent colleague). --%>
                        <c:set var="authorIsAgent" value="${false}"/>
                        <c:set var="authorIsMe"    value="${false}"/>
                        <c:if test="${note.getCreatedBy() != null}">
                            <c:if test="${note.getCreatedBy().getId() == sessionScope.local.getCurrentPerson().getId()}">
                                <c:set var="authorIsAgent" value="${true}"/>
                                <c:set var="authorIsMe"    value="${true}"/>
                            </c:if>
                            <c:if test="${sellingAgent != null && note.getCreatedBy().getId() == sellingAgent.getId()}">
                                <c:set var="authorIsAgent" value="${true}"/>
                            </c:if>
                        </c:if>

                        <div class="note-bubble ${authorIsAgent ? 'from-agent' : 'from-psp'}">
                            <div class="note-meta">
                                <span class="note-author">
                                    <c:choose>
                                        <c:when test="${authorIsMe}">You</c:when>
                                        <c:otherwise><c:out value="${note.getCreatedBy().getFullName()}"/></c:otherwise>
                                    </c:choose>
                                </span>
                                <c:choose>
                                    <c:when test="${note.getDateCreated() != null}">
                                        <span><fmt:formatDate value="${note.getDateCreated()}" pattern="MMM d, yyyy · h:mm a"/></span>
                                    </c:when>
                                    <c:when test="${note.getDateGenerated() != null}">
                                        <span><fmt:formatDate value="${note.getDateGenerated()}" pattern="MMM d, yyyy"/></span>
                                    </c:when>
                                </c:choose>
                                <c:if test="${note.getStatus() != null}">
                                    <c:choose>
                                        <c:when test="${note.getStatus().getId() == 3}">
                                            <span class="note-chip chip-wait-psp">Waiting on <c:out value="${pspName}"/></span>
                                        </c:when>
                                        <c:when test="${note.getStatus().getId() == 1}">
                                            <span class="note-chip chip-wait-you">Waiting on You</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="note-chip chip-info"><c:out value="${note.getStatus().getDescription()}"/></span>
                                        </c:otherwise>
                                    </c:choose>
                                </c:if>
                                <c:if test="${authorIsMe}">
                                    <span class="note-chip chip-you-sent"><i class="bi bi-send"></i> Sent</span>
                                </c:if>
                            </div>
                            <div class="note-body">
                                ${note.getDetail()}
                            </div>
                        </div>
                    </c:if>
                </c:forEach>

                <c:if test="${visibleNotes == 0}">
                    <div class="timeline-hint">— No messages yet —</div>
                    <div class="empty-notes">
                        Messages between you and the <c:out value="${pspName}"/> setup team will appear here.
                        Use the box above to send the first message.
                    </div>
                </c:if>
            </div>
        </div>

    </div>
</div>

<script>
  // Gentle guard: don't send empty nudges; disable button while submitting.
  (function() {
    var form = document.getElementById('nudgeForm');
    var body = document.getElementById('nudgeBody');
    var btn  = document.getElementById('nudgeSubmit');
    if (!form) return;
    form.addEventListener('submit', function(e) {
      if (body.value.trim().length === 0) { e.preventDefault(); return; }
      btn.disabled = true;
      btn.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Sending...';
    });
  })();
</script>
</body>
</html>
