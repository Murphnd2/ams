package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;

@WebServlet(name = "filterActivities", value = "/filterActivities")
public class filterActivities extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        changeView(request,response);
    }

    private void changeView(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException {
        String fOnUs = request.getParameter("fOnUs");
        boolean onUs = fOnUs != null;

        String fCall = request.getParameter("fCall");
        boolean needContact = fCall != null;

        String stat = "0";
        if(onUs&needContact)
            stat = "1";
        else if(needContact)
            stat = "2";
        else if(onUs)
            stat = "3";

        request.getSession().setAttribute("qNwf",stat);

        String fAlpha = request.getParameter("fAlpha");
        boolean alphaSort = false;
        if(fAlpha!=null && fAlpha.equals("1"))
            alphaSort = true;
        request.getSession().setAttribute("qAlpha",alphaSort);

        int whoFilter = Integer.parseInt(request.getParameter("whoFilter"));
        request.getSession().setAttribute("qWhoFilter",whoFilter);

        String rn = request.getParameter("vRenew");
        if(rn==null)
            rn = "0";
        request.getSession().setAttribute("vRn",rn);
        String st = request.getParameter("vSetup");
        if(st==null)
            st="0";
        request.getSession().setAttribute("vSt",st);
        String tk = request.getParameter("vTicket");
        if(tk==null)
            tk="0";
        request.getSession().setAttribute("vTk",tk);

        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);

    }
}
