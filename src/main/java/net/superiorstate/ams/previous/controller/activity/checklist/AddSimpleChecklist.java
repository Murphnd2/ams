package net.superiorstate.ams.previous.controller.activity.checklist;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.creates.dC;
import net.superiorstate.ams.previous.model.general.User;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AddSimpleChecklist", value = "/AddSimpleChecklist")
public class AddSimpleChecklist extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addSimpleCheck(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ResetAdminView");
        dispatcher.forward(request,response);
    }

    private void addSimpleCheck(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        User user = (User) request.getSession().getAttribute("currentUser");
        String reminderName = request.getParameter("reminderName");
        Date dateDue = Date.valueOf(request.getParameter("reminderDate"));

        List<String> taskList = new ArrayList<>();
        String pName;
        for(int i = 1; i < 9; i++){
            pName="s"+i+"name";
            if(hasValue(request,pName))
                taskList.add(request.getParameter(pName));
        }
        dC.createChecklist(em,reminderName,taskList,dateDue,user);
        em.close();
    }

    private boolean hasValue(HttpServletRequest request,String pName){
        String name = request.getParameter(pName);
        return (name != null && !name.equals(""));
    }
}
