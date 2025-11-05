package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.sales.agency.PriceItem;
import net.superiorstate.ams.previous.model.sales.agency.Rate;
import net.superiorstate.ams.previous.model.sales.agency.RateTable;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

import java.io.IOException;

@WebServlet(name = "BuildRate", value = "/BuildRate")
public class BuildRate extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logInRateTable(request,response);
        goToPspAdminHome(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logInRateTable(request,response);
        goToPspAdminHome(request,response);
    }

    private void goToPspAdminHome(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request, response);
    }
    private void logInRateTable(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Rate currentRate = (Rate) request.getSession().getAttribute("currentRate");
        Rate rate = em.find(Rate.class,currentRate.getId());
        long priceItemId = Long.parseLong(request.getParameter("priceItemList"));
        PriceItem priceItem = em.find(PriceItem.class,priceItemId);
        long serviceModuleId = Long.parseLong(request.getParameter("serviceModuleList"));
        ServiceModule serviceModule = em.find(ServiceModule.class,serviceModuleId);
        try{
            RateTable rateTable = new RateTable();
            rateTable.setRate(rate);
            rateTable.setModule(serviceModule);
            rateTable.setPriceItem(priceItem);
            rateTable.setPrice(Double.parseDouble(request.getParameter("price")));
            em.getTransaction().begin();
            em.persist(rateTable);
            em.getTransaction().commit();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            em.close();
            request.getSession().setAttribute("currentModule",serviceModule);
            request.getSession().setAttribute("currentPriceItem",priceItem);
        }
    }
}
