package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.activity.aList;
import net.superiorstate.ams.previous.data.checklist.dbRec;
import net.superiorstate.ams.previous.data.misc.*;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.RecurringTaskList;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.UpcomingSequence;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.previous.model.activity.note.Note;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "GoAdminHome", value = "/GoAdminHome")
public class GoAdminHome extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doThisFirst(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher= request.getRequestDispatcher("/WEB-INF/view/adminHome.jsp");
        dispatcher.forward(request,response);
    }

    private void doThisFirst(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        User currentUser = (User) request.getSession().getAttribute("currentUser");

        int nwf = 1;
        if(request.getSession().getAttribute("qNwf")!=null)
            nwf = Integer.parseInt(request.getSession().getAttribute("qNwf").toString());

        boolean alpha = false;
        if(request.getSession().getAttribute("qAlpha")!=null)
            alpha = (boolean) request.getSession().getAttribute("qAlpha");

        int whoFilter = 1;
        if(request.getSession().getAttribute("qWhoFilter")!=null)
            whoFilter = Integer.parseInt(request.getSession().getAttribute("qWhoFilter").toString());
        System.out.println("Who Filter: " + whoFilter);


        String rn = request.getSession().getAttribute("vRn").toString();
        String st = request.getSession().getAttribute("vSt").toString();
        String tk = request.getSession().getAttribute("vTk").toString();
        int dFilter = 0;
        if(rn!=null && rn.equals("1")) {
            dFilter += 1;
            System.out.println("Renewal Checked: " + dFilter);
        }
        if(st!=null && st.equals("3")) {
            dFilter += 3;
            System.out.println("Setup Checked: " + dFilter);
        }
        if(tk!=null && tk.equals("5")) {
            dFilter += 5;
            System.out.println("Ticket Checked: " + dFilter);
        }

        request.getSession().setAttribute("staffList", dbAuth.getPspStaff(em,currentUser.getPerson().getPsp()));
        request.getSession().setAttribute("userStatus", dbTime.getMyLastPunch(em,currentUser).isIn());
        request.getSession().setAttribute("myTimeList", dbTime.getTodaysTimeHistory(em, currentUser.getPerson()));
        request.getSession().setAttribute("activityShellList", aList.getActivityShellList1(em,currentUser.getPerson(),whoFilter,false,alpha,nwf,dFilter));
        request.getSession().setAttribute("employerRenewalList", dbRenew.getEmployersWithUpcomingRenewals(em));
        request.getSession().setAttribute("employerRenewalListAlt",dbRenew.getEmployerRenewals(em));
        request.getSession().setAttribute("pspEmployerList", dPSP.getPspEmployerList(em));
        request.getSession().setAttribute("ticketReasonList", dbTicket.getTicketSubCats(em));
        request.getSession().setAttribute("fullActivityList", aList.getFullList(em));
        request.getSession().setAttribute("uninitialized",1);

        autoGenerateRecurringChecklists(request,em,currentUser.getPerson());
        aList.setMyCurrentItems(em,currentUser.getPerson(),request);
        aList.getMyListsClosedToday(em,currentUser.getPerson(),request);
        refreshActivityHistory(request,em);
        em.close();
    }

    private void autoGenerateRecurringChecklists(HttpServletRequest request,EntityManager em, Person person){
        List<UpcomingSequence> listsToGenerate = null;
        try{
            listsToGenerate = dbRec.getMyUpcomingLists(em,person);
        } catch (Exception e){
            e.printStackTrace();
        }
        if(listsToGenerate==null)
            return;

        for(UpcomingSequence s: listsToGenerate){
            System.out.println("Sequence: " + s.getRecurringTaskList().getDescription());
            em.getTransaction().begin();
            RecurringTaskList rtl = dbRec.getRecurringListById(em,s.getRecurringTaskList().getId());
            CheckList c = new CheckList();
            c.setRecurringTaskList(rtl);
            c.setLoggedBy(person);
            c.setDueDate(s.getNextDue());
            c.setAssignedTo(s.getAssignedTo());
            c.setFullName(rtl.getDescription());
            c.setComplete(false);
            em.persist(c);
            em.getTransaction().commit();

            List<TaskSequenceTable> rtlTaskList = rtl.getTaskSequenceTableList();
            for(TaskSequenceTable tst:rtlTaskList){
                em.getTransaction().begin();
                ToDo toDo = new ToDo();
                toDo.setComplete(false);
                toDo.setTask(tst.getTask());
                toDo.setSortOrder(tst.getSortOrder());
                toDo.setCheckList(c);
                em.persist(toDo);
                em.getTransaction().commit();

                em.getTransaction().begin();
                CheckList c1 = dM.getCheckListById(em,c.getId());
                c1.getToDoList().add(toDo);
                em.persist(c1);
                em.getTransaction().commit();
            }
        }
    }
    private void refreshActivityHistory(HttpServletRequest request, EntityManager em){
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        List<Note> noteList;
        try{
            noteList = dbNote.getActivityHistory(em,a);
        } catch (Exception e){
            noteList = new ArrayList<>();
        }
        request.getSession().setAttribute("activityHistory",noteList);


    }
}
