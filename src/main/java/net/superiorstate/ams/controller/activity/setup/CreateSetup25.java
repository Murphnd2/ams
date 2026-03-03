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
import net.superiorstate.ams.data.service.BpoTaskPushService;
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
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.agency.Rate;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates a Setup activity directly from the Add Activity modal,
 * bypassing the full sales pipeline (Proposal → Application Review → Setup).
 * Behind the scenes a shell Proposal and Application are still created
 * for commission tracking and data-chain integrity.
 *
 * Setup/CheckList/ToDo creation follows the same pattern as {@link ReviewApplication}.
 */
@WebServlet(name = "CreateSetup25", value = "/CreateSetup25")
public class CreateSetup25 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("ViewHome25");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        Person currentPerson = local.getCurrentPerson();

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // 1. Resolve prospect (existing or new)
            Prospect prospect = resolveProspect(request, em, currentPerson);
            if (prospect == null) {
                response.sendRedirect("ViewHome25");
                return;
            }

            // 2. Create shell Proposal with rate
            long rateId = Long.parseLong(request.getParameter("rateId"));
            Rate rate = EntityLookup.getRateById(em, rateId);

            em.getTransaction().begin();
            Proposal proposal = new Proposal();
            proposal.setProspect(prospect);
            proposal.setRate(rate);
            proposal.setApplicationGUID(UUID.randomUUID().toString());
            proposal.setStatus("CREATED");
            proposal.setCreatedBy(currentPerson);
            proposal.setLosList(new ArrayList<>());
            em.persist(proposal);
            em.getTransaction().commit();

            // Add LOS items to proposal (bidirectional join table)
            long[] losIds = parseLongCsv(request.getParameter("losIds"));
            for (long losId : losIds) {
                em.getTransaction().begin();
                Proposal p = EntityLookup.getProposalById(em, proposal.getId());
                LOS los = EntityLookup.getLosById(em, losId);
                p.getLosList().add(los);
                los.getListOfProposalsThatIncludeThisLOS().add(p);
                em.persist(p);
                em.persist(los);
                em.getTransaction().commit();
            }

            // Clear L1 cache — LOS relationships loaded above leave objects in the
            // persistence context that cause cascade PERSIST errors when addModule commits
            long proposalId = proposal.getId();
            long prospectId = prospect.getId();
            long currentPersonId = currentPerson.getId();
            em.clear();
            proposal = EntityLookup.getProposalById(em, proposalId);
            prospect = EntityLookup.getProspectById(em, prospectId);
            currentPerson = EntityLookup.getPersonById(em, currentPersonId);

            // 3. Create shell Application (auto-approved)
            Timestamp now = Timestamp.from(Instant.now());
            em.getTransaction().begin();
            Application app = new Application();
            app.setProposal(proposal);
            app.setStatus("APPROVED");
            app.setDateStarted(now);
            app.setDateSubmitted(now);
            em.persist(app);
            em.getTransaction().commit();

            // 4. Create ApplicationModule records for each LOS's ServiceItem
            for (long losId : losIds) {
                LOS los = EntityLookup.getLosById(em, losId);
                if (los != null && los.getServiceItem() != null) {
                    ServiceItem si = EntityLookup.getServiceItemById(em, los.getServiceItem().getId());
                    ActivityDAO.addModule(em, app, si);
                }
            }

            // Create ApplicationModule records for each Enhancement's ServiceItem
            long[] enhIds = parseLongCsv(request.getParameter("enhancementIds"));
            for (long enhId : enhIds) {
                Enhancement enh = em.find(Enhancement.class, enhId);
                if (enh != null && enh.getServiceItem() != null) {
                    ServiceItem si = EntityLookup.getServiceItemById(em, enh.getServiceItem().getId());
                    ActivityDAO.addModule(em, app, si);
                }
            }

            // 5. Create CheckList (follows ReviewApplication.createChecklist)
            CheckList checkList = createChecklist(em, prospect.getName(), currentPerson);

            // 6. Create Setup (follows ReviewApplication.createSetup)
            Setup setup = createSetup(em, prospect, app, checkList, currentPerson);

            // 7. Fill ToDo list from task sequences
            fillToDoList(em, setup, currentPerson);

            // 8. Update caches
            em.refresh(setup);
            local.respondToActivityUpdate(em, "ADD_TICKET", setup);
            request.getSession().setAttribute("local", local);

            // Update global activity list
            try {
                Query q = em.createQuery("SELECT a FROM Activity25 a WHERE a.activity.id = :id");
                q.setParameter("id", setup.getId());
                Activity25 a25 = (Activity25) q.getSingleResult();
                Activity25u au = new Activity25u(a25);
                List<Activity25u> allActivities = new ArrayList<>(global.getActivitiesAllOpen());
                allActivities.add(au);
                global.setActivitiesAllOpen(allActivities);
                local.setActivitiesAllOpen(allActivities);
            } catch (Exception e) {
                System.out.println("[CreateSetup25] Global activity cache update skipped: " + e.getMessage());
            }

            // Refresh sales data (new prospects appear immediately in dropdowns)
            global.refreshSalesData(em);
            request.getServletContext().setAttribute("global", global);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[CreateSetup25] Error: " + e.getMessage());
        } finally {
            em.close();
        }

        response.sendRedirect("ViewHome25");
    }

    // ======================== Prospect Resolution ========================

    private Prospect resolveProspect(HttpServletRequest request, EntityManager em, Person currentPerson) {
        String prospectMode = request.getParameter("prospectMode");

        if ("existing".equals(prospectMode)) {
            long prospectId = Long.parseLong(request.getParameter("prospectId"));
            return EntityLookup.getProspectById(em, prospectId);
        }

        // New prospect — create contact Person + Prospect
        String companyName = request.getParameter("companyName");
        if (companyName == null || companyName.trim().isEmpty()) return null;

        String contactFirst = request.getParameter("contactFirst");
        String contactLast = request.getParameter("contactLast");
        String contactEmail = request.getParameter("contactEmail");

        // Resolve agent: use selected agentId if provided, otherwise default to current user
        Person agent;
        String agentIdStr = request.getParameter("agentId");
        if (agentIdStr != null && !agentIdStr.isBlank()) {
            agent = EntityLookup.getPersonById(em, Long.parseLong(agentIdStr));
        } else {
            agent = currentPerson;
        }

        // Check if person already exists by email
        Person contact = findPersonByEmail(em, contactEmail);
        if (contact == null) {
            em.getTransaction().begin();
            contact = new Person();
            contact.setFirstName(contactFirst != null ? contactFirst.trim() : "");
            contact.setLastName(contactLast != null ? contactLast.trim() : "");
            contact.setFullName((contact.getFirstName() + " " + contact.getLastName()).trim());
            contact.setEmail(contactEmail != null ? contactEmail.trim() : "");
            contact.setPsp(currentPerson.getPsp());
            em.persist(contact);
            em.getTransaction().commit();
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

    private Person findPersonByEmail(EntityManager em, String email) {
        if (email == null || email.isBlank()) return null;
        try {
            Query q = em.createQuery("SELECT p FROM PersonV p WHERE p.email = :email");
            q.setParameter("email", email.toLowerCase().trim());
            List<PersonV> results = (List<PersonV>) q.getResultList();
            if (results != null && !results.isEmpty()) {
                return EntityLookup.getPersonById(em, results.get(0).getId());
            }
        } catch (NoResultException ignored) {}
        return null;
    }

    // ======================== CheckList Creation (mirrors ReviewApplication) ========================

    private CheckList createChecklist(EntityManager em, String prospectName, Person currentPerson) {
        em.getTransaction().begin();
        CheckList c = new CheckList();
        c.setFullName(prospectName + " Checklist");
        c.setDueDate(Date.valueOf(LocalDate.now().plusWeeks(2L)));
        c.setComplete(false);
        c.setLoggedBy(currentPerson);
        em.persist(c);
        em.getTransaction().commit();

        // Add task 153 as the first (completed) todo — same as ReviewApplication
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

    // ======================== Setup Creation (mirrors ReviewApplication) ========================

    private Setup createSetup(EntityManager em, Prospect prospect, Application application,
                              CheckList checkList, Person currentPerson) {
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

    // ======================== ToDo List (mirrors ReviewApplication) ========================

    private void fillToDoList(EntityManager em, Setup setup, Person currentPerson) {
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

        // Push sourced tasks to BPO vendors (non-fatal, after all transactions committed)
        CheckList freshChecklist = EntityLookup.getCheckListById(em, c.getId());
        BpoTaskPushService.pushDelegatedTasks(em, freshChecklist);
    }

    // ======================== Helpers ========================

    /** Parse a comma-separated string of longs. Returns empty array for null/blank input. */
    private long[] parseLongCsv(String csv) {
        if (csv == null || csv.isBlank()) return new long[0];
        String[] parts = csv.split(",");
        long[] result = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Long.parseLong(parts[i].trim());
        }
        return result;
    }
}
