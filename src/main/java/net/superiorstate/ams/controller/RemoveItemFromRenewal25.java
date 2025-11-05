package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.data.renewal.dR;
import net.superiorstate.ams.previous.model.activity.renewal.RenewalItem;

import java.io.IOException;

@WebServlet(name = "RemoveItemFromRenewal25", value = "/RemoveItemFromRenewal25")
public class RemoveItemFromRenewal25 extends HttpServlet {
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
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Long renewalItemId = Long.parseLong(request.getParameter("btnRemoveItem"));
        RenewalItem ri = dM.getRenewalItemById(em,renewalItemId);
        dR.removeBenefitFromRenewal(request,em,ri);

        local.getCurrentActivity().intializeActivity(em,local.getCurrentActivity().getActivity().getId());
        local.refreshRenewals(em);
        request.getSession().setAttribute("local",local);
        em.close();
    }
}
