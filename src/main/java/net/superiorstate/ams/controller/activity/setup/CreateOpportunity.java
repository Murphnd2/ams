package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Prospect;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "CreateOpportunity", value = "/CreateOpportunity")
public class CreateOpportunity extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("AgentHome");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Opportunity opp = createOpportunity(request);

        String returnTo = request.getParameter("returnTo");
        if ("home".equals(returnTo) && opp != null) {
            updateGlobalState(request, opp);
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
            dispatcher.forward(request, response);
        } else {
            response.sendRedirect("AgentHome");
        }
    }

    private Opportunity createOpportunity(HttpServletRequest request) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            Person currentUser = local.getCurrentPerson();

            long agencyId = Long.parseLong(request.getParameter("agencyId"));
            Agency agency = EntityLookup.getAgencyById(em, agencyId);
            if (agency == null) return null;

            // Determine prospect — existing or new
            Prospect prospect;
            String prospectMode = request.getParameter("prospectMode");

            if ("new".equals(prospectMode)) {
                prospect = createNewProspect(em, request, agency, currentUser);
            } else {
                long prospectId = Long.parseLong(request.getParameter("prospectId"));
                prospect = EntityLookup.getProspectById(em, prospectId);
            }

            if (prospect == null) return null;

            // Create Opportunity
            em.getTransaction().begin();
            Opportunity opp = new Opportunity();
            opp.setProspect(prospect);
            opp.setAgency(agency);
            opp.setStage("NEW");
            opp.setAssignedTo(currentUser);
            opp.setLoggedBy(currentUser);
            opp.setPrimaryContact(prospect.getContact());
            opp.setFullName(prospect.getName().trim().toUpperCase());
            opp.setDueDate(Date.valueOf(LocalDate.now().plusDays(30)));
            opp.setComplete(false);
            em.persist(opp);
            em.getTransaction().commit();

            // Create CheckList
            em.getTransaction().begin();
            CheckList c = new CheckList();
            c.setAssignedTo(opp);
            c.setComplete(false);
            c.setFullName(prospect.getName().trim().toUpperCase() + " Checklist");
            c.setDueDate(opp.getDueDate());
            c.setLoggedBy(currentUser);
            em.persist(c);
            em.getTransaction().commit();

            // Add default ToDo (task 153 - pre-completed placeholder)
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(EntityLookup.getTaskById(em, 153L));
            toDo.setSortOrder(1000);
            toDo.setCheckList(c);
            toDo.setComplete(true);
            em.persist(toDo);
            em.getTransaction().commit();

            // Link CheckList back to Opportunity
            CheckList checkList = EntityLookup.getCheckListById(em, c.getId());
            em.getTransaction().begin();
            opp.setCheckList(checkList);
            em.persist(opp);
            em.getTransaction().commit();

            // Update session
            em.refresh(opp);
            local.respondToActivityUpdate(em, "ADD_TICKET", opp);
            request.getSession().setAttribute("local", local);

            return opp;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    /** Update global activity list and prospect cache when creating from PSP home */
    private void updateGlobalState(HttpServletRequest request, Opportunity opp) {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");

            Activity25u newActivity = local.getActivity25u(em, opp);
            List<Activity25u> allActivities = new ArrayList<>(global.getActivitiesAllOpen());
            allActivities.add(newActivity);

            global.setActivitiesAllOpen(allActivities);
            local.setActivitiesAllOpen(allActivities);

            // Refresh full sales data cache (prospect + agent/agency data)
            global.refreshSalesData(em);

            local.getCurrentActivity().setActivity(opp);
            local.getCurrentActivity().setReFilterOnExit(true);

            request.getSession().setAttribute("local", local);
            request.getServletContext().setAttribute("global", global);
        } finally {
            em.close();
        }
    }

    private Prospect createNewProspect(EntityManager em, HttpServletRequest request, Agency agency, Person currentUser) {
        String companyName = request.getParameter("companyName");
        String contactFirst = request.getParameter("contactFirst");
        String contactLast = request.getParameter("contactLast");
        String contactEmail = request.getParameter("contactEmail");

        if (companyName == null || companyName.trim().isEmpty()) return null;

        // Create contact Person
        em.getTransaction().begin();
        Person contact = new Person();
        contact.setFirstName(contactFirst != null ? contactFirst.trim() : "");
        contact.setLastName(contactLast != null ? contactLast.trim() : "");
        contact.setFullName((contact.getFirstName() + " " + contact.getLastName()).trim());
        contact.setEmail(contactEmail != null ? contactEmail.trim() : "");
        contact.setPsp(currentUser.getPsp());
        em.persist(contact);
        em.getTransaction().commit();

        // Determine agent: explicit agentId param > current user if in agency > first agent in agency > fallback
        Person agent = null;
        String agentIdParam = request.getParameter("agentId");
        if (agentIdParam != null && !agentIdParam.isEmpty()) {
            try {
                agent = EntityLookup.getPersonById(em, Long.parseLong(agentIdParam));
            } catch (NumberFormatException ignored) {}
        }
        if (agent == null && agency.getAgentList() != null && !agency.getAgentList().isEmpty()) {
            // Check if current user is an agent of this agency
            for (Person p : agency.getAgentList()) {
                if (p.getId().equals(currentUser.getId())) {
                    agent = currentUser;
                    break;
                }
            }
            // Fallback: first agent in the agency
            if (agent == null) {
                agent = agency.getAgentList().get(0);
            }
        }
        // Last resort fallback
        if (agent == null) {
            agent = currentUser;
        }

        // Create Prospect
        em.getTransaction().begin();
        Prospect prospect = new Prospect();
        prospect.setName(companyName.trim());
        prospect.setContact(contact);
        prospect.setAgent(agent);
        em.persist(prospect);
        em.getTransaction().commit();

        return prospect;
    }
}
