package net.superiorstate.ams.previous.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.IOException;

@WebServlet(name = "ShowFileUpload", value = "/ShowFileUpload")
public class ShowFileUpload extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String docGuid = request.getParameter("doc");
        WebLink w = getWebLinkByGuid(docGuid);
        request.getSession().setAttribute("currentWebLink",w);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/emailAttachments.jsp");
        dispatcher.forward(request,response);
    }

    private WebLink getWebLinkByGuid(String guid){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.linkPath = :id");
        q.setParameter("id",guid);
        WebLink webLink;
        try{
            webLink = (WebLink) q.getSingleResult();
        } catch (NoResultException e){
            return null;
        }
        em.close();
        return webLink;
    }
}
