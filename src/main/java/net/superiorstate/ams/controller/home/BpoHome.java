package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

            // Determine filter mode: "mine" (default) or "all"
            String viewMode = request.getParameter("viewMode");
            if (viewMode == null) viewMode = "mine";

            // Load delegated ToDos for BPO
            List<Object[]> bpoToDos;
            if ("all".equals(viewMode)) {
                bpoToDos = getAllOpenBpoToDos(em);
            } else {
                bpoToDos = getMyBpoToDos(em, currentUser.getId());
            }

            request.setAttribute("bpoToDos", bpoToDos);
            request.setAttribute("viewMode", viewMode);

        } finally {
            em.close();
        }
    }

    /**
     * Get all open sourced ToDos assigned to a specific BPO user.
     * Also includes unassigned ToDos (backlog pool).
     * Returns Object[] rows: [ToDo, checklistName, dueDate, pspName]
     */
    private List<Object[]> getMyBpoToDos(EntityManager em, Long bpoUserId) {
        String jpql = "SELECT t, cl.fullName, cl.dueDate, task.psp.fullName " +
                "FROM ToDo t " +
                "JOIN t.checkList cl " +
                "JOIN t.task task " +
                "WHERE task.isSourced = true " +
                "AND t.isComplete = false " +
                "AND t.bpoCompleted = false " +
                "AND (t.bpoAssignedTo.id = :userId OR t.bpoAssignedTo IS NULL) " +
                "ORDER BY cl.dueDate, t.sortOrder";
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
                "ORDER BY cl.dueDate, t.sortOrder";
        Query q = em.createQuery(jpql);
        try {
            return q.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
