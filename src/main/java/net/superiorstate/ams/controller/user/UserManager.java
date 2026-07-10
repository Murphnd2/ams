package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.User;
import net.superiorstate.ams.model.general.UserRole;
import net.superiorstate.ams.model.sales.agency.Agency;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * User Manager — PSP Admin modal for creating and managing users.
 *
 * GET  → JSON: user list (for Manage Users tab) or getCounts
 * POST → AJAX JSON handler for management actions (deactivate, reactivate, role changes)
 */
@WebServlet(name = "UserManager", value = "/UserManager")
public class UserManager extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isPspAdmin(request)) {
            sendJson(response, 403, "{\"error\":\"Forbidden\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Route getCounts via GET (read-only operation)
            String action = request.getParameter("action");
            if ("getCounts".equals(action)) {
                handleGetCounts(request, response, em);
                return;
            }

            // Default: return full user list as JSON for the Manage Users tab
            List<User> allUsers = em.createQuery(
                    "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userRoleList " +
                    "WHERE u.person.psp.id = :pspId " +
                    "ORDER BY u.person.lastName, u.person.firstName", User.class)
                    .setParameter("pspId", 4L)
                    .getResultList();

            Agency homeAgency = findHomeAgency(em);
            long homeAgencyId = homeAgency != null ? homeAgency.getId() : -1;

            // Collect home agency agent person IDs
            Set<Long> homeAgentIds = new HashSet<>();
            if (homeAgency != null && homeAgency.getAgentList() != null) {
                for (Person p : homeAgency.getAgentList()) {
                    homeAgentIds.add(p.getId());
                }
            }

            // Build map of personId -> agencyIds for reassignment filtering
            Map<Long, Set<Long>> personAgencyMap = new HashMap<>();
            @SuppressWarnings("unchecked")
            List<Object[]> agencyAgents = em.createQuery(
                    "SELECT ag.id, a.id FROM Agency a JOIN a.agentList ag WHERE a.psp.id = :pspId")
                    .setParameter("pspId", 4L)
                    .getResultList();
            for (Object[] row : agencyAgents) {
                long agentPersonId = (long) row[0];
                long agencyId = (long) row[1];
                personAgencyMap.computeIfAbsent(agentPersonId, k -> new HashSet<>()).add(agencyId);
            }

            // Build JSON response
            StringBuilder sb = new StringBuilder();
            sb.append("{\"homeAgencyId\":").append(homeAgencyId);
            sb.append(",\"users\":[");

            boolean first = true;
            for (User u : allUsers) {
                // Skip users whose only roles are BPO (102/103)
                boolean hasPspRole = false;
                if (u.getUserRoleList() != null) {
                    for (UserRole r : u.getUserRoleList()) {
                        if (r.getId() != 102 && r.getId() != 103) {
                            hasPspRole = true;
                            break;
                        }
                    }
                }
                if (!hasPspRole && u.getUserRoleList() != null && !u.getUserRoleList().isEmpty()) continue;

                if (!first) sb.append(",");
                first = false;

                long pid = u.getPerson().getId();
                String firstName = u.getPerson().getFirstName() != null ? u.getPerson().getFirstName() : "";
                String lastName = u.getPerson().getLastName() != null ? u.getPerson().getLastName() : "";

                sb.append("{\"personId\":").append(pid);
                sb.append(",\"name\":\"").append(escapeJson(lastName + ", " + firstName)).append("\"");
                sb.append(",\"email\":\"").append(escapeJson(u.getEmail() != null ? u.getEmail() : "")).append("\"");
                sb.append(",\"active\":").append(u.isActive());
                sb.append(",\"homeAgent\":").append(homeAgentIds.contains(pid));

                // Roles array (exclude BPO roles from display)
                sb.append(",\"roles\":[");
                boolean firstRole = true;
                if (u.getUserRoleList() != null) {
                    for (UserRole r : u.getUserRoleList()) {
                        if (r.getId() == 102 || r.getId() == 103) continue;
                        if (!firstRole) sb.append(",");
                        firstRole = false;
                        sb.append("{\"id\":").append(r.getId());
                        sb.append(",\"desc\":\"").append(escapeJson(r.getDescription())).append("\"}");
                    }
                }
                sb.append("]");

                // Agency memberships for reassignment filtering
                Set<Long> agIds = personAgencyMap.getOrDefault(pid, Set.of());
                sb.append(",\"agencyIds\":[");
                boolean firstAg = true;
                for (Long agId : agIds) {
                    if (!firstAg) sb.append(",");
                    firstAg = false;
                    sb.append(agId);
                }
                sb.append("]");

                sb.append("}");
            }

            sb.append("]}");
            sendJson(response, 200, sb.toString());

        } finally {
            em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isPspAdmin(request)) {
            sendJson(response, 403, "{\"error\":\"Forbidden\"}");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            sendJson(response, 400, "{\"error\":\"Missing action\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            switch (action) {
                case "getCounts" -> handleGetCounts(request, response, em);
                case "deactivate" -> handleDeactivate(request, response, em);
                case "reactivate" -> handleReactivate(request, response, em);
                case "addAgentRole" -> handleAddAgentRole(request, response, em);
                case "removeAgentRole" -> handleRemoveAgentRole(request, response, em);
                case "addPspUserRole" -> handleAddPspUserRole(request, response, em);
                default -> sendJson(response, 400, "{\"error\":\"Unknown action\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            sendJson(response, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            em.close();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // GET COUNTS — Returns reassignment counts for a given person
    // ══════════════════════════════════════════════════════════════════════

    private void handleGetCounts(HttpServletRequest request, HttpServletResponse response,
                                  EntityManager em) throws IOException {
        long personId = Long.parseLong(request.getParameter("personId"));

        long openActivities = countQuery(em,
                "SELECT COUNT(a) FROM Activity a WHERE a.assignedTo.id = :pid AND a.isComplete = false",
                personId);

        long managedOpportunities = countQuery(em,
                "SELECT COUNT(o) FROM Opportunity o WHERE o.managedBy.id = :pid AND o.isComplete = false",
                personId);

        long delegatedTodos = countQuery(em,
                "SELECT COUNT(t) FROM ToDo t WHERE t.bpoAssignedTo.id = :pid AND t.isComplete = false",
                personId);

        long ownedTasks = countQuery(em,
                "SELECT COUNT(t) FROM Task t WHERE t.owner.id = :pid",
                personId);

        sendJson(response, 200, "{" +
                "\"openActivities\":" + openActivities + "," +
                "\"managedOpportunities\":" + managedOpportunities + "," +
                "\"delegatedTodos\":" + delegatedTodos + "," +
                "\"ownedTasks\":" + ownedTasks +
                "}");
    }

    // ══════════════════════════════════════════════════════════════════════
    // DEACTIVATE — Reassign and deactivate a user
    // ══════════════════════════════════════════════════════════════════════

    private void handleDeactivate(HttpServletRequest request, HttpServletResponse response,
                                   EntityManager em) throws IOException {
        long personId = Long.parseLong(request.getParameter("personId"));
        long targetPersonId = Long.parseLong(request.getParameter("targetPersonId"));

        // Safety: cannot deactivate yourself
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        if (local.getCurrentPerson().getId() == personId) {
            sendJson(response, 400, "{\"error\":\"You cannot deactivate yourself.\"}");
            return;
        }

        User user = findUserByPersonId(em, personId);
        if (user == null) {
            sendJson(response, 404, "{\"error\":\"User not found.\"}");
            return;
        }

        // Safety: cannot deactivate the last PSP Admin
        if (hasRole(user, 5)) {
            long adminCount = countQuery(em,
                    "SELECT COUNT(DISTINCT u.person.id) FROM User u JOIN u.userRoleList ur " +
                    "WHERE ur.id = 5 AND u.isActive = true AND u.person.psp.id = :pid",
                    4L);
            if (adminCount <= 1) {
                sendJson(response, 400, "{\"error\":\"Cannot deactivate the last PSP Admin.\"}");
                return;
            }
        }

        Person target = EntityLookup.getPersonById(em, targetPersonId);
        if (target == null) {
            sendJson(response, 404, "{\"error\":\"Target user not found.\"}");
            return;
        }

        em.getTransaction().begin();

        // Reassign open activities
        em.createQuery("UPDATE Activity a SET a.assignedTo = :target " +
                        "WHERE a.assignedTo.id = :source AND a.isComplete = false")
                .setParameter("target", target)
                .setParameter("source", personId)
                .executeUpdate();

        // Reassign managed opportunities
        em.createQuery("UPDATE Opportunity o SET o.managedBy = :target " +
                        "WHERE o.managedBy.id = :source AND o.isComplete = false")
                .setParameter("target", target)
                .setParameter("source", personId)
                .executeUpdate();

        // Reassign delegated todos
        em.createQuery("UPDATE ToDo t SET t.bpoAssignedTo = :target " +
                        "WHERE t.bpoAssignedTo.id = :source AND t.isComplete = false")
                .setParameter("target", target)
                .setParameter("source", personId)
                .executeUpdate();

        // Transfer task ownership
        em.createQuery("UPDATE Task t SET t.owner = :target WHERE t.owner.id = :source")
                .setParameter("target", target)
                .setParameter("source", personId)
                .executeUpdate();

        // Clear agency manager if applicable
        em.createQuery("UPDATE Agency a SET a.manager = null WHERE a.manager.id = :pid")
                .setParameter("pid", personId)
                .executeUpdate();

        // Deactivate user
        user.setActive(false);
        em.merge(user);

        em.getTransaction().commit();

        // Refresh global caches
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        global.refreshUserCaches(em);
        getServletContext().setAttribute("global", global);

        sendJson(response, 200, "{\"success\":true}");
    }

    // ══════════════════════════════════════════════════════════════════════
    // REACTIVATE — Reactivate a deactivated user
    // ══════════════════════════════════════════════════════════════════════

    private void handleReactivate(HttpServletRequest request, HttpServletResponse response,
                                   EntityManager em) throws IOException {
        long personId = Long.parseLong(request.getParameter("personId"));

        User user = findUserByPersonId(em, personId);
        if (user == null) {
            sendJson(response, 404, "{\"error\":\"User not found.\"}");
            return;
        }

        em.getTransaction().begin();
        user.setActive(true);
        em.merge(user);
        em.getTransaction().commit();

        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        global.refreshUserCaches(em);
        getServletContext().setAttribute("global", global);

        sendJson(response, 200, "{\"success\":true}");
    }

    // ══════════════════════════════════════════════════════════════════════
    // ADD AGENT ROLE — Auto-assign to PSP home agency
    // ══════════════════════════════════════════════════════════════════════

    private void handleAddAgentRole(HttpServletRequest request, HttpServletResponse response,
                                     EntityManager em) throws IOException {
        long personId = Long.parseLong(request.getParameter("personId"));

        User user = findUserByPersonId(em, personId);
        if (user == null) {
            sendJson(response, 404, "{\"error\":\"User not found.\"}");
            return;
        }
        if (hasRole(user, 2)) {
            sendJson(response, 400, "{\"error\":\"User already has the Agent role.\"}");
            return;
        }

        Agency homeAgency = findHomeAgency(em);
        if (homeAgency == null) {
            sendJson(response, 400, "{\"error\":\"No PSP home agency found.\"}");
            return;
        }

        UserRole agentRole = EntityLookup.getUserRoleById(em, 2);
        if (agentRole == null) {
            sendJson(response, 500, "{\"error\":\"Agent role not found.\"}");
            return;
        }

        em.getTransaction().begin();
        user.addUserToRole(agentRole);
        em.merge(user);

        // Add person to home agency's agent list
        homeAgency = em.find(Agency.class, homeAgency.getId());
        homeAgency.addAgent(user.getPerson());
        em.merge(homeAgency);

        em.getTransaction().commit();

        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        global.refreshUserCaches(em);
        global.refreshSalesData(em);
        getServletContext().setAttribute("global", global);

        sendJson(response, 200, "{\"success\":true}");
    }

    // ══════════════════════════════════════════════════════════════════════
    // REMOVE AGENT ROLE — Remove from home agency, reassign opportunities
    // ══════════════════════════════════════════════════════════════════════

    private void handleRemoveAgentRole(HttpServletRequest request, HttpServletResponse response,
                                        EntityManager em) throws IOException {
        long personId = Long.parseLong(request.getParameter("personId"));
        long targetPersonId = Long.parseLong(request.getParameter("targetPersonId"));

        User user = findUserByPersonId(em, personId);
        if (user == null) {
            sendJson(response, 404, "{\"error\":\"User not found.\"}");
            return;
        }
        if (!hasRole(user, 2)) {
            sendJson(response, 400, "{\"error\":\"User does not have the Agent role.\"}");
            return;
        }

        Person target = EntityLookup.getPersonById(em, targetPersonId);
        if (target == null) {
            sendJson(response, 404, "{\"error\":\"Target user not found.\"}");
            return;
        }

        em.getTransaction().begin();

        // Reassign open opportunities where this person is managedBy
        em.createQuery("UPDATE Opportunity o SET o.managedBy = :target " +
                        "WHERE o.managedBy.id = :source AND o.isComplete = false")
                .setParameter("target", target)
                .setParameter("source", personId)
                .executeUpdate();

        // Remove Agent role (2)
        UserRole agentRole = EntityLookup.getUserRoleById(em, 2);
        if (agentRole != null) {
            user.removeUserFromRole(agentRole);
        }

        // Also remove Agency Admin role (8) if present — meaningless without Agent
        if (hasRole(user, 8)) {
            UserRole agencyAdminRole = EntityLookup.getUserRoleById(em, 8);
            if (agencyAdminRole != null) {
                user.removeUserFromRole(agencyAdminRole);
            }
        }
        em.merge(user);

        // Remove from home agency
        Agency homeAgency = findHomeAgency(em);
        if (homeAgency != null) {
            homeAgency = em.find(Agency.class, homeAgency.getId());
            homeAgency.removeAgent(user.getPerson());
            em.merge(homeAgency);

            // Clear manager if this person was the manager
            if (homeAgency.getManager() != null && homeAgency.getManager().getId().equals(personId)) {
                homeAgency.setManager(null);
                em.merge(homeAgency);
            }
        }

        em.getTransaction().commit();

        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        global.refreshUserCaches(em);
        global.refreshSalesData(em);
        getServletContext().setAttribute("global", global);

        sendJson(response, 200, "{\"success\":true}");
    }

    // ══════════════════════════════════════════════════════════════════════
    // ADD PSP USER ROLE — Expand agent to include PSP User role
    // ══════════════════════════════════════════════════════════════════════

    private void handleAddPspUserRole(HttpServletRequest request, HttpServletResponse response,
                                       EntityManager em) throws IOException {
        long personId = Long.parseLong(request.getParameter("personId"));

        User user = findUserByPersonId(em, personId);
        if (user == null) {
            sendJson(response, 404, "{\"error\":\"User not found.\"}");
            return;
        }
        if (hasRole(user, 1)) {
            sendJson(response, 400, "{\"error\":\"User already has the PSP User role.\"}");
            return;
        }

        UserRole pspUserRole = EntityLookup.getUserRoleById(em, 1);
        if (pspUserRole == null) {
            sendJson(response, 500, "{\"error\":\"PSP User role not found.\"}");
            return;
        }

        em.getTransaction().begin();
        user.addUserToRole(pspUserRole);
        em.merge(user);
        em.getTransaction().commit();

        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        global.refreshUserCaches(em);
        getServletContext().setAttribute("global", global);

        sendJson(response, 200, "{\"success\":true}");
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ══════════════════════════════════════════════════════════════════════

    private boolean isPspAdmin(HttpServletRequest request) {
        Boolean admin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        return admin != null && admin;
    }

    private User findUserByPersonId(EntityManager em, long personId) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.person.id = :pid", User.class)
                    .setParameter("pid", personId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private boolean hasRole(User user, int roleId) {
        if (user.getUserRoleList() == null) return false;
        for (UserRole ur : user.getUserRoleList()) {
            if (ur.getId() == roleId) return true;
        }
        return false;
    }

    private Agency findHomeAgency(EntityManager em) {
        try {
            List<Agency> agencies = em.createQuery(
                    "SELECT a FROM Agency a WHERE a.psp.id = :pspId ORDER BY a.id",
                    Agency.class)
                    .setParameter("pspId", 4L)
                    .getResultList();
            return agencies.isEmpty() ? null : agencies.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private long countQuery(EntityManager em, String jpql, long paramValue) {
        try {
            return (long) em.createQuery(jpql)
                    .setParameter("pid", paramValue)
                    .getSingleResult();
        } catch (Exception e) {
            return 0;
        }
    }

    private void sendJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
