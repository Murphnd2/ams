package net.superiorstate.ams.controller.monthly.initial;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

public class InitialHelper {
    public static void markTaskAndForward(
            HttpServletRequest request,
            HttpServletResponse response,
            long todoId,
            String dispatcherName
    ) throws IOException, ServletException {

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            if (local == null || local.getCurrentChecklist() == null || local.getCurrentChecklist().getCheckList() == null) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Checklist context is not initialized.");
                return;
            }

            ToDo t = EntityLookup.getToDoById(em, todoId);
            em.getTransaction().begin();
            t.setComplete(true);
            t.setDateCompleted(Date.valueOf(LocalDate.now()));
            em.getTransaction().commit();

            local.respondToActivityUpdate(em,"TODO_CLOSE",t.getId());

        } catch (Exception e) {
            e.printStackTrace();
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getServletContext().getNamedDispatcher(dispatcherName);
        if (dispatcher != null) {
            dispatcher.forward(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Target servlet not found or invalid.");
        }
    }
}

