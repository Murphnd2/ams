package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * One-time demo seeder: finds open, incomplete ToDos and marks their Tasks as sourced.
 * Hit /SeedBpoDemoData to run. Add to LoginFilter whitelist temporarily, or run while logged in as admin.
 * 
 * Optional params:
 *   count - number of tasks to source (default 8)
 *   bpoPersonId - Person ID of the BPO user to assign as sourceOwner (optional, leaves unassigned if omitted)
 */
@WebServlet(name = "SeedBpoDemoData", value = "/SeedBpoDemoData")
public class SeedBpoDemoData extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><head><title>BPO Demo Seeder</title></head><body>");
        out.println("<h2>BPO Demo Data Seeder</h2>");

        int count = 8;
        try {
            String countStr = request.getParameter("count");
            if (countStr != null) count = Integer.parseInt(countStr);
        } catch (Exception ignored) {}

        Long bpoPersonId = null;
        try {
            String pidStr = request.getParameter("bpoPersonId");
            if (pidStr != null) bpoPersonId = Long.parseLong(pidStr);
        } catch (Exception ignored) {}

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Find the BPO person if specified
            Person bpoPerson = null;
            if (bpoPersonId != null) {
                bpoPerson = em.find(Person.class, bpoPersonId);
            }

            // Find open, incomplete ToDos whose Tasks are NOT already sourced
            String jpql = "SELECT t FROM ToDo t " +
                    "JOIN t.checkList cl " +
                    "JOIN t.task task " +
                    "WHERE t.isComplete = false " +
                    "AND task.isSourced = false " +
                    "AND task.id != 153 " +
                    "ORDER BY cl.dueDate ASC, t.sortOrder ASC";
            Query q = em.createQuery(jpql);
            q.setMaxResults(count);

            List<ToDo> todos = q.getResultList();

            if (todos.isEmpty()) {
                out.println("<p style='color:orange;'>No eligible open ToDos found. Create some activities first.</p>");
                out.println("</body></html>");
                return;
            }

            em.getTransaction().begin();

            int sourced = 0;
            for (ToDo todo : todos) {
                Task task = todo.getTask();
                // Mark the Task as sourced
                task.setSourced(true);
                task.setAllowNonOwner(false); // Vendor-only
                if (bpoPerson != null) {
                    task.setSourceOwner(bpoPerson);
                }

                em.persist(task);
                sourced++;

                out.println("<p>✅ Sourced: <b>" + task.getDescription() + "</b> (Task ID " + task.getId() +
                        ", ToDo ID " + todo.getId() + ")" +
                        (bpoPerson != null ? " → assigned to " + bpoPerson.getFullNameFirstLast() : " → unassigned") +
                        "</p>");
            }

            em.getTransaction().commit();

            out.println("<hr>");
            out.println("<p style='color:green; font-weight:bold;'>Done! Sourced " + sourced + " tasks.</p>");
            out.println("<p>Log in as a BPO user to see them on the BPO Dashboard.</p>");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            out.println("<p style='color:red;'>Error: " + e.getMessage() + "</p>");
            e.printStackTrace();
        } finally {
            em.close();
        }

        out.println("</body></html>");
    }
}
