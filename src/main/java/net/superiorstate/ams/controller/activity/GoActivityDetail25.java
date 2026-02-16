package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.V;
import net.superiorstate.ams.previous.model.general.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "GoActivityDetail25", value = "/GoActivityDetail25")
public class GoActivityDetail25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewActivity(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        viewActivity(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }

    private void viewActivity(HttpServletRequest request) {
        HttpSession session = request.getSession();
        AmsDataLocal local = (AmsDataLocal) session.getAttribute("local");

        // Determine if past activity view is needed
        boolean viewPast = "1".equals(String.valueOf(session.getAttribute("vp")));
        session.setAttribute("vp", "0");

        // Get activity ID
        String activityIdString = viewPast
                ? (String) session.getAttribute("pastActivityId")
                : request.getParameter("btnViewActivity");

        // Fallback if parameter is missing
        if (activityIdString == null) {
            activityIdString = (String) request.getAttribute("btnViewActivity");
        }

        // Initialize activity
        EntityManagerFactory emf = (EntityManagerFactory)getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            local.getCurrentActivity().intializeActivity(em, Long.parseLong(activityIdString));
        } finally {
            em.close();
        }

        fillRecipientList(request, local);
        session.setAttribute("local", local);
    }

    private void fillRecipientList(HttpServletRequest request, AmsDataLocal local) {
        List<Person> recipients = new ArrayList<>();

        // Add primary contact if valid
        Person primary = local.getCurrentActivity().getPrimaryContact();
        if (isValidPerson(primary)) {
            recipients.add(primary);
        }

        // Add valid additional contacts
        List<Person> additional = local.getCurrentActivity().getAdditionalContacts();
        if (additional != null) {
            for (Person p : additional) {
                if (isValidPerson(p)) {
                    recipients.add(p);
                }
            }
        }

        // Set recipient list (empty if none)
        local.getCurrentEmail().setRecipientList(recipients);
        request.getSession().setAttribute("local", local);
    }

    // Helper method to centralize validation
    private boolean isValidPerson(Person person) {
        return person != null && person.getEmail() != null && V.isValidEmail(person.getEmail());
    }

}
