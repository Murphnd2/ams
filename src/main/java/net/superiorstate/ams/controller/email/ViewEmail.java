package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.model.activity.note.Email;

import java.io.IOException;

@WebServlet(name = "ViewEmail", value = "/ViewEmail")
public class ViewEmail extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idString = request.getParameter("id");
        if (idString == null || idString.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email id");
            return;
        }

        long emailId;
        try {
            emailId = Long.parseLong(idString.trim());
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email id");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Fetch-join recipients and attachments to avoid lazy-load after em.close()
            Query q = em.createQuery(
                "SELECT DISTINCT e FROM Email e " +
                "LEFT JOIN FETCH e.recipientList " +
                "LEFT JOIN FETCH e.webLinkList " +
                "WHERE e.id = :id"
            );
            q.setParameter("id", emailId);

            Email theEmail;
            try {
                theEmail = (Email) q.getSingleResult();
            } catch (NoResultException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                return;
            }

            // Force-init nested fields while EM is still open
            if (theEmail.getCreatedBy() != null) {
                theEmail.getCreatedBy().getFullName();
            }
            if (theEmail.getActivity() != null) {
                theEmail.getActivity().getFullName();
            }
            if (theEmail.getRecipientList() != null) {
                theEmail.getRecipientList().size();
                theEmail.getRecipientList().forEach(p -> {
                    if (p.getEmail() != null) p.getEmail();
                });
            }
            if (theEmail.getWebLinkList() != null) {
                theEmail.getWebLinkList().size();
                theEmail.getWebLinkList().forEach(w -> {
                    if (w.getLinkType() != null) w.getLinkType().getId();
                });
            }

            // Use request attributes (not session) — this is a read-only view
            request.setAttribute("viewEmail", theEmail);

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/a/general/emailView25.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
