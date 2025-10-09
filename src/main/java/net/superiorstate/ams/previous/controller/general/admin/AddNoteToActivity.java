package net.superiorstate.ams.previous.controller.general.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.activity.ticket.CreateTicket;
import net.superiorstate.ams.previous.data.misc.dP;
import net.superiorstate.ams.previous.data.misc.dbAuth;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.creates.dC;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.note.Note;
import net.superiorstate.ams.previous.model.activity.ticket.Ticket;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AddNoteToActivity", value = "/AddNoteToActivity")
public class AddNoteToActivity extends HttpServlet {
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
    private void maybeUpdateEmail(HttpServletRequest request, EntityManager em, Ticket t, Person user){
        String newEmail = request.getParameter("ticketEmail");
        if(dbEmail.isValidEmail(newEmail)) {
            if (t.getContact().getEmail()== null  || newEmail.compareTo(t.getContact().getEmail()) != 0) {
                em.getTransaction().begin();
                Person p = dM.getPersonById(em, t.getContact().getId());
                p.setEmail(newEmail);
                em.persist(p);
                em.getTransaction().commit();
                if (dP.getEmployeeByPerson(em, p) != null) {
                    em.getTransaction().begin();
                    Employee e = dP.getEmployeeByPerson(em, p);
                    assert e != null;
                    e.setEmail(newEmail);
                    em.persist(e);
                    em.getTransaction().commit();
                    String reminder = "Update " + p.getFirstName().charAt(0) + " " + p.getLastName() + "'s email to " + newEmail + " in Summit";
                    dC.createReminder(em, reminder, dbAuth.getUserFromPerson(em, user), Date.valueOf(LocalDate.now()));
                }
            }
        }
    }

    private void changeView(HttpServletRequest request){
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        request.getSession().setAttribute("lastTab",5);
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        Person p = (Person) request.getSession().getAttribute("currentPerson");
        String btValue = request.getParameter("btnAddNote1");
        if(btValue.equals("UpdateEmail")){
            maybeUpdateEmail(request,em,(Ticket) a,p);
            ViewSelectedActivity.setActivityView(request,em,a);
        } else{
            int status = Integer.parseInt(request.getParameter("noteStatus"));
            int reason = Integer.parseInt(request.getParameter("reasonList"));
            String noteText = request.getParameter("noteText");
            em.getTransaction().begin();
            Note note = new Note();
            note.setDetail(noteText);
            note.setActivity(a);
            note.setStatus(dM.getActivityStatusById(em,status));
            note.setReasonCreated(dM.getReasonById(em,reason));
            note.setDateGenerated(Date.valueOf(LocalDate.now()));
            note.setCreatedBy(p);
            em.persist(note);
            em.getTransaction().commit();

            em.getTransaction().begin();
            Activity a1 = dM.getActivityById(em,a.getId());
            assert a1 != null;
            a1.getNoteList().add(note);
            em.persist(a1);
            em.getTransaction().commit();
            request.getSession().setAttribute("currentActivity",a1);
            ViewSelectedActivity.setActivityView(request,em,a1);
        }
    }

    private void refreshNoteList(EntityManager em, Activity a){
        Query q = em.createQuery("SELECT n FROM Note n WHERE n.activity.id = :aId");
        q.setParameter("aId",a.getId());
        List<Note> noteList;
        try{
            noteList = (List<Note>) q.getResultList();
        } catch (NoResultException e){
            e.printStackTrace();
            noteList = new ArrayList<>();
        }
        em.getTransaction().begin();
        Activity activity = dM.getActivityById(em,a.getId());
        activity.setNoteList(noteList);
        em.persist(activity);
        em.getTransaction().commit();
    }
    private void updateEmailOrPhone(HttpServletRequest request, EntityManager em){
        String formEmail = request.getParameter("ticketEmail");
        Ticket ticket = (Ticket) request.getSession().getAttribute("currentActivity");
        String noteText = (String) request.getSession().getAttribute("noteText");
        String boxEmail = CreateTicket.findEmail(noteText);
        String ticketEmail = ticket.getContact().getEmail();
        String newEmail = "";
        boolean update = false;
        //Email
        if(dbEmail.isValidEmail(ticketEmail)){
            if(dbEmail.isValidEmail(formEmail) && formEmail.compareTo(ticketEmail)!=0){
                newEmail = formEmail;
                update = true;
            }
        } else{
          if(dbEmail.isValidEmail(formEmail)){
              newEmail = formEmail;
              update = true;
          } else if(dbEmail.isValidEmail(boxEmail)){
              newEmail = boxEmail;
              update = true;
          }
        }
        if(update){
            em.getTransaction().begin();
            Person contact = dM.getPersonById(em,ticket.getContact().getId());
            contact.setEmail(newEmail);
            em.persist(contact);
            em.getTransaction().commit();

        }
    }

}
