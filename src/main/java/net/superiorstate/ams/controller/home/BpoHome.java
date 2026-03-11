package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.DelegatedToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.util.*;

@WebServlet(name = "BpoHome", value = "/BpoHome")
public class BpoHome extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        loadData(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        loadData(request);
        goToPage(request, response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("pageTitle", "BPO Dashboard");
        request.setAttribute("pageIcon", "bi-headset");
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/bpo/bpoHome25.jsp");
        dispatcher.forward(request, response);
    }

    private void loadData(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentUser = local.getCurrentPerson();
            Boolean isBpoAdmin = (Boolean) request.getSession().getAttribute("isBpoAdmin");

            String viewMode = request.getParameter("viewMode");
            if (viewMode == null) viewMode = "mine";

            boolean crossSystemMode = AppConfig.isBpo();
            request.setAttribute("crossSystemMode", crossSystemMode);

            if (crossSystemMode) {
                // Always compute pending count for the badge (BPO Admin sees it)
                if (Boolean.TRUE.equals(isBpoAdmin)) {
                    long pendingCount = getPendingCount(em);
                    request.setAttribute("pendingCount", pendingCount);
                }

                // Cross-system BPO: query DelegatedToDo from remote PSPs
                if ("pending".equals(viewMode) && Boolean.TRUE.equals(isBpoAdmin)) {
                    List<DelegatedToDo> pendingToDos = getPendingDelegatedToDos(em);
                    request.setAttribute("pendingToDos", pendingToDos);
                    request.setAttribute("pspGroups", groupDelegatedToDos(pendingToDos));
                } else if ("all".equals(viewMode)) {
                    List<DelegatedToDo> delegatedToDos = getAllDelegatedToDos(em);
                    request.setAttribute("delegatedToDos", delegatedToDos);
                    request.setAttribute("pspGroups", groupDelegatedToDos(delegatedToDos));
                } else {
                    viewMode = "mine"; // Normalize if non-admin tried "pending"
                    List<DelegatedToDo> delegatedToDos = getMyDelegatedToDos(em, currentUser.getId());
                    request.setAttribute("delegatedToDos", delegatedToDos);
                    request.setAttribute("pspGroups", groupDelegatedToDos(delegatedToDos));
                }
            } else {
                // Co-located BPO: query local ToDo with sourced tasks
                List<Object[]> bpoToDos;
                if ("all".equals(viewMode)) {
                    bpoToDos = getAllOpenBpoToDos(em);
                } else {
                    bpoToDos = getMyBpoToDos(em, currentUser.getId());
                }
                request.setAttribute("bpoToDos", bpoToDos);
                request.setAttribute("pspGroups", groupBpoToDos(bpoToDos));
            }

            request.setAttribute("viewMode", viewMode);

        } finally {
            em.close();
        }
    }

    // --- Cross-system DelegatedToDo queries ---

    private long getPendingCount(EntityManager em) {
        String jpql = "SELECT COUNT(d) FROM DelegatedToDo d " +
                "WHERE d.status = 'PENDING' AND d.isCompleted = false";
        try {
            return (Long) em.createQuery(jpql).getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private List<DelegatedToDo> getPendingDelegatedToDos(EntityManager em) {
        String jpql = "SELECT d FROM DelegatedToDo d " +
                "WHERE d.status = 'PENDING' " +
                "AND d.isCompleted = false " +
                "ORDER BY d.dateReceived DESC, d.activityName, d.sortOrder";
        Query q = em.createQuery(jpql, DelegatedToDo.class);
        try {
            return q.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<DelegatedToDo> getMyDelegatedToDos(EntityManager em, Long bpoUserId) {
        String jpql = "SELECT d FROM DelegatedToDo d " +
                "WHERE d.status = 'ACTIVE' " +
                "AND d.isCompleted = false " +
                "AND (d.assignedTo.id = :userId OR d.assignedTo IS NULL) " +
                "ORDER BY d.dueDate, d.activityName, d.sortOrder";
        Query q = em.createQuery(jpql, DelegatedToDo.class);
        q.setParameter("userId", bpoUserId);
        try {
            return q.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<DelegatedToDo> getAllDelegatedToDos(EntityManager em) {
        String jpql = "SELECT d FROM DelegatedToDo d " +
                "WHERE d.status = 'ACTIVE' " +
                "AND d.isCompleted = false " +
                "ORDER BY d.dueDate, d.activityName, d.sortOrder";
        Query q = em.createQuery(jpql, DelegatedToDo.class);
        try {
            return q.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // --- Co-located BPO local ToDo queries ---

    private List<Object[]> getMyBpoToDos(EntityManager em, Long bpoUserId) {
        String jpql = "SELECT t, cl.fullName, cl.dueDate, task.psp.fullName " +
                "FROM ToDo t " +
                "JOIN t.checkList cl " +
                "JOIN t.task task " +
                "WHERE task.isSourced = true " +
                "AND t.isComplete = false " +
                "AND t.bpoCompleted = false " +
                "AND (t.bpoAssignedTo.id = :userId OR t.bpoAssignedTo IS NULL) " +
                "ORDER BY cl.dueDate, cl.fullName, t.sortOrder";
        Query q = em.createQuery(jpql);
        q.setParameter("userId", bpoUserId);
        try {
            return q.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<Object[]> getAllOpenBpoToDos(EntityManager em) {
        String jpql = "SELECT t, cl.fullName, cl.dueDate, task.psp.fullName " +
                "FROM ToDo t " +
                "JOIN t.checkList cl " +
                "JOIN t.task task " +
                "WHERE task.isSourced = true " +
                "AND t.isComplete = false " +
                "AND t.bpoCompleted = false " +
                "ORDER BY cl.dueDate, cl.fullName, t.sortOrder";
        Query q = em.createQuery(jpql);
        try {
            return q.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ═══ GROUPING: PSP → Activity → Tasks ═══

    private List<BpoPspGroup> groupDelegatedToDos(List<DelegatedToDo> todos) {
        if (todos == null || todos.isEmpty()) return new ArrayList<>();

        // Group by PSP name → Activity name, preserving insertion order
        Map<String, Map<String, List<DelegatedToDo>>> grouped = new LinkedHashMap<>();

        // Sort: PSP alphabetical, then due date (nulls last), then activity name, then sort order
        List<DelegatedToDo> sorted = new ArrayList<>(todos);
        sorted.sort(Comparator
                .comparing((DelegatedToDo d) -> d.getPspClient() != null ? d.getPspClient().getPspName() : "", String.CASE_INSENSITIVE_ORDER)
                .thenComparing((DelegatedToDo d) -> d.getDueDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(d -> d.getActivityName() != null ? d.getActivityName() : "", String.CASE_INSENSITIVE_ORDER)
                .thenComparing(DelegatedToDo::getSortOrder));

        for (DelegatedToDo dt : sorted) {
            String psp = dt.getPspClient() != null ? dt.getPspClient().getPspName() : "(Unknown PSP)";
            String activity = dt.getActivityName() != null ? dt.getActivityName() : "(No Activity)";
            grouped.computeIfAbsent(psp, k -> new LinkedHashMap<>())
                    .computeIfAbsent(activity, k -> new ArrayList<>())
                    .add(dt);
        }

        List<BpoPspGroup> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<DelegatedToDo>>> pspEntry : grouped.entrySet()) {
            BpoPspGroup pspGroup = new BpoPspGroup();
            pspGroup.pspName = pspEntry.getKey();
            pspGroup.activities = new ArrayList<>();
            int pspTaskCount = 0;

            for (Map.Entry<String, List<DelegatedToDo>> actEntry : pspEntry.getValue().entrySet()) {
                BpoActivityGroup ag = new BpoActivityGroup();
                ag.activityName = actEntry.getKey();
                ag.delegatedTasks = actEntry.getValue();
                ag.taskCount = actEntry.getValue().size();
                DelegatedToDo first = actEntry.getValue().get(0);
                ag.activityType = first.getActivityType() != null ? first.getActivityType() : "Checklist";
                ag.dueDate = actEntry.getValue().stream()
                        .map(DelegatedToDo::getDueDate)
                        .filter(Objects::nonNull)
                        .min(Date::compareTo)
                        .orElse(null);
                pspGroup.activities.add(ag);
                pspTaskCount += ag.taskCount;
            }
            pspGroup.taskCount = pspTaskCount;
            result.add(pspGroup);
        }
        return result;
    }

    private List<BpoPspGroup> groupBpoToDos(List<Object[]> todos) {
        if (todos == null || todos.isEmpty()) return new ArrayList<>();

        // Group by PSP name [3] → Activity name [1]
        Map<String, Map<String, List<Object[]>>> grouped = new LinkedHashMap<>();

        // Sort: PSP alphabetical, then due date (nulls last), then activity name, then sort order
        List<Object[]> sorted = new ArrayList<>(todos);
        sorted.sort(Comparator
                .comparing((Object[] r) -> (String) r[3], String.CASE_INSENSITIVE_ORDER)
                .thenComparing((Object[] r) -> (Date) r[2], Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(r -> (String) r[1], String.CASE_INSENSITIVE_ORDER)
                .thenComparing(r -> ((ToDo) r[0]).getSortOrder()));

        for (Object[] row : sorted) {
            String psp = (String) row[3];
            String activity = (String) row[1];
            grouped.computeIfAbsent(psp, k -> new LinkedHashMap<>())
                    .computeIfAbsent(activity, k -> new ArrayList<>())
                    .add(row);
        }

        List<BpoPspGroup> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Object[]>>> pspEntry : grouped.entrySet()) {
            BpoPspGroup pspGroup = new BpoPspGroup();
            pspGroup.pspName = pspEntry.getKey();
            pspGroup.activities = new ArrayList<>();
            int pspTaskCount = 0;

            for (Map.Entry<String, List<Object[]>> actEntry : pspEntry.getValue().entrySet()) {
                BpoActivityGroup ag = new BpoActivityGroup();
                ag.activityName = actEntry.getKey();
                ag.localTasks = actEntry.getValue();
                ag.taskCount = actEntry.getValue().size();
                // Derive activity type from first task's CheckList
                ToDo firstTodo = (ToDo) actEntry.getValue().get(0)[0];
                CheckList cl = firstTodo.getCheckList();
                if (cl.getRenewal() != null) ag.activityType = "Renewal";
                else if (cl.getSetup() != null) ag.activityType = "Setup";
                else if (cl.getTicket() != null) ag.activityType = "Ticket";
                else ag.activityType = "Checklist";
                ag.dueDate = (Date) actEntry.getValue().get(0)[2]; // All tasks in same activity share due date
                pspGroup.activities.add(ag);
                pspTaskCount += ag.taskCount;
            }
            pspGroup.taskCount = pspTaskCount;
            result.add(pspGroup);
        }
        return result;
    }

    // ═══ GROUPING DTOs ═══

    public static class BpoPspGroup {
        private String pspName;
        private int taskCount;
        private List<BpoActivityGroup> activities;

        public String getPspName() { return pspName; }
        public int getTaskCount() { return taskCount; }
        public List<BpoActivityGroup> getActivities() { return activities; }
    }

    public static class BpoActivityGroup {
        private String activityName;
        private String activityType;
        private Date dueDate;
        private int taskCount;
        private List<DelegatedToDo> delegatedTasks;  // cross-system mode
        private List<Object[]> localTasks;            // co-located mode

        public String getActivityName() { return activityName; }
        public String getActivityType() { return activityType; }
        public Date getDueDate() { return dueDate; }
        public int getTaskCount() { return taskCount; }
        public List<DelegatedToDo> getDelegatedTasks() { return delegatedTasks; }
        public List<Object[]> getLocalTasks() { return localTasks; }
    }
}
