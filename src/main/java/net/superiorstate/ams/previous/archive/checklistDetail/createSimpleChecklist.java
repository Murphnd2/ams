package net.superiorstate.ams.previous.archive.checklistDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.creates.dC;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "createSimpleChecklist", value = "/createSimpleChecklist")
public class createSimpleChecklist extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addSimpleCheck(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("goPspHome");
        dispatcher.forward(request,response);
    }

    private void addSimpleCheck(HttpServletRequest request){
        String reminderName = null;
        Date dateDue = null;
        try{
            reminderName = request.getParameter("reminderName");
            dateDue = Date.valueOf(request.getParameter("reminderDate"));
        } catch (Exception e){return;}

        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        if(sVar==null || sVar.getCurrentUser()==null)
            return;

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        List<String> taskList = new ArrayList<>();
        String pName;
        for(int i = 1; i < 9; i++){
            pName="s"+i+"name";
            if(hasValue(request,pName))
                taskList.add(request.getParameter(pName));
        }
        CheckList c = dC.createChecklist(em,reminderName,taskList,dateDue,sVar.getCurrentUser());
        sVar.setCurrentCheckList(null);
        sVar.setCurrentActivity(null);
        sVar.refreshOpenChecklists(em);

        request.getSession().setAttribute("sVar",sVar);

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
