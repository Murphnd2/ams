package net.superiorstate.ams.previous.controller.psp.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.checklist.dbCheck;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RequiredTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.TaskSequence;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ConvertCheckToSeq", value = "/ConvertCheckToSeq")
public class ConvertCheckToSeq extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        long seqId = Long.parseLong(request.getParameter("requiredList"));
        RequiredTaskList requiredTaskList = dbCheck.getRequiredTaskListById(em,seqId);
        CheckList currentChecklist = (CheckList)request.getSession().getAttribute("currentChecklist");
        convertChecklist(request,em, currentChecklist,requiredTaskList);
        em.close();
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void convertChecklist(HttpServletRequest request, EntityManager em, CheckList checkList, TaskSequence taskSequence) {
        clearExistingTable(em,taskSequence);
        List<SortedTask> sortedTaskList = getNewTaskList(em,checkList);
        setNewTaskSequence(em,sortedTaskList,taskSequence);
    }

    private void setNewTaskSequence(EntityManager em, List<SortedTask> stl, TaskSequence ts){
        for(SortedTask st:stl){
            em.getTransaction().begin();
            TaskSequenceTable tst = new TaskSequenceTable();
            tst.setTaskSequence(ts);
            tst.setTask(st.getTask());
            tst.setSortOrder(st.getSortOrder());
            em.persist(tst);
            em.getTransaction().commit();
        }
    }

    private List<SortedTask> getNewTaskList(EntityManager em, CheckList checkList){
        List<SortedTask> sortedTaskList = new ArrayList<>();
        Query q = em.createQuery("SELECT t FROM ToDo t WHERE t.checkList.id = :cId");
        q.setParameter("cId",checkList.getId());
        List<ToDo> toDoList;
        try{
            toDoList = (List<ToDo>) q.getResultList();
        } catch (NoResultException e){
            return sortedTaskList;
        }
        for(ToDo toDo:toDoList){
            SortedTask st = new SortedTask(toDo.getTask(), toDo.getSortOrder());
            sortedTaskList.add(st);
            System.out.println("Task: " + st.getTask().getDescription() + "- Sort: " + st.getSortOrder());
        }
        return sortedTaskList;
    }

    private void clearExistingTable(EntityManager em, TaskSequence taskSequence){
        Query q = em.createQuery("SELECT tst FROM TaskSequenceTable tst WHERE tst.taskSequence.id = :tsId order by tst.task.id");
        q.setParameter("tsId",taskSequence.getId());
        List<TaskSequenceTable> taskSequenceTableList;
        try{
            taskSequenceTableList = (List<TaskSequenceTable>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            taskSequenceTableList = null;
        }
        if(taskSequenceTableList == null || taskSequenceTableList.size()==0)
            return;
        for(TaskSequenceTable tst:taskSequenceTableList){
            em.getTransaction().begin();
            em.remove(tst);
            em.getTransaction().commit();
        }
    }
}
