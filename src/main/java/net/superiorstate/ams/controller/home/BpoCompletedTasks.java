package net.superiorstate.ams.controller.home;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "BpoCompletedTasks", value = "/BpoCompletedTasks")
public class BpoCompletedTasks extends HttpServlet {

    private final Gson gson = new GsonBuilder().create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        int days = 1;
        try {
            String daysStr = request.getParameter("days");
            if (daysStr != null) days = Integer.parseInt(daysStr);
        } catch (Exception ignored) {}

        Date startDate = Date.valueOf(LocalDate.now().minusDays(days - 1));

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            String jpql = "SELECT t, cl.fullName, task.psp.fullName, t.bpoCompletedBy.firstName, t.bpoCompletedBy.lastName " +
                    "FROM ToDo t " +
                    "JOIN t.checkList cl " +
                    "JOIN t.task task " +
                    "WHERE task.isSourced = true " +
                    "AND t.bpoCompleted = true " +
                    "AND t.bpoCompletedDate >= :startDate " +
                    "ORDER BY t.bpoCompletedDate DESC, cl.fullName";
            Query q = em.createQuery(jpql);
            q.setParameter("startDate", startDate);

            List<Object[]> results = q.getResultList();
            List<Map<String, String>> items = new ArrayList<>();

            for (Object[] row : results) {
                ToDo todo = (ToDo) row[0];
                String activityName = (String) row[1];
                String pspName = (String) row[2];
                String firstName = row[3] != null ? (String) row[3] : "";
                String lastName = row[4] != null ? (String) row[4] : "";

                Map<String, String> item = new HashMap<>();
                item.put("taskName", todo.getTask().getDescription());
                item.put("activityName", activityName);
                item.put("pspName", pspName);
                item.put("completedBy", (firstName + " " + lastName).trim());
                item.put("completedDate", todo.getBpoCompletedDate() != null
                        ? todo.getBpoCompletedDate().toString() : "");
                items.add(item);
            }

            response.getWriter().write(gson.toJson(items));

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("[]");
        } finally {
            em.close();
        }
    }
}
