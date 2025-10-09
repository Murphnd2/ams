package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.psp.admin.agency.helper.hRate;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.sales.agency.Agency;
import net.superiorstate.ams.previous.model.sales.agency.PriceItem;
import net.superiorstate.ams.previous.model.sales.agency.Rate;
import net.superiorstate.ams.previous.model.sales.agency.RateTable;
import net.superiorstate.ams.previous.model.sales.offering.LOS;
import net.superiorstate.ams.previous.model.sales.offering.ServiceItem;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "PspAdminHome", value = "/PspAdminHome")
public class PspAdminHome extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("GET METHOD");
        setTheParameters(request,response);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("POST METHOD");
        setTheParameters(request,response);
        goToPage(request,response);
    }
    private void setTheParameters(HttpServletRequest request, HttpServletResponse response){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        PSP psp = (PSP) request.getSession().getAttribute("psp");

        List<Rate> rateList = dG.getRateList(em, Integer.parseInt(psp.getId().toString()));
        Collections.sort(rateList);
        request.setAttribute("rateList",rateList);

        List<Agency> agencyList = dG.getAgencyList(em, Integer.parseInt(psp.getId().toString()));
        Collections.sort(agencyList);
        request.getSession().setAttribute("agencyList",agencyList);

        List<ServiceModule> serviceModuleList = dG.getServiceModuleList(em,  Integer.parseInt(psp.getId().toString()));
        Collections.sort(serviceModuleList);
        request.getSession().setAttribute("serviceModuleList",serviceModuleList);

        List<PriceItem> priceItemList = dG.getPriceItemList(em, Integer.parseInt(psp.getId().toString()));
        Collections.sort(priceItemList);
        request.getSession().setAttribute("priceItemList",priceItemList);

        List<ServiceItem> serviceItemList = dG.getServiceItemList(em, Integer.parseInt(psp.getId().toString()));
        request.getSession().setAttribute("serviceItemList",serviceItemList);


        boolean hasCurrentRate = false;
        try{
            hasCurrentRate = (boolean) request.getSession().getAttribute("hasCurrentRate");
        } catch (Exception e2){
            e2.printStackTrace();
        }
        if(hasCurrentRate){
            Rate currentRate = (Rate) request.getSession().getAttribute("currentRate");
            List<RateTable> rateTableList = dG.getRateTableList(em,currentRate.getId());
            request.getSession().setAttribute("rateTableList",rateTableList);
            List<Agency> agenciesAssigned = dG.getAgenciesAssignedToRate(em, currentRate.getId());
            request.getSession().setAttribute("agenciesAssigned", agenciesAssigned);
            boolean rateLock = hRate.wasUsed(em, dM.getRateById(em,currentRate.getId()));
            request.getSession().setAttribute("rateLock",rateLock);
        } /*else if(request.getSession().getAttribute("pspAdminHomeSender").toString()=="1"){
            Rate rate = new Rate();
            request.getSession().setAttribute("currentRate",rate);
            request.getSession().setAttribute("pspAdminHomeSender",0);
        }*/

        boolean hasCurrentLos = false;
        try{
            hasCurrentLos = (boolean) request.getSession().getAttribute("hasCurrentLos");
        } catch (Exception e1){
            e1.printStackTrace();
        }
        if(hasCurrentLos){
            LOS currentLos = (LOS) request.getSession().getAttribute("currentLos");
            LOS los = dG.getLosFull(em,currentLos.getId());
            request.getSession().setAttribute("currentLos",los);
        } else {
            LOS los = new LOS();
            request.getSession().setAttribute("currentLos",los);
        }

        boolean hasCurrentModule = (boolean) request.getSession().getAttribute("hasCurrentModule");
        if(hasCurrentModule){
            ServiceModule currentModule = (ServiceModule) request.getSession().getAttribute("currentModule");
            ServiceModule serviceModule = dG.getModuleFull(em,currentModule.getId());
            request.getSession().setAttribute("currentModule",serviceModule);
        }

        boolean hasCurrentPriceItem = (boolean) request.getSession().getAttribute("hasCurrentPriceItem");
        if(hasCurrentPriceItem){
            PriceItem currentItem = (PriceItem) request.getSession().getAttribute("currentPriceItem");
            PriceItem priceItem = dM.getPriceItemById(em,currentItem.getId());
            request.getSession().setAttribute("currentPriceItem",priceItem);
        }
        em.close();
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/psp/admin/adminRateHome.jsp");
        dispatcher.forward(request,response);
    }











}
