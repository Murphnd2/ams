package net.superiorstate.ams.controller.emailItems;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.IOException;

@WebServlet(name = "RemoveAttachment25", value = "/RemoveAttachment25")
public class RemoveAttachment25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThis(request,response);
    }

    private void doThis(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        String btnString = request.getSession().getAttribute("emailAction").toString();

        Long webLinkId = Long.parseLong(btnString.substring(3));
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");

        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.id = :id");
        q.setParameter("id",webLinkId);
        for(WebLink w: local.getCurrentEmail().getAttachments())
            if(w.getId().equals(webLinkId)){
                local.getCurrentEmail().getAttachments().remove(w);
                break;
            }

        request.getSession().setAttribute("local",local);
        em.close();
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("CreateEmail25");
        dispatcher.forward(request,response);
    }
}
