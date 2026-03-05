package net.superiorstate.ams.controller.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.RecurringChecklistDAO;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;

import java.io.IOException;
import java.util.List;

@WebServlet("/ViewRecurringHistory25")
public class ViewRecurringHistory25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local == null || !local.isAuthenticated()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String checklistIdParam = request.getParameter("checklistId");
        if (checklistIdParam == null || checklistIdParam.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        long checklistId;
        try {
            checklistId = Long.parseLong(checklistIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Get the current checklist to find its RTL id
            CheckList current = em.find(CheckList.class, checklistId);
            if (current == null || current.getRecurringTaskList() == null) {
                response.setContentType("application/json");
                response.getWriter().write("[]");
                return;
            }

            Long rtlId = current.getRecurringTaskList().getId();
            List<CheckList> past = RecurringChecklistDAO.getPastCompletedInstances(em, rtlId, checklistId);

            // Serialize to JSON
            response.setContentType("application/json");
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < past.size(); i++) {
                CheckList cl = past.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"checklistId\":").append(cl.getId()).append(",");
                json.append("\"dueDate\":\"").append(cl.getDueDate() != null ? cl.getDueDate().toString() : "").append("\",");
                json.append("\"dateCompleted\":\"").append(cl.getDateCompleted() != null ? cl.getDateCompleted().toString() : "").append("\",");
                String completedByName = "";
                if (cl.getCompletedBy() != null) {
                    completedByName = escapeJson(cl.getCompletedBy().getFullName());
                }
                json.append("\"completedBy\":\"").append(completedByName).append("\",");

                // Tasks
                json.append("\"tasks\":[");
                List<ToDo> todos = cl.getToDoList();
                if (todos != null) {
                    boolean firstTask = true;
                    for (ToDo td : todos) {
                        if (td.getTask() == null || td.getTask().getId() == 153) continue;
                        if (!firstTask) json.append(",");
                        firstTask = false;
                        json.append("{");
                        json.append("\"taskName\":\"").append(escapeJson(td.getTask().getPlainDescription())).append("\",");
                        json.append("\"isComplete\":").append(td.isComplete()).append(",");
                        String tdCompletedBy = td.getCompletedBy() != null ? escapeJson(td.getCompletedBy().getFullName()) : "";
                        json.append("\"completedBy\":\"").append(tdCompletedBy).append("\",");

                        // Notes — query per ToDo since ToDo has no notes collection
                        json.append("\"notes\":[");
                        try {
                            List<ToDoNote> notes = em.createQuery(
                                "SELECT n FROM ToDoNote n WHERE n.toDo.id = :todoId ORDER BY n.createdDate ASC",
                                ToDoNote.class)
                                .setParameter("todoId", td.getId())
                                .getResultList();
                            boolean firstNote = true;
                            for (ToDoNote n : notes) {
                                if (!firstNote) json.append(",");
                                firstNote = false;
                                json.append("{");
                                json.append("\"text\":\"").append(escapeJson(n.getNoteText())).append("\",");
                                json.append("\"sourceType\":\"").append(escapeJson(n.getSourceType())).append("\",");
                                String noteAuthor = n.getCreatedBy() != null ? escapeJson(n.getCreatedBy().getFullName())
                                        : (n.getAuthorName() != null ? escapeJson(n.getAuthorName()) : "");
                                json.append("\"author\":\"").append(noteAuthor).append("\",");
                                json.append("\"date\":\"").append(n.getCreatedDate() != null ? n.getCreatedDate().toString() : "").append("\"");
                                json.append("}");
                            }
                        } catch (Exception ignored) {}
                        json.append("]");
                        json.append("}");
                    }
                }
                json.append("]");
                json.append("}");
            }
            json.append("]");
            response.getWriter().write(json.toString());

        } finally {
            em.close();
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
