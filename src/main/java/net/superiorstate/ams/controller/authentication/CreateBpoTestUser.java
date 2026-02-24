package net.superiorstate.ams.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.general.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One-time utility to create BPO test users.
 * Hit /CreateBpoTestUser to create a BPO Admin user.
 * Remove or restrict this servlet after demo setup.
 */
@WebServlet(name = "CreateBpoTestUser", value = "/CreateBpoTestUser")
public class CreateBpoTestUser extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        try {
            // Check if user already exists
            if (AuthDAO.getUserByUserName(em, "bpoadmin@test.com") != null
                    && AuthDAO.getUserByUserName(em, "bpoadmin@test.com").getUserName() != null) {
                out.println("<h3>BPO Admin user already exists.</h3>");
                out.println("<p>Username: bpoadmin@test.com</p>");
                return;
            }
        } catch (Exception ignored) {
            // User doesn't exist, proceed to create
        }

        try {
            // Get the PSP (use the existing one)
            PSP psp = EntityLookup.getPspById(em, 4L);
            if (psp == null) {
                // Try ID 1 as fallback
                psp = EntityLookup.getPspById(em, 1L);
            }

            // Create Person
            em.getTransaction().begin();
            Address address = new Address();
            address.setAddress1("123 BPO Lane");
            address.setCity("Mumbai");
            address.setState("MH");
            address.setZipCode("40001");
            em.persist(address);
            em.getTransaction().commit();

            em.getTransaction().begin();
            Person person = new Person();
            person.setFirstName("BPO");
            person.setLastName("Admin");
            person.setFullName("BPO Admin");
            person.setEmail("bpoadmin@test.com");
            person.setAddress(address);
            person.setPsp(psp);
            em.persist(person);
            em.getTransaction().commit();

            // Create User with password "bpo123"
            String password = "bpo123";
            String salt = AuthDAO.generateSalt();
            String passwordHash = AuthDAO.generatePasswordHash(password, salt);

            em.getTransaction().begin();
            User user = new User();
            user.setPerson(person);
            user.setUserName("bpoadmin@test.com");
            user.setEmail("bpoadmin@test.com");
            user.setEmailVerified(true);
            user.setPasswordHash(passwordHash);
            user.setSalt(salt);
            user.setAllowSetPassword(false);
            user.setGuidUsed(true);
            user.setTempGuid(UUID.randomUUID().toString());
            user.setGuidExpiration(Date.valueOf(LocalDate.now()));

            // Assign BPO Admin role (102)
            UserRole bpoAdminRole = AuthDAO.getUserRoleById(em, 102);
            List<UserRole> roles = new ArrayList<>();
            roles.add(bpoAdminRole);
            user.setUserRoleList(roles);

            em.persist(user);
            em.getTransaction().commit();

            // Also create a regular BPO User for testing assignment
            em.getTransaction().begin();
            Address address2 = new Address();
            address2.setAddress1("456 BPO Street");
            address2.setCity("Mumbai");
            address2.setState("MH");
            address2.setZipCode("40002");
            em.persist(address2);
            em.getTransaction().commit();

            em.getTransaction().begin();
            Person person2 = new Person();
            person2.setFirstName("BPO");
            person2.setLastName("User");
            person2.setFullName("BPO User");
            person2.setEmail("bpouser@test.com");
            person2.setAddress(address2);
            person2.setPsp(psp);
            em.persist(person2);
            em.getTransaction().commit();

            String salt2 = AuthDAO.generateSalt();
            String passwordHash2 = AuthDAO.generatePasswordHash(password, salt2);

            em.getTransaction().begin();
            User user2 = new User();
            user2.setPerson(person2);
            user2.setUserName("bpouser@test.com");
            user2.setEmail("bpouser@test.com");
            user2.setEmailVerified(true);
            user2.setPasswordHash(passwordHash2);
            user2.setSalt(salt2);
            user2.setAllowSetPassword(false);
            user2.setGuidUsed(true);
            user2.setTempGuid(UUID.randomUUID().toString());
            user2.setGuidExpiration(Date.valueOf(LocalDate.now()));

            UserRole bpoUserRole = AuthDAO.getUserRoleById(em, 103);
            List<UserRole> roles2 = new ArrayList<>();
            roles2.add(bpoUserRole);
            user2.setUserRoleList(roles2);

            em.persist(user2);
            em.getTransaction().commit();

            out.println("<h3>BPO Test Users Created Successfully</h3>");
            out.println("<table border='1' cellpadding='8'>");
            out.println("<tr><th>Role</th><th>Username</th><th>Password</th></tr>");
            out.println("<tr><td>BPO Admin (102)</td><td>bpoadmin@test.com</td><td>bpo123</td></tr>");
            out.println("<tr><td>BPO User (103)</td><td>bpouser@test.com</td><td>bpo123</td></tr>");
            out.println("</table>");
            out.println("<br><a href='login'>Go to Login</a>");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            out.println("<h3>Error creating BPO users</h3>");
            out.println("<pre>");
            e.printStackTrace(out);
            out.println("</pre>");
        } finally {
            em.close();
        }
    }
}
