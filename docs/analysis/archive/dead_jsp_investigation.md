# Dead JSP Investigation Report

**Date:** 2026-03-03
**Branch:** `cleanup/dead-jsp-removal`
**Methodology:** Forward reachability from servlet dispatch roots + recursive c:import/include tracing

---

## Summary

| Metric | Count |
|--------|-------|
| Total JSP files | 431 |
| **Live JSPs (remaining)** | **246** |
| **Dead JSPs (deleted)** | **185** |
| Uncertain (kept) | 0 |

---

## Live Entry Points

JSPs are "live" if dispatched by a servlet or directly accessible at the webapp root:

### Servlet-Dispatched JSPs (52 roots)
- `index.jsp` — LogOut, login, AuthenticateUser, CreateBilling25
- `initialize.jsp` — InitializeDataBase, GoInitialize25
- `landing-page.jsp` — directly accessible (public marketing)
- `125eligibility.jsp` — directly accessible (eligibility form)
- `125eligibilitySuccess.jsp` — Eligibility125SubmitServlet redirect
- `login.jsp` — directly accessible (alternate login page)
- `WEB-INF/view/a/pspHome/pspHome25.jsp` — ViewHome25
- `WEB-INF/view/a/activityDetail/activityDetail25.jsp` — ViewActivity25, ViewChecklist25
- `WEB-INF/view/a/general/pspDashboard25.jsp` — PspDashboardHome
- `WEB-INF/view/a/general/sequenceBuilder/sequenceManager25.jsp` — SequenceBuilder25
- `WEB-INF/view/a/general/sequenceBuilder/sequenceBuilderForm.jsp` — TaskBuilder25, ClearGrid25
- `WEB-INF/view/a/taskManager/taskManager25.jsp` — ManageTask25
- `WEB-INF/view/a/general/emailHistoryList25.jsp` — ViewEmailHistory
- `WEB-INF/view/a/general/emailView25.jsp` — ViewEmail
- `WEB-INF/view/a/general/emailMaster25.jsp` — CreateEmail25
- `WEB-INF/view/a/taskManager/autoInputScreen25.jsp` — SendAuto25
- `WEB-INF/view/a/taskManager/autoConfirmSend25.jsp` — SendAuto25
- `WEB-INF/view/a/taskManager/previewEmailModal.jsp` — PreviewAutomation
- `WEB-INF/view/a/renew/upcomingRenewals25.jsp` — UpcomingRenewals25
- `WEB-INF/view/a/general/benefitAudit25.jsp` — BenefitAudit25
- `WEB-INF/view/a/admin/vendorManager25.jsp` — VendorManager25
- `WEB-INF/view/a/z_acessory/fileUploadPage.jsp` — ShowUploadPage
- `WEB-INF/view/a/z_acessory/uploadSummary.jsp` — UploadCsvServlet
- `WEB-INF/view/a/general/summitImport/step1Upload.jsp` — SummitImportWizard (dynamic dispatch)
- `WEB-INF/view/a/general/summitImport/step2Configure.jsp` — SummitImportWizard (dynamic dispatch)
- `WEB-INF/view/a/general/summitImport/step4Results.jsp` — SummitImportWizard (dynamic dispatch)
- `WEB-INF/view/a/pspHome/columns/timeClock/reviewTimeCorrections.jsp` — ReviewTimeCorrections
- `WEB-INF/view/billing/billingHome25.jsp` — GoBillingHome
- `WEB-INF/view/billing/erBilling25.jsp` — EmployerBillingDetail
- `WEB-INF/view/billing/sendBillingForm.jsp` — EmailBillingToEmployer
- `WEB-INF/view/user/pspBranding25.jsp` — UploadPspBranding
- `WEB-INF/view/bpo/bpoHome25.jsp` — BpoHome
- `WEB-INF/view/bpo/pspClients25.jsp` — BpoPspClients
- `WEB-INF/view/sales/agentHome25.jsp` — AgentHome
- `WEB-INF/view/sales/agencyManager25.jsp` — PspAgencyHome
- `WEB-INF/view/sales/rateManager25.jsp` — PspAdminHome
- `WEB-INF/view/sales/serviceManager25.jsp` — ServiceManagerHome
- `WEB-INF/view/sales/viewProposal.jsp` — ViewProposal
- `WEB-INF/view/sales/proposalDetail.jsp` — ProposalDetail
- `WEB-INF/view/sales/proposalBuilder.jsp` — ProposalBuilder
- `WEB-INF/view/sales/proposalSettings.jsp` — ProposalSettings
- `WEB-INF/view/sales/reviewApplication.jsp` — ReviewApplication
- `WEB-INF/view/sales/reviewApplications.jsp` — ReviewApplications
- `WEB-INF/view/sales/sendProposal.jsp` — SendProposal
- `WEB-INF/view/sales/library25.jsp` — LibraryHome
- `WEB-INF/view/sales/manualSetup.jsp` — GenerateProp25
- `WEB-INF/view/sales/acceptInvite.jsp` — AcceptInvite
- `WEB-INF/view/sales/applyForProposal.jsp` — ApplyForProposal
- `WEB-INF/view/sales/applicationConfirmation.jsp` — ApplyForProposal (POST)
- `WEB-INF/view/authentication/setPassword.jsp` — ResetLogin, OneTimeUserLogin
- `WEB-INF/view/authentication/loginHelp.jsp` — NeedsHelp
- `WEB-INF/view/market/landing.jsp` — LandingServlet

