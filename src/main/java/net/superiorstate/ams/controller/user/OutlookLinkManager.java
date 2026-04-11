package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.model.general.OutlookUserLink;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * PSP Admin — Outlook Link Manager.
 *
 * CRUD admin for outlook_user_link rows. Links AMS Person records to
 * Microsoft 365 email addresses so the Outlook add-in can authenticate
 * token-based without repeat logins. Each active link carries a 64-char
 * api_token stored in the row and in the add-in's localStorage.
 *
 * GET  → List existing links + PSP users dropdown
 * POST → action=link | action=unlink | action=relink
 */
@WebServlet(name = "OutlookLinkManager", value = "/OutlookLinkManager")
public class OutlookLinkManager extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!isAuthorized(request.getSession())) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            List<OutlookUserLink> links = em.createQuery(
                    "SELECT l FROM OutlookUserLink l JOIN FETCH l.person " +
                    "ORDER BY l.isActive DESC, l.m365Email", OutlookUserLink.class)
                    .getResultList();

            // PSP users for the dropdown — active Person rows with a User account
            // scoped to PSP id 4 (hard-coded elsewhere in the app).
            List<Person> pspUsers = em.createQuery(
                    "SELECT DISTINCT p FROM User u JOIN u.person p " +
                    "WHERE p.psp.id = :pspId AND u.isActive = true " +
                    "ORDER BY p.lastName, p.firstName", Person.class)
                    .setParameter("pspId", 4L)
                    .getResultList();

            request.setAttribute("links", links);
            request.setAttribute("pspUsers", pspUsers);
            request.setAttribute("pageTitle", "Outlook Link Manager");
            request.setAttribute("pageIcon", "bi-envelope-plus");
            request.getRequestDispatcher("/WEB-INF/view/user/outlookLinkManager.jsp")
                    .forward(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        if (!isAuthorized(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
            return;
        }

        switch (action) {
            case "link"   -> handleLink(request, response);
            case "unlink" -> handleUnlink(request, response);
            case "relink" -> handleRelink(request, response);
            default -> response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Actions
    // ═══════════════════════════════════════════════════════════════════

    private void handleLink(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String personIdStr = request.getParameter("personId");
        String m365Email = request.getParameter("m365Email");

        if (personIdStr == null || personIdStr.isBlank()
                || m365Email == null || m365Email.isBlank()) {
            flashError(request, "Person and M365 email are required.");
            response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
            return;
        }

        m365Email = m365Email.trim().toLowerCase();
        if (!m365Email.contains("@")) {
            flashError(request, "Invalid email address.");
            response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long personId = Long.parseLong(personIdStr);
            Person person = em.find(Person.class, personId);
            if (person == null) {
                flashError(request, "Person not found.");
                response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
                return;
            }

            // Check for existing link with this email
            Long existing = em.createQuery(
                            "SELECT COUNT(l) FROM OutlookUserLink l WHERE LOWER(l.m365Email) = :email",
                            Long.class)
                    .setParameter("email", m365Email)
                    .getSingleResult();
            if (existing != null && existing > 0) {
                flashError(request, "A link already exists for " + m365Email + ".");
                response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
                return;
            }

            OutlookUserLink link = new OutlookUserLink();
            link.setPerson(person);
            link.setM365Email(m365Email);
            link.setApiToken(generateToken());
            link.setActive(true);

            em.getTransaction().begin();
            em.persist(link);
            em.getTransaction().commit();

            flashMessage(request,
                    "Linked " + person.getFullName() + " to " + m365Email + ".");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            flashError(request, "Error creating link: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
        response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
    }

    private void handleUnlink(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String linkIdStr = request.getParameter("linkId");
        if (linkIdStr == null || linkIdStr.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long id = Long.parseLong(linkIdStr);
            OutlookUserLink link = em.find(OutlookUserLink.class, id);
            if (link != null) {
                em.getTransaction().begin();
                link.setActive(false);
                em.merge(link);
                em.getTransaction().commit();
                flashMessage(request, "Link for " + link.getM365Email() + " deactivated.");
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            flashError(request, "Error unlinking: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
        response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
    }

    private void handleRelink(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String linkIdStr = request.getParameter("linkId");
        if (linkIdStr == null || linkIdStr.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            Long id = Long.parseLong(linkIdStr);
            OutlookUserLink link = em.find(OutlookUserLink.class, id);
            if (link != null) {
                em.getTransaction().begin();
                link.setApiToken(generateToken());
                link.setActive(true);
                em.merge(link);
                em.getTransaction().commit();
                flashMessage(request,
                        "Token regenerated for " + link.getM365Email() +
                        ". The user must re-open the add-in to pick up the new token.");
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            flashError(request, "Error regenerating token: " + e.getMessage());
        } finally {
            if (em.isOpen()) em.close();
        }
        response.sendRedirect(request.getContextPath() + "/OutlookLinkManager");
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    /** 64 hex chars — two UUIDs concatenated with dashes stripped. */
    private static String generateToken() {
        return (UUID.randomUUID().toString() + UUID.randomUUID().toString())
                .replace("-", "");
    }

    private boolean isAuthorized(HttpSession session) {
        Object isPspAdmin = session.getAttribute("isPspAdmin");
        return Boolean.TRUE.equals(isPspAdmin);
    }

    private void flashMessage(HttpServletRequest request, String msg) {
        request.getSession().setAttribute("outlookLinkMessage", msg);
    }

    private void flashError(HttpServletRequest request, String msg) {
        request.getSession().setAttribute("outlookLinkError", msg);
    }
}
