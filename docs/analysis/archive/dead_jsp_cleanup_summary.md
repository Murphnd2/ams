# Dead JSP Cleanup Summary

**Date:** 2026-03-03
**Branch:** `cleanup/dead-jsp-removal`
**Base branch:** `refactor/modernize-architecture`

---

## Results

| Metric | Count |
|--------|-------|
| JSP files deleted | **185** |
| JSP files remaining | 246 |
| Compilation errors | **0** (BUILD SUCCESS) |

---

## Categories Breakdown

| Category | Count | Description |
|----------|-------|-------------|
| 1. Old pre-25 versions in modern tree | 9 | Replaced by "25" variants (e.g., `detailRenewal.jsp` → `detailRenewal25.jsp`) |
| 2. Abandoned/orphaned in modern tree | 14 | Never wired up or replaced by inline implementations |
| 3. Old `activity/` tree (zero-ref) | 58 | Pre-modernization pages and components |
| 4. Old authentication pages | 4 | Replaced by modern auth flow |
| 5. Old root view files | 2 | `autoInputScreen.jsp`, `billingHome.jsp` — replaced by 25 versions |
| 6. Old `automate/` tree | 2 | Abandoned automation pages |
| 7. Old `checklist/` manager tree | 4 | Old checklist builder/manager — no servlet dispatches |
| 8. Old `general/` components | 11 | Legacy dropdowns, forms, email views |
| 9. Old `psp/admin/` tree | 60 | Entire dead tree — replaced by modern `sales/` pages |
| 10. Old `agency/` pages | 3 | Legacy agency pages |
| 11. Dead chain — old `navbar.jsp` | 8 | Old navbar + its exclusive includes (all parents dead) |
| 12. Dead chain — `general/` blocks | 10 | Only referenced by dead `psp/admin/` forms |
| **Total** | **185** | |

---

## Deleted Files (Full List)

### Category 1: Old Pre-25 Versions in Modern Tree (9)
- `WEB-INF/view/a/activityDetail/columns/detail/detailRenewal.jsp`
- `WEB-INF/view/a/activityDetail/columns/detail/detailSetup.jsp`
- `WEB-INF/view/a/activityDetail/columns/detail/detailTicket.jsp`
- `WEB-INF/view/a/activityDetail/columns/detail/modals/modContactModal.jsp`
- `WEB-INF/view/a/activityDetail/columns/modals/contactManager.jsp`
- `WEB-INF/view/a/general/activityToolbar/ownerAndDateChange/ownerModal.jsp`
- `WEB-INF/view/a/pspHome/columns/activities/activityHeader.jsp`
- `WEB-INF/view/a/pspHome/pspHome.jsp`
- `WEB-INF/view/a/taskManager/taskManager.jsp`

### Category 2: Abandoned/Orphaned in Modern Tree (14)
- `WEB-INF/view/a/activityDetail/columns/checklist/modals/manageTaskModal.jsp`
- `WEB-INF/view/a/activityDetail/columns/modals/otherContact25.jsp`
- `WEB-INF/view/a/activityDetail/columns/modals/otherContactModal.jsp`
- `WEB-INF/view/a/activityDetail/columns/modals/viewPastActivities25.jsp`
- `WEB-INF/view/a/activityDetail/webLinkListModal25.jsp`
- `WEB-INF/view/a/checklistDetail/addToDoModal.jsp`
- `WEB-INF/view/a/checklistDetail/checklistDetail25.jsp`
- `WEB-INF/view/a/general/activityToolbar/ownerAndDateChange/changeOwnerForm25.jsp`
- `WEB-INF/view/a/general/activityToolbar/ownerAndDateChange/dateModal25.jsp`
- `WEB-INF/view/a/general/email/addRecipientModal25.jsp`
- `WEB-INF/view/a/navbar/createUserForm25.jsp`
- `WEB-INF/view/a/navbar/ddContactMethod25.jsp`
- `WEB-INF/view/a/pspHome/columns/activities/activityList26.jsp`
- `WEB-INF/view/a/renew/upcomingRenewalGenerator.jsp`

