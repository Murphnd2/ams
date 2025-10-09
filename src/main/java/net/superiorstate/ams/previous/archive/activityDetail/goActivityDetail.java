package net.superiorstate.ams.previous.archive.activityDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;

import java.io.IOException;

@WebServlet(name = "goActivityDetail", value = "/goActivityDetail")
public class goActivityDetail extends HttpServlet {
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setSessionVariables(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setSessionVariables(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher(getPath());
        dispatcher.forward(request,response);
    }

    private void setSessionVariables(HttpServletRequest request){
        String activityIdString = request.getParameter("btnViewActivity");
        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        setPath("/WEB-INF/view/a/activityDetail/activityDetail.jsp");
        if(activityIdString==null && sVar.getCurrentActivity()==null) {
            setPath("/WEB-INF/view/a/pspHome/pspHome.jsp");
            return;
        } else if(activityIdString==null) return;

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        long id = Long.parseLong(activityIdString);

        String sender;
        try{
            sender = request.getParameter("formSender");
        }catch (Exception e){
            sender = "";
        }
        if(sender!= null){
            if(sender.equals("viewActivity")){
                Activity a = dM.getActivityById(em,id);
                request.getSession().setAttribute("activityOwner1",a.getAssignedTo());
                sVar.setCurrentActivity(a);
                sVar.refreshActivityHistory(em);
                sVar.refreshCurrentChecklistFromCurrentActivity();
                sVar.refreshToDoOutList(em);
                sVar.refreshContactListFromCurrentActivity(em);
                sVar.refreshEmployeeList(em,a,sVar);
                request.getSession().setAttribute("sVar",sVar);
                request.getSession().setAttribute("currentActivity",a);
            }
            else if(sender.equals("closeTask")){

            } else if(sender.equals("changeTaskOwner")){

            } else if(sender.equals("modifyContacts")){

            } else if(sender.equals("addBenefit")){

            } else if(sender.equals("removeBenefit")){

            } else if(sender.equals("addHistory")){

            } else if(sender.equals("reopenTask")){

            }
        } else{

        }

        em.close();
    }
}
