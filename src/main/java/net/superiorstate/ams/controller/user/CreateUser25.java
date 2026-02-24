package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.service.DatabaseInitializer;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.dao.SalesDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.User;
import net.superiorstate.ams.model.general.UserRole;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.summit.archive.Employee;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Unified user creation servlet. Handles PSP User, Agent, and BPO User creation
 * with role-based gating:
 * <ul>
 *   <li>PSP Admin → can create PSP Users (role 1, optional role 5) and Agents (role 2, optional role 8 + agency manager)</li>
 *   <li>Agency Admin → can create Agents (role 2) for their own agency</li>
 *   <li>BPO Admin → can create BPO Users (role 103, optional role 102)</li>
 * </ul>
 */
@WebServlet(name = "CreateUser25", value = "/CreateUser25")
public class CreateUser25 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String result;
        try {
            result = createUser(request);
        } catch (Exception e) {
            e.printStackTrace();
            result = "Error creating user: " + e.getMessage();
        }

        if (result != null) {
            request.getSession().setAttribute("createUserError", result);
        } else {
            request.getSession().setAttribute("createUserSuccess", "User created successfully.");
        }

        goToHome(request, response);
    }

    private void goToHome(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean isBpo = Boolean.TRUE.equals(request.getSession().getAttribute("isBpo"));
        boolean isBpoAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoAdmin"));
        boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));

        if (isBpo || isBpoAdmin) {
            response.sendRedirect("BpoHome");
        } else if (isAgencyAdmin) {
            response.sendRedirect("AgentHome");
        } else {
            response.sendRedirect("ViewHome25");
        }
    }

    /**
     * Main creation method. Returns null on success, error message string on failure.
     */
    private String createUser(HttpServletRequest request) throws NoSuchAlgorithmException {
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // ── Read common form fields ──
            String firstName = request.getParameter("firstName");
            String lastName = request.getParameter("lastName");
            String email = request.getParameter("userEmail");
            String password = request.getParameter("tempPassword");
            String userType = request.getParameter("userType"); // psp, agent, bpo

            if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
                return "First and last name are required.";
            }
            if (!EmailDAO.isValidEmail(email)) {
                return "Invalid email address.";
            }
            if (EntityLookup.getUserById(em, email) != null) {
                return "A user with that email already exists.";
            }

            // ── Role gate: verify caller has permission ──
            boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
            boolean isBpoAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoAdmin"));
            boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
            Person admin = local.getCurrentPerson();

            if (userType == null) userType = "psp"; // default

            switch (userType) {
                case "psp" -> {
                    if (!isPspAdmin) return "Only PSP Admins can create PSP users.";
                    return createPspUser(em, request, admin, global);
                }
                case "agent" -> {
                    if (!isPspAdmin && !isAgencyAdmin) return "Only PSP Admins or Agency Admins can create agents.";
                    return createAgentUser(em, request, admin, global, isPspAdmin, isAgencyAdmin);
                }
                case "bpo" -> {
                    if (!isBpoAdmin) return "Only BPO Admins can create BPO users.";
                    return createBpoUser(em, request, admin, global);
                }
                default -> {
                    return "Invalid user type.";
                }
            }
        } finally {
            em.close();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // PSP User Creation
    // ══════════════════════════════════════════════════════════════════════

    private String createPspUser(EntityManager em, HttpServletRequest request, Person admin, AmsDataGlobal global) throws NoSuchAlgorithmException {
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String email = request.getParameter("userEmail");
        String password = request.getParameter("tempPassword");

        // Create Employee → Person → User
        Employee ee = getOrCreateEmployeeForUser(em, email, lastName, firstName, admin);
        Person newPerson = getOrCreatePersonFromEmployee(em, ee, admin);

        User u = DatabaseInitializer.createUser(em, newPerson, email, password);

        // Assign PSP User role (1)
        UserRole pspUserRole = EntityLookup.getUserRoleById(em, 1);
        em.getTransaction().begin();
        u.addUserToRole(pspUserRole);
        em.persist(u);
        em.getTransaction().commit();

        // Optional: Make PSP Admin (role 5)
        String makeAdmin = request.getParameter("makeAdmin");
        if ("1".equals(makeAdmin)) {
            UserRole adminRole = EntityLookup.getUserRoleById(em, 5);
            em.getTransaction().begin();
            u.addUserToRole(adminRole);
            em.persist(u);
            em.getTransaction().commit();
        }

        DatabaseInitializer.createTimeEntry(em, newPerson, null, null);
        global.addUser(newPerson);
        request.getServletContext().setAttribute("global", global);

        return null; // success
    }

    // ══════════════════════════════════════════════════════════════════════
    // Agent Creation
    // ══════════════════════════════════════════════════════════════════════

    private String createAgentUser(EntityManager em, HttpServletRequest request, Person admin, AmsDataGlobal global,
                                   boolean isPspAdmin, boolean isAgencyAdmin) throws NoSuchAlgorithmException {
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String email = request.getParameter("userEmail");
        String password = request.getParameter("tempPassword");

        // Determine agency
        Agency agency;
        if (isPspAdmin) {
            // PSP Admin picks the agency from dropdown
            String agencyIdStr = request.getParameter("agencyId");
            if (agencyIdStr == null || agencyIdStr.isBlank()) {
                return "Please select an agency.";
            }
            long agencyId = Long.parseLong(agencyIdStr);
            agency = em.find(Agency.class, agencyId);
            if (agency == null) return "Agency not found.";
        } else {
            // Agency Admin — find their agency
            agency = findAgencyForUser(em, admin);
            if (agency == null) return "Could not determine your agency.";
        }

        // Create Person (no Employee needed for agents — they're external)
        PSP psp = EntityLookup.getPspById(em, 4L);
        Person newPerson = createAgentPerson(em, firstName, lastName, email, psp, admin);

        // Create User with Agent role (2)
        User u = DatabaseInitializer.createUser(em, newPerson, email, password);
        UserRole agentRole = EntityLookup.getUserRoleById(em, 2);
        em.getTransaction().begin();
        u.addUserToRole(agentRole);
        em.persist(u);
        em.getTransaction().commit();

        // Optional: Make Agency Admin (role 8) — only PSP Admin can set this
        String makeAgencyAdmin = request.getParameter("makeAgencyAdmin");
        if ("1".equals(makeAgencyAdmin) && isPspAdmin) {
            UserRole agencyAdminRole = EntityLookup.getUserRoleById(em, 8);
            em.getTransaction().begin();
            u.addUserToRole(agencyAdminRole);
            em.persist(u);
            em.getTransaction().commit();
        }

        // Add agent to agency's agent list
        em.getTransaction().begin();
        agency.addAgent(newPerson);
        em.merge(agency);
        em.getTransaction().commit();

        // Optional: Make Agency Manager — only PSP Admin can set this
        String makeManager = request.getParameter("makeManager");
        if ("1".equals(makeManager) && isPspAdmin) {
            em.getTransaction().begin();
            agency.setManager(newPerson);
            em.merge(agency);
            em.getTransaction().commit();
        }

        DatabaseInitializer.createTimeEntry(em, newPerson, null, null);

        return null; // success
    }

    private Person createAgentPerson(EntityManager em, String firstName, String lastName, String email, PSP psp, Person admin) {
        em.getTransaction().begin();
        Person p = new Person();
        p.setPsp(psp);
        p.setEmail(email.trim().toLowerCase());
        p.setFirstName(firstName.trim().toUpperCase());
        p.setLastName(lastName.trim().toUpperCase());
        p.setFullName(firstName.trim().toUpperCase() + " " + lastName.trim().toUpperCase());
        p.setAddress(admin.getAddress());
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    // BPO User Creation
    // ══════════════════════════════════════════════════════════════════════

    private String createBpoUser(EntityManager em, HttpServletRequest request, Person admin, AmsDataGlobal global) throws NoSuchAlgorithmException {
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String email = request.getParameter("userEmail");
        String password = request.getParameter("tempPassword");

        // Create Person (no Employee for BPO users)
        PSP psp = EntityLookup.getPspById(em, 4L);
        em.getTransaction().begin();
        Person newPerson = new Person();
        newPerson.setPsp(psp);
        newPerson.setEmail(email.trim().toLowerCase());
        newPerson.setFirstName(firstName.trim().toUpperCase());
        newPerson.setLastName(lastName.trim().toUpperCase());
        newPerson.setFullName(firstName.trim().toUpperCase() + " " + lastName.trim().toUpperCase());
        newPerson.setAddress(admin.getAddress());
        em.persist(newPerson);
        em.getTransaction().commit();

        // Create User with BPO User role (103)
        User u = DatabaseInitializer.createUser(em, newPerson, email, password);
        UserRole bpoUserRole = EntityLookup.getUserRoleById(em, 103);
        em.getTransaction().begin();
        u.addUserToRole(bpoUserRole);
        em.persist(u);
        em.getTransaction().commit();

        // Optional: Make BPO Admin (role 102)
        String makeBpoAdmin = request.getParameter("makeBpoAdmin");
        if ("1".equals(makeBpoAdmin)) {
            UserRole bpoAdminRole = EntityLookup.getUserRoleById(em, 102);
            em.getTransaction().begin();
            u.addUserToRole(bpoAdminRole);
            em.persist(u);
            em.getTransaction().commit();
        }

        DatabaseInitializer.createTimeEntry(em, newPerson, null, null);

        // Refresh BPO user list in global
        refreshBpoUsers(em, global);
        request.getServletContext().setAttribute("global", global);

        return null; // success
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ══════════════════════════════════════════════════════════════════════

    private Agency findAgencyForUser(EntityManager em, Person user) {
        // Check if user is a manager of an agency
        Query q = em.createQuery("SELECT a FROM Agency a WHERE a.manager.id = :userId");
        q.setParameter("userId", user.getId());
        try {
            return (Agency) q.getSingleResult();
        } catch (NoResultException ignored) {}

        // Check if user is an agent in an agency
        Query q2 = em.createQuery("SELECT a FROM Agency a JOIN a.agentList ag WHERE ag.id = :userId");
        q2.setParameter("userId", user.getId());
        try {
            List<Agency> agencies = q2.getResultList();
            if (!agencies.isEmpty()) return agencies.get(0);
        } catch (NoResultException ignored) {}

        return null;
    }

    private void refreshBpoUsers(EntityManager em, AmsDataGlobal global) {
        List<Person> allBpo = new java.util.ArrayList<>();
        allBpo.addAll(net.superiorstate.ams.controller.authentication.AuthenticateUser.getUsersByRole(em, 101));
        for (Person p : net.superiorstate.ams.controller.authentication.AuthenticateUser.getUsersByRole(em, 102)) {
            if (!allBpo.contains(p)) allBpo.add(p);
        }
        for (Person p : net.superiorstate.ams.controller.authentication.AuthenticateUser.getUsersByRole(em, 103)) {
            if (!allBpo.contains(p)) allBpo.add(p);
        }
        java.util.Collections.sort(allBpo);
        global.setBpoUsers(allBpo);
    }

    private Person getOrCreatePersonFromEmployee(EntityManager em, Employee ee, Person admin) {
        Person p;
        Query q = em.createQuery("SELECT p FROM PersonV p WHERE p.employee is null AND p.email = :email");
        q.setParameter("email", ee.getEmail().trim().toLowerCase());
        List<Person> personList;
        try {
            personList = q.getResultList();
        } catch (NoResultException e) {
            personList = null;
        }

        if (personList == null || personList.isEmpty()) {
            PSP psp = EntityLookup.getPspById(em, 4L);
            em.getTransaction().begin();
            p = new Person();
            p.setPsp(psp);
            p.setEmployee(ee);
            p.setEmail(ee.getEmail());
            p.setAddress(admin.getAddress());
            p.setLastName(ee.getLastName().toUpperCase());
            p.setFullName(ee.getFirstName().toUpperCase() + " " + ee.getLastName().toUpperCase());
            p.setFirstName(ee.getFirstName().toUpperCase());
        } else {
            p = personList.get(0);
            em.getTransaction().begin();
            p.setEmployee(ee);
            p.setFirstName(ee.getFirstName().toUpperCase());
            p.setLastName(ee.getLastName().toUpperCase());
            p.setFullName(p.getFirstName() + " " + p.getLastName());
        }
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    private Employee getOrCreateEmployeeForUser(EntityManager em, String email, String lastName, String firstName, Person admin) {
        // Does Employee already exist by email?
        Employee ee = EmailDAO.getEmployeeByEmail(em, email, admin);
        if (ee != null) return ee;

        // By name?
        Query q = em.createQuery("SELECT e FROM EmployeeV e WHERE e.lastName=:lName and e.firstName=:fName");
        q.setParameter("lName", lastName.trim().toUpperCase());
        q.setParameter("fName", firstName.trim().toUpperCase());
        List<Employee> employeeList;
        try {
            employeeList = q.getResultList();
        } catch (NoResultException e) {
            employeeList = null;
        }
        if (employeeList != null && !employeeList.isEmpty()) {
            for (Employee employee : employeeList) {
                if (employee.getEmployer().getId() == admin.getEmployee().getEmployer().getId())
                    return employee;
            }
        }

        // Create new Employee
        int eeId = getNewEmployeeId(em);
        em.getTransaction().begin();
        ee = new Employee();
        ee.setEmployer(admin.getEmployee().getEmployer());
        ee.setLastName(lastName.toUpperCase());
        ee.setFirstName(firstName.toUpperCase());
        ee.setEmail(email);
        ee.setAddress1(admin.getAddress().getAddress1());
        ee.setAddress2(admin.getAddress().getAddress2());
        ee.setCity(admin.getAddress().getCity());
        ee.setState(admin.getAddress().getState());
        ee.setZipCode(admin.getAddress().getZipCode());
        ee.setId(eeId);
        ee.setActive(true);
        em.persist(ee);
        em.getTransaction().commit();
        return ee;
    }

    private int getNewEmployeeId(EntityManager em) {
        Query q = em.createQuery("SELECT e FROM Employee e ORDER BY e.id");
        List<Employee> employeeList = q.getResultList();
        return employeeList.get(0).getId() - 1;
    }
}
