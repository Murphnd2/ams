package net.superiorstate.ams.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.data.renewal.dR;
import net.superiorstate.ams.previous.model.activity.renewal.Renewal;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.PersonV;
import net.superiorstate.ams.previous.model.summit.archive.Benefit;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AddRenewal25", value = "/AddRenewal25")
public class AddRenewal25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request);
        forwardToView(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request);
        forwardToView(request, response);
    }

    private void forwardToView(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request, response);
    }

    private void handleRequest(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
            Employer currentEmployer = (Employer) request.getSession().getAttribute("currentEmployer");
            Person currentPerson = local.getCurrentPerson();

            Renewal renewal = dR.createRenewal(em, currentEmployer, currentPerson);
            dR.createCheckListForRenewal(em, renewal, currentPerson);
            createRenewalItems(request, em, renewal);
            assignPrimaryContact(em, renewal);

            local.refreshRenewals(em);
            Activity25u au = local.getActivity25u(em, renewal);

            List<Activity25u> activityList = new ArrayList<>(global.getActivitiesAllOpen());
            activityList.add(au);
            global.setActivitiesAllOpen(activityList);
            local.setActivitiesAllOpen(activityList);
            local.getCurrentActivity().setActivity(renewal);
            local.getCurrentActivity().setReFilterOnExit(true);

            request.getSession().setAttribute("local", local);
            request.getServletContext().setAttribute("global", global);
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    private void createRenewalItems(HttpServletRequest request, EntityManager em, Renewal renewal) {
        List<Benefit> benefitList = (List<Benefit>) request.getSession().getAttribute("benefitsForRenewalList");
        if (benefitList != null) {
            for (Benefit b : benefitList) {
                String buttonVal = request.getParameter("btnBen" + b.getId());
                if (buttonVal != null && !buttonVal.isEmpty()) {
                    dR.addBenefitToRenewal(request, em, b, renewal);
                }
            }
        }
    }

    public static void assignPrimaryContact(EntityManager em, Renewal r) {
        Renewal current = dM.getRenewalById(em, r.getId());
        if (current == null) return;

        Renewal previousRenewal = getPreviousRenewal(em, current);
        if (previousRenewal != null) {
            assignFromPrevious(em, current, previousRenewal);
        } else {
            assignFromEmployer(em, current);
        }
    }

    private static Renewal getPreviousRenewal(EntityManager em, Renewal r) {
        try {
            Query q = em.createQuery("SELECT r FROM Renewal r WHERE r.id < :id AND r.employer.id = :eid order by r.id desc");
            q.setParameter("id",r.getId());
            q.setParameter("eid",r.getEmployer().getId());
            List<Renewal> renewals = (List<Renewal>) q.getResultList();
            if(renewals!=null && renewals.size()>0)
                return renewals.get(0);
            else return null;
        } catch (NoResultException e) {
            return null;
        }
    }

    private static void assignFromPrevious(EntityManager em, Renewal current, Renewal past) {
        em.getTransaction().begin();
        if (past.getPrimaryContact() != null) {
            current.setPrimaryContact(past.getPrimaryContact());
        }
        if (past.getAssigneeContactList() != null && !past.getAssigneeContactList().isEmpty()) {
            current.setAssigneeContactList(past.getAssigneeContactList());
        }
        em.persist(current);
        em.getTransaction().commit();
    }

    private static void assignFromEmployer(EntityManager em, Renewal current) {
        Employer employer = em.find(Employer.class, current.getEmployer().getId());
        if (employer == null) return;

        String email = employer.getEmail().toLowerCase().trim();

        List<PersonV> matches = em.createQuery("SELECT p FROM PersonV p WHERE p.email = :email", PersonV.class)
                .setParameter("email", email)
                .getResultList();

        if (!matches.isEmpty()) {
            Person p = dM.getPersonById(em, matches.get(0).getId());
            if (p == null) return;

            em.getTransaction().begin();
            current.setPrimaryContact(p);
            em.persist(current);
            em.getTransaction().commit();
        } else {
            createAndAssignNewPerson(em, current, employer);
        }
    }

    private static void createAndAssignNewPerson(EntityManager em, Renewal current, Employer employer) {
        int spaceIndex = employer.getContactName().indexOf(" ");
        if (spaceIndex <= 0) return;

        String first = employer.getContactName().substring(0, spaceIndex).toUpperCase();
        String last = employer.getContactName().substring(spaceIndex).trim().toUpperCase();

        Person newPerson = new Person();
        newPerson.setEmail(employer.getEmail());
        newPerson.setFirstName(first);
        newPerson.setLastName(last);
        newPerson.setFullName(first + " " + last);
        newPerson.setPhone(employer.getPhone());
        newPerson.setTitle("Contact");
        newPerson.setPsp(current.getLoggedBy().getPsp());

        em.getTransaction().begin();
        em.persist(newPerson);
        em.getTransaction().commit();

        em.getTransaction().begin();
        current.setPrimaryContact(newPerson);
        em.persist(current);
        em.getTransaction().commit();
    }
}

