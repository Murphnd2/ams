package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.previous.model.general.LinkType;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "CreateInsertLink25", value = "/CreateInsertLink25")
public class CreateInsertLink25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addLink(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addLink(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ManageTask25");
        dispatcher.forward(request,response);
    }

    private void addLink(HttpServletRequest request){
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        String linkName = request.getParameter("linkName").trim().toUpperCase();
        String linkPath = request.getParameter("linkPath").trim();
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Query q1 = em.createQuery("SELECT l FROM LinkType l WHERE l.id = :id");
        q1.setParameter("id",3);
        LinkType linkType = (LinkType) q1.getSingleResult();

        em.getTransaction().begin();
        WebLink webLink = new WebLink();
        webLink.setLinkType(linkType);
        webLink.setLinkPath(linkPath);
        webLink.setPlainText(linkName);
        webLink.setActive(true);
        em.persist(webLink);
        em.getTransaction().commit();

        List<WebLink> insertLinkList;
        Query q = em.createQuery("SELECT w FROM WebLink w WHERE w.linkType.id = :id AND w.active=true order by w.plainText");
        q.setParameter("id",3);
        try{
            insertLinkList = (List<WebLink>) q.getResultList();
        } catch (NoResultException e){
            insertLinkList = null;
        }
        global.setInsertLinks(insertLinkList);
        request.getServletContext().setAttribute("global",global);
        request.getSession().setAttribute("insertLinkList",insertLinkList);

        em.close();
    }
}