### Category 3: Old activity/ Tree (58)
- `WEB-INF/view/activity/activityDetail.jsp`
- `WEB-INF/view/activity/addDocumentToActivity.jsp`
- `WEB-INF/view/activity/addNoteToActivityForm.jsp`
- `WEB-INF/view/activity/addUrlToActivity.jsp`
- `WEB-INF/view/activity/adminHomeHeader.jsp`
- `WEB-INF/view/activity/adminHomeHeader2.jsp`
- `WEB-INF/view/activity/adminHomeHeader3.jsp`
- `WEB-INF/view/activity/checklist/automationModal.jsp`
- `WEB-INF/view/activity/checklist/checklistDetail.jsp`
- `WEB-INF/view/activity/checklist/checklistDetailV1.jsp`
- `WEB-INF/view/activity/checklist/checklistHome.jsp`
- `WEB-INF/view/activity/checklist/component/ddRemainingChecklist.jsp`
- `WEB-INF/view/activity/checklist/component/viewRemainingChecklistForm.jsp`
- `WEB-INF/view/activity/checklist/myChecklistAccordion.jsp`
- `WEB-INF/view/activity/checklist/myChecklists.jsp`
- `WEB-INF/view/activity/checklist/myChecklistsNew.jsp`
- `WEB-INF/view/activity/checklist/myChecklistsV1.jsp`
- `WEB-INF/view/activity/checklist/sequences/recurringList/modifyRecurringSequenceFromChecklist.jsp`
- `WEB-INF/view/activity/checklist/sequences/sequenceHome.jsp`
- `WEB-INF/view/activity/checklist/task/addLinkShortcutForm.jsp`
- `WEB-INF/view/activity/checklist/task/components/addLinkToTaskAlt.jsp`
- `WEB-INF/view/activity/checklist/task/components/addLinkToTaskAltModal.jsp`
- `WEB-INF/view/activity/checklist/toDo/closeToDoForm.jsp`
- `WEB-INF/view/activity/checklist/toDo/reOpenToDoForm.jsp`
- `WEB-INF/view/activity/checklist/toDo/toDoNotCompleteMain.jsp`
- `WEB-INF/view/activity/checklist/toDoListForm.jsp`
- `WEB-INF/view/activity/checklist/toDoListFormNew.jsp`
- `WEB-INF/view/activity/checklist/toDoListFormV01.jsp`
- `WEB-INF/view/activity/hold.jsp`
- `WEB-INF/view/activity/modContactModal1.jsp`
- `WEB-INF/view/activity/modals/docLinksModal.jsp`
- `WEB-INF/view/activity/modals/otherContactsModal.jsp`
- `WEB-INF/view/activity/modals/ownershipModal.jsp`
- `WEB-INF/view/activity/modals/pastActivityModal.jsp`
- `WEB-INF/view/activity/note/activityHistory.jsp`
- `WEB-INF/view/activity/note/addNoteToActivityNew.jsp`
- `WEB-INF/view/activity/note/emailList.jsp`
- `WEB-INF/view/activity/renew/addRenewalItemMod.jsp`
- `WEB-INF/view/activity/renew/components/ddRemoveContactForm.jsp`
- `WEB-INF/view/activity/renew/components/listRenewalItemsNew.jsp`
- `WEB-INF/view/activity/renew/components/renewalListOC.jsp`
- `WEB-INF/view/activity/renew/renewHome.jsp`
- `WEB-INF/view/activity/renew/renewalDetail.jsp`
- `WEB-INF/view/activity/renew/renewalList.jsp`
- `WEB-INF/view/activity/setup/components/itemsInSetupForm.jsp`
- `WEB-INF/view/activity/setup/components/itemsInSetupFormNew.jsp`
- `WEB-INF/view/activity/setup/components/listContactsSetup.jsp`
- `WEB-INF/view/activity/setup/components/listSetupItems.jsp`
- `WEB-INF/view/activity/setup/components/listSetupItemsNew.jsp`
- `WEB-INF/view/activity/template/createTicketTemplate.jsp`
- `WEB-INF/view/activity/ticket/components/ddContactMethod.jsp`
- `WEB-INF/view/activity/ticket/components/ddContactMethodNew.jsp`
- `WEB-INF/view/activity/ticket/components/ddEmployeeList.jsp`
- `WEB-INF/view/activity/ticket/components/ddEmployeeListAlt.jsp`
- `WEB-INF/view/activity/ticket/components/ddEmployeeListNew.jsp`
- `WEB-INF/view/activity/ticket/components/ticketContactForm.jsp`
- `WEB-INF/view/activity/ticket/createTicketModal.jsp`
- `WEB-INF/view/activity/ticket/ticketDetailNew.jsp`

### Category 4: Old Authentication Pages (4)
- `WEB-INF/view/authentication/loginHelpForm.jsp`
- `WEB-INF/view/authentication/loginPage.jsp`
- `WEB-INF/view/authentication/resetPassword.jsp`
- `WEB-INF/view/authentication/timeclock/punchClockOC.jsp`

