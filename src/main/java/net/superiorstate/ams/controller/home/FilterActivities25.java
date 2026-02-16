package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.ActivityFilter;
import net.superiorstate.ams.data.AmsDataLocal;

import java.io.IOException;

@WebServlet(name = "FilterActivities25", value = "/FilterActivities25")
public class FilterActivities25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setFilterItems(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setFilterItems(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request,response);
    }

    private void setFilterItems(HttpServletRequest request){
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        String viewAll = request.getParameter("viewAllActivities");
        ActivityFilter af = local.getActivityFilter();

        if(viewAll!=null && viewAll.equals("ALL")){
            af.setViewWaitingOnUs(false);
            af.setViewNeedsContact(false);
            af.setSortAlphabetically(true);
            af.setOwnershipFilter(0);
            af.setViewRenewal(true);
            af.setViewSetup(true);
            af.setViewTicket(true);
        } else if(viewAll!=null && viewAll.equals("MY")) {
            af.setViewWaitingOnUs(true);
            af.setViewNeedsContact(true);
            af.setSortAlphabetically(false);
            af.setOwnershipFilter(1);
            af.setViewRenewal(true);
            af.setViewSetup(true);
            af.setViewTicket(true);
        } else if(viewAll!=null && viewAll.equals("REN")){
            af.setViewWaitingOnUs(false);
            af.setViewNeedsContact(false);
            af.setSortAlphabetically(true);
            af.setOwnershipFilter(1);
            af.setViewRenewal(true);
            af.setViewSetup(false);
            af.setViewTicket(false);
        } else {
            String fOnUs = request.getParameter("fOnUs");
            af.setViewWaitingOnUs(fOnUs != null);

            String fCall = request.getParameter("fCall");
            af.setViewNeedsContact(fCall != null);

            String fAlpha = request.getParameter("fAlpha");
            af.setSortAlphabetically(fAlpha != null && fAlpha.equals("1"));

            String whose = request.getParameter("whoFilter");
            int whoFilter;
            try {
                whoFilter = Integer.parseInt(whose);
            } catch (Exception e){
                whoFilter = 1;
            }
            af.setOwnershipFilter(whoFilter);

            String rn = request.getParameter("vRenew");
            af.setViewRenewal(rn !=null);

            String st = request.getParameter("vSetup");
            af.setViewSetup(st != null);

            String tk = request.getParameter("vTicket");
            af.setViewTicket(tk!=null);
        }

        local.setActivityFilter(af);
        local.setFilteredActivityList(local.filterActivityListing());   // ← this is the correct line

        request.getSession().setAttribute("local",local);
    }
}