### Live via Include Chain (50 files)
These are included by live root pages via `<c:import>`, `<%@ include %>`, or `<jsp:include>`:

**Shared resources:**
- `WEB-INF/view/css-js.jsp`
- `WEB-INF/view/a/general/navbar25.jsp`

**Navbar25 sub-includes:**
- `a/navbar/createUserModal25.jsp`
- `a/general/updatePspMod25.jsp` → `a/general/updatePspForm25.jsp`
- `a/renew/createBlankRenewalMod25.jsp` → `a/renew/createBlankRenewal25.jsp`
- `a/general/addInsertLinkModal25.jsp` → `a/general/addInsertLinkForm25.jsp`
- `a/setup/generateSetupMod25.jsp` → `a/setup/generateSetupForm25.jsp`
- `authentication/loginFormModal.jsp` → `authentication/loginForm.jsp`
- `general/admin/adminMenuOC.jsp`
- `a/todo/addReminder25.jsp`, `a/todo/addChecklist25.jsp`
- `a/renew/upcomingRenewalsModal25.jsp` → `a/renew/renewalList25.jsp`
- `a/navbar/createTicket25.jsp`
- `a/pspHome/columns/activities/addActivityModal25.jsp`
- `a/checklistDetail/makeRecurringModal25.jsp` → `a/checklistDetail/MakeRecurringForm25.jsp` → `a/checklistDetail/ddFrequency25.jsp`
- `a/general/smtpSettingsMod25.jsp`, `a/general/userManager25.jsp`, `a/general/chatAssistant25.jsp`

**PspHome25 sub-includes:**
- `a/pspHome/columns/timeClock/timeClockHeader.jsp`, `timeClockDetail25.jsp`, `timeCorrectionModal.jsp`
- `a/pspHome/columns/quickTicket25.jsp`
- `a/pspHome/columns/activities/activityHeader25.jsp`, `activityList25.jsp`
- `a/pspHome/columns/toDos/toDoHeader.jsp`, `toDoCurrentList25.jsp` → `a/general/ddUserList25.jsp`
- `a/pspHome/columns/toDos/toDoFutureList25.jsp`

