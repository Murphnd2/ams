package net.superiorstate.ams.previous.archive.checklistDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.general.Person;
import java.io.IOException;

@WebServlet(name = "goCheckListDetail", value = "/goCheckListDetail")
public class goCheckListDetail extends HttpServlet {
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
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/a/checklistDetail/checklistDetail.jsp");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request){
        long checklistId;
        try{
             checklistId = Long.parseLong(request.getParameter("btnCheckList").toString());
        } catch (Exception e){return;}

        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        CheckList c = dM.getCheckListById(em,checklistId);
        if(c==null)
            return;
        sVar.setCurrentActivity(c);
        sVar.setCurrentCheckList(c);
        Person p = null;
        if(c.getAssignedTo()!=null)
            p = (Person) c.getAssignedTo();
        request.getSession().setAttribute("activityOwner1",p);
        sVar.refreshActivityHistory(em);
        sVar.refreshContactListFromCurrentActivity(em);
        sVar.refreshToDoOutList(em);
        request.getSession().setAttribute("sVar",sVar);

        em.close();
    }

}
