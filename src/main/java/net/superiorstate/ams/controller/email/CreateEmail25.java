package net.superiorstate.ams.controller.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CreateEmail25", value = "/CreateEmail25")
public class CreateEmail25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        bindActivityIfPresent(request);
        goToPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        bindActivityIfPresent(request);
        goToPage(request, response);
    }

    /**
     * When an activity id is supplied (e.g. the agent pipeline drawer's Email link sends
     * CreateEmail25?activityId=<oppId>), bind that activity onto the session's CurrentActivity
     * and pre-fill the recipient list from its contacts — mirroring the bind/prefill that
     * GoActivityDetail25.viewActivity performs for the PSP flow. This is what lets the sent
     * email attach to the opportunity (instead of a throwaway CheckList) and return to its
     * detail view after send.
     *
     * Strictly conditional: with no id param this method does nothing, so the existing
     * activity-less navbar "Email" compose path (PSP users) is unchanged.
     */
    private void bindActivityIfPresent(HttpServletRequest request) {
        String idParam = request.getParameter("activityId");
        if (idParam == null) idParam = request.getParameter("btnViewActivity");
        if (idParam == null) return;

        long activityId;
        try {
            activityId = Long.parseLong(idParam.trim());
        } catch (NumberFormatException e) {
            return; // non-numeric id — leave session state untouched
        }

        HttpSession session = request.getSession();
        AmsDataLocal local = (AmsDataLocal) session.getAttribute("local");
        if (local == null) return;

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            local.getCurrentActivity().intializeActivity(em, activityId);
        } finally {
            em.close();
        }

        // Pre-fill recipients from the bound activity's contacts (valid emails only).
        List<Person> recipients = new ArrayList<>();
        Person primary = local.getCurrentActivity().getPrimaryContact();
        if (isValidRecipient(primary)) recipients.add(primary);

        List<Person> additional = local.getCurrentActivity().getAdditionalContacts();
        if (additional != null) {
            for (Person p : additional) {
                if (isValidRecipient(p)) recipients.add(p);
            }
        }
        local.getCurrentEmail().setRecipientList(recipients);

        session.setAttribute("local", local);
    }

    private boolean isValidRecipient(Person person) {
        return person != null && person.getEmail() != null && Validator.isValidEmail(person.getEmail());
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/WEB-INF/view/a/general/emailMaster25.jsp");
        dispatcher.forward(request, response);
    }
}
