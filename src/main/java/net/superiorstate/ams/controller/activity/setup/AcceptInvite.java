package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.User;
import net.superiorstate.ams.model.general.UserRole;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Invitation;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WebServlet(name = "AcceptInvite", value = "/AcceptInvite")
public class AcceptInvite extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String guid = request.getParameter("guid");

        if (guid == null || guid.isEmpty()) {
            request.setAttribute("inviteError", "No invitation token provided.");
            request.getRequestDispatcher("/WEB-INF/view/sales/acceptInvite.jsp").forward(request, response);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Invitation invitation = findInvitation(em, guid);

            if (invitation == null) {
                request.setAttribute("inviteError", "This invitation link is not valid.");
            } else if (invitation.isUsed()) {
                request.setAttribute("inviteError", "This invitation has already been used.");
            } else if (invitation.getDateExpires().before(Timestamp.valueOf(LocalDateTime.now()))) {
                request.setAttribute("inviteError", "This invitation has expired. Please contact your administrator for a new one.");
            } else {
                request.setAttribute("invitation", invitation);
                request.setAttribute("agency", invitation.getAgency());
                request.setAttribute("isManager", "AGENCY_MANAGER".equals(invitation.getRole()));
            }
        } finally {
            em.close();
        }

        request.getRequestDispatcher("/WEB-INF/view/sales/acceptInvite.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String guid = request.getParameter("guid");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Invitation invitation = findInvitation(em, guid);

            // Re-validate
            if (invitation == null || invitation.isUsed() ||
                    invitation.getDateExpires().before(Timestamp.valueOf(LocalDateTime.now()))) {
                request.setAttribute("inviteError", "This invitation is no longer valid.");
                request.getRequestDispatcher("/WEB-INF/view/sales/acceptInvite.jsp").forward(request, response);
                return;
            }

            String firstName = request.getParameter("firstName").trim();
            String lastName = request.getParameter("lastName").trim();
            String email = request.getParameter("email").trim();
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");
            boolean isManager = "AGENCY_MANAGER".equals(invitation.getRole());

            // Validate passwords match
            if (!password.equals(confirmPassword)) {
                request.setAttribute("invitation", invitation);
                request.setAttribute("agency", invitation.getAgency());
                request.setAttribute("isManager", isManager);
                request.setAttribute("formError", "Passwords do not match.");
                request.getRequestDispatcher("/WEB-INF/view/sales/acceptInvite.jsp").forward(request, response);
                return;
            }

            if (password.length() < 8) {
                request.setAttribute("invitation", invitation);
                request.setAttribute("agency", invitation.getAgency());
                request.setAttribute("isManager", isManager);
                request.setAttribute("formError", "Password must be at least 8 characters.");
                request.getRequestDispatcher("/WEB-INF/view/sales/acceptInvite.jsp").forward(request, response);
                return;
            }

            // Check if a User already exists with this email
            User existingUser = findUserByEmail(em, email);
            Agency agency = invitation.getAgency();
            if (existingUser != null) {
                Person existingPerson = existingUser.getPerson();

                // Check if they're already assigned to a DIFFERENT agency
                boolean inDifferentAgency = false;
                if (existingPerson.getListOfAgenciesWithThisAgent() != null) {
                    for (Object a : existingPerson.getListOfAgenciesWithThisAgent()) {
                        Agency ag = (Agency) a;
                        if (!ag.getId().equals(invitation.getAgency().getId())) {
                            inDifferentAgency = true;
                            break;
                        }
                    }
                }

                if (inDifferentAgency) {
                    request.setAttribute("invitation", invitation);
                    request.setAttribute("agency", invitation.getAgency());
                    request.setAttribute("isManager", isManager);
                    request.setAttribute("formError", "This email is already associated with a different agency. Please contact your administrator.");
                    request.getRequestDispatcher("/WEB-INF/view/sales/acceptInvite.jsp").forward(request, response);
                    return;
                }

                // Auto-grant: add role if missing
                int roleId = isManager ? 8 : 2;
                UserRole newRole = AuthDAO.getUserRoleById(em, roleId);
                boolean hasRole = false;
                for (UserRole ur : existingUser.getUserRoleList()) {
                    if (ur.getId() == roleId) { hasRole = true; break; }
                }
                if (!hasRole) {
                    em.getTransaction().begin();
                    existingUser.getUserRoleList().add(newRole);
                    em.merge(existingUser);
                    em.getTransaction().commit();
                }

                // Add to agency agent list if not already
                boolean alreadyInAgency = false;

                if (agency.getAgentList() != null) {
                    for (Person p : agency.getAgentList()) {
                        if (p.getId().equals(existingPerson.getId())) { alreadyInAgency = true; break; }
                    }
                }
                if (!alreadyInAgency) {
                    em.getTransaction().begin();
                    agency.addAgent(existingPerson);
                    em.merge(agency);
                    em.getTransaction().commit();
                }

                // If Agency Manager, set on agency
                if (isManager) {
                    em.getTransaction().begin();
                    agency.setManager(existingPerson);
                    if (agency.getPrimaryContact() == null) {
                        agency.setPrimaryContact(existingPerson);
                    }
                    em.merge(agency);
                    em.getTransaction().commit();
                }

                // Mark invitation used
                em.getTransaction().begin();
                invitation.setIsUsed(true);
                invitation.setDateAccepted(Timestamp.valueOf(LocalDateTime.now()));
                em.merge(invitation);
                em.getTransaction().commit();

                System.out.println("✅ Existing user " + email + " auto-granted " + invitation.getRole() + " for " + agency.getName());

                response.sendRedirect(request.getContextPath() + "/login?updated=true");
                return;
            }

            // Get the Person that was created during SendInvitation (linked directly)
            Person person = invitation.getPerson();
            if (person == null) {
                // Fallback: shouldn't happen with new invitations, but handle gracefully
                em.getTransaction().begin();
                person = new Person();
                person.setPsp(invitation.getAgency().getPsp());
                em.persist(person);
                em.getTransaction().commit();
            }

            // Update person fields
            em.getTransaction().begin();
            person.setFirstName(firstName);
            person.setLastName(lastName);
            person.setFullName(firstName + " " + lastName);
            person.setEmail(email);
            em.merge(person);
            em.getTransaction().commit();

            // Handle Agency Manager specific fields
            if (isManager) {
                String agencyName = request.getParameter("agencyName");
                String taxId = request.getParameter("taxId");
                String address1 = request.getParameter("address1");
                String address2 = request.getParameter("address2");
                String city = request.getParameter("city");
                String state = request.getParameter("state");
                String zipCode = request.getParameter("zipCode");

                em.getTransaction().begin();

                if (agencyName != null && !agencyName.trim().isEmpty()) {
                    agency.setName(agencyName.trim());
                }
                if (taxId != null && !taxId.trim().isEmpty()) {
                    agency.setTaxId(taxId.trim());
                }

                // Update address
                Address address = agency.getAddress();
                if (address == null) {
                    address = new Address();
                    em.persist(address);
                    agency.setAddress(address);
                }
                if (address1 != null) address.setAddress1(address1.trim());
                if (address2 != null) address.setAddress2(address2.trim());
                if (city != null) address.setCity(city.trim());
                if (state != null) address.setState(state.trim());
                if (zipCode != null) address.setZipCode(zipCode.trim());

                // Set this person as agency manager and primary contact
                agency.setManager(person);
                agency.setPrimaryContact(person);

                em.merge(agency);
                em.getTransaction().commit();
            }

            // Add person to agency's agent list if not already there
            em.getTransaction().begin();
            boolean alreadyInList = false;
            if (agency.getAgentList() != null) {
                for (Person p : agency.getAgentList()) {
                    if (p.getId().equals(person.getId())) {
                        alreadyInList = true;
                        break;
                    }
                }
            }
            if (!alreadyInList) {
                agency.addAgent(person);
            }
            em.merge(agency);
            em.getTransaction().commit();

            // Create User account using AuthDAO (matches existing login validation)
            String salt = AuthDAO.generateSalt();
            String passwordHash = AuthDAO.generatePasswordHash(password, salt);

            em.getTransaction().begin();
            User user = new User();
            user.setPerson(person);
            user.setUserName(email);
            user.setEmail(email);
            user.setEmailVerified(true);
            user.setPasswordHash(passwordHash);
            user.setSalt(salt);
            user.setAllowSetPassword(false);
            user.setGuidUsed(true);
            user.setTempGuid(UUID.randomUUID().toString());
            user.setGuidExpiration(Date.valueOf(LocalDate.now()));

            // Assign role: 8 = Agency Admin, 2 = Agent
            int roleId = isManager ? 8 : 2;
            UserRole userRole = AuthDAO.getUserRoleById(em, roleId);
            List<UserRole> roles = new ArrayList<>();
            roles.add(userRole);
            user.setUserRoleList(roles);

            em.persist(user);
            em.getTransaction().commit();

            // Mark invitation as used
            em.getTransaction().begin();
            invitation.setIsUsed(true);
            invitation.setDateAccepted(Timestamp.valueOf(LocalDateTime.now()));
            em.merge(invitation);
            em.getTransaction().commit();

            System.out.println("✅ Invitation accepted: " + email + " joined " + agency.getName() + " as " + invitation.getRole());

            // Redirect to login page
            response.sendRedirect(request.getContextPath() + "/login?registered=true");
            return;

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("inviteError", "An error occurred during registration. Please try again.");
            request.getRequestDispatcher("/WEB-INF/view/sales/acceptInvite.jsp").forward(request, response);
        } finally {
            em.close();
        }
    }

    private Invitation findInvitation(EntityManager em, String guid) {
        try {
            Query q = em.createQuery("SELECT i FROM Invitation i WHERE i.guid = :guid");
            q.setParameter("guid", guid);
            return (Invitation) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private User findUserByEmail(EntityManager em, String email) {
        try {
            Query q = em.createQuery("SELECT u FROM User u WHERE u.email = :email");
            q.setParameter("email", email);
            return (User) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
