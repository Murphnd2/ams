package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.sales.agency.PriceItem;
import net.superiorstate.ams.previous.model.sales.agency.RateTable;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

import java.io.IOException;

@WebServlet(name = "ModifyRateTable", value = "/ModifyRateTable")
public class ModifyRateTable extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        modifyRateTable(request,response);
        goToHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        modifyRateTable(request,response);
        goToHomePage(request,response);

    }
    private void goToHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }
    private void modifyRateTable(HttpServletRequest request, HttpServletResponse response){
        String stringId = request.getParameter("btnRateTable1");
        int firstColon = stringId.indexOf("-");
        int secondColon = stringId.indexOf("-",firstColon+1);
        int stringLength = stringId.length();
        Long rateId = Long.parseLong(stringId.substring(0,firstColon));
        Long moduleId = Long.parseLong(stringId.substring(firstColon+1,secondColon));
        Long itemId = Long.parseLong(stringId.substring(secondColon+1,stringLength));
        System.out.println("Rate ID: " + rateId);
        System.out.println("Module ID: " + moduleId);
        System.out.println("Item ID: " + itemId);

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try{
            Query q = em.createQuery("SELECT rt FROM RateTable rt WHERE rt.rate.id = :rate_id AND rt.module.id = :module_id AND rt.priceItem.id = :item_id");

            q.setParameter("rate_id",rateId);
            q.setParameter("module_id",moduleId);
            q.setParameter("item_id",itemId);
            RateTable rateTable = (RateTable) q.getSingleResult();
            ServiceModule serviceModule = dG.getModuleFull(em,moduleId);
            PriceItem priceItem = dM.getPriceItemById(em,itemId);
            request.getSession().setAttribute("hasCurrentModule",true);
            request.getSession().setAttribute("currentModule",serviceModule);
            request.getSession().setAttribute("hasCurrentPriceItem",true);
            request.getSession().setAttribute("currentPriceItem",priceItem);
            em.getTransaction().begin();
            em.remove(rateTable);
            em.getTransaction().commit();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}
