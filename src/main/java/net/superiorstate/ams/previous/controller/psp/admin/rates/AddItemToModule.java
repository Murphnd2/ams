package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.sales.offering.ServiceItem;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

import java.io.IOException;

@WebServlet(name = "AddItemToModule", value = "/AddItemToModule")
public class AddItemToModule extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        linkItemToModule(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        linkItemToModule(request,response);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }

    private void linkItemToModule(HttpServletRequest request, HttpServletResponse response) {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        long serviceItemId = Long.parseLong(request.getParameter("serviceItemList"));
        long moduleId = Long.parseLong(request.getParameter("serviceModuleList"));
        Query qServiceItem = em.createQuery("SELECT si FROM ServiceItem si WHERE si.id = :item_id");
        qServiceItem.setParameter("item_id",serviceItemId);
        Query qServiceModule = em.createQuery("SELECT sm FROM ServiceModule sm WHERE sm.id = :module_id");
        qServiceModule.setParameter("module_id",moduleId);
        ServiceItem serviceItem = (ServiceItem) qServiceItem.getSingleResult();
        ServiceModule serviceModule = (ServiceModule) qServiceModule.getSingleResult();
        try{
            em.getTransaction().begin();
            serviceModule.addServiceItem(serviceItem);
            em.persist(serviceModule);
            em.persist(serviceItem);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
    }



}
