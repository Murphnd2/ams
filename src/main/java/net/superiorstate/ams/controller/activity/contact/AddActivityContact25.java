package net.superiorstate.ams.controller.activity.contact;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.util.Validator;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.data.resolver.PersonResolutionService;

import java.io.IOException;

@WebServlet(name = "AddActivityContact25", value = "/AddActivityContact25")
public class AddActivityContact25 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            addContact(request);
        } finally {
            goToPage(request, response);
        }
    }

    private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewActivity25");
        dispatcher.forward(request, response);
    }

    private void addContact(HttpServletRequest request) {
        String buttonClicked = request.getParameter("btnAddContact");
        boolean makePrimary = "1".equals(request.getParameter("makePrimaryCheck"));

        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        Activity activity = local.getCurrentActivity().getActivity();

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Activity a = EntityLookup.getActivityById(em, activity.getId());
            if (a == null) return;

            Person p = determineContactPerson(request, em, buttonClicked);
            if (p == null) return;

            // Add contact if not already present
            em.getTransaction().begin();
            if (!a.getAssigneeContactList().contains(p)) {
                a.addAssigneeContact(p);
                local.respondToActivityUpdate(em, "ADD_CONTACT", p);
            }
            em.persist(a);
            em.getTransaction().commit();
            em.refresh(a);

            // Handle primary contact replacement
            if (makePrimary) {
                swapPrimaryContact(em, local, a, p, request);
            }

        } finally {
            em.close();
        }
    }

    private Person determineContactPerson(HttpServletRequest request, EntityManager em, String buttonClicked) {
        try {
            if ("2".equals(buttonClicked)) {
                // ───── Employee selected from dropdown ─────
                String param = request.getParameter("addEmployeeList");
                if (param == null || param.isBlank()) return null;

                int eeId = Integer.parseInt(param);
                Employee ee = EntityLookup.getEmployeeById(em, eeId);
                if (ee == null) return null;

                // NEW: use our clean, tested, thread-safe method
                return PersonResolutionService.getInstance()
                        .getOrCreatePersonForEmployee(em, ee);
            }

            if ("1".equals(buttonClicked)) {
                // ───── Free-text / email path (already perfect) ─────
                String input = request.getParameter("contactEmail");
                if (input == null || input.trim().isBlank()) return null;

                return PersonResolutionService.getInstance()
                        .resolveOrCreatePerson(em, input.trim());
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO: replace with proper logger later
        }
        return null;
    }

    private void swapPrimaryContact(EntityManager em, AmsDataLocal local, Activity a, Person newPrimary, HttpServletRequest request) {
        String email = getValidEmail(newPrimary);
        if (email == null) return;

        Person currentPrimary = (local.getCurrentActivity() != null) ? local.getCurrentActivity().getPrimaryContact() : null;
        if (currentPrimary == null) return;

        em.getTransaction().begin();
        a.setPrimaryContact(newPrimary);
        em.persist(a);
        em.getTransaction().commit();

        em.getTransaction().begin();
        a.removeAssigneeContact(newPrimary);
        if (!a.getAssigneeContactList().contains(currentPrimary)) {
            a.addAssigneeContact(currentPrimary);
        }
        em.persist(a);
        em.getTransaction().commit();

        em.refresh(a);
        local.respondToActivityUpdate(em, "SWAP_CONTACT", newPrimary);
        request.getSession().setAttribute("local", local);
    }

    private String getValidEmail(Person p) {
        if (p.getEmployee() != null && Validator.isValidEmail(p.getEmployee().getEmail())) {
            return p.getEmployee().getEmail();
        } else if (Validator.isValidEmail(p.getEmail())) {
            return p.getEmail();
        }
        return null;
    }
}

