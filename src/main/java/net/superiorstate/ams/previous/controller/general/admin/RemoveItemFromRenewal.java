package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.data.renewal.dR;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;

@WebServlet(name = "RemoveItemFromRenewal", value = "/RemoveItemFromRenewal")
public class RemoveItemFromRenewal extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Person p = (Person) request.getSession().getAttribute("currentPerson");
        Renewal r = (Renewal) request.getSession().getAttribute("currentActivity");
        Long renewalItemId = Long.parseLong(request.getParameter("btnRemoveItem"));
        RenewalItem ri = dM.getRenewalItemById(em,renewalItemId);
        dR.removeBenefitFromRenewal(request,em,ri);
        ViewSelectedActivity.setActivityView(request,em,r);
        em.close();
    }
}
