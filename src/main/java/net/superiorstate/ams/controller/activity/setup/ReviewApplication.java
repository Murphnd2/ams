package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.ActivityDAO;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.ApplicationTaskDAO;
import net.superiorstate.ams.data.dao.StorageDAO;
import net.superiorstate.ams.data.service.BpoTaskPushService;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.Activity25;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.application.*;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "ReviewApplication", value = "/ReviewApplication")
public class ReviewApplication extends HttpServlet {

    // ======================== GET — Load application detail ========================

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        if (currentPerson == null) {
            response.sendRedirect("Login");
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect("ReviewApplications?err=" + encode("Missing application ID"));
            return;
        }

        Boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        Boolean isPspUser = Boolean.TRUE.equals(request.getSession().getAttribute("isPspUser"));
        Boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            long proposalId = Long.parseLong(idParam);

            // Load application with proposal, prospect, contact, agent, LOS list
            Query aq = em.createQuery(
                    "SELECT a FROM Application a " +
                            "JOIN FETCH a.proposal p " +
                            "JOIN FETCH p.prospect pr " +
                            "JOIN FETCH pr.contact " +
                            "LEFT JOIN FETCH pr.agent " +
                            "LEFT JOIN FETCH p.losList " +
                            "LEFT JOIN FETCH a.setup " +
                            "LEFT JOIN FETCH a.reviewedBy " +
                            "WHERE a.proposal.id = :pid");
            aq.setParameter("pid", proposalId);
            Application application;
            try {
                application = (Application) aq.getSingleResult();
            } catch (Exception e) {
                response.sendRedirect("ReviewApplications?err=" + encode("Application not found"));
                return;
            }

            // Agent access check: agent-only users can only view their own prospects
            if (isAgent && !isPspAdmin && !isPspUser) {
                Person prospectAgent = application.getProposal().getProspect().getAgent();
                if (prospectAgent == null || prospectAgent.getId() != currentPerson.getId()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                    return;
                }
            }

            // Load field values for this application
            Query fvq = em.createQuery(
                    "SELECT fv FROM ApplicationFieldValue fv " +
                            "JOIN FETCH fv.applicationField af " +
                            "JOIN FETCH af.applicationSection " +
                            "WHERE fv.application.proposal.id = :pid");
            fvq.setParameter("pid", proposalId);
            List<ApplicationFieldValue> fieldValues = fvq.getResultList();

            // Build a map: fieldKey -> fieldValue
            Map<String, String> valueMap = new LinkedHashMap<>();
            for (ApplicationFieldValue fv : fieldValues) {
                valueMap.put(fv.getApplicationField().getFieldKey(), fv.getFieldValue());
            }

            // Use selected LOS/Enhancement IDs if available, fall back to full proposal LOS list
            List<Long> losIds;
            List<Long> enhIds;
            if (application.hasServiceSelections()) {
                losIds = application.getSelectedLosIdList();
                enhIds = application.getSelectedEnhancementIdList();
            } else {
                losIds = application.getProposal().getLosList().stream()
                        .map(LOS::getId).collect(Collectors.toList());
                enhIds = java.util.Collections.emptyList();
            }
            List<Long> safeLosIds = (losIds == null || losIds.isEmpty()) ? List.of(-1L) : losIds;
            List<Long> safeEnhIds = (enhIds == null || enhIds.isEmpty()) ? List.of(-1L) : enhIds;

            Query sq = em.createQuery(
                    "SELECT DISTINCT s FROM ApplicationSection s " +
                            "LEFT JOIN FETCH s.fieldList f " +
                            "LEFT JOIN s.losList los " +
                            "LEFT JOIN s.enhancementList enh " +
                            "WHERE s.suppressed = false AND (s.scope = 'ALL' OR los.id IN :losIds OR enh.id IN :enhIds) " +
                            "ORDER BY s.sortOrder");
            sq.setParameter("losIds", safeLosIds);
            sq.setParameter("enhIds", safeEnhIds);
            List<ApplicationSection> sections = sq.getResultList();

            // Remove suppressed fields and re-sort (EclipseLink DISTINCT can scramble @OrderBy)
            for (ApplicationSection sec : sections) {
                if (sec.getFieldList() != null) {
                    sec.getFieldList().removeIf(ApplicationField::isSuppressed);
                    sec.getFieldList().sort(java.util.Comparator.comparingInt(ApplicationField::getSortOrder));
                }
            }

