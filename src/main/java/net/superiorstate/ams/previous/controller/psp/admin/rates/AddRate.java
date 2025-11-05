package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.sales.agency.Rate;

import java.io.IOException;

@WebServlet(name = "AddRate", value = "/AddRate")
public class AddRate extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addNewRate(request,response);
        goToAdminHome(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addNewRate(request,response);
        goToAdminHome(request,response);
    }

    private void goToAdminHome(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }

    private void addNewRate(HttpServletRequest request, HttpServletResponse response){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = (PSP) request.getSession().getAttribute("psp");
        Rate rate = new Rate();
        rate.setPsp(psp);
        rate.setDescription(request.getParameter("rateName"));
        em.getTransaction().begin();
        em.persist(rate);
        em.getTransaction().commit();
        em.close();
        request.getSession().setAttribute("currentRate",rate);
        request.getSession().setAttribute("hasCurrentRate",true);
    }
}
