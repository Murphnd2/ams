package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.model.activity.note.Email;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ViewEmailHistory", value = "/ViewEmailHistory")
public class ViewEmailHistory extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String emailAddress = request.getParameter("em");
        if (emailAddress == null || emailAddress.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email address parameter");
            return;
        }

        emailAddress = emailAddress.trim().toLowerCase();

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            List<Email> emailList = EmailDAO.getEmailsToRecipient(em, emailAddress);

            // Force-init lazy fields while EM is still open
            for (Email e : emailList) {
                if (e.getCreatedBy() != null) {
                    e.getCreatedBy().getFirstName();
                    e.getCreatedBy().getLastName();
                }
                if (e.getSubject() != null) {
                    e.getSubject().length();
                }
            }

            request.setAttribute("emailHistoryList", emailList);
            request.setAttribute("emailHistoryAddress", emailAddress);
            request.setAttribute("emailHistoryCount", emailList.size());

        } finally {
            em.close();
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/a/general/emailHistoryList25.jsp");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
