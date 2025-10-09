package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.previous.data.V;
import net.superiorstate.ams.previous.data.activity.dActivity;
import net.superiorstate.ams.previous.data.eV;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.activity.Activity;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.summit.archive.Employee;

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
            Activity a = dM.getActivityById(em, activity.getId());
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
                int eeId = Integer.parseInt(request.getParameter("addEmployeeList"));
                Employee ee = dM.getEmployeeById(em, eeId);
                return dActivity.getEmployeePerson(em, ee);
            } else if ("1".equals(buttonClicked)) {
                String email = request.getParameter("contactEmail");
                if (dbEmail.isValidEmail(email)) {
                    Person p = eV.getBestPersonFromString(em, email);
                    return (p != null) ? p : eV.createPersonFromEmail(em, email);
                }
            }
        } catch (Exception e) {
            // Log if needed
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
        if (p.getEmployee() != null && V.isValidEmail(p.getEmployee().getEmail())) {
            return p.getEmployee().getEmail();
        } else if (V.isValidEmail(p.getEmail())) {
            return p.getEmail();
        }
        return null;
    }
}