### Category 5: Old Root View Files (2)
- `WEB-INF/view/autoInputScreen.jsp`
- `WEB-INF/view/billingHome.jsp`

### Category 6: Old automate/ Tree (2)
- `WEB-INF/view/automate/addAutomationForm.jsp`
- `WEB-INF/view/automate/automationPreview.jsp`

### Category 7: Old checklist/ Manager Tree (4)
- `WEB-INF/view/checklist/checklistBuilder.jsp`
- `WEB-INF/view/checklist/checklistManager.jsp`
- `WEB-INF/view/checklist/components/ddTemplateGroupsRaw.jsp`
- `WEB-INF/view/checklist/recurringManager.jsp`

### Category 8: Old general/ Components (11)
- `WEB-INF/view/general/admin/xferForm.jsp`
- `WEB-INF/view/general/dropDown/ddContactMethods.jsp`
- `WEB-INF/view/general/dropDown/ddInsuranceTypes.jsp`
- `WEB-INF/view/general/dropDown/ddLDOCs.jsp`
- `WEB-INF/view/general/dropDown/ddPreTaxOptions.jsp`
- `WEB-INF/view/general/dropDown/ddPriceItems.jsp`
- `WEB-INF/view/general/dropDown/ddRecurringFrequencies.jsp`
- `WEB-INF/view/general/dropDown/ddStandardCopays.jsp`
- `WEB-INF/view/general/email/emailView.jsp`
- `WEB-INF/view/general/emailAutomation.jsp`
- `WEB-INF/view/general/radioButton/rbContactMethods.jsp`

### Category 9: Old psp/admin/ Tree (60)
- `WEB-INF/view/psp/admin/adminAgencyHome.jsp`
- `WEB-INF/view/psp/admin/adminRateHome.jsp`
- `WEB-INF/view/psp/admin/accordion/agencyMainAccord.jsp`
- `WEB-INF/view/psp/admin/forms/addAgencyForm.jsp`
- `WEB-INF/view/psp/admin/forms/addNewRateForm.jsp`
- `WEB-INF/view/psp/admin/forms/addPriceItemForm.jsp`
- `WEB-INF/view/psp/admin/forms/addProspectForm.jsp`
- `WEB-INF/view/psp/admin/forms/addPspAgentForm.jsp`
- `WEB-INF/view/psp/admin/forms/addServiceItemForm.jsp`
- `WEB-INF/view/psp/admin/forms/addServiceModuleForm.jsp`
- `WEB-INF/view/psp/admin/forms/agencyAgentListForm.jsp`
- `WEB-INF/view/psp/admin/forms/assignAgencyToRateForm.jsp`
- `WEB-INF/view/psp/admin/forms/assignAgentToAgencyForm.jsp`
- `WEB-INF/view/psp/admin/forms/assignedAgenciesForm.jsp`
- `WEB-INF/view/psp/admin/forms/buildRatePricingForm.jsp`
- `WEB-INF/view/psp/admin/forms/itemListForServiceForm.jsp`
- `WEB-INF/view/psp/admin/forms/linkLosToModulesForm.jsp`
- `WEB-INF/view/psp/admin/forms/linkModulesToServiceItemsForm.jsp`
- `WEB-INF/view/psp/admin/forms/modAgencyForm.jsp`
- `WEB-INF/view/psp/admin/forms/modAgentForm.jsp`
- `WEB-INF/view/psp/admin/forms/priceListForRateForm.jsp`
- `WEB-INF/view/psp/admin/forms/proposalListForm.jsp`
- `WEB-INF/view/psp/admin/forms/prospectListForm.jsp`
- `WEB-INF/view/psp/admin/forms/pspAgencyListForm.jsp`
- `WEB-INF/view/psp/admin/forms/pspLosListForm.jsp`
- `WEB-INF/view/psp/admin/forms/pspRateListForm.jsp`
- `WEB-INF/view/psp/admin/forms/removeAgentFromAgencyForm.jsp`
- `WEB-INF/view/psp/admin/forms/serviceListForLosForm.jsp`
- `WEB-INF/view/psp/admin/modals/addAgencyModal.jsp`
- `WEB-INF/view/psp/admin/modals/addNewPriceItemModal.jsp`
- `WEB-INF/view/psp/admin/modals/addNewRateModal.jsp`
- `WEB-INF/view/psp/admin/modals/addNewServiceItemModal.jsp`
- `WEB-INF/view/psp/admin/modals/addNewServiceModuleModal.jsp`
- `WEB-INF/view/psp/admin/modals/addProposalModal.jsp`
- `WEB-INF/view/psp/admin/modals/addProspectModal.jsp`
- `WEB-INF/view/psp/admin/modals/addPspAgentModal.jsp`
- `WEB-INF/view/psp/admin/modals/assignRemoveAgentModal.jsp`
- `WEB-INF/view/psp/admin/modals/buildRateTableModal.jsp`
- `WEB-INF/view/psp/admin/modals/linkModuleToLosModal.jsp`
- `WEB-INF/view/psp/admin/modals/linkServiceItemToModuleModal.jsp`
- `WEB-INF/view/psp/admin/modals/pspAgencyListModal.jsp`
- `WEB-INF/view/psp/admin/modals/sendSummitForm.jsp`
- `WEB-INF/view/psp/admin/modals/sendSummitIntroMod.jsp`
- `WEB-INF/view/psp/admin/pages/adminAgencyHome/leftColumn.jsp`
- `WEB-INF/view/psp/admin/pages/adminAgencyHome/leftColumn/agencyList.jsp`
- `WEB-INF/view/psp/admin/pages/adminAgencyHome/leftColumn/agentList.jsp`
- `WEB-INF/view/psp/admin/pages/adminAgencyHome/leftColumn/proposalList.jsp`
- `WEB-INF/view/psp/admin/pages/adminAgencyHome/leftColumn/prospectList.jsp`
- `WEB-INF/view/psp/admin/pages/adminAgencyHome/mainSection.jsp`
- `WEB-INF/view/psp/admin/parts/ddAgencyRateList.jsp`
- `WEB-INF/view/psp/admin/parts/ddLosRaw.jsp`
- `WEB-INF/view/psp/admin/parts/ddPriceItemsRaw.jsp`
- `WEB-INF/view/psp/admin/parts/ddProposalListForm.jsp`
- `WEB-INF/view/psp/admin/parts/ddProspectListForm.jsp`
- `WEB-INF/view/psp/admin/parts/ddProspectListFormAlt.jsp`
- `WEB-INF/view/psp/admin/parts/ddRatesRaw.jsp`
- `WEB-INF/view/psp/admin/parts/ddServiceItemsRaw.jsp`
- `WEB-INF/view/psp/admin/parts/ddServiceModulesRaw.jsp`
- `WEB-INF/view/psp/admin/parts/headerRateTable.jsp`
- `WEB-INF/view/psp/admin/parts/pspAdminNavbar.jsp`