**ActivityDetail25 sub-includes:**
- `a/activityDetail/columns/checklist/checklistHeader.jsp`, `checklistBasic25.jsp`, `checklistAutomation25.jsp`
- `a/activityDetail/columns/checklist/checklistFooter25.jsp` → `closeActivityModal.jsp`, `a/checklistDetail/addToDo25.jsp` → `addToDoForm.jsp`
- `a/checklistDetail/modifyRecurring25.jsp`
- `a/activityDetail/columns/detail/detailHeader25.jsp` → `a/general/activityToolbar/ownerAndDateChange/ownerModal25.jsp`, `pastActivityModal25.jsp`
- `a/activityDetail/columns/detail/*25.jsp` (all detail sub-panels)
- `a/activityDetail/columns/detail/modals/addRenewalItemMod25.jsp` → `ddBensNotInRenewalForm25.jsp` → `ddBensNotInRenewal25.jsp`
- `a/activityDetail/columns/detail/modals/modContact25.jsp`
- `a/activityDetail/columns/modals/addContactToActivity25.jsp`, `contactManager25.jsp`, `addContactToActivityMod.jsp` → `addContactToActivityForm.jsp`
- `a/activityDetail/columns/history/historyHeader.jsp`, `historyDetail25.jsp`
- `a/activityDetail/columns/detail/detailDocsLinks25.jsp` → `activity/addDocumentToActivityMod.jsp`, `activity/addUrlToActivityMod.jsp`
- `a/activityDetail/columns/detail/detailFooter25.jsp` → `activity/setup/addSetupItemMod.jsp` → `activity/setup/components/addModuleToSetupForm.jsp`
- `a/activityDetail/columns/detail/detailAddNote25.jsp` → `a/general/globalDropDowns/ddReasons25.jsp`, `ddNoteStatus25.jsp`
- `a/activityDetail/webLinkList25.jsp`

**Billing sub-includes:**
- `billing/billingGrid25.jsp`

**BPO sub-includes:**
- `a/pspHome/columns/toDos/toDoCurrentList25.jsp` (shared with bpoHome25)

**Sales sub-includes:**
- `sales/proposalFeatures.jsp`, `sales/proposalPricing.jsp` (from viewProposal.jsp)
- `sales/adminNav.jsp`

**Shared old components still used by modern pages:**
- `activity/checklist/sequences/recurringList/components/ddDaysAdvance.jsp`
- `activity/checklist/sequences/recurringList/components/ddUsers.jsp`
- `activity/checklist/sequences/recurringList/components/ddFrequency.jsp`
- `general/utility/master/css-js.jsp`

---

## Dead JSPs — Full List (141 files)

### Category 1: Old Pre-25 Versions in Modern Tree (9 files)
Replaced by "25" variants in the same directory. Zero references.

| # | File | Replaced By |
|---|------|------------|
| 1 | `a/activityDetail/columns/detail/detailRenewal.jsp` | `detailRenewal25.jsp` |
| 2 | `a/activityDetail/columns/detail/detailSetup.jsp` | `detailSetup25.jsp` |
| 3 | `a/activityDetail/columns/detail/detailTicket.jsp` | `detailTicket25.jsp` |
| 4 | `a/activityDetail/columns/detail/modals/modContactModal.jsp` | `modContact25.jsp` |
| 5 | `a/activityDetail/columns/modals/contactManager.jsp` | `contactManager25.jsp` |
| 6 | `a/general/activityToolbar/ownerAndDateChange/ownerModal.jsp` | `ownerModal25.jsp` |
| 7 | `a/pspHome/columns/activities/activityHeader.jsp` | `activityHeader25.jsp` |
| 8 | `a/pspHome/pspHome.jsp` | `pspHome25.jsp` |
| 9 | `a/taskManager/taskManager.jsp` | `taskManager25.jsp` |

### Category 2: Abandoned/Orphaned in Modern Tree (14 files)
Never wired up, or replaced by inline implementations. Zero references.

| # | File | Reason |
|---|------|--------|
| 10 | `a/activityDetail/columns/checklist/modals/manageTaskModal.jsp` | Never included by any page |
| 11 | `a/activityDetail/columns/modals/otherContact25.jsp` | Orphaned — not imported |
| 12 | `a/activityDetail/columns/modals/otherContactModal.jsp` | Old modal, never included |
| 13 | `a/activityDetail/columns/modals/viewPastActivities25.jsp` | Orphaned view |
| 14 | `a/activityDetail/webLinkListModal25.jsp` | Orphaned modal wrapper |
| 15 | `a/checklistDetail/addToDoModal.jsp` | Replaced by `addToDo25.jsp` |
| 16 | `a/checklistDetail/checklistDetail25.jsp` | No servlet dispatches to it |
| 17 | `a/general/activityToolbar/ownerAndDateChange/changeOwnerForm25.jsp` | Orphaned |
| 18 | `a/general/activityToolbar/ownerAndDateChange/dateModal25.jsp` | Orphaned |
| 19 | `a/general/email/addRecipientModal25.jsp` | Replaced by inline modal in emailMaster25 |
| 20 | `a/navbar/createUserForm25.jsp` | Orphaned — not imported by createUserModal25 |
| 21 | `a/navbar/ddContactMethod25.jsp` | Orphaned dropdown |
| 22 | `a/pspHome/columns/activities/activityList26.jsp` | Abandoned future version |
| 23 | `a/renew/upcomingRenewalGenerator.jsp` | No servlet dispatches |

