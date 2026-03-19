package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.service.BpoTaskPushService;
import net.superiorstate.ams.data.util.ApiClient;
import net.superiorstate.ams.model.ToDoOut25;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;
import net.superiorstate.ams.model.general.BpoRegistration;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PSP admin action: reject a BPO completion and send the task back to the vendor for rework.
 * Clears bpoCompleted, sets isReverted, sends REVERT to BPO, adds audit note.
 */
@WebServlet(name = "SendBackToDo25", value = "/SendBackToDo25")
public class SendBackToDo25 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processSendBack(request);
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }

    private void processSendBack(HttpServletRequest request) {
        String toDoIdParam = request.getParameter("btnSendBack");
        if (toDoIdParam == null) return;

        long toDoId;
        try {
            toDoId = Long.parseLong(toDoIdParam);
        } catch (NumberFormatException e) {
            return;
        }

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            ToDo toDo = EntityLookup.getToDoById(em, toDoId);
            if (toDo == null || !toDo.isBpoCompleted()) return;

            // === DB UPDATE ===
            em.getTransaction().begin();

            toDo.setBpoCompleted(false);
            toDo.setBpoCompletedDate(null);
            toDo.setReverted(true);
            em.merge(toDo);

            // Audit note
            ToDoNote note = new ToDoNote();
            note.setToDo(toDo);
            note.setTodoGuid(toDo.getTodoGuid());
            note.setNoteText("Task sent back to vendor — PSP rejected completion.");
            note.setSourceType("PSP");
            note.setAuthorName(local.getCurrentPerson().getFirstName() + " " + local.getCurrentPerson().getLastName());
            em.persist(note);

            em.getTransaction().commit();

            // === NOTIFY BPO ===
            BpoTaskPushService.revertTaskCompletion(em, toDo);
            pushNoteToBpo(toDo, note.getNoteText(),
                    local.getCurrentPerson().getFirstName() + " " + local.getCurrentPerson().getLastName());

            // === IN-MEMORY UPDATE ===
            local.getCurrentActivity().getToDoList().stream()
                    .filter(out -> out.getToDo().getId() == toDoId)
                    .findFirst()
                    .ifPresent(out -> out.setBpoCompleted(false));

            // Recompute display states so icon flips back to outsourcing-blocked
            ToDoOut25.computeAllDisplayStates(
                    local.getCurrentActivity().getToDoList(),
                    local.getCurrentPerson().getId(),
                    local.isPspAdmin(),
                    local.getCurrentActivity().getActivity().getAssignedTo().getId());

            request.getSession().setAttribute("local", local);
        } finally {
            em.close();
        }
    }

    /**
     * Push the send-back audit note to the BPO's TaskNotesApi so the vendor can see it.
     */
    private void pushNoteToBpo(ToDo toDo, String noteText, String authorName) {
        try {
            BpoRegistration reg = toDo.getTask().getBpoRegistration();
            if (reg == null || reg.getPartnerUrl() == null || reg.getApiTokenOutbound() == null) return;

            String url = reg.getPartnerUrl() + "/api/v1/tasks/notes";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("todoGuid", toDo.getTodoGuid());
            payload.put("noteText", noteText);
            payload.put("authorName", authorName);

            ApiClient.ApiResponse resp = ApiClient.postJsonObject(url, payload, reg.getApiTokenOutbound());
            System.out.println("[BPO-API] SendBackToDo25 pushNote: todoGuid=" + toDo.getTodoGuid() + " (" + resp.statusCode + ")");
        } catch (Exception e) {
            System.out.println("[BPO-API] SendBackToDo25 pushNote failed (non-fatal): " + e.getMessage());
        }
    }
}
