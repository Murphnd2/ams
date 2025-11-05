package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.model.creates.dC;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CreateChecklist25", value = "/CreateChecklist25")
public class CreateChecklist25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addSimpleCheck(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request,response);
    }

    private void addSimpleCheck(HttpServletRequest request){
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        String reminderName = request.getParameter("reminderName");
        Date dateDue = Date.valueOf(request.getParameter("reminderDate"));
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        List<String> taskList = new ArrayList<>();
        String pName;
        for(int i = 1; i < 9; i++){
            pName="s"+i+"name";
            if(hasValue(request,pName))
                taskList.add(request.getParameter(pName));
        }
        CheckList c = dC.createChecklist(em,reminderName,taskList,dateDue,local.getCurrentUser());

        local.respondToActivityUpdate(em,"CHECK_REMINDER",c);

        request.getSession().setAttribute("local",local);

        em.close();
    }

    private boolean hasValue(HttpServletRequest request,String pName){
        String name;
        try{
            name = request.getParameter(pName);
        } catch (Exception e){
            return false;
        }
        return (name != null && !name.equals(""));
    }
}
