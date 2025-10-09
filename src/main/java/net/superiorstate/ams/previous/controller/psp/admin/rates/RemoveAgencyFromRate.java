package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.sales.agency.Agency;
import net.superiorstate.ams.previous.model.sales.agency.Rate;

import java.io.IOException;

@WebServlet(name = "RemoveAgencyFromRate", value = "/RemoveAgencyFromRate")
public class RemoveAgencyFromRate extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeAgencyFromRate(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        removeAgencyFromRate(request,response);
        goToAdminHomePage(request,response);
    }

    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }

    private void removeAgencyFromRate(HttpServletRequest request,HttpServletResponse response){
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("default");
        EntityManager em = emf.createEntityManager();
        Rate currentRate = (Rate) request.getSession().getAttribute("currentRate");
        Long agencyId = Long.parseLong(request.getParameter("btnRemoveAgency"));
        Rate rate = em.find(Rate.class,currentRate.getId());
        Agency agency = em.find(Agency.class,agencyId);
        em.getTransaction().begin();
        agency.removeRate(rate);
        em.persist(agency);
        em.getTransaction().commit();
        em.close();
        emf.close();

    }
}
