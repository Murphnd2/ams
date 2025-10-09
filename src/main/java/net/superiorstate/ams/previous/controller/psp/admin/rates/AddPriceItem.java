package net.superiorstate.ams.previous.controller.psp.admin.rates;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.sales.agency.PriceItem;

import java.io.IOException;

@WebServlet(name = "AddPriceItem", value = "/AddPriceItem")
public class AddPriceItem extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        insertPriceItem(request,response);
        goToHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        insertPriceItem(request,response);
        goToHomePage(request,response);
    }

    private void goToHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAdminHome");
        dispatcher.forward(request,response);
    }

    private void insertPriceItem(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PriceItem pi = new PriceItem();
        pi.setDescription(request.getParameter("priceItemName"));
        pi.setPsp((PSP) request.getSession().getAttribute("psp"));
        pi.setSortOrder(Integer.parseInt(request.getParameter("piSortOrder")));
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(pi);
        em.getTransaction().commit();
        em.close();
        request.getSession().setAttribute("currentPriceItem",pi);
    }
}
