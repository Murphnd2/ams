package net.superiorstate.ams.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;

import java.io.IOException;
import java.util.ArrayList;

@WebServlet(name = "ViewHome25", value = "/ViewHome25")
public class ViewHome25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processData(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processData(request);
        goToPage(request,response);
    }

    private void processData(HttpServletRequest request) {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if(local.getCurrentActivity().isReFilterOnExit()){
            local.setFilteredActivityList(local.filterActivityListing());
            System.out.println("-----I REFILTERED !!!!------------------------------------------------------------");
        }
        if (local.getCurrentActivity()!=null && local.getCurrentActivity().isReFilterOnExit()) {
            local.getCurrentActivity().setActivity(null);
            local.getCurrentActivity().setPrimaryContact(null);
            local.getCurrentActivity().setAdditionalContacts(null);
        } else if(local.getCurrentActivity()!=null && local.getCurrentActivity().getActivity()!=null){
            local.getCurrentActivity().setActivity(null);
            local.getCurrentActivity().setPrimaryContact(null);
            local.getCurrentActivity().setAdditionalContacts(new ArrayList<>());
        }
        local.getCurrentEmail().setRecipientList(new ArrayList<>());
        local.getCurrentEmail().setAttachments(new ArrayList<>());
        local.getCurrentEmail().setSubject("");
        local.getCurrentEmail().setBody("");
        request.getSession().setAttribute("local", local);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/pspHome/pspHome25.jsp");
        dispatcher.forward(request,response);
    }
}
