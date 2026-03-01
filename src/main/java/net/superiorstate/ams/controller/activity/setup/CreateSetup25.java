package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.dao.ActivityDAO;
import net.superiorstate.ams.data.dao.ApplicationTaskDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.Activity25;
import net.superiorstate.ams.model.Activity25u;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.SortedTask;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.PersonV;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.agency.Rate;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "CreateSetup25", value = "/CreateSetup25")
public class CreateSetup25 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        Person currentPerson = (Person) request.getSession().getAttribute("currentPerson");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // 1. Resolve or create prospect
            Prospect prospect = resolveProspect(request, em, local, global, currentPerson);

            // 2. Create proposal with selected rate and LOS items
            Proposal proposal = createProposal(request, em, prospect, currentPerson);
            fillProposalLos(request, em, proposal);

            // Clear L1 cache — LOS relationships loaded during fillProposalLos leave
            // ServiceItem objects in the persistence context that cause cascade PERSIST
            // errors when ActivityDAO.addModule commits later
            long proposalId = proposal.getId();
            long prospectId = prospect.getId();
            long currentPersonId = currentPerson.getId();
            em.clear();
            proposal = EntityLookup.getProposalById(em, proposalId);
            prospect = EntityLookup.getProspectById(em, prospectId);
            currentPerson = EntityLookup.getPersonById(em, currentPersonId);

            // 3. Create application and add modules from LOS serviceItems + extras
            Application application = createApplication(em, proposal);
            fillApplicationModules(request, em, application);

            // 4. Create checklist with seed task
            CheckList checkList = createChecklist(em, prospect.getName(), currentPerson);

            // 5. Create setup
            Setup setup = createSetup(em, prospect, application, checkList, currentPerson);

            // 6. Fill todo list from task sequences
            fillToDoList(em, setup);

            // 7. Update activity cache
            updateActivityCache(em, setup, local, global);

            request.getSession().setAttribute("local", local);
            request.getServletContext().setAttribute("global", global);
        } finally {
            em.close();
        }

        // Forward to home
        RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("ViewHome25");
        dispatcher.forward(request, response);
    }

    private Prospect resolveProspect(HttpServletRequest request, EntityManager em, AmsDataLocal local,
                                     AmsDataGlobal global, Person currentPerson) {
        String prospectMode = request.getParameter("prospectMode");

        if ("existing".equals(prospectMode)) {
            long prospectId = Long.parseLong(request.getParameter("prospectId"));
            return EntityLookup.getProspectById(em, prospectId);
        }

        // New prospect — create person + prospect
        String companyName = request.getParameter("companyName");
        String contactFirst = request.getParameter("contactFirst");
        String contactLast = request.getParameter("contactLast");
        String contactEmail = request.getParameter("contactEmail");
        long agencyId = Long.parseLong(request.getParameter("agencyId"));

        // Resolve agent: use selected agentId if provided, otherwise default to current user
        Person agent;
        String agentIdStr = request.getParameter("agentId");
        if (agentIdStr != null && !agentIdStr.isBlank()) {
            agent = EntityLookup.getPersonById(em, Long.parseLong(agentIdStr));
        } else {
            agent = currentPerson;
        }

        Agency agency = EntityLookup.getAgencyById(em, agencyId);
        Person contact = findOrCreatePerson(em, contactFirst, contactLast, contactEmail, agency, local);
        Prospect prospect = createNewProspect(em, companyName, contact, agent);

        // Add new prospect to global list immediately
        List<Prospect> updatedProspects = new ArrayList<>(global.getProspects());
        updatedProspects.add(prospect);
        global.setProspects(updatedProspects);

        return prospect;
    }

    private Person findOrCreatePerson(EntityManager em, String firstName, String lastName, String email, Agency agency, AmsDataLocal local) {
        if (email != null && !email.isBlank()) {
            Query q = em.createQuery("SELECT p FROM PersonV p WHERE p.email = :email");
            q.setParameter("email", email.toLowerCase().trim());
            try {
                List<PersonV> results = (List<PersonV>) q.getResultList();
                if (results != null && !results.isEmpty()) {
                    return EntityLookup.getPersonById(em, results.get(0).getId());
                }
            } catch (NoResultException ignored) {}
        }

        em.getTransaction().begin();
        Person p = new Person();
        p.setFirstName(firstName != null ? firstName.trim() : "");
        p.setLastName(lastName != null ? lastName.trim() : "");
        p.setFullName(((firstName != null ? firstName.trim() : "") + " " + (lastName != null ? lastName.trim() : "")).trim());
        if (email != null && !email.isBlank()) {
            p.setEmail(email.trim());
        }
        p.setPsp(local.getCurrentPerson().getPsp());
        if (agency != null && agency.getAddress() != null) {
            p.setAddress(agency.getAddress());
        }
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    private Prospect createNewProspect(EntityManager em, String companyName, Person contact, Person agent) {
        em.getTransaction().begin();
        Prospect p = new Prospect();
        p.setName(companyName);
        p.setContact(contact);
        p.setAddress(contact.getAddress());
        p.setAgent(agent);
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    private Proposal createProposal(HttpServletRequest request, EntityManager em, Prospect prospect, Person createdBy) {
        long rateId = Long.parseLong(request.getParameter("rateId"));
        Rate rate = EntityLookup.getRateById(em, rateId);
        String appKey = UUID.randomUUID().toString();

        em.getTransaction().begin();
        Proposal proposal = new Proposal();
        proposal.setRate(rate);
        proposal.setApplicationGUID(appKey);
        proposal.setProspect(prospect);
        proposal.setCreatedBy(createdBy);
        em.persist(proposal);
        em.getTransaction().commit();
        return proposal;
    }

    private void fillProposalLos(HttpServletRequest request, EntityManager em, Proposal proposal) {
        String[] losIds = request.getParameterValues("losIds");
        if (losIds == null) return;
        for (String losIdStr : losIds) {
            long losId = Long.parseLong(losIdStr);
            em.getTransaction().begin();
            Proposal p = EntityLookup.getProposalById(em, proposal.getId());
            // Use getLosById (simple query) instead of getLosFull (eager-fetches serviceModuleList
            // graph which causes cascade PERSIST errors when addModule commits later)
            LOS los = EntityLookup.getLosById(em, losId);
            p.getLosList().add(los);
            los.getListOfProposalsThatIncludeThisLOS().add(p);
            em.persist(p);
            em.persist(los);
            em.getTransaction().commit();
        }
    }

    private Application createApplication(EntityManager em, Proposal proposal) {
        em.getTransaction().begin();
        Application a = new Application();
        a.setProposal(proposal);
        em.persist(a);
        em.getTransaction().commit();
        return a;
    }

    private void fillApplicationModules(HttpServletRequest request, EntityManager em, Application application) {
        // Add modules from selected LOS items (each LOS has a serviceItem)
        // Use getLosById (simple query) instead of getLosFull (eager-fetches serviceModuleList
        // graph which causes cascade PERSIST errors when addModule commits)
        String[] losIds = request.getParameterValues("losIds");
        if (losIds != null) {
            for (String losIdStr : losIds) {
                long losId = Long.parseLong(losIdStr);
                LOS los = EntityLookup.getLosById(em, losId);
                if (los != null && los.getServiceItem() != null) {
                    ServiceItem si = EntityLookup.getServiceItemById(em, los.getServiceItem().getId());
                    ActivityDAO.addModule(em, application, si);
                }
            }
        }

        // Add extra modules (e.g., Payments=17, Cards=19)
        String[] extraModuleIds = request.getParameterValues("extraModuleIds");
        if (extraModuleIds != null) {
            for (String extraIdStr : extraModuleIds) {
                int serviceItemId = Integer.parseInt(extraIdStr);
                ServiceItem si = EntityLookup.getServiceItemById(em, serviceItemId);
                ActivityDAO.addModule(em, application, si);
            }
        }
    }

    private CheckList createChecklist(EntityManager em, String prospectName, Person currentPerson) {
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setFullName(prospectName + " Checklist");
        c.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2L)));
        c.setComplete(false);
        c.setLoggedBy(currentPerson);
        em.persist(c);
        em.getTransaction().commit();

        // Seed with Task 153 (system placeholder, marked complete)
        Task t = EntityLookup.getTaskById(em, 153L);
        em.getTransaction().begin();
        ToDo toDo = new ToDo();
        toDo.setDateCompleted(Date.valueOf(LocalDate.now()));
        toDo.setCompletedBy(currentPerson);
        toDo.setComplete(true);
        toDo.setTask(t);
        toDo.setCheckList(c);
        toDo.setSortOrder(0);
        em.persist(toDo);
        em.getTransaction().commit();

        return c;
    }

    private Setup createSetup(EntityManager em, Prospect prospect, Application application, CheckList checkList, Person currentPerson) {
        em.getTransaction().begin();
        Setup setup = new Setup();
        setup.setApplication(application);
        setup.setPrimaryContactSetup(prospect.getContact());
        setup.setComplete(false);
        setup.setFullName(prospect.getName());
        setup.setLoggedBy(currentPerson);
        setup.setAssignedTo(currentPerson);
        setup.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2)));
        setup.setCheckList(checkList);
        em.persist(setup);
        em.getTransaction().commit();

        // Link checklist back to setup (bidirectional)
        em.getTransaction().begin();
        CheckList cl = EntityLookup.getCheckListById(em, checkList.getId());
        cl.setAssignedTo(setup);
        cl.setSetup(setup);
        em.persist(cl);
        em.getTransaction().commit();

        return setup;
    }

    private void fillToDoList(EntityManager em, Setup setup) {
        Application a = setup.getApplication();
        CheckList c = setup.getCheckList();
        List<SortedTask> sortedTaskList = ApplicationTaskDAO.getTasksRequiredForApplication(em, a);

        if (sortedTaskList.isEmpty()) {
            sortedTaskList.add(new SortedTask(EntityLookup.getTaskById(em, 153L), 1000));
        }

        for (SortedTask st : sortedTaskList) {
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(st.getTask());
            toDo.setSortOrder(st.getSortOrder());
            toDo.setCheckList(c);
            toDo.setComplete(st.getTask().getId() == 153L);
            em.persist(toDo);
            em.getTransaction().commit();

            em.getTransaction().begin();
            CheckList checkList = EntityLookup.getCheckListById(em, c.getId());
            checkList.getToDoList().add(toDo);
            em.persist(checkList);
            em.getTransaction().commit();
        }
    }

    private void updateActivityCache(EntityManager em, Setup setup, AmsDataLocal local, AmsDataGlobal global) {
        Query q = em.createQuery("SELECT a FROM Activity25 a WHERE a.activity.id = :id");
        q.setParameter("id", setup.getId());
        Activity25 a25 = (Activity25) q.getSingleResult();
        Activity25u au = new Activity25u(a25);

        List<Activity25u> listToModify = new ArrayList<>(global.getActivitiesAllOpen());
        listToModify.add(au);
        global.setActivitiesAllOpen(listToModify);
        local.setActivitiesAllOpen(global.getActivitiesAllOpen());
        local.getCurrentActivity().setReFilterOnExit(true);
    }
}
