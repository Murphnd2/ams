package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.sales.agency.Rate;
import net.superiorstate.ams.previous.model.sales.offering.LOS;
import net.superiorstate.ams.previous.model.sales.offering.ServiceModule;

import java.io.IOException;

@WebServlet(name = "LosView", value = "/LosView")
public class LosView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillLosTable(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("HERE");
        fillLosTable(request,response);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }

    private void fillLosTable(HttpServletRequest request, HttpServletResponse response){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        int losId = Integer.parseInt(request.getParameter("losSelectButton"));
        request.getSession().setAttribute("pspAdminHomeSender",2);
        request.getSession().setAttribute("hasCurrentRate",false);
        request.getSession().setAttribute("currentRate",new Rate());
        request.getSession().setAttribute("hasCurrentModule",false);
        request.getSession().setAttribute("currentModule", new ServiceModule());

        LOS currentLos = dM.getLosById(losId);
        request.getSession().setAttribute("hasCurrentLos",true);
        request.getSession().setAttribute("currentLos",currentLos);
        em.close();
        System.out.println("This Happened");
    }
}
