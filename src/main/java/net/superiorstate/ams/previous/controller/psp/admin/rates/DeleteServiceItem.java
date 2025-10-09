package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.model.sales.offering.ServiceItem;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

import java.io.IOException;

@WebServlet(name = "DeleteServiceItem", value = "/DeleteServiceItem")
public class DeleteServiceItem extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeServiceItem(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeServiceItem(request,response);
        goToAdminHomePage(request,response);
    }

    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }

    private void removeServiceItem(HttpServletRequest request,HttpServletResponse response){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        int serviceItemId = Integer.parseInt(request.getParameter("itemSelectButton"));
        ServiceModule currentModule = (ServiceModule) request.getSession().getAttribute("currentModule");
        ServiceModule serviceModule = dG.getModuleFull(em,currentModule.getId());
        Query q = em.createQuery("SELECT si FROM ServiceItem si WHERE si.id = :item_id");
        q.setParameter("item_id",serviceItemId);
        ServiceItem serviceItem = (ServiceItem) q.getSingleResult();
        em.getTransaction().begin();
        serviceModule.removeServiceItem(serviceItem);
        em.persist(serviceModule);
        em.persist(serviceItem);
        em.getTransaction().commit();
        em.close();
    }
}