### Category 3: Old activity/ Tree — Direct Zero-Reference Files (58 files)
Pre-modernization pages and components with zero cross-references.

| # | File |
|---|------|
| 24 | `activity/activityDetail.jsp` |
| 25 | `activity/addDocumentToActivity.jsp` |
| 26 | `activity/addNoteToActivityForm.jsp` |
| 27 | `activity/addUrlToActivity.jsp` |
| 28 | `activity/adminHomeHeader.jsp` |
| 29 | `activity/adminHomeHeader2.jsp` |
| 30 | `activity/adminHomeHeader3.jsp` |
| 31 | `activity/checklist/automationModal.jsp` |
| 32 | `activity/checklist/checklistDetail.jsp` |
| 33 | `activity/checklist/checklistDetailV1.jsp` |
| 34 | `activity/checklist/checklistHome.jsp` |
| 35 | `activity/checklist/component/ddRemainingChecklist.jsp` |
| 36 | `activity/checklist/component/viewRemainingChecklistForm.jsp` |
| 37 | `activity/checklist/myChecklistAccordion.jsp` |
| 38 | `activity/checklist/myChecklists.jsp` |
| 39 | `activity/checklist/myChecklistsNew.jsp` |
| 40 | `activity/checklist/myChecklistsV1.jsp` |
| 41 | `activity/checklist/sequences/recurringList/modifyRecurringSequenceFromChecklist.jsp` |
| 42 | `activity/checklist/sequences/sequenceHome.jsp` |
| 43 | `activity/checklist/task/addLinkShortcutForm.jsp` |
| 44 | `activity/checklist/task/components/addLinkToTaskAlt.jsp` |
| 45 | `activity/checklist/task/components/addLinkToTaskAltModal.jsp` |
| 46 | `activity/checklist/toDo/closeToDoForm.jsp` |
| 47 | `activity/checklist/toDo/reOpenToDoForm.jsp` |
| 48 | `activity/checklist/toDo/toDoNotCompleteMain.jsp` |
| 49 | `activity/checklist/toDoListForm.jsp` |
| 50 | `activity/checklist/toDoListFormNew.jsp` |
| 51 | `activity/checklist/toDoListFormV01.jsp` |
| 52 | `activity/hold.jsp` |
| 53 | `activity/modContactModal1.jsp` |
| 54 | `activity/modals/docLinksModal.jsp` |
| 55 | `activity/modals/otherContactsModal.jsp` |
| 56 | `activity/modals/ownershipModal.jsp` |
| 57 | `activity/modals/pastActivityModal.jsp` |
| 58 | `activity/note/activityHistory.jsp` |
| 59 | `activity/note/addNoteToActivityNew.jsp` |
| 60 | `activity/note/emailList.jsp` |
| 61 | `activity/renew/addRenewalItemMod.jsp` |
| 62 | `activity/renew/components/ddRemoveContactForm.jsp` |
| 63 | `activity/renew/components/listRenewalItemsNew.jsp` |
| 64 | `activity/renew/components/renewalListOC.jsp` |
| 65 | `activity/renew/renewHome.jsp` |
| 66 | `activity/renew/renewalDetail.jsp` |
| 67 | `activity/renew/renewalList.jsp` |
| 68 | `activity/setup/components/itemsInSetupForm.jsp` |
| 69 | `activity/setup/components/itemsInSetupFormNew.jsp` |
| 70 | `activity/setup/components/listContactsSetup.jsp` |
| 71 | `activity/setup/components/listSetupItems.jsp` |
| 72 | `activity/setup/components/listSetupItemsNew.jsp` |
| 73 | `activity/template/createTicketTemplate.jsp` |
| 74 | `activity/ticket/components/ddContactMethod.jsp` |
| 75 | `activity/ticket/components/ddContactMethodNew.jsp` |
| 76 | `activity/ticket/components/ddEmployeeList.jsp` |
| 77 | `activity/ticket/components/ddEmployeeListAlt.jsp` |
| 78 | `activity/ticket/components/ddEmployeeListNew.jsp` |
| 79 | `activity/ticket/components/ticketContactForm.jsp` |
| 80 | `activity/ticket/createTicketModal.jsp` |
| 81 | `activity/ticket/ticketDetailNew.jsp` |

