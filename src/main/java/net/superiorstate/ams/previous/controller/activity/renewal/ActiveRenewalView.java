package net.superiorstate.ams.previous.controller.activity.renewal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.data.renewal.dR;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ActiveRenewalView", value = "/ActiveRenewalView")
public class ActiveRenewalView extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewRenewalDetail(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewRenewalDetail(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("RenewalHome");
        dispatcher.forward(request,response);
    }

    private void viewRenewalDetail(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        populateTheToRenewDetailForSelection(request,em);
        em.close();
    }

    private void populateTheToRenewDetailForSelection(HttpServletRequest request, EntityManager em){
        Long renewalId = Long.parseLong(request.getParameter("renewalSelectButton"));
        Renewal renewal = dM.getRenewalById(em,renewalId);
        List<RenewalItem> renewalItemList = dR.getRenewalItems(em,renewal.getId());
        List<Renewal> pastRenewalsList = dR.getPastRenewalsForEmployer(em,renewal.getEmployer());
        request.getSession().setAttribute("currentEmployer",new Employer());
        request.getSession().setAttribute("currentRenewal", renewal);
        request.getSession().setAttribute("renewalItemList",renewalItemList);
        request.getSession().setAttribute("pastRenewalList",pastRenewalsList);
        request.getSession().setAttribute("renewalView",2);
    }
}