            // Build display lists of selected service names for reviewer
            if (application.hasServiceSelections()) {
                List<String> selectedServiceNames = new ArrayList<>();
                for (LOS los : application.getProposal().getLosList()) {
                    if (application.getSelectedLosIdList().contains(los.getId())) {
                        selectedServiceNames.add(los.getDescription());
                    }
                }
                request.setAttribute("selectedServiceNames", selectedServiceNames);

                List<Long> selEnhIds = application.getSelectedEnhancementIdList();
                if (!selEnhIds.isEmpty()) {
                    List<Enhancement> selEnhancements = em.createQuery(
                            "SELECT e FROM Enhancement e WHERE e.id IN :ids ORDER BY e.sortOrder", Enhancement.class)
                            .setParameter("ids", selEnhIds)
                            .getResultList();
                    List<String> selectedEnhNames = selEnhancements.stream()
                            .map(Enhancement::getDescription).collect(Collectors.toList());
                    request.setAttribute("selectedEnhancementNames", selectedEnhNames);
                }
            }

            // Generate pre-signed download URLs for any rate sheet storage keys in JSON plans
            String pspName = getPspName(em);
            Map<String, String> downloadUrls = new HashMap<>();
            String plansJson = valueMap.get("bill_benefit_plans");
            if (plansJson != null && plansJson.contains("storageKey")) {
                // Extract storageKey values from JSON — simple approach without a JSON library
                // Each storageKey looks like: "storageKey":"applications/123/uuid/filename.pdf"
                int idx = 0;
                while ((idx = plansJson.indexOf("\"storageKey\"", idx)) >= 0) {
                    int colonIdx = plansJson.indexOf(":", idx);
                    int startQuote = plansJson.indexOf("\"", colonIdx + 1);
                    int endQuote = plansJson.indexOf("\"", startQuote + 1);
                    if (startQuote >= 0 && endQuote > startQuote) {
                        String key = plansJson.substring(startQuote + 1, endQuote);
                        if (!key.isEmpty()) {
                            try {
                                String url = StorageDAO.getDownloadUrl(em, pspName, key);
                                downloadUrls.put(key, url);
                            } catch (Exception e) {
                                System.out.println("[ReviewApplication] Could not generate URL for: " + key);
                            }
                        }
                    }
                    idx = endQuote + 1;
                }
            }