### Category 4: Old Authentication Pages (4 files)
No servlet dispatches; replaced by modern auth flow.

| # | File | Reason |
|---|------|--------|
| 82 | `authentication/loginHelpForm.jsp` | Orphaned form |
| 83 | `authentication/loginPage.jsp` | Old login page, not dispatched |
| 84 | `authentication/resetPassword.jsp` | Old reset page, not dispatched |
| 85 | `authentication/timeclock/punchClockOC.jsp` | Orphaned timeclock OC |

### Category 5: Old Root View Files (2 files)

| # | File | Replaced By |
|---|------|------------|
| 86 | `autoInputScreen.jsp` | `a/taskManager/autoInputScreen25.jsp` |
| 87 | `billingHome.jsp` | `billing/billingHome25.jsp` |

### Category 6: Old automate/ Tree (2 files)

| # | File |
|---|------|
| 88 | `automate/addAutomationForm.jsp` |
| 89 | `automate/automationPreview.jsp` |

### Category 7: Old checklist/ Manager Tree (4 files)
Old checklist builder/manager pages — no servlet dispatches.

| # | File |
|---|------|
| 90 | `checklist/checklistBuilder.jsp` |
| 91 | `checklist/checklistManager.jsp` |
| 92 | `checklist/components/ddTemplateGroupsRaw.jsp` |
| 93 | `checklist/recurringManager.jsp` |

### Category 8: Old general/ Components (11 files)

| # | File |
|---|------|
| 94 | `general/admin/xferForm.jsp` |
| 95 | `general/dropDown/ddContactMethods.jsp` |
| 96 | `general/dropDown/ddInsuranceTypes.jsp` |
| 97 | `general/dropDown/ddLDOCs.jsp` |
| 98 | `general/dropDown/ddPreTaxOptions.jsp` |
| 99 | `general/dropDown/ddPriceItems.jsp` |
| 100 | `general/dropDown/ddRecurringFrequencies.jsp` |
| 101 | `general/dropDown/ddStandardCopays.jsp` |
| 102 | `general/email/emailView.jsp` |
| 103 | `general/emailAutomation.jsp` |
| 104 | `general/radioButton/rbContactMethods.jsp` |

### Category 9: Old psp/admin/ Tree — All 60 Files
Both root pages (adminAgencyHome, adminRateHome) have zero servlet dispatches. All sub-components are only referenced by other files within this dead tree. Replaced by modern `sales/agencyManager25.jsp` and `sales/rateManager25.jsp`.

