package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.previous.data.misc.dbTicket;
import net.superiorstate.ams.previous.model.activity.ticket.tEmployee;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "RefreshTicketEmployees", value = "/RefreshTicketEmployees")
public class RefreshTicketEmployees extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        List<tEmployee> te = dbTicket.getTicketEmployeeList(em);
        request.getServletContext().setAttribute("employeeList",te);
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        global.setEmployees(te);
        request.getServletContext().setAttribute("global",global);
        em.close();
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request,response);
    }
}
