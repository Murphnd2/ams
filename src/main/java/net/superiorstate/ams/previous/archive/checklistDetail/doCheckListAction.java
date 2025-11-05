package net.superiorstate.ams.previous.archive.checklistDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.SessionVar;
import net.superiorstate.ams.previous.data.checklist.dbRec;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.UpcomingSequence;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

@WebServlet(name = "doCheckListAction", value = "/doCheckListAction")
public class doCheckListAction extends HttpServlet {
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        takeAction(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        takeAction(request);
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher(getPath());
        dispatcher.forward(request,response);
    }

    private void takeAction(HttpServletRequest request){
        setPath("/WEB-INF/view/a/pspHome/pspHome.jsp");
        String buttonCode;
        try{
            buttonCode = request.getParameter("btnCheckList").toString();
        } catch (Exception e){return;}

        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        String actionCode;
        long checklistId;
        CheckList checkList;
        try{
            actionCode = buttonCode.substring(0,1).toString();
            checklistId = Long.parseLong(buttonCode.substring(2));
            checkList = dM.getCheckListById(em,checklistId);
        } catch (Exception e1){return;}
        if(checkList==null)
            return;

        SessionVar sVar = (SessionVar) request.getSession().getAttribute("sVar");
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");

        switch (actionCode) {
            case "C" -> {  //Close the CheckList Item
                setPath("/WEB-INF/view/a/pspHome/pspHome.jsp");
                em.getTransaction().begin();
                checkList.setComplete(true);
                checkList.setCompletedBy(currentPerson);
                checkList.setDateCompleted(Date.valueOf(LocalDate.now()));
                em.persist(checkList);
                em.getTransaction().commit();
                em.refresh(checkList);

                if(checkList.getRecurringTaskList()!=null){
                    UpcomingSequence us = dbRec.getUpcomingSequence(em,checkList.getRecurringTaskList());
                    if(us!=null)
                        dbRec.createNewRecurringChecklist(em,us,currentPerson);
                }

                sVar.setCurrentCheckList(null);
                sVar.setCurrentActivity(null);
                sVar.refreshOpenChecklists(em);
                sVar.refreshClosedCheckLists(em);
            }
            case "V" -> {  //View the CheckList Detail
                setPath("/WEB-INF/view/a/checklistDetail/checklistDetail.jsp");
                sVar.setCurrentActivity(checkList);
                sVar.setCurrentCheckList(checkList);
                Person p = null;
                if (checkList.getAssignedTo() != null)
                    p = (Person) checkList.getAssignedTo();
                request.getSession().setAttribute("activityOwner1", p);
                sVar.refreshActivityHistory(em);
                sVar.refreshContactListFromCurrentActivity(em);
                sVar.refreshToDoOutList(em);
            }
            case "R" -> {  //Reassign the CheckList
                setPath("/WEB-INF/view/a/pspHome/pspHome.jsp");
                long userListId;
                Person user = null;
                try {
                    userListId = Long.parseLong(request.getParameter("userList"));
                    user = dM.getPersonById(em, userListId);
                } catch (Exception e3) {
                    return;
                }
                if (user == null)
                    return;
                em.getTransaction().begin();
                checkList.setAssignedTo(user);
                em.persist(checkList);
                em.getTransaction().commit();
                em.refresh(checkList);
                sVar.refreshOpenChecklists(em);
            }
            case "D" -> {  //Change the Due Date
                setPath("/WEB-INF/view/a/pspHome/pspHome.jsp");
                Date newDate = null;
                String dateString;
                try {
                    dateString = request.getParameter("newDueDate").toString();
                    newDate = Date.valueOf(dateString);
                } catch (Exception e22) {
                    return;
                }
                if (newDate == null)
                    return;
                em.getTransaction().begin();
                checkList.setDueDate(newDate);
                em.persist(checkList);
                em.getTransaction().commit();
                em.refresh(checkList);
                sVar.refreshOpenChecklists(em);
            }
            case "U" -> { // Undo Closure
                setPath("/WEB-INF/view/a/pspHome/pspHome.jsp");
                em.getTransaction().begin();
                checkList.setComplete(false);
                checkList.setCompletedBy(null);
                checkList.setDateCompleted(null);
                em.persist(checkList);
                em.getTransaction().commit();
                em.refresh(checkList);

                dbRec.removeFutureRecurring(em,checkList);

                sVar.setCurrentCheckList(null);
                sVar.setCurrentActivity(null);
                sVar.refreshOpenChecklists(em);
                sVar.refreshClosedCheckLists(em);
            }
        }
        request.getSession().setAttribute("sVar",sVar);
        em.close();
    }

}
