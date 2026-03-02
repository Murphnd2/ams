package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.dao.ActivityLandingDao;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.TimeTrackingDAO;
import net.superiorstate.ams.model.ActivityLandingFilter;
import net.superiorstate.ams.model.ActivityLandingRow;
import net.superiorstate.ams.model.ToDoOut25;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.DaySummary;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(name = "ViewHome25", value = "/ViewHome25")
public class ViewHome25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processData(request);
        loadLandingRows(request);      // <-- Step 1 add
        loadTimeclockData(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processData(request);
        loadLandingRows(request);      // <-- Step 1 add
        goToPage(request, response);
    }

    private void loadTimeclockData(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Weekly summary (Mon–Sun of current week)
            List<DaySummary> weekSummary = TimeTrackingDAO.getWeeklySummary(em, local.getCurrentPerson());
            int weekTotalMinutes = TimeTrackingDAO.getWeeklyTotalMinutes(weekSummary);

            // Find today's entry for the "today" tab
            DaySummary todaySummary = null;
            for (DaySummary ds : weekSummary) {
                if (ds.isToday()) {
                    todaySummary = ds;
                    break;
                }
            }

            // Week label: "Feb 17 — Feb 21" (Monday through today or Friday)
            LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            LocalDate friday = monday.plusDays(4);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
            String weekLabel = monday.format(fmt) + " — " + friday.format(fmt);

            // Avg per day (only count days that have stretches)
            long daysWorked = weekSummary.stream()
                    .filter(ds -> ds.getTotalMinutes() > 0)
                    .count();
            int avgMinutes = daysWorked > 0 ? (int)(weekTotalMinutes / daysWorked) : 0;

            // Remaining toward 40h target
            int targetMinutes = 2400; // 40 hours
            int remainingMinutes = Math.max(0, targetMinutes - weekTotalMinutes);

            // Set request attributes for JSP
            request.setAttribute("weekSummary", weekSummary);
            request.setAttribute("weekTotalMinutes", weekTotalMinutes);
            request.setAttribute("weekTotalFormatted", TimeTrackingDAO.formatMinutes(weekTotalMinutes));
            request.setAttribute("weekLabel", weekLabel);
            request.setAttribute("avgPerDayFormatted", TimeTrackingDAO.formatMinutes(avgMinutes));
            request.setAttribute("remainingFormatted", TimeTrackingDAO.formatMinutes(remainingMinutes));
            request.setAttribute("todaySummary", todaySummary);

            // Load correction request statuses for this week's stretches (keyed by inLogId)
            Map<Long, String> correctionMap = new java.util.HashMap<>();
            try {
                jakarta.persistence.Query cq = em.createQuery(
                        "SELECT t.inLog.id, t.status FROM TimeCorrectionRequest t " +
                                "WHERE t.requestor = :person AND t.inLog IS NOT NULL " +
                                "ORDER BY t.dateRequested DESC");
                cq.setParameter("person", local.getCurrentPerson());
                List<Object[]> cResults = cq.getResultList();
                for (Object[] row : cResults) {
                    Long logId = ((Number) row[0]).longValue();
                    String status = (String) row[1];
                    // Only keep the most recent request per stretch (first one wins due to ORDER BY DESC)
                    if (!correctionMap.containsKey(logId)) {
                        correctionMap.put(logId, status);
                    }
                }
            } catch (Exception e) {
                System.out.println("[ViewHome25] Could not load correction map: " + e.getMessage());
            }
            request.setAttribute("correctionMap", correctionMap);

        } finally {
            em.close();
        }
    }
    private void processData(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        // NOTE: Leaving your existing behavior intact for now.
        // We'll remove/replace the refilter/session-list logic in a later step.

        if (local.getCurrentActivity().isReFilterOnExit()) {
            local.setFilteredActivityList(local.filterActivityListing());
            System.out.println("-----I REFILTERED !!!!------------------------------------------------------------");
        }

        if (local.getCurrentActivity() != null && local.getCurrentActivity().isReFilterOnExit()) {
            local.getCurrentActivity().setActivity(null);
            local.getCurrentActivity().setPrimaryContact(null);
            local.getCurrentActivity().setAdditionalContacts(null);
        } else if (local.getCurrentActivity() != null && local.getCurrentActivity().getActivity() != null) {
            local.getCurrentActivity().setActivity(null);
            local.getCurrentActivity().setPrimaryContact(null);
            local.getCurrentActivity().setAdditionalContacts(new ArrayList<>());
        }

        local.getCurrentEmail().setRecipientList(new ArrayList<>());
        local.getCurrentEmail().setAttachments(new ArrayList<>());
        local.getCurrentEmail().setSubject("");
        local.getCurrentEmail().setBody("");

        // Persist ToDo completion changes (your existing block)
        if (local.getCurrentActivity().isReFilterOnExit()) {
            EntityManagerFactory emf =
                    (EntityManagerFactory) getServletContext().getAttribute("emf");
            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                for (ToDoOut25 t : local.getCurrentActivity().getToDoList()) {
                    if (t.isComplete() != t.wasComplete() && t.getToDo().getId() != null) {
                        ToDo managed = em.find(ToDo.class, t.getToDo().getId());
                        if (managed != null) {
                            managed.setComplete(t.isComplete());
                            managed.setCompletedBy(t.getToDo().getCompletedBy());
                            managed.setDateCompleted(t.getToDo().getDateCompleted());
                        }
                    }
                }
                em.getTransaction().commit();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                e.printStackTrace();
            } finally {
                if (em.isOpen()) {
                    em.close();
                }
            }
        }

        request.getSession().setAttribute("local", local);
    }


    /**
     * SQL-first landing dataset — loads ALL open activities for client-side filtering.
     * JS handles ownership, type, attention, and sort filtering in the browser.
     */
    private void loadLandingRows(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        long me = local.getCurrentPerson().getId();

        AmsDataGlobal global =
                (AmsDataGlobal) getServletContext().getAttribute("global");

        int daysWarn = (global != null) ? global.getDaysSinceWarning() : 7;

        // Load ALL open activities — JS will filter client-side
        ActivityLandingFilter f = new ActivityLandingFilter();
        f.ownershipFilter = 0;          // All open — JS will filter
        f.includeRenewal = true;
        f.includeSetup = true;
        f.includeTicket = true;
        f.includeOpportunity = true;    // Always load, JS will hide if user lacks role
        f.viewNeedsContact = false;
        f.viewWaitingOnUs = false;
        f.sortAlphabetically = false;
        f.pageSize = 500;
        f.offset = 0;

        EntityManagerFactory emf =
                (EntityManagerFactory) getServletContext().getAttribute("emf");

        ActivityLandingDao dao = new ActivityLandingDao(emf);

        List<ActivityLandingRow> rows = dao.fetchLandingRows(me, daysWarn, f);

        request.setAttribute("activityRows", rows);

        // Expose data for client-side filtering JS
        request.setAttribute("mePersonId", me);
        request.setAttribute("daysSinceWarning", daysWarn);

        boolean isPspSales = Boolean.TRUE.equals(request.getSession().getAttribute("isPspSales"));
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
        request.setAttribute("canSeeOpportunities", isPspSales || isPspAdmin || isAgent);
    }


    private void goToPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher dispatcher =
                request.getRequestDispatcher("/WEB-INF/view/a/pspHome/pspHome25.jsp");
        dispatcher.forward(request, response);
    }
}