### Category 10: Old agency/ Pages (3)
- `WEB-INF/view/agency/noProposalFound.jsp`
- `WEB-INF/view/agency/proposalMain.jsp`
- `WEB-INF/view/agency/addProposalForm.jsp`

### Category 11: Dead Chain — Old navbar.jsp (8)
- `WEB-INF/view/navbar.jsp`
- `WEB-INF/view/altNavbar.jsp`
- `WEB-INF/view/authentication/createUserModal.jsp`
- `WEB-INF/view/authentication/createUserForm.jsp`
- `WEB-INF/view/takeover/updatePspMod.jsp`
- `WEB-INF/view/takeover/updatePspForm.jsp`
- `WEB-INF/view/weblink/addInsertLinkModal.jsp`
- `WEB-INF/view/weblink/addInsertLinkForm.jsp`

### Category 12: Dead Chain — general/ Blocks (10)
- `WEB-INF/view/general/personBlock.jsp`
- `WEB-INF/view/general/addressBlock.jsp`
- `WEB-INF/view/general/personBlockMod.jsp`
- `WEB-INF/view/general/addressBlockMod.jsp`
- `WEB-INF/view/general/personBlockAgentMod.jsp`
- `WEB-INF/view/general/addressBlockAgentMod.jsp`
- `WEB-INF/view/general/agencyBlock.jsp`
- `WEB-INF/view/general/agencyBlockMod.jsp`
- `WEB-INF/view/general/personBlockProspectMod.jsp`
- `WEB-INF/view/general/dropDown/ddStates.jsp`

---

## Testing Instructions

To test this branch, build with `mvn clean package -Plocal` and deploy. Verify all navbar links, activity detail, admin pages, sales pipeline, BPO dashboard, and billing flows still work.

---

## Analysis

Full investigation report: `docs/analysis/dead_jsp_investigation.md`
