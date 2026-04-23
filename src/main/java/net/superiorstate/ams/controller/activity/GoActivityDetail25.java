package net.superiorstate.ams.controller.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "GoActivityDetail25", value = "/GoActivityDetail25")
public class GoActivityDetail25 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (agentBlocked(request, response)) return;
        viewActivity(request);
        goToPage(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (agentBlocked(request, response)) return;
        viewActivity(request);
        goToPage(request,response);
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request,response);
    }

    /**
     * V061: gate agent access to activity-detail pages.
     *
     * Agents are only allowed to view an activity if they own at least one
     * open ToDo on that activity's checklist via the per-Setup override
     * (overrideOwnership=true, hasOwner=true, owner=them). PSP staff and
     * agency admins are unaffected.
     *
     * Returns true when the request was blocked (redirect issued), false
     * when the caller should continue normal processing.
     */
    private boolean agentBlocked(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        boolean isAgent = Boolean.TRUE.equals(session.getAttribute("isAgent"));
        boolean isAgencyAdmin = Boolean.TRUE.equals(session.getAttribute("isAgencyAdmin"));
        boolean isPspUser = Boolean.TRUE.equals(session.getAttribute("isPspUser"));
        boolean isPspAdmin = Boolean.TRUE.equals(session.getAttribute("isPspAdmin"));
        boolean isPspSales = Boolean.TRUE.equals(session.getAttribute("isPspSales"));

        // Only block when user is agent-only (no PSP-side role)
        if (!isAgent || isPspUser || isPspAdmin || isPspSales || isAgencyAdmin) return false;

        AmsDataLocal local = (AmsDataLocal) session.getAttribute("local");
        if (local == null || local.getCurrentPerson() == null) return false;

        String activityIdString = request.getParameter("btnViewActivity");
        if (activityIdString == null) activityIdString = (String) request.getAttribute("btnViewActivity");
        if (activityIdString == null) return false;

        long activityId;
        try { activityId = Long.parseLong(activityIdString); } catch (NumberFormatException e) { return false; }

        long agentId = local.getCurrentPerson().getId();

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Query q = em.createQuery(
                    "SELECT COUNT(td) FROM ToDo td " +
                    "WHERE td.overrideOwnership = true " +
                    "  AND td.hasOwner = true " +
                    "  AND td.owner.id = :agentId " +
                    "  AND td.checkList.id IN (" +
                    "    SELECT s.checkList.id FROM Setup s WHERE s.id = :actId" +
                    "  )");
            q.setParameter("agentId", agentId);
            q.setParameter("actId", activityId);
            long count = ((Number) q.getSingleResult()).longValue();
            if (count == 0) {
                response.sendRedirect(request.getContextPath() + "/AgentHome");
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/AgentHome");
            return true;
        } finally {
            em.close();
        }
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
        return person != null && person.getEmail() != null && Validator.isValidEmail(person.getEmail());
    }

}
