package net.superiorstate.ams.previous.controller.checklist;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.ddC;

import java.io.IOException;

@WebServlet(name = "ApplySequenceFilter", value = "/ApplySequenceFilter")
public class ApplySequenceFilter extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setFilter(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ChecklistManagerGo");
        dispatcher.forward(request,response);
    }

    private void setFilter(HttpServletRequest request){
        String fSetup = "checked";
        if(request.getParameter("switchSetups")==null)
            fSetup = "";
        String fRenewal = "checked";
        if(request.getParameter("switchRenewals")==null)
            fRenewal = "";
        String fTicket = "checked";
        if(request.getParameter("switchTickets")==null)
            fTicket = "";
        String fUser = "checked";
        if(request.getParameter("switchUsers")==null)
            fUser = "";

        request.getSession().setAttribute("fS",fSetup);
        request.getSession().setAttribute("fR",fRenewal);
        request.getSession().setAttribute("fT",fTicket);
        request.getSession().setAttribute("fU",fUser);
        request.getSession().setAttribute("checkView",0);
        ddC.clearCurrentTask(request);

    }
}