            // CSV export action
            String action = request.getParameter("action");
            if ("exportCsv".equals(action)) {
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"application_" + proposalId + ".csv\"");
                PrintWriter writer = response.getWriter();
                writer.println("Field,Value");
                for (ApplicationFieldValue fv : fieldValues) {
                    String label = fv.getApplicationField().getLabel();
                    String val = fv.getFieldValue() != null ? fv.getFieldValue() : "";
                    writer.println(csvEscape(label) + "," + csvEscape(val));
                }
                writer.flush();
                return;
            }

            // Role-based view flags
            boolean canReview = Boolean.TRUE.equals(isPspAdmin);
            request.setAttribute("canReview", canReview);
            request.setAttribute("isAgentView", isAgent && !canReview);

            // Gate setup link visibility — agents should not see setup info
            if (isAgent && !isPspAdmin && !isPspUser) {
                request.setAttribute("hideSetupLink", true);
            }

            request.setAttribute("application", application);
            request.setAttribute("sections", sections);
            request.setAttribute("valueMap", valueMap);
            request.setAttribute("downloadUrls", downloadUrls);
            request.setAttribute("plansJson", plansJson != null ? plansJson : "[]");

            request.getRequestDispatcher("/WEB-INF/view/sales/reviewApplication.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            response.sendRedirect("ReviewApplications?err=" + encode("Invalid application ID"));
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Error loading application");
        } finally {
            em.close();
        }
    }

    // ======================== POST — Approve / Deny / More Info ========================

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");
        if (currentPerson == null) {
            response.sendRedirect("Login");
            return;
        }

        // Only PSP Admins may take review actions
        Boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!Boolean.TRUE.equals(isPspAdmin)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "PSP Admin access required for review actions");
            return;
        }

        String idParam = request.getParameter("id");
        String action = request.getParameter("action");
        String reviewNotes = request.getParameter("reviewNotes");

        if (idParam == null || action == null) {
            response.sendRedirect("ReviewApplications?err=" + encode("Missing parameters"));
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            long proposalId = Long.parseLong(idParam);
            Application application = EntityLookup.getApplicationById(em, proposalId);
            if (application == null) {
                response.sendRedirect("ReviewApplications?err=" + encode("Application not found"));
                return;
            }

            Proposal proposal = application.getProposal();

            switch (action) {
                case "approve":
                    // Update Application status
                    em.getTransaction().begin();
                    application.setStatus("APPROVED");
                    application.setDateReviewed(Timestamp.from(Instant.now()));
                    application.setReviewedBy(currentPerson);
                    if (reviewNotes != null && !reviewNotes.isBlank())
                        application.setReviewNotes(reviewNotes.trim());
                    em.merge(application);
                    em.getTransaction().commit();

                    // Update Proposal status
                    em.getTransaction().begin();
                    proposal.setStatus("APPROVED");
                    em.merge(proposal);
                    em.getTransaction().commit();

                    // Create Setup activity
                    Prospect prospect = proposal.getProspect();
                    CheckList checkList = createChecklist(em, prospect.getName(), currentPerson);
                    Setup setup = createSetup(em, prospect, application, checkList, currentPerson);
                    fillToDoList(em, setup, currentPerson);
                    updateActivityCache(request, em, setup);

                    response.sendRedirect("ReviewApplications?msg=" +
                            encode("Application approved. Setup #" + setup.getId() + " created for " + prospect.getName()));
                    break;

                case "deny":
                    em.getTransaction().begin();
                    application.setStatus("DENIED");
                    application.setDateReviewed(Timestamp.from(Instant.now()));
                    application.setReviewedBy(currentPerson);
                    if (reviewNotes != null && !reviewNotes.isBlank())
                        application.setReviewNotes(reviewNotes.trim());
                    em.merge(application);
                    em.getTransaction().commit();

                    em.getTransaction().begin();
                    proposal.setStatus("DENIED");
                    em.merge(proposal);
                    em.getTransaction().commit();

                    response.sendRedirect("ReviewApplications?msg=" +
                            encode("Application denied for " + proposal.getProspect().getName()));
                    break;

                case "more_info":
                    em.getTransaction().begin();
                    application.setStatus("MORE_INFO");
                    application.setDateReviewed(Timestamp.from(Instant.now()));
                    application.setReviewedBy(currentPerson);
                    if (reviewNotes != null && !reviewNotes.isBlank())
                        application.setReviewNotes(reviewNotes.trim());
                    em.merge(application);
                    em.getTransaction().commit();

                    response.sendRedirect("ReviewApplications?msg=" +
                            encode("Requested additional information for " + proposal.getProspect().getName()));
                    break;

                case "under_review":
                    em.getTransaction().begin();
                    application.setStatus("UNDER_REVIEW");
                    em.merge(application);
                    em.getTransaction().commit();

                    response.sendRedirect("ReviewApplication?id=" + proposalId);
                    break;

                default:
                    response.sendRedirect("ReviewApplication?id=" + proposalId);
            }

            // Invalidate navbar badge count after any status change
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            if (local != null) local.invalidateAwaitingReviewCount();

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("ReviewApplications?err=" + encode("Error processing action: " + e.getMessage()));
        } finally {
            em.close();
        }
    }

    // ======================== Setup Creation (mirrors GenerateProp25) ========================

    private CheckList createChecklist(EntityManager em, String erName, Person currentPerson) {
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setFullName(erName + " Checklist");
        c.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2L)));
        c.setComplete(false);
        c.setLoggedBy(currentPerson);
        em.persist(c);
        em.getTransaction().commit();

        // Add task 153 as the first (completed) todo — same as GenerateProp25
        Task t = EntityLookup.getTaskById(em, 153L);
        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setDateCompleted(Date.valueOf(LocalDate.now()));
        toDo.setCompletedBy(currentPerson);
        toDo.setComplete(true);
        toDo.setTask(t);
        toDo.setCheckList(c);
        toDo.setSortOrder(0);
        em.persist(toDo);
        em.getTransaction().commit();

        System.out.println("[ReviewApplication] Checklist Created: " + c.getFullName() + " ID: " + c.getId());
        return c;
    }

    private Setup createSetup(EntityManager em, Prospect prospect, Application application,
                              CheckList checkList, Person currentPerson) {
        em.getTransaction().begin();
        Setup setup = new Setup();
        setup.setApplication(application);
        setup.setPrimaryContactSetup(prospect.getContact());
        setup.setComplete(false);
        setup.setFullName(prospect.getName());
        setup.setLoggedBy(currentPerson);
        setup.setAssignedTo(currentPerson);
        setup.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2)));
        setup.setCheckList(checkList);
        em.persist(setup);
        em.getTransaction().commit();
        System.out.println("[ReviewApplication] Setup Created for Proposal ID: " + application.getProposal().getId());

        // Link checklist back to setup (use em.find to avoid dual JOIN FETCH)
        em.getTransaction().begin();
        CheckList cl = em.find(CheckList.class, checkList.getId());
        cl.setAssignedTo(setup);
        cl.setSetup(setup);
        em.persist(cl);
        em.getTransaction().commit();
        System.out.println("[ReviewApplication] Checklist assigned to Setup");

        return setup;
    }

    private void fillToDoList(EntityManager em, Setup setup, Person currentPerson) {
        Application a = setup.getApplication();
        CheckList c = setup.getCheckList();
        List<SortedTask> sortedTaskList = ApplicationTaskDAO.getTasksRequiredForApplication(em, a);

        if (sortedTaskList.isEmpty()) {
            sortedTaskList.add(new SortedTask(EntityLookup.getTaskById(em, 153L), 1000));
        } else {
            System.out.println("[ReviewApplication] Sorted Task List Size = " + sortedTaskList.size());
        }

        // Persist each ToDo — the FK (toDo.checkList) handles the DB relationship.
        // Avoids reloading CheckList via getCheckListById (dual JOIN FETCH) in each
        // iteration, which triggers EclipseLink SINGLE_TABLE descriptor confusion.
        for (SortedTask st : sortedTaskList) {
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(st.getTask());
            toDo.setSortOrder(st.getSortOrder());
            toDo.setCheckList(c);
            toDo.setComplete(st.getTask().getId() == 153L);
            em.persist(toDo);
            em.getTransaction().commit();
            System.out.println("[ReviewApplication] Todo created for Task: " + st.getTask().getDescription());
        }

        // Refresh checklist to pick up all new ToDos via simple find
        // (avoids dual JOIN FETCH in getCheckListById that causes inheritance issues)
        CheckList freshChecklist = em.find(CheckList.class, c.getId());
        BpoTaskPushService.pushDelegatedTasks(em, freshChecklist);
    }

    // ======================== Activity Cache Update (mirrors GenerateProp25) ========================

    private void updateActivityCache(HttpServletRequest request, EntityManager em, Setup setup) {
        try {
            AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

            List<Activity25u> listToModify = new ArrayList<>(global.getActivitiesAllOpen());
            Query q = em.createQuery("SELECT a FROM Activity25 a WHERE a.activity.id = :id");
            q.setParameter("id", setup.getId());
            Activity25 a25 = (Activity25) q.getSingleResult();
            Activity25u au = new Activity25u(a25);
            listToModify.add(au);
            global.setActivitiesAllOpen(listToModify);

            if (local != null) {
                local.setActivitiesAllOpen(global.getActivitiesAllOpen());
                local.getCurrentActivity().setReFilterOnExit(true);
                request.getSession().setAttribute("local", local);
            }
            request.getServletContext().setAttribute("global", global);
        } catch (Exception e) {
            // Non-fatal — activity list will refresh on next page load
            System.out.println("[ReviewApplication] Cache update skipped: " + e.getMessage());
        }
    }

    // ======================== Helpers ========================

    private String getPspName(EntityManager em) {
        try {
            return AppConstantDAO.getConstantValue(em, "PSP_NAME");
        } catch (Exception e) {
            try {
                Query q = em.createQuery("SELECT p FROM PSP p WHERE p.id = 4");
                net.superiorstate.ams.model.general.PSP psp =
                        (net.superiorstate.ams.model.general.PSP) q.getSingleResult();
                return psp.getFullName();
            } catch (Exception ex) {
                return "default";
            }
        }
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}