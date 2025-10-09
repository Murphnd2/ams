package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

@WebServlet(name = "CloseSingleItemChecklist", value = "/CloseSingleItemChecklist")
public class CloseSingleItemChecklist extends HttpServlet {

    private final String whereTo = "GoAdminHome";
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
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher(whereTo);
        dispatcher.forward(request,response);
    }

    private void changeView(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        String buttonValue = request.getParameter("btnCheckList");
        if(buttonValue.contains("R-")){
            Long id = Long.parseLong(buttonValue.substring(2));
            Long personId = Long.parseLong(request.getParameter("userList"));
            em.getTransaction().begin();
            CheckList c = dM.getCheckListById(em,id);
            Person p = dM.getPersonById(em,personId);
            c.setAssignedTo(p);
            em.persist(c);
            em.getTransaction().commit();
            request.getSession().setAttribute("adminView",99);
            request.getSession().setAttribute("currentChecklist", new CheckList());
        } else if(buttonValue.contains("D-")){
            Long id = Long.parseLong(buttonValue.substring(2));
            Date newDueDate = Date.valueOf(request.getParameter("newDueDate"));
            em.getTransaction().begin();
            CheckList c = dM.getCheckListById(em,id);
            c.setDueDate(newDueDate);
            em.persist(c);
            em.getTransaction().commit();
            request.getSession().setAttribute("adminView",99);
            request.getSession().setAttribute("currentChecklist", new CheckList());
        } else if(buttonValue.contains("V-")){
            Long id = Long.parseLong(buttonValue.substring(2));
            CheckList c = dM.getCheckListById(em,id);
            request.getSession().setAttribute("currentPrimaryContact",dM.getPersonById(em,c.getAssignedTo().getId()));
            request.getSession().setAttribute("currentChecklist", dM.getCheckListById(em,id));
            request.getSession().setAttribute("currentToDoList", ViewSelectedChecklist.getToDoListByChecklistId(em,id));
            request.getSession().setAttribute("adminView",4);
            request.getSession().setAttribute("currentActivity", dM.getCheckListById(em,id) );
        } else{
            Long id = Long.parseLong(request.getParameter("btnCheckList"));
            request.getSession().setAttribute("adminView",99);
            request.getSession().setAttribute("currentChecklist", new CheckList());
            closeChecklist(request,em,id);
        }
        em.close();
    }

    private void closeChecklist(HttpServletRequest request,EntityManager em, Long id){
        Query q = em.createQuery("SELECT c FROM CheckList c WHERE c.id = :id");
        q.setParameter("id",id);
        User currentUser = (User) request.getSession().getAttribute("currentUser");
        CheckList checkList;
        try{
            checkList = (CheckList) q.getSingleResult();
            System.out.println("go this far");
        } catch (NoResultException e){
            System.out.println("failed");
            e.printStackTrace();
            return;
        }
        if(!checkList.getAssignedTo().getClass().getSimpleName().equals("Person")){
            Activity activity = (Activity) request.getSession().getAttribute("currentActivity");
            em.getTransaction().begin();
            try{
                Activity a = dM.getActivityById(em,activity.getId());
                if(a!=null){
                    a.setComplete(true);
                    a.setDateCompleted(Date.valueOf(LocalDate.now()));
                    a.setCompletedBy(currentUser.getPerson());
                    em.persist(a);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            em.getTransaction().commit();
        }


        em.getTransaction().begin();
        checkList.setComplete(true);
        checkList.setCompletedBy(currentUser.getPerson());
        checkList.setDateCompleted(Date.valueOf(LocalDate.now()));
        em.persist(checkList);
        em.getTransaction().commit();
        System.out.println("finished");
    }

}
