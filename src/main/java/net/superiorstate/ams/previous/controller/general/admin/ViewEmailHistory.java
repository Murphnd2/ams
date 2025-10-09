package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.model.activity.note.Email;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ViewEmailHistory", value = "/ViewEmailHistory")
public class ViewEmailHistory extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String theEmail = request.getParameter("em");
        List<Email> emails = getEmailList(request,theEmail);
        request.getSession().setAttribute("emailList",emails);
        request.getSession().setAttribute("theEmail",theEmail);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/activity/note/emailList.jsp");
        dispatcher.forward(request,response);
    }

    private List<Email> getEmailList(HttpServletRequest request, String email){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        List<Email> emails = dbEmail.getEmailsToRecipient(em,email);
        em.close();
        return emails;
    }
}
