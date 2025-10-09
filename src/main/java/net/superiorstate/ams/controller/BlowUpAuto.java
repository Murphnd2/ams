package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.ToDoOut25;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@WebServlet(name = "BlowUpAuto", value = "/BlowUpAuto")
public class BlowUpAuto extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        blowItUp(request,response);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        blowItUp(request,response);
        goToPage(request,response);
    }

    private void blowItUp(HttpServletRequest request,HttpServletResponse response){
        String taskIdString = request.getParameter("taskId").toString();
        long taskId;
        Task t;
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try{
            taskId = Long.parseLong(taskIdString);
           t = dM.getTaskById(em,taskId);
           System.out.println("Task Found: " + taskId);
        } catch (Exception e){
            return;
        }
        if(t==null)
            return;
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        List<ToDoOut25> myList = new ArrayList<>(local.getCurrentActivity().getToDoList());
        int index = IntStream.range(0, myList.size()).filter(i->myList.get(i).getTask().getId().equals(taskId)).findFirst().orElse(-1);
        if(index==-1)
            return;
        System.out.println("ToDoOut25 Found: ");

        ToDoOut25 td = myList.get(index);
        String servletName = td.getServletName();
        Task task = td.getTask();

       // Create CheckList for Servlet Conversion
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setDueDate(Date.valueOf(LocalDate.now()));
        c.setAssignedTo(local.getCurrentPerson());
        c.setPrimaryContact(local.getCurrentPerson());
        c.setFullName("Convert Servlet for Task");
        c.setLoggedBy(local.getCurrentPerson());
        em.persist(c);
        em.getTransaction().commit();
        System.out.println("CREATED CHECKLIST");

        String description = "SERVLET: " + servletName;
        Task t1 = createTask(em,local,description);
        ToDo td1 = createToDo(em,t1,c,10);
        description = "TASK NAME: " + task.getDescription();
        Task t2 = createTask(em,local,description);
        ToDo td2 = createToDo(em,t2,c,20);
        description = "TASK ID: " + task.getId();
        Task t3 = createTask(em,local,description);
        ToDo td3 = createToDo(em,t3,c,30);

        List<ToDo> toDoList = new ArrayList<>();
        toDoList.add(td1);
        toDoList.add(td2);
        toDoList.add(td3);
        System.out.println("CREATED TO DO LIST");

        em.getTransaction().begin();
        c.setToDoList(toDoList);
        em.persist(c);
        em.getTransaction().commit();
        em.refresh(c);
        System.out.println("ADDED TO DO LIST TO CHECKLIST");

        local.respondToActivityUpdate(em,"CHECK_REMINDER",c);
        System.out.println("RESPONDED TO UPDATE");

        Task t100 = dM.getTaskById(em,t.getId());
        if(t100==null)
            return;
        em.getTransaction().begin();
        t100.setServletName(null);
        t100.setAutomation(null);
        t100.setHasAutomation(false);
        em.persist(t100);
        em.getTransaction().commit();
        em.refresh(t100);
        System.out.println("CLEARED TASK DATA");

        td.setTask(t100);
        td.setServletName("");
        td.setAutomation(null);
        td.setHasAutomation(false);

        myList.remove(td);
        myList.add(index,td);
        local.getCurrentActivity().setToDoList(myList);

        request.getSession().setAttribute("local",local);

    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher =  getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }
    private ToDo createToDo(EntityManager em, Task task, CheckList c, int sortOrder){
        em.getTransaction().begin();
        ToDo td1 = new ToDo();
        td1.setTask(task);
        td1.setComplete(false);
        td1.setSortOrder(sortOrder);
        td1.setCheckList(c);
        em.persist(td1);
        em.getTransaction().commit();
        return td1;
    }
    private Task createTask(EntityManager em, AmsDataLocal local, String description ){
        em.getTransaction().begin();
        Task t1 = new Task();
        t1.setPsp(local.getCurrentPerson().getPsp());
        t1.setDescription(description);
        t1.setReUsable(false);
        t1.setHasAutomation(false);
        t1.setHasOwner(false);
        t1.setAllowEarly(true);
        t1.setAllowFuture(true);
        t1.setHasInfo(false);
        t1.setHasGoTo(false);
        t1.setSourced(false);
        t1.setAllowNonOwner(true);
        em.persist(t1);
        em.getTransaction().commit();
        return t1;
    }
}
