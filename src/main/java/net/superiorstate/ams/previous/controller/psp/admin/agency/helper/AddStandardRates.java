package net.superiorstate.ams.previous.controller.psp.admin.agency.helper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.sales.agency.Rate;

import java.io.IOException;

@WebServlet(name = "AddStandardRates", value = "/AddStandardRates")
public class AddStandardRates extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        clearRates(request);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        clearRates(request);
        goToAdminHomePage(request,response);
    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }
    private void clearRates(HttpServletRequest request){
        Rate rate = (Rate) request.getSession().getAttribute("currentRate");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        hRate.fillStandardRates(em,rate);
        em.close();
    }
}
