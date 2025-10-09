package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.sales.offering.ServiceItem;

import java.io.IOException;

@WebServlet(name = "AddServiceItem", value = "/AddServiceItem")
public class AddServiceItem extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addServiceItem(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addServiceItem(request,response);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }

    private void addServiceItem(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String itemName = request.getParameter("serviceItemName");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        EntityManager em = emf.createEntityManager();
        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setDescription(itemName);
        serviceItem.setPsp(psp);
        em.getTransaction().begin();
        em.persist(serviceItem);
        em.getTransaction().commit();
        em.close();
        request.getSession().setAttribute("currentServiceItem",serviceItem);
    }
}
