package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

import java.io.IOException;

@WebServlet(name = "AddServiceModule", value = "/AddServiceModule")
public class AddServiceModule extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        insertServiceModule(request,response);
        goToHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        insertServiceModule(request,response);
        goToHomePage(request,response);
    }
    private void goToHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }
    private void insertServiceModule(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServiceModule sm = new ServiceModule();
        sm.setDescription(request.getParameter("serviceModuleName"));
        sm.setPsp((PSP) request.getSession().getAttribute("psp"));
        sm.setSortOrder(Integer.parseInt(request.getParameter("smSortOrder")));
        sm.setShortText(request.getParameter("smShortText"));
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(sm);
        em.getTransaction().commit();
        em.close();
        request.getSession().setAttribute("hasCurrentModule",true);
        request.getSession().setAttribute("currentModule",sm);
    }
}
