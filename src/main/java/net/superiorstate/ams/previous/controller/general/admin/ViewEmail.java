package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.note.Email;

import java.io.IOException;

@WebServlet(name = "ViewEmail", value = "/ViewEmail")
public class ViewEmail extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idString = request.getParameter("id");
        Long emailId = Long.parseLong(idString);
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Email theEmail = dM.getEmailById(em, emailId);
        em.close();
        request.getSession().setAttribute("currentEmail", theEmail);
        request.getSession().setAttribute("recipientList2", theEmail.getRecipientList());
        request.getSession().setAttribute("attachmentList",theEmail.getWebLinkList());
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/general/email/emailView.jsp");
        dispatcher.forward(request,response);
    }


}
