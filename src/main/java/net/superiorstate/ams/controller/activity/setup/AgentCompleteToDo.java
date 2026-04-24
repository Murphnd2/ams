package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

/**
 * Mark a delegated ToDo complete from the agent portal sidebar (Mockup B) or
 * any context where the session's currentActivity is not populated.
 *
 * Differs from {@link net.superiorstate.ams.controller.checklist.CloseToDo25}:
 *   - doesn't require {@code local.getCurrentActivity()} to be set
 *   - persists the completion directly on the ToDo entity
 *   - enforces ownership: the ToDo must be delegated (override_ownership=true)
 *     and its owner must be the current user, or {@code allow_non_owner=true}
 *
 * Redirects back to the referer (typically AgentHome) or /AgentHome as a
 * fallback. Marks the global delegation cache dirty so PSP dashboards pick
 * up the change on their next refresh.
 */
@WebServlet(name = "AgentCompleteToDo", value = "/AgentCompleteToDo")
public class AgentCompleteToDo extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idParam = request.getParameter("toDoId");
        if (idParam == null) { redirectBack(request, response); return; }
        long toDoId;
        try { toDoId = Long.parseLong(idParam); }
        catch (NumberFormatException e) { redirectBack(request, response); return; }

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (local == null || local.getCurrentPerson() == null) { redirectBack(request, response); return; }
        long meId = local.getCurrentPerson().getId();

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            ToDo td = EntityLookup.getToDoById(em, toDoId);
            if (td == null) { redirectBack(request, response); return; }

            // Authorization: ToDo must be delegated (override_ownership=true) and
            // the current user must be the owner, or the ToDo must allow any user
            // to complete it.
            boolean ownedByMe = td.isOverrideOwnership()
                    && td.hasOwner()
                    && td.getOwner() != null
                    && td.getOwner().getId() != null
                    && td.getOwner().getId() == meId;
            boolean openToAll = td.isOverrideOwnership() && td.allowNonOwner();
            if (!ownedByMe && !openToAll) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not your task");
                return;
            }

            if (!td.isComplete()) {
                em.getTransaction().begin();
                td.setComplete(true);
                td.setDateCompleted(Date.valueOf(LocalDate.now()));
                td.setCompletedBy(local.getCurrentPerson());
                em.merge(td);
                em.getTransaction().commit();

                if (global != null) global.markDelegationDirty();
            }
        } finally {
            if (em.isOpen()) em.close();
        }

        redirectBack(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // GET is intentionally rejected — completion is a state-changing action.
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private void redirectBack(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String referer = request.getHeader("Referer");
        response.sendRedirect((referer != null && !referer.isBlank()) ? referer : "AgentHome");
    }
}
