package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.Q;
import net.superiorstate.ams.previous.data.misc.dP;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.activity.ticket.tEmployee;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "Send105Reminder", value = "/Send105Reminder")
public class Send105Reminder extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            sendQuickAction(request);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            sendQuickAction(request);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        goToPage(request,response);
    }
    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
        dispatcher.forward(request,response);
    }

    private void sendQuickAction(HttpServletRequest request) throws MessagingException {
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        Renewal r = (Renewal) request.getSession().getAttribute("currentActivity");
        Activity a = r;
        Person p = (Person) request.getSession().getAttribute("currentPerson");
        List<tEmployee> contactList = (List<tEmployee>) request.getSession().getAttribute("contactList");
        if(contactList==null || contactList.size()==0)
            return;

        List<Person> recipientList = new ArrayList<>();
        for(tEmployee te: contactList){
            Employee e = dM.getEmployeeById(em,te.getId());
            Person person = dP.getPersonByEe(em,e,p.getPsp());
            recipientList.add(person);
        }

        String eSubject = "Health FSA / HRA Participation (Non-Discrimination) Test";
        String eMessage = Q.qText(em,r,Q.TEST_PARTICIPATION)  + Q.insert105TestGoogle(r.getEmployer().getEmployerName()) + StdAuto.userSignature(p);
        Email email = StdAuto.createEmail(request,em,a,eSubject,eMessage,1,p);
        Email emailToSend = dM.getEmailById(em,email.getId());
        dbEmail.sendEmail(emailToSend,em);

        em.close();
    }
}
