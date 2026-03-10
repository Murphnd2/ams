package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.controller.authentication.HelpUserLogin;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.service.DatabaseInitializer;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.data.dao.EmailDAO;
import net.superiorstate.ams.data.dao.PersonDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.data.util.EmailTemplate;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.User;
import net.superiorstate.ams.model.general.UserRole;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.summit.archive.Employee;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

/**
 * Unified user creation servlet.
 * One creation path for all user types — role IDs and agency are resolved
 * based on caller context (PSP Admin, Agency Admin, BPO Admin), then
 * fed into a single createUserWithRoles() method.
 */
@WebServlet(name = "CreateUser25", value = "/CreateUser25")
public class CreateUser25 extends HttpServlet {

    // Roles that require an agency assignment
    private static final Set<Integer> AGENCY_ROLES = Set.of(2, 8);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String result;
        try {
            result = handleRequest(request);
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
        boolean isPspUser = Boolean.TRUE.equals(request.getSession().getAttribute("isPspUser"));
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
        boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
        boolean isBpo = Boolean.TRUE.equals(request.getSession().getAttribute("isBpo"));
        boolean isBpoAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoAdmin"));

        if (isBpo || isBpoAdmin) {
            response.sendRedirect("BpoHome");
        } else if (isPspUser || isPspAdmin) {
            response.sendRedirect("ViewHome25");
        } else if (isAgent || isAgencyAdmin) {
            response.sendRedirect("AgentHome");
        } else {
            response.sendRedirect("ViewHome25");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Request Handler — resolves role IDs + agency, then delegates
    // ══════════════════════════════════════════════════════════════════════

    private String handleRequest(HttpServletRequest request) {
        AmsDataGlobal global = (AmsDataGlobal) request.getServletContext().getAttribute("global");
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // ── Common fields ──
            String firstName = request.getParameter("firstName");
            String lastName = request.getParameter("lastName");
            String email = request.getParameter("userEmail");
            String password = request.getParameter("tempPassword");
            if (password == null || password.isBlank()) {
                password = UUID.randomUUID().toString().substring(0, 8);
            }

            if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
                return "First and last name are required.";
            }
            if (!EmailDAO.isValidEmail(email)) {
                return "Invalid email address.";
            }
            if (EntityLookup.getUserById(em, email) != null) {
                return "A user with that email already exists.";
            }

            // ── Resolve caller context ──
            boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
            boolean isBpoAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoAdmin"));
            boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
            Person admin = local.getCurrentPerson();
            String creatorType = request.getParameter("creatorType");
            if (creatorType == null) creatorType = "";

            // ── Resolve role IDs and agency based on caller type ──
            Set<Integer> roleIds = new HashSet<>();
            Long agencyId = null;
            boolean makeManager = false;

            switch (creatorType) {
                case "pspAdmin" -> {
                    if (!isPspAdmin) return "Only PSP Admins can create users this way.";

                    // Collect any checked role checkboxes (may be none if sales-only)
                    String[] roleIdStrs = request.getParameterValues("roleIds");
                    if (roleIdStrs != null) {
                        for (String s : roleIdStrs) {
                            try { roleIds.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
                        }
                    }

                    // Sales Capability — programmatically adds Agent (2) and optionally Agency Admin (8)
                    if ("1".equals(request.getParameter("salesCapability"))) {
                        roleIds.add(2); // Agent
                        String agencyIdStr = request.getParameter("agencyId");
                        if (agencyIdStr == null || agencyIdStr.isBlank()) {
                            return "Please select an agency for the sales role.";
                        }
                        agencyId = Long.parseLong(agencyIdStr);
                        makeManager = "1".equals(request.getParameter("makeManager"));
                        if (makeManager) {
                            roleIds.add(8); // Agency Admin
                        }
                    }

                    // Validate — must have at least one role from either source
                    if (roleIds.isEmpty()) {
                        return "Please select at least one role or enable sales capability.";
                    }
                }
                case "agencyAdmin" -> {
                    if (!isAgencyAdmin) return "Only Agency Admins can create agents.";
                    roleIds.add(2); // Agent
                    Agency agency = findAgencyForUser(em, admin);
                    if (agency == null) return "Could not determine your agency.";
                    agencyId = agency.getId();
                }
                case "bpoAdmin" -> {
                    if (!isBpoAdmin) return "Only BPO Admins can create BPO users.";
                    roleIds.add(103); // BPO User
                    if ("1".equals(request.getParameter("makeBpoAdmin"))) {
                        roleIds.add(102); // BPO Admin
                    }
                }
                default -> { return "Invalid creator type."; }
            }

            // ── Create the user ──
            return createUserWithRoles(em, firstName, lastName, email, password,
                    roleIds, agencyId, makeManager, admin, global, request);

        } finally {
            em.close();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Unified Creation Method
    // ══════════════════════════════════════════════════════════════════════

    private String createUserWithRoles(EntityManager em,
                                        String firstName, String lastName,
                                        String email, String password,
                                        Set<Integer> roleIds, Long agencyId,
                                        boolean makeManager, Person admin,
                                        AmsDataGlobal global,
                                        HttpServletRequest request) {

        // 1. Create Person
        Person newPerson = createPerson(em, firstName, lastName, email, admin);

        // 2. Check if an Employee exists with this email — link if found
        linkEmployeeIfExists(em, newPerson, email);

        // 3. Create User
        User newUser = DatabaseInitializer.createUser(em, newPerson, email, password);

        // 4. Assign all selected roles
        for (int roleId : roleIds) {
            UserRole ur = EntityLookup.getUserRoleById(em, roleId);
            if (ur != null) {
                em.getTransaction().begin();
                newUser.addUserToRole(ur);
                em.persist(newUser);
                em.getTransaction().commit();
            }
        }

        // 5. Agency assignment (if applicable)
        if (agencyId != null) {
            Agency agency = em.find(Agency.class, agencyId);
            if (agency == null) return "Agency not found.";

            em.getTransaction().begin();
            agency.addAgent(newPerson);
            em.merge(agency);
            em.getTransaction().commit();

            if (makeManager) {
                em.getTransaction().begin();
                agency.setManager(newPerson);
                em.merge(agency);
                em.getTransaction().commit();
            }
        }

        // 6. Create TimeEntry
        DatabaseInitializer.createTimeEntry(em, newPerson, null, null);

        // 7. Send welcome email with 7-day GUID
        sendWelcomeEmail(em, newUser, firstName, global);

        // 8. Refresh global caches
        refreshGlobalCaches(em, global, roleIds);
        request.getServletContext().setAttribute("global", global);

        return null; // success
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ══════════════════════════════════════════════════════════════════════

    private Person createPerson(EntityManager em, String firstName, String lastName, String email, Person admin) {
        PSP psp = EntityLookup.getPspById(em, 4L);
        em.getTransaction().begin();
        Person p = new Person();
        p.setPsp(psp);
        p.setEmail(email.trim().toLowerCase());
        String capFirst = capitalCase(firstName);
        String capLast = capitalCase(lastName);
        p.setFirstName(capFirst);
        p.setLastName(capLast);
        p.setFullName(capFirst + " " + capLast);
        p.setAddress(admin.getAddress());
        em.persist(p);
        em.getTransaction().commit();
        return p;
    }

    private void linkEmployeeIfExists(EntityManager em, Person person, String email) {
        Employee ee = PersonDAO.getEmployeeByEmail(em, email);
        if (ee != null) {
            em.getTransaction().begin();
            person.setEmployee(ee);
            em.merge(person);
            em.getTransaction().commit();
        }
    }

    private Agency findAgencyForUser(EntityManager em, Person user) {
        Query q = em.createQuery("SELECT a FROM Agency a WHERE a.manager.id = :userId");
        q.setParameter("userId", user.getId());
        try {
            return (Agency) q.getSingleResult();
        } catch (NoResultException ignored) {}

        Query q2 = em.createQuery("SELECT a FROM Agency a JOIN a.agentList ag WHERE ag.id = :userId");
        q2.setParameter("userId", user.getId());
        try {
            List<Agency> agencies = q2.getResultList();
            if (!agencies.isEmpty()) return agencies.get(0);
        } catch (NoResultException ignored) {}

        return null;
    }

    private void sendWelcomeEmail(EntityManager em, User newUser, String firstName, AmsDataGlobal global) {
        try {
            // Generate GUID with 7-day expiration
            String guid = UUID.randomUUID().toString();
            em.getTransaction().begin();
            newUser.setTempGuid(guid);
            newUser.setGuidExpiration(Date.valueOf(LocalDate.now().plusDays(7)));
            newUser.setGuidUsed(false);
            newUser.setAllowSetPassword(true);
            em.merge(newUser);
            em.getTransaction().commit();

            // Build link — use WEB_PATH constant, fallback to email domain
            String webPath = AppConstantDAO.getConstantValue(em, "WEB_PATH");
            if (webPath == null || webPath.isBlank()) {
                String emailDomain = newUser.getEmail().substring(newUser.getEmail().indexOf("@") + 1);
                webPath = "https://" + emailDomain;
            }
            if (webPath.endsWith("/")) webPath = webPath.substring(0, webPath.length() - 1);
            String resetLink = webPath + "/OneTimeUserLogin?guid=" + guid;

            // Build branded email body
            String pspName = global.getPsp().getFullName();
            String body = "<p>Hello " + firstName + ",</p>"
                + "<p>Your account has been created at <strong>" + pspName + "</strong>. "
                + "Click the button below to set your password and log in:</p>"
                + "<p style=\"text-align:center; margin:24px 0;\">"
                + "<a href=\"" + resetLink + "\" style=\"background:#0d5681; color:white; "
                + "padding:12px 28px; text-decoration:none; border-radius:6px; font-weight:bold;\">"
                + "Set My Password</a></p>"
                + "<p style=\"font-size:0.85rem; color:#6c757d;\">This link expires in 7 days. "
                + "If you did not expect this email, you can safely ignore it.</p>";

            String wrappedBody = EmailTemplate.wrapBodyOnly(body, pspName, em);

            EmailDAO.sendEmail("noreply@superiorstate.net", newUser.getEmail(),
                    "Your Account Has Been Created", wrappedBody, em);
            System.out.println("✅ Welcome email sent to " + newUser.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to send welcome email to " + newUser.getEmail() + ": " + e.getMessage());
        }
    }

    private void refreshGlobalCaches(EntityManager em, AmsDataGlobal global, Set<Integer> roleIds) {
        // Refresh BPO user list if any BPO role was assigned
        boolean hasBpoRole = roleIds.contains(102) || roleIds.contains(103);
        if (hasBpoRole) {
            List<Person> allBpo = new ArrayList<>();
            allBpo.addAll(net.superiorstate.ams.controller.authentication.AuthenticateUser.getUsersByRole(em, 102));
            for (Person p : net.superiorstate.ams.controller.authentication.AuthenticateUser.getUsersByRole(em, 103)) {
                if (!allBpo.contains(p)) allBpo.add(p);
            }
            Collections.sort(allBpo);
            global.setBpoUsers(allBpo);
        }

        // Refresh PSP user list if any non-agency, non-BPO role was assigned
        boolean hasPspRole = roleIds.stream().anyMatch(id -> !AGENCY_ROLES.contains(id) && id < 100);
        if (hasPspRole) {
            // addUser refreshes the internal user list
        }
    }

    /** Capitalize first letter, lowercase the rest. e.g. "KEVIN" → "Kevin" */
    private String capitalCase(String s) {
        if (s == null || s.isBlank()) return s;
        String t = s.trim().toLowerCase();
        return t.substring(0, 1).toUpperCase() + t.substring(1);
    }
}
