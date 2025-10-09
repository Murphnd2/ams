package net.superiorstate.ams.previous.controller.psp.admin.agency;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dG;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;

@WebServlet(name = "ToggleState", value = "/ToggleState")
public class ToggleState extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        toggleState(request,response);
        goToAdminHomePage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        toggleState(request,response);
        goToAdminHomePage(request,response);
    }

    private void toggleState(HttpServletRequest request,HttpServletResponse response){
        Person agent = new Person();
        request.getSession().setAttribute("hasCurrentAgent",false);
        request.getSession().setAttribute("currentAgent",agent);
        request.getSession().setAttribute("hasCurrentProspect",false);
        dG.setAgencyAccordion(request,1);
        try{
            boolean currentState = (boolean) request.getSession().getAttribute("formDisable");
            if(currentState)
                request.getSession().setAttribute("formDisable",false);
            else
                request.getSession().setAttribute("formDisable",true);
        }
        catch (Exception e){
            e.printStackTrace();
            request.getSession().setAttribute("formDisable",false);
        }
    }

    private void goToAdminHomePage(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException{
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("PspAgencyHome");
        dispatcher.forward(request,response);
    }
}
