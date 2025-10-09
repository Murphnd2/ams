package net.superiorstate.ams.previous.controller.psp.admin.agency.helper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.sales.agency.Rate;

import java.io.IOException;

@WebServlet(name = "UnlockRates", value = "/UnlockRates")
public class UnlockRates extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        unlockRates(request);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        unlockRates(request);
        goToAdminHomePage(request,response);
    }

    private void unlockRates(HttpServletRequest request){
        Rate rate = (Rate) request.getSession().getAttribute("currentRate");
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Rate newRate = hRate.unlockRate(em,rate);
        request.getSession().setAttribute("currentRate", newRate);

    }
    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }
}
