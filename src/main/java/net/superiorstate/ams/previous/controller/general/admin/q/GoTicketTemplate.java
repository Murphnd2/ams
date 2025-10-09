package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.GenSeq;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TemplatePurpose;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.ticket.TicketCategory;
import net.superiorstate.ams.previous.model.activity.ticket.TicketSubCategory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "GoTicketTemplate", value = "/GoTicketTemplate")
public class GoTicketTemplate extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doStuff(request,response);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doStuff(request,response);
        goToPage(request,response);
    }

    private void doStuff(HttpServletRequest request,HttpServletResponse response){
        request.getSession().setAttribute("taskListStat",0);
        List<String> taskList = new ArrayList<>();
        taskList.add("");
        request.getSession().setAttribute("taskListBuilder",taskList);
        request.getSession().setAttribute("templateName","");
        request.getSession().setAttribute("templateCategoryId",-1);
        request.getSession().setAttribute("listBuilder",new ArrayList<>());
        request.getSession().setAttribute("showTaskBuilder","N");

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        request.getSession().setAttribute("reqRenewalList",getRequiredTaskLists(em,1));
        request.getSession().setAttribute("reqSetupList",getRequiredTaskLists(em,2));
        request.getSession().setAttribute("reqTicketList",getRequiredTaskLists(em,3));
        request.getSession().setAttribute("notRenewalList",getTemplatesNotSet(em,1));
        request.getSession().setAttribute("notSetupList",getTemplatesNotSet(em,2));
        request.getSession().setAttribute("availableTasks",getTaskList(em,request));
        request.getSession().setAttribute("lockTemplateSelector",0);
        request.getSession().setAttribute("tCategories",getTicketCategories(em));
        request.getSession().setAttribute("tSubCategories",getTicketSubCategories(em));
        request.getSession().setAttribute("tikDescription","");
        request.getSession().setAttribute("tikCatId","");
        em.close();
    }

    private List<Task> getTaskList(EntityManager em,HttpServletRequest request){
        Query q = em.createQuery("SELECT t FROM Task t WHERE t.reUsable = true order by t.id");
        List<Task> reUsableTasks;
        try{
            reUsableTasks = (List<Task>) q.getResultList();
        } catch (NoResultException e){
            return new ArrayList<>();
        }
        List<GenSeq> listBuilder = (List<GenSeq>) request.getSession().getAttribute("listBuilder");

        for(GenSeq g:listBuilder){
            if(g.getTask()!=null )
                reUsableTasks.remove(g.getTask());
        }
        System.out.println("RESET RAN *******************************************************");
        return reUsableTasks;
    }

    private List<TicketCategory> getTicketCategories(EntityManager em){
        Query q = em.createQuery("SELECT t FROM TicketCategory t WHERE t.active=true order by t.description");
        return (List<TicketCategory>) q.getResultList();
    }
    private List<TicketSubCategory> getTicketSubCategories(EntityManager em){
        Query q = em.createQuery("SELECT t FROM TicketSubCategory t WHERE t.isActive=true ORDER BY t.description");
        return (List<TicketSubCategory>) q.getResultList();
    }

    private List<TemplatePurpose> getTemplatesNotSet(EntityManager em, int groupId){
        List<RequiredTaskList> requiredTaskLists = getRequiredTaskLists(em,groupId);
        Query q = em.createQuery("SELECT t FROM TemplatePurpose t WHERE t.templateGroup.id =:id");
        q.setParameter("id",groupId);
        List<TemplatePurpose> purposeList;
        try{
            purposeList = (List<TemplatePurpose>) q.getResultList();
        } catch (NoResultException e){
            return new ArrayList<>();
        }
        if(purposeList==null || purposeList.size()==0)
            return new ArrayList<>();
        List<TemplatePurpose> newList = new ArrayList<>();
        for(RequiredTaskList rtl: requiredTaskLists){
            if(!newList.contains(rtl.getTemplatePurpose()))
                newList.add(rtl.getTemplatePurpose());
        }
        for(TemplatePurpose tp:newList){
            purposeList.remove(tp);
        }

        return purposeList;

    }

    private List<RequiredTaskList> getRequiredTaskLists(EntityManager em, int groupId){
        Query q = em.createQuery("SELECT r From RequiredTaskList r WHERE r.templatePurpose.templateGroup.id=:id AND r.inActive=false");
        q.setParameter("id",groupId);
        List<RequiredTaskList> requiredTaskLists;
        try{
            requiredTaskLists = (List<RequiredTaskList>) q.getResultList();
        } catch (NoResultException e){
            return new ArrayList<>();
        }
        return requiredTaskLists;
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/activity/template/createTicketTemplate.jsp");

        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/checklist/checklistBuilder.jsp");
        dispatcher.forward(request,response);
    }
}
