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

/**
 * Reopen (undo the completion of) a delegated ToDo from the agent portal —
 * mirror of {@link AgentCompleteToDo} with isComplete flipped back to false.
 *
 * Differs from {@link net.superiorstate.ams.controller.checklist.ReOpenToDo25}:
 *   - doesn't require {@code local.getCurrentActivity()} to be populated
 *   - persists directly on the ToDo entity, no PSP-session refresh needed
 *   - enforces the same ownership gate as AgentCompleteToDo
 */
@WebServlet(name = "AgentReopenToDo", value = "/AgentReopenToDo")
public class AgentReopenToDo extends HttpServlet {

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

            // Authorization: must be a delegated ToDo AND either owned by me
            // or open to non-owners. Same rule as AgentCompleteToDo.
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

            if (td.isComplete()) {
                em.getTransaction().begin();
                td.setComplete(false);
                td.setDateCompleted(null);
                td.setCompletedBy(null);
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
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private void redirectBack(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String referer = request.getHeader("Referer");
        response.sendRedirect((referer != null && !referer.isBlank()) ? referer : "AgentHome");
    }
}
