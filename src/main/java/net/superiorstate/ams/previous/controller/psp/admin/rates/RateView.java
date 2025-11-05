package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.sales.offering.LOS;

import java.io.IOException;

@WebServlet(name = "RateView", value = "/RateView")
public class RateView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillRateTable(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        fillRateTable(request,response);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }

    private void fillRateTable(HttpServletRequest request,HttpServletResponse response){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        long rateId = Long.parseLong(request.getParameter("rateSelectButton"));
        request.getSession().setAttribute("pspAdminHomeSender",1);
        request.getSession().setAttribute("hasCurrentRate",true);
        request.getSession().setAttribute("currentRate", dM.getRateById(em,rateId));
        request.getSession().setAttribute("hasCurrentLos",false);
        request.getSession().setAttribute("currentLos",new LOS());
        em.close();
    }
}