| # | File |
|---|------|
| 105 | `psp/admin/adminAgencyHome.jsp` |
| 106 | `psp/admin/adminRateHome.jsp` |
| 107 | `psp/admin/accordion/agencyMainAccord.jsp` |
| 108 | `psp/admin/forms/addAgencyForm.jsp` |
| 109 | `psp/admin/forms/addNewRateForm.jsp` |
| 110 | `psp/admin/forms/addPriceItemForm.jsp` |
| 111 | `psp/admin/forms/addProspectForm.jsp` |
| 112 | `psp/admin/forms/addPspAgentForm.jsp` |
| 113 | `psp/admin/forms/addServiceItemForm.jsp` |
| 114 | `psp/admin/forms/addServiceModuleForm.jsp` |
| 115 | `psp/admin/forms/agencyAgentListForm.jsp` |
| 116 | `psp/admin/forms/assignAgencyToRateForm.jsp` |
| 117 | `psp/admin/forms/assignAgentToAgencyForm.jsp` |
| 118 | `psp/admin/forms/assignedAgenciesForm.jsp` |
| 119 | `psp/admin/forms/buildRatePricingForm.jsp` |
| 120 | `psp/admin/forms/itemListForServiceForm.jsp` |
| 121 | `psp/admin/forms/linkLosToModulesForm.jsp` |
| 122 | `psp/admin/forms/linkModulesToServiceItemsForm.jsp` |
| 123 | `psp/admin/forms/modAgencyForm.jsp` |
| 124 | `psp/admin/forms/modAgentForm.jsp` |
| 125 | `psp/admin/forms/priceListForRateForm.jsp` |
| 126 | `psp/admin/forms/proposalListForm.jsp` |
| 127 | `psp/admin/forms/prospectListForm.jsp` |
| 128 | `psp/admin/forms/pspAgencyListForm.jsp` |
| 129 | `psp/admin/forms/pspLosListForm.jsp` |
| 130 | `psp/admin/forms/pspRateListForm.jsp` |
| 131 | `psp/admin/forms/removeAgentFromAgencyForm.jsp` |
| 132 | `psp/admin/forms/serviceListForLosForm.jsp` |
| 133 | `psp/admin/modals/addAgencyModal.jsp` |
| 134 | `psp/admin/modals/addNewPriceItemModal.jsp` |
| 135 | `psp/admin/modals/addNewRateModal.jsp` |
| 136 | `psp/admin/modals/addNewServiceItemModal.jsp` |
| 137 | `psp/admin/modals/addNewServiceModuleModal.jsp` |
| 138 | `psp/admin/modals/addProposalModal.jsp` |
| 139 | `psp/admin/modals/addProspectModal.jsp` |
| 140 | `psp/admin/modals/addPspAgentModal.jsp` |
| 141 | `psp/admin/modals/assignRemoveAgentModal.jsp` |
| 142 | `psp/admin/modals/buildRateTableModal.jsp` |
| 143 | `psp/admin/modals/linkModuleToLosModal.jsp` |
| 144 | `psp/admin/modals/linkServiceItemToModuleModal.jsp` |
| 145 | `psp/admin/modals/pspAgencyListModal.jsp` |
| 146 | `psp/admin/modals/sendSummitForm.jsp` |
| 147 | `psp/admin/modals/sendSummitIntroMod.jsp` |
| 148 | `psp/admin/pages/adminAgencyHome/leftColumn.jsp` |
| 149 | `psp/admin/pages/adminAgencyHome/leftColumn/agencyList.jsp` |
| 150 | `psp/admin/pages/adminAgencyHome/leftColumn/agentList.jsp` |
| 151 | `psp/admin/pages/adminAgencyHome/leftColumn/proposalList.jsp` |
| 152 | `psp/admin/pages/adminAgencyHome/leftColumn/prospectList.jsp` |
| 153 | `psp/admin/pages/adminAgencyHome/mainSection.jsp` |
| 154 | `psp/admin/parts/ddAgencyRateList.jsp` |
| 155 | `psp/admin/parts/ddLosRaw.jsp` |
| 156 | `psp/admin/parts/ddPriceItemsRaw.jsp` |
| 157 | `psp/admin/parts/ddProposalListForm.jsp` |
| 158 | `psp/admin/parts/ddProspectListForm.jsp` |
| 159 | `psp/admin/parts/ddProspectListFormAlt.jsp` |
| 160 | `psp/admin/parts/ddRatesRaw.jsp` |
| 161 | `psp/admin/parts/ddServiceItemsRaw.jsp` |
| 162 | `psp/admin/parts/ddServiceModulesRaw.jsp` |
| 163 | `psp/admin/parts/headerRateTable.jsp` |
| 164 | `psp/admin/parts/pspAdminNavbar.jsp` |

### Category 10: Old agency/ Pages (3 files)

| # | File | Reason |
|---|------|--------|
| 165 | `agency/noProposalFound.jsp` | Zero references |
| 166 | `agency/proposalMain.jsp` | Zero references |
| 167 | `agency/addProposalForm.jsp` | Dead chain — only from dead psp/admin |

