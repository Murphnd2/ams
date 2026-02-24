package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

@WebServlet(name = "BpoGetNotes", value = "/BpoGetNotes")
public class BpoGetNotes extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String todoIdParam = request.getParameter("todoId");
        if (todoIdParam == null) {
            out.print("[]");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            long todoId = Long.parseLong(todoIdParam);
            Query q = em.createQuery(
                    "SELECT n FROM ToDoNote n WHERE n.toDo.id = :todoId ORDER BY n.createdDate DESC");
            q.setParameter("todoId", todoId);
            List<ToDoNote> notes = q.getResultList();

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < notes.size(); i++) {
                ToDoNote n = notes.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"source\":\"").append(escapeJson(n.getSourceType())).append("\",");
                json.append("\"author\":\"").append(escapeJson(n.getCreatedBy().getFullName())).append("\",");
                json.append("\"date\":\"").append(sdf.format(n.getCreatedDate())).append("\",");
                json.append("\"text\":\"").append(escapeJson(n.getNoteText())).append("\"");
                json.append("}");
            }
            json.append("]");
            out.print(json);
        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
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
