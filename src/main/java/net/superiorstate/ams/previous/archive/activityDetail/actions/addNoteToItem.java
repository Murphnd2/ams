package net.superiorstate.ams.previous.archive.activityDetail.actions;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.ActivityOut;
import net.superiorstate.ams.previous.model.activity.note.ActivityStatus;
import net.superiorstate.ams.previous.model.activity.note.Note;
import net.superiorstate.ams.previous.model.activity.note.ReasonCreated;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@WebServlet(name = "addNoteToItem", value = "/addNoteToItem")
public class addNoteToItem extends HttpServlet {

    private String className1;

    public String getClassName1() {
        return className1;
    }

    public void setClassName1(String className1) {
        this.className1 = className1;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addNoteToActivity(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addNoteToActivity(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher;
        if(getClassName1().equals("CheckList"))
            dispatcher = getServletContext().getNamedDispatcher("goCheckListDetail");
        else dispatcher = getServletContext().getNamedDispatcher("goActivityDetail");

        dispatcher.forward(request,response);
    }

    private void addNoteToActivity(HttpServletRequest request){
        //Retrieve Session Variable
        SessionVar sVar;
        Person currentPerson;
        Activity a;
        try{
            sVar = (SessionVar) request.getSession().getAttribute("sVar");
            currentPerson = (Person) request.getSession().getAttribute("currentPerson");
            a = sVar.getCurrentActivity();
        } catch (Exception e1){return;}

        setClassName1("");
        if(a.getClass().getSimpleName().equals("CheckList"))
            setClassName1("CheckList");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        //Retrieve Form Parameters
        String noteText;
        ActivityStatus as;
        ReasonCreated rc;
        Date dt;
        try{
            noteText = request.getParameter("noteText").toString();
            as = dM.getActivityStatusById(em,Integer.parseInt(request.getParameter("noteStatus").toString()));
            rc = dM.getReasonById(em,Integer.parseInt(request.getParameter("reasonList").toString()));
            dt = Date.valueOf(LocalDate.now());
        } catch (Exception e){
            em.close();
            return;
        }

        em.getTransaction().begin();
        Note n = new Note();
        n.setDetail(noteText);
        n.setActivity(a);
        n.setStatus(as);
        n.setReasonCreated(rc);
        n.setDateGenerated(dt);
        n.setCreatedBy(currentPerson);
        em.persist(n);
        em.getTransaction().commit();
        em.refresh(n);

        Activity activity = dM.getActivityById(em, a.getId());
        em.getTransaction().begin();
        if(activity!=null && activity.getNoteList()!=null)
            activity.getNoteList().add(n);
        em.persist(activity);
        em.getTransaction().commit();
        em.refresh(activity);

        ActivityOut ao = null;
        for(ActivityOut a1: sVar.getOpenActivityList())
            if(activity!=null && Objects.equals(a1.getActivity().getId(), activity.getId())){
                ao = a1;
                break;
            }
        if(ao!=null)
            System.out.println("AO ID: " + ao.getActivity().getId());
        else System.out.println("AO is null");

        boolean shouldRefresh = false;
        if(rc!=null && rc.isOutbound() && ao!=null && ao.isNeedingContact()) {
            shouldRefresh = true;
            System.out.println("REFRESH LINE 1 TRUE");
        }else System.out.println("LINE 1 FALSE");
        if(as!=null && as.getId()==1 && ao!=null && ao.isWaitingOnUs()) {
            shouldRefresh = true;
            System.out.println("REFRESH LINE 2 TRUE");
        }else System.out.println("LINE 2 FALSE");
        if(as!=null && as.getId()==3 && ao!=null && !ao.isWaitingOnUs()){
            shouldRefresh = true;
            System.out.println("REFRESH LINE 3 TRUE");
        }else System.out.println("LINE 3 FALSE");

        if(shouldRefresh) {
            sVar.setNeedsActivityRefresh(true);
            sVar.setActivityOutNeedingRefresh(ao);
            System.out.println("REFRESHED");
        }

        sVar.setCurrentActivity(activity);
        sVar.refreshActivityHistory(em);

        em.close();
        request.getSession().setAttribute("sVar",sVar);
    }

    private List<ActivityOut> refreshActivityOutList(EntityManager em){
        Query q = em.createQuery("SELECT a FROM ActivityOut a");
        List<ActivityOut> activityOutList;
        try{
            activityOutList = (List<ActivityOut>) q.getResultList();
        } catch (Exception e){
            return null;
        }
        for(ActivityOut a:activityOutList)
            em.refresh(a);
        return activityOutList;
    }
}