### Category 11: Dead Chain — Old navbar.jsp and Its Includes (8 files)
`navbar.jsp` is included by 10 pages, ALL of which are dead (old root pages with zero servlet dispatches). Its modals that are NOT also included by `navbar25.jsp` are dead chain.

| # | File | Reason |
|---|------|--------|
| 168 | `navbar.jsp` | All 10 parent pages are dead |
| 169 | `altNavbar.jsp` | Only parent: dead taskManager.jsp |
| 170 | `authentication/createUserModal.jsp` | Only from dead navbar.jsp |
| 171 | `authentication/createUserForm.jsp` | Only from dead createUserModal.jsp |
| 172 | `takeover/updatePspMod.jsp` | Only from dead navbar.jsp |
| 173 | `takeover/updatePspForm.jsp` | Only from dead updatePspMod.jsp |
| 174 | `weblink/addInsertLinkModal.jsp` | Only from dead navbar.jsp |
| 175 | `weblink/addInsertLinkForm.jsp` | Only from dead addInsertLinkModal.jsp |

### Category 12: Dead Chain — general/ Blocks Only Used by Dead psp/admin (10 files)

| # | File | Referenced Only By |
|---|------|-------------------|
| 176 | `general/personBlock.jsp` | Dead psp/admin forms |
| 177 | `general/addressBlock.jsp` | Dead psp/admin forms |
| 178 | `general/personBlockMod.jsp` | Dead psp/admin modAgencyForm |
| 179 | `general/addressBlockMod.jsp` | Dead psp/admin modAgencyForm |
| 180 | `general/personBlockAgentMod.jsp` | Dead psp/admin modAgentForm |
| 181 | `general/addressBlockAgentMod.jsp` | Dead psp/admin modAgentForm |
| 182 | `general/agencyBlock.jsp` | Dead psp/admin addAgencyForm |
| 183 | `general/agencyBlockMod.jsp` | Dead psp/admin modAgencyForm |
| 184 | `general/personBlockProspectMod.jsp` | Dead psp/admin agencyMainAccord |
| 185 | `general/dropDown/ddStates.jsp` | Dead general address blocks |

---

## Files Kept Despite Low References

These files have REFS>0 from live pages or have ambiguous reference chains. Kept per "when in doubt, keep it" rule:

- `a/general/ddUserList.jsp` — basename collision with `general/ddUserList.jsp`, may be live
- `a/general/activityToolbar/ownerAndDateChange/changeOwnerForm.jsp` — basename collision
- `a/general/activityToolbar/ownerAndDateChange/dateModal.jsp` — may be imported by live ownerModal
- `a/pspHome/columns/activities/activityList.jsp` — basename collision with activity/activityList.jsp
- `a/pspHome/columns/toDos/toDoCurrentList.jsp`, `toDoFutureList.jsp` — included by dead pspHome.jsp but basename collision
- `a/pspHome/columns/timeClock/timeClockDetail.jsp` — may be used by live page
- `a/uploadPreview.jsp` — servlet path mismatch (dispatches to /uploadPreview.jsp root)
- `activity/closeActivityModal.jsp` — basename collision with modern version
- `activity/closeActivityForm.jsp` — may be imported by live closeActivityModal
- Various old `activity/` files with REFS>0 from ambiguous basename matches
- `general/ddUserList.jsp` — basename collision
- `checklist/` sub-components (task/taskList.jsp etc.) — may have live refs from checklistManager dead chain but also from other live pages
- `activity/checklist/` sub-components with REFS>0 (addChecklistForm, addReminderForm, etc.) — basename collisions

---

## Notes

1. **SummitImportWizard** uses dynamic dispatch: `forwardTo(request, response, "step1Upload")` which builds the path at runtime. These JSPs have REFS=0 in basename search but ARE live.
2. **PreviewUploadedFilesServlet** dispatches to `/uploadPreview.jsp` (webapp root) but the file exists at `/WEB-INF/view/a/uploadPreview.jsp` — potential path mismatch. File kept.
3. **ReviewHsaAccounts25** dispatches to `/admin/result.jsp` which does not exist — broken servlet reference.
4. **css-js.jsp** exists in two locations: `/WEB-INF/view/css-js.jsp` (live) and `/WEB-INF/view/general/utility/master/css-js.jsp`. Both kept.
