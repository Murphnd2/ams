package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.sales.offering.LOS;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

import java.io.IOException;

@WebServlet(name = "AddModuleToLos", value = "/AddModuleToLos")
public class AddModuleToLos extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addModuleToLos(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addModuleToLos(request,response);
        goToAdminHomePage(request,response);
    }

    private void goToAdminHomePage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }

    private void addModuleToLos(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int serviceModuleID = Integer.parseInt(request.getParameter("serviceModuleList"));
        LOS currentLos = (LOS) request.getSession().getAttribute("currentLos");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("SELECT DISTINCT l FROM LOS l INNER JOIN FETCH l.serviceModuleList sm INNER JOIN FETCH sm.serviceItemList si WHERE l.id = :los_id");
        q.setParameter("los_id",currentLos.getId());
        LOS los = (LOS) q.getSingleResult();
        Query query = em.createQuery("SELECT sm FROM ServiceModule sm WHERE sm.id = :module_id");
        query.setParameter("module_id",serviceModuleID);
        ServiceModule serviceModule = (ServiceModule) query.getSingleResult();
        try{
            em.getTransaction().begin();
            los.addServiceModule(serviceModule);
            em.persist(los);
            em.persist(serviceModule);
            em.getTransaction().commit();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            em.close();
            request.getSession().setAttribute("currentLos",los);
        }
    }
}
