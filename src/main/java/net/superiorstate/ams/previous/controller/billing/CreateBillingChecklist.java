package net.superiorstate.ams.previous.controller.billing;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.checklist.dbCheck;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CreateBillingChecklist", value = "/CreateBillingChecklist")
public class CreateBillingChecklist extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createChecklist(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createChecklist(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }
    private void createChecklist(HttpServletRequest request){

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PSP psp = dM.getPspById(em,4L);
        Person admin = dM.getPersonById(em,104L);

        Task t1 = dM.getTaskById(em,10L);
        Task t2 = dM.getTaskById(em,11L);
        Task t3 = dM.getTaskById(em,12L);
        Task t4 = dM.getTaskById(em,13L);
        Task t5 = dM.getTaskById(em,14L);
        Task t6 = dM.getTaskById(em,15L);

        em.getTransaction().begin();
        if(t1==null){
            t1 = createTask(10L,"Clear Import Tables", "ClearImport",admin,psp);
            em.persist(t1);
        }
        if(t2==null){
            t2 = createTask(11L,"Clear Monthly Billing", "ClearMonthlyBilling",admin,psp);
            em.persist(t2);
        }
        if(t3==null){
            t3 = createTask(12L,"Import Summit Export Files",null,admin,psp);
            t3.setAutomationText(null);
            t3.setHasAutomation(false);
            em.persist(t3);
        }
        if(t4==null){
            t4 = createTask(13L,"Update Tables from Imports","UpdateTables",admin,psp);
            em.persist(t4);
        }
        if(t5==null){
            t5 = createTask(14L,"Create Monthly Billing","CreateMonthlyBilling",admin,psp);
            em.persist(t5);
        }
        if(t6==null){
            t6 = createTask(15L,"Refresh Employee List","RefreshTicketEmployees",admin,psp);
            em.persist(t6);
        }
        em.getTransaction().commit();

        List<Task> taskList = new ArrayList<>();
        taskList.add(t1);
        taskList.add(t2);
        taskList.add(t3);
        taskList.add(t4);
        taskList.add(t5);
        taskList.add(t6);
        dbCheck.createAdminOnlyChecklist(em,"Monthly Data Processes",taskList,admin);
        em.close();
    }



    private Task createTask(long id, String desc, String servletName, Person admin, PSP psp){
        Task t2 = new Task();
        t2.setId(id);
        t2.setAllowFuture(false);
        t2.setAllowEarly(false);
        t2.setHasOwner(true);
        t2.setAllowNonOwner(false);
        t2.setHasInfo(false);
        t2.setHasAutomation(true);
        t2.setDescription(desc);
        t2.setSourced(false);
        t2.setOwner(admin);
        t2.setPsp(psp);
        t2.setReUsable(false);
        t2.setHasGoTo(false);
        t2.setServletName(servletName);
        t2.setAutomationText(desc);
        return t2;
    }


}
