package net.superiorstate.ams.controller.checklist;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

@WebServlet(name = "PersistChecklist25", value = "/PersistChecklist25")
public class PersistChecklist25 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManager em = getOpenEntityManager(request);
        String autoSave = request.getParameter("autoSave");
        boolean isBeacon = "true".equals(autoSave);

        try {
            em.getTransaction().begin();
            for (Long toDoId : local.getPendingCloseIds()) {
                ToDo toDo = EntityLookup.getToDoById(em, toDoId);
                if (toDo != null && !toDo.isComplete()) {
                    toDo.setComplete(true);
                    toDo.setDateCompleted(Date.valueOf(LocalDate.now()));
                    toDo.setCompletedBy(local.getCurrentPerson());
                    em.persist(toDo);
                }
            }
            em.getTransaction().commit();
            local.clearPendingCloseIds();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new ServletException("Failed to persist checklist", e);
        } finally {
            em.close();
        }

        // === Response handling ===
        if (isBeacon) {
            // sendBeacon: no redirect, minimal response
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            response.getWriter().print("saved");
        } else {
            // Manual Save button: redirect back
            response.sendRedirect("ViewActivity25");
        }
    }

    private EntityManager getOpenEntityManager(HttpServletRequest request) {
        var emf = (jakarta.persistence.EntityManagerFactory) request.getServletContext().getAttribute("emf");
        return emf.createEntityManager();
    }
}
