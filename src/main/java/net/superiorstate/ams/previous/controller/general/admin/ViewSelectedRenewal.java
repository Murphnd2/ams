package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.checklist.dbCheck;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ViewSelectedRenewal", value = "/ViewSelectedRenewal")
public class ViewSelectedRenewal extends HttpServlet {
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
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        //FIXME: Get selected renewal here
        Renewal renewal = new Renewal(); // Should do a get by ID here
        if(renewal!=null && renewal.getId()>0){
            request.getSession().setAttribute("adminView",2);
            request.getSession().setAttribute("currentChecklist", getCheckListForRenewal(em,renewal));
            request.getSession().setAttribute("currentSetup", new Setup());
            request.getSession().setAttribute("currentTicket", new Ticket());
            request.getSession().setAttribute("currentRenewal",renewal);
        }
        em.close();
    }

    private CheckList getCheckListForRenewal(EntityManager em, Renewal r){
        CheckList checkList = dM.getCheckListByAssignee(em,r);
        if(checkList==null)
            checkList = createAndAssignChecklist(em,r);
        return checkList;
    }

    private CheckList createAndAssignChecklist(EntityManager em, Renewal r){
        List<RenewalItem> renewalItemList = r.getRenewalItemList();
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setAssignedTo(r);
        c.setComplete(false);
        c.setDueDate(Date.valueOf(LocalDate.ofInstant(r.getDateCreated().toInstant(), ZoneId.systemDefault()).plusDays(14L)));
        c.setLoggedBy((Person) r.getAssignedTo());
        c.setFullName(r.getFullName()+" Checklist");
        em.persist(c);
        em.getTransaction().commit();
        List<Task> fullTaskList = new ArrayList<>();
        List<SortedTask> fullSortedTaskList = new ArrayList<>();
        for(RenewalItem ri: renewalItemList){
            List<SortedTask> taskList = getTasksForRenewalItem(em,ri);
            for(SortedTask st: taskList){
                if(!fullTaskList.contains(st.getTask())){
                    fullTaskList.add(st.getTask());
                    fullSortedTaskList.add(st);
                }
            }
        }
        for(SortedTask sortedTask:fullSortedTaskList){
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(sortedTask.getTask());
            toDo.setSortOrder(sortedTask.getSortOrder());
            toDo.setCheckList(c);
            toDo.setComplete(false);
            em.persist(toDo);
            em.getTransaction().commit();
        }
        return c;
    }

    private List<SortedTask> getTasksForRenewalItem(EntityManager em, RenewalItem ri){
        Query q = em.createQuery("SELECT rtl FROM RequiredTaskList rtl where rtl.templatePurpose.id = :id");
        q.setParameter("id",ri.getBenefit().getPlanType().getTemplatePurpose().getId());
        List<RequiredTaskList> requiredTaskLists;
        List<SortedTask> sortedTaskList;
        List<Task> fullTaskList = new ArrayList<>();
        List<SortedTask> fullSortedTaskList = new ArrayList<>();
        Task task;
        try{
            requiredTaskLists = (List<RequiredTaskList>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            return null;
        }
        for(RequiredTaskList rtl: requiredTaskLists){
            sortedTaskList = dbCheck.getTasksForRequiredItem(em,rtl);
            for(SortedTask st:sortedTaskList) {
                task = st.getTask();
                if (!fullTaskList.contains(task)) {
                    fullTaskList.add(task);
                    fullSortedTaskList.add(st);
                }
            }
        }
        return fullSortedTaskList;
    }
}
