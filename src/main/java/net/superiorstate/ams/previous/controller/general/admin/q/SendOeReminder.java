package net.superiorstate.ams.previous.controller.general.admin.q;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.controller.general.admin.ViewSelectedActivity;
import net.superiorstate.ams.previous.data.Q;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.activity.note.Email;
import net.superiorstate.ams.previous.model.general.Person;

import jakarta.mail.MessagingException;
import java.io.IOException;

@WebServlet(name = "SendOeReminder", value = "/SendOeReminder")
public class SendOeReminder extends HttpServlet {
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
        Activity a = (Activity) request.getSession().getAttribute("currentActivity");
        Person p = (Person) request.getSession().getAttribute("currentPerson");

        String eSubject = "Open Enrollment Materials Requested";
        String eMessage = Q.qText(em,a,Q.COBRA_NEED_OPEN_ENROLLMENT_PACKET) + StdAuto.userSignature(p);
        Email email = StdAuto.createEmail(request,em,a,eSubject,eMessage,1,p);
        Email emailToSend = dM.getEmailById(em,email.getId());
        dbEmail.sendEmail(emailToSend,em);
        ViewSelectedActivity.setActivityView(request,em,a);
        em.close();
    }
}
