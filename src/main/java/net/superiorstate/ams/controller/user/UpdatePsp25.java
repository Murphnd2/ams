package net.superiorstate.ams.controller.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.previous.data.misc.dbEmail;
import net.superiorstate.ams.previous.data.model.getByIds.dM;
import net.superiorstate.ams.previous.model.general.Address;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.User;
import net.superiorstate.ams.previous.model.sales.agency.Agency;
import net.superiorstate.ams.previous.model.summit.archive.Employee;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

import java.io.IOException;

@WebServlet(name = "UpdatePsp25", value = "/UpdatePsp25")
public class UpdatePsp25 extends HttpServlet {
    public class CreateTpa extends HttpServlet {
        private EntityManager em;
        private String companyName;
        private String address1;
        private String address2;
        private String city;
        private String state;
        private String zipCode;
        private String phone;
        private String primaryLastName;
        private String primaryFirstName;
        private String primaryEmail;
        private String userLastName;
        private String userFirstName;
        private String userEmail;
        private String accessCode;

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            createTpa(request);
            goToPage(request, response);
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            createTpa(request);
            goToPage(request, response);
        }

        private void goToPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("GoAdminHome");
            dispatcher.forward(request, response);
        }

        private void createTpa(HttpServletRequest request) {
            if (!hasCredentials(request))
                return;
            getAndSetParameters(request);
            updatePspInfo();
            em.close();
        }

        private void updatePspInfo() {
            // Update PSP Table
            PSP psp = dM.getPspById(em, 4L);
            if (companyName != null) {
                em.getTransaction().begin();
                psp.setFullName(companyName);
                em.persist(psp);
                em.getTransaction().commit();
            }

            // Update Address Table
            Address a = dM.getAddressById(em, 3L);
            boolean goodAddress = false;
            if (address1 != null && city != null && state != null && zipCode != null) {
                em.getTransaction().begin();
                a.setAddress1(address1);
                a.setCity(city);
                a.setState(state);
                a.setZipCode(zipCode);
                if (address2 != null)
                    a.setAddress2(address2);
                em.persist(a);
                em.getTransaction().commit();
                goodAddress = true;
            }

            // Update Employer Table
            Employer er = dM.getEmployerById(em, -1);
            if (companyName != null) {
                em.getTransaction().begin();
                er.setEmployerName(companyName);
                em.persist(er);
                em.getTransaction().commit();
            }

            // Update Employee Table
            Employee ee = dM.getEmployeeById(em, -1);
            Person p = dM.getPersonById(em, 104L);
            updatePersonAndEmployee(primaryLastName, primaryFirstName, primaryEmail, p, ee);

            Employee eu = dM.getEmployeeById(em, -2);
            Person u = dM.getPersonById(em, 105L);
            updatePersonAndEmployee(userLastName, userFirstName, userEmail, u, eu);

            // Update Agency Table
            Query q = em.createQuery("SELECT a FROM Agency a WHERE a.id = 14");
            Agency agency = (Agency) q.getSingleResult();
            if (companyName != null) {
                em.getTransaction().begin();
                agency.setName(companyName);
                if (phone != null)
                    agency.setPhone(phone);
                em.persist(agency);
                em.getTransaction().commit();
            }

            // Update User Table
            if (primaryEmail != null) {
                Query q1 = em.createQuery("SELECT u FROM User u WHERE u.person.id = :uid");
                q1.setParameter("uid", 104L);
                User u1 = (User) q1.getSingleResult();
                em.getTransaction().begin();
                u1.setUserName(primaryEmail);
                u1.setEmail(primaryEmail);
                em.persist(u1);
                em.getTransaction().commit();
            }
            if (userEmail != null) {
                Query q2 = em.createQuery("SELECT u FROM User u WHERE u.person.id = :uid");
                q2.setParameter("uid", 105L);
                User u2 = (User) q2.getSingleResult();
                em.getTransaction().begin();
                u2.setUserName(userEmail);
                u2.setEmail(userEmail);
                em.persist(u2);
                em.getTransaction().commit();
            }

        }

        private void updatePersonAndEmployee(String lName, String fName, String email, Person p, Employee ee) {
            if (email != null || fName != null || lName != null) {
                em.getTransaction().begin();
                if (lName != null) {
                    ee.setLastName(lName);
                    p.setLastName(lName);
                    p.setFullName(fName + " " + lName);
                }
                if (fName != null) {
                    ee.setFirstName(fName);
                    p.setFirstName(fName);
                    p.setFullName(fName + " " + lName);
                }
                if (email != null) {
                    ee.setEmail(email);
                    p.setEmail(email);
                }
                em.persist(ee);
                em.persist(p);
                em.getTransaction().commit();
            }
        }

        private void getAndSetParameters(HttpServletRequest request) {
            EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
            em = emf.createEntityManager();

            String cName = request.getParameter("companyName");
            companyName = null;
            if (cName != null && !cName.equals(""))
                companyName = cName;

            String add1 = request.getParameter("address1");
            address1 = null;
            if (add1 != null && !add1.equals(""))
                address1 = add1;

            String add2 = request.getParameter("address2");
            address2 = null;
            if (add2 != null && !add2.equals(""))
                address2 = add2;

            String cit = request.getParameter("city");
            city = null;
            if (cit != null && !cit.equals(""))
                city = cit;

            String st = request.getParameter("state");
            state = null;
            if (st != null && !st.equals(""))
                state = st;

            String zp = request.getParameter("zip");
            zipCode = null;
            if (zp != null && !zp.equals(""))
                zipCode = zp;

            String ph = request.getParameter("phone");
            phone = null;
            if (ph != null && !ph.equals(""))
                phone = ph;

            String pLn = request.getParameter("lastNameP");
            primaryLastName = null;
            if (pLn != null && !pLn.equals(""))
                primaryLastName = pLn;

            String pFn = request.getParameter("firstNameP");
            primaryFirstName = null;
            if (pFn != null && !pFn.equals(""))
                primaryFirstName = pFn;

            String pEmail = request.getParameter("emailPrimary");
            primaryEmail = null;
            if (dbEmail.isValidEmail(pEmail))
                primaryEmail = pEmail;

            String sLn = request.getParameter("lastNameS");
            userLastName = null;
            if (sLn != null && !sLn.equals(""))
                userLastName = sLn;

            String sFn = request.getParameter("firstNameS");
            userFirstName = null;
            if (sFn != null && !sFn.equals(""))
                userFirstName = sFn;

            String sEmail = request.getParameter("emailSecondary");
            userEmail = null;
            if (dbEmail.isValidEmail(sEmail))
                userEmail = sEmail;

            accessCode = request.getParameter("accessCode");

        }

        private boolean hasCredentials(HttpServletRequest request) {
            try {
                accessCode = request.getParameter("accessCode");
            } catch (Exception e) {
                return false;
            }
            if (accessCode != null && accessCode.equals("Ams2024@")) {
                System.out.println("SENT");
                return true;
            }
            return false;
        }
    }
}
