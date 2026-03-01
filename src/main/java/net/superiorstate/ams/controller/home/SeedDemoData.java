package net.superiorstate.ams.controller.home;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.dao.AuthDAO;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.Constant;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.renewal.Renewal;
import net.superiorstate.ams.model.activity.renewal.RenewalItem;
import net.superiorstate.ams.model.activity.ticket.ContactMethod;
import net.superiorstate.ams.model.activity.ticket.Ticket;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.*;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.Employee;
import net.superiorstate.ams.model.summit.archive.Employer;
import net.superiorstate.ams.model.summit.archive.PlanType;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Conference Demo Data Seeder (D-24)
 *
 * Creates purpose-built demo data for live demonstrations:
 *   - 5 employers with realistic names and contacts
 *   - 16 employees across all employers
 *   - 12 benefits (FSA, HRA, HSA, COBRA, DCA, Dental, Vision)
 *   - 3 open renewals (upcoming, in-progress, overdue)
 *   - 2 in-progress setups
 *   - 5 tickets (mix of open and completed)
 *   - 2 BPO vendor users with login credentials
 *   - 1 additional PSP staff user
 *
 * Idempotent: checks DEMO_DATA_SEEDED constant before running.
 * Access: requires authenticated PSP Admin session.
 *
 * Demo credentials created:
 *   jmartinez@superiorstate.net / demo123  (PSP User)
 *   arivera@accelvantage.com / demo123     (BPO Admin)
 *   psharma@accelvantage.com / demo123     (BPO User)
 */
@WebServlet(name = "SeedDemoData", value = "/SeedDemoData")
public class SeedDemoData extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // PSP Admin guard
        Boolean isPspAdmin = (Boolean) request.getSession().getAttribute("isPspAdmin");
        if (isPspAdmin == null || !isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "PSP Admin access required");
            return;
        }

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><head><title>Demo Data Seeder</title>"
                + "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css'>"
                + "</head><body class='p-4' style='max-width:900px; margin:auto;'>");
        out.println("<h2 style='color:#0d5681;'>Conference Demo Data Seeder</h2><hr>");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Check if already seeded
            if (isAlreadySeeded(em)) {
                out.println("<div class='alert alert-warning'>Demo data has already been seeded. "
                        + "To re-seed, delete the <code>DEMO_DATA_SEEDED</code> constant from the database and restart.</div>");
                out.println("<p><a href='ViewHome25' class='btn btn-outline-primary btn-sm'>Back to Home</a></p>");
                out.println("</body></html>");
                return;
            }

            seedAllDemoData(em, out);

            // Reload global data so new users/service items appear in dropdowns
            AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
            global.initializeGlobalData(em);

            out.println("<hr>");
            out.println("<div class='alert alert-success'>"
                    + "<strong>Demo data seeded successfully!</strong><br>"
                    + "5 employers, 16 employees, 12 benefits, 3 renewals, 2 setups, 5 tickets, "
                    + "2 BPO users, 1 staff user.</div>");

            out.println("<h5 class='mt-3'>Demo Login Credentials</h5>");
            out.println("<table class='table table-sm table-bordered' style='max-width:600px;'>"
                    + "<thead class='table-light'><tr>"
                    + "<th>Name</th><th>Email / Username</th><th>Password</th><th>Role</th></tr></thead><tbody>"
                    + "<tr><td>Jennifer Martinez</td><td><code>jmartinez@superiorstate.net</code></td>"
                    + "<td><code>demo123</code></td><td>PSP User</td></tr>"
                    + "<tr><td>Alex Rivera</td><td><code>arivera@accelvantage.com</code></td>"
                    + "<td><code>demo123</code></td><td>BPO Admin</td></tr>"
                    + "<tr><td>Priya Sharma</td><td><code>psharma@accelvantage.com</code></td>"
                    + "<td><code>demo123</code></td><td>BPO User</td></tr>"
                    + "</tbody></table>");

            out.println("<p class='mt-3'><strong>Tip:</strong> Run <code>/SeedBpoDemoData</code> next "
                    + "to source some checklist tasks to BPO for the vendor delegation demo.</p>");
            out.println("<p><a href='ViewHome25' class='btn btn-primary btn-sm'>Go to Home</a></p>");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            out.println("<div class='alert alert-danger'><strong>Error:</strong> " + escapeHtml(e.getMessage()) + "</div>");
            e.printStackTrace();
        } finally {
            em.close();
        }

        out.println("</body></html>");
    }

    /**
     * Core seeding logic — callable from both the doGet flow and ReSeedDemoData.
     * Creates all demo employers, employees, benefits, activities, and users.
     */
    public void seedAllDemoData(EntityManager em, PrintWriter out) {
        PSP psp = EntityLookup.getPspById(em, 4L);
        Person adminPerson = EntityLookup.getPersonById(em, 104L);

        if (psp == null || adminPerson == null) {
            throw new IllegalStateException("Database not initialized — PSP or admin person not found.");
        }

        log(out, "<strong>Starting demo data seed...</strong>");

        // ═══════════════════════════════════════════
        //  EMPLOYERS
        // ═══════════════════════════════════════════
        log(out, "<h5 class='mt-3' style='color:#0d5681;'>Employers</h5>");

            Employer acme = seedEmployer(em, out, -100,
                    "Acme Manufacturing Corp", "hr@acmemfg.com", "Sarah Chen", -100, true);
            Employer bright = seedEmployer(em, out, -101,
                    "Bright Horizons Childcare", "admin@brighthorizons.org", "Karen White", -101, false);
            Employer cascade = seedEmployer(em, out, -102,
                    "Cascade Financial Services", "hr@cascadefinancial.com", "Tom Anderson", -102, true);
            Employer delta = seedEmployer(em, out, -103,
                    "Delta Regional Medical Center", "hr@deltamedical.org", "Nancy Clark", -103, true);
            Employer evergreen = seedEmployer(em, out, -104,
                    "Evergreen Landscaping LLC", "jack@evergreenlandscaping.com", "Jack Thompson", -104, false);

            // ═══════════════════════════════════════════
            //  EMPLOYEES
            // ═══════════════════════════════════════════
            log(out, "<h5 class='mt-3' style='color:#0d5681;'>Employees</h5>");

            // Acme (5)
            Employee aChen = seedEmployee(em, out, -100, "Sarah", "Chen", acme, "schen@acmemfg.com");
            seedEmployee(em, out, -101, "Mike", "Rodriguez", acme, "mrodriguez@acmemfg.com");
            seedEmployee(em, out, -102, "Lisa", "Johnson", acme, "ljohnson@acmemfg.com");
            seedEmployee(em, out, -103, "James", "Wilson", acme, "jwilson@acmemfg.com");
            seedEmployee(em, out, -104, "Amy", "Park", acme, "apark@acmemfg.com");

            // Bright Horizons (3)
            Employee bWhite = seedEmployee(em, out, -105, "Karen", "White", bright, "kwhite@brighthorizons.org");
            seedEmployee(em, out, -106, "David", "Brown", bright, "dbrown@brighthorizons.org");
            seedEmployee(em, out, -107, "Rachel", "Green", bright, "rgreen@brighthorizons.org");

            // Cascade (3)
            Employee cAnderson = seedEmployee(em, out, -108, "Tom", "Anderson", cascade, "tanderson@cascadefinancial.com");
            seedEmployee(em, out, -109, "Maria", "Garcia", cascade, "mgarcia@cascadefinancial.com");
            seedEmployee(em, out, -110, "Robert", "Kim", cascade, "rkim@cascadefinancial.com");

            // Delta (3)
            Employee dClark = seedEmployee(em, out, -111, "Nancy", "Clark", delta, "nclark@deltamedical.org");
            seedEmployee(em, out, -112, "Kevin", "Lee", delta, "klee@deltamedical.org");
            seedEmployee(em, out, -113, "Sandra", "Mitchell", delta, "smitchell@deltamedical.org");

            // Evergreen (2)
            Employee eThompson = seedEmployee(em, out, -114, "Jack", "Thompson", evergreen, "jack@evergreenlandscaping.com");
            seedEmployee(em, out, -115, "Chris", "Davis", evergreen, "cdavis@evergreenlandscaping.com");

            // Link HR contacts to employer contact lists
            addContactToEmployer(em, acme, aChen);
            addContactToEmployer(em, bright, bWhite);
            addContactToEmployer(em, cascade, cAnderson);
            addContactToEmployer(em, delta, dClark);
            addContactToEmployer(em, evergreen, eThompson);

            // ═══════════════════════════════════════════
            //  CONTACT PERSONS (for activity assignments)
            // ═══════════════════════════════════════════
            log(out, "<h5 class='mt-3' style='color:#0d5681;'>Contact Persons</h5>");

            Address acmeAddr = seedAddress(em, "1250 Industrial Blvd", "", "Detroit", "MI", "48201");
            Address brightAddr = seedAddress(em, "400 Oak Park Drive", "Suite 200", "Columbus", "OH", "43215");
            Address cascadeAddr = seedAddress(em, "8900 Financial Center Way", "", "Portland", "OR", "97201");
            Address deltaAddr = seedAddress(em, "2100 Medical Parkway", "", "Nashville", "TN", "37203");
            Address evergreenAddr = seedAddress(em, "75 Maple Street", "", "Burlington", "VT", "05401");

            Person pChen = seedPerson(em, out, "Sarah", "Chen", "HR Director",
                    "schen@acmemfg.com", "313-555-0101", acmeAddr, psp);
            Person pWhite = seedPerson(em, out, "Karen", "White", "Benefits Administrator",
                    "kwhite@brighthorizons.org", "614-555-0201", brightAddr, psp);
            Person pAnderson = seedPerson(em, out, "Tom", "Anderson", "VP Human Resources",
                    "tanderson@cascadefinancial.com", "503-555-0301", cascadeAddr, psp);
            Person pClark = seedPerson(em, out, "Nancy", "Clark", "HR Manager",
                    "nclark@deltamedical.org", "615-555-0401", deltaAddr, psp);
            Person pThompson = seedPerson(em, out, "Jack", "Thompson", "Owner",
                    "jack@evergreenlandscaping.com", "802-555-0501", evergreenAddr, psp);

            // Link persons to their employee records
            linkPersonToEmployee(em, pChen, aChen);
            linkPersonToEmployee(em, pWhite, bWhite);
            linkPersonToEmployee(em, pAnderson, cAnderson);
            linkPersonToEmployee(em, pClark, dClark);
            linkPersonToEmployee(em, pThompson, eThompson);

            // Additional PSP staff member for variety in assignments
            Address staffAddr = seedAddress(em, "123 Main Street", "Suite 100", "Marquette", "MI", "49855");
            Person staffMember = seedPerson(em, out, "Jennifer", "Martinez", "Account Manager",
                    "jmartinez@superiorstate.net", "906-555-0102", staffAddr, psp);

            // ═══════════════════════════════════════════
            //  BENEFITS
            // ═══════════════════════════════════════════
            log(out, "<h5 class='mt-3' style='color:#0d5681;'>Benefits</h5>");

            LocalDate today = LocalDate.now();
            LocalDate firstOfMonth = LocalDate.of(today.getYear(), today.getMonthValue(), 1);

            PlanType ptFSA = em.find(PlanType.class, 2);
            PlanType ptHRA = em.find(PlanType.class, 3);
            PlanType ptHSA = em.find(PlanType.class, 4);
            PlanType ptDCA = em.find(PlanType.class, 1);
            PlanType ptDental = em.find(PlanType.class, 9);
            PlanType ptMedical = em.find(PlanType.class, 12);
            PlanType ptVision = em.find(PlanType.class, 14);

            // Acme (4 benefits)
            Benefit bAcmeFsa = seedBenefit(em, out, 99800, "CDH", "Acme Health FSA", acme, ptFSA,
                    Date.valueOf(firstOfMonth.plusMonths(1)));
            Benefit bAcmeHra = seedBenefit(em, out, 99801, "CDH", "Acme HRA", acme, ptHRA,
                    Date.valueOf(firstOfMonth.plusMonths(3)));
            seedBenefit(em, out, 99802, "COBRA", "Acme COBRA", acme, ptMedical,
                    Date.valueOf(firstOfMonth.plusMonths(6)));
            seedBenefit(em, out, 99803, "CDH", "Acme HSA", acme, ptHSA,
                    Date.valueOf(firstOfMonth.plusMonths(2)));

            // Bright Horizons (2)
            seedBenefit(em, out, 99804, "CDH", "Bright Horizons FSA", bright, ptFSA,
                    Date.valueOf(firstOfMonth.plusMonths(4)));
            seedBenefit(em, out, 99805, "CDH", "Bright Horizons DCA", bright, ptDCA,
                    Date.valueOf(firstOfMonth.plusMonths(4)));

            // Cascade (2)
            Benefit bCascadeHra = seedBenefit(em, out, 99806, "CDH", "Cascade HRA", cascade, ptHRA,
                    Date.valueOf(firstOfMonth.plusMonths(2)));
            seedBenefit(em, out, 99807, "CDH", "Cascade HSA", cascade, ptHSA,
                    Date.valueOf(firstOfMonth.plusMonths(5)));

            // Delta (3)
            Benefit bDeltaCobra = seedBenefit(em, out, 99808, "COBRA", "Delta COBRA", delta, ptMedical,
                    Date.valueOf(firstOfMonth.minusMonths(1)));
            seedBenefit(em, out, 99809, "CDH", "Delta Dental", delta, ptDental,
                    Date.valueOf(firstOfMonth.plusMonths(8)));
            seedBenefit(em, out, 99810, "CDH", "Delta Vision", delta, ptVision,
                    Date.valueOf(firstOfMonth.plusMonths(8)));

            // Evergreen (1)
            seedBenefit(em, out, 99811, "CDH", "Evergreen FSA", evergreen, ptFSA,
                    Date.valueOf(firstOfMonth.plusMonths(7)));

            // ═══════════════════════════════════════════
            //  RENEWALS
            // ═══════════════════════════════════════════
            log(out, "<h5 class='mt-3' style='color:#0d5681;'>Renewals</h5>");

            // Acme FSA — due in ~30 days, in progress
            Renewal rAcme = seedRenewal(em, out, "Acme Manufacturing Corp \u2014 Health FSA Renewal",
                    acme, adminPerson, staffMember, pChen,
                    Date.valueOf(firstOfMonth.plusMonths(1)));
            seedRenewalItem(em, rAcme, bAcmeFsa, Date.valueOf(firstOfMonth.plusMonths(1)));

            // Cascade HRA — due in ~60 days
            Renewal rCascade = seedRenewal(em, out, "Cascade Financial Services \u2014 HRA Renewal",
                    cascade, staffMember, adminPerson, pAnderson,
                    Date.valueOf(firstOfMonth.plusMonths(2)));
            seedRenewalItem(em, rCascade, bCascadeHra, Date.valueOf(firstOfMonth.plusMonths(2)));

            // Delta COBRA — overdue, needs attention
            Renewal rDelta = seedRenewal(em, out, "Delta Regional Medical Center \u2014 COBRA Renewal",
                    delta, adminPerson, adminPerson, pClark,
                    Date.valueOf(firstOfMonth.minusMonths(1)));
            seedRenewalItem(em, rDelta, bDeltaCobra, Date.valueOf(firstOfMonth.minusMonths(1)));

            // ═══════════════════════════════════════════
            //  SETUPS
            // ═══════════════════════════════════════════
            log(out, "<h5 class='mt-3' style='color:#0d5681;'>Setups</h5>");

            seedSetup(em, out, "Evergreen Landscaping LLC \u2014 New Employer Setup",
                    adminPerson, staffMember, pThompson,
                    Date.valueOf(today.plusDays(14)));

            seedSetup(em, out, "Bright Horizons Childcare \u2014 DCA Plan Addition",
                    staffMember, adminPerson, pWhite,
                    Date.valueOf(today.plusDays(21)));

            // ═══════════════════════════════════════════
            //  TICKETS
            // ═══════════════════════════════════════════
            log(out, "<h5 class='mt-3' style='color:#0d5681;'>Tickets</h5>");

            ContactMethod cmPhone = em.find(ContactMethod.class, 1);
            ContactMethod cmEmail = em.find(ContactMethod.class, 2);
            ServiceItem siHowTo = EntityLookup.getServiceItemById(em, 25, true);
            ServiceItem siBanking = EntityLookup.getServiceItemById(em, 30, true);

            // Ticket 1: Acme — portal access issue (open, phone)
            seedTicket(em, out, "Acme Mfg \u2014 Employee portal access issue",
                    "Employee Mike Rodriguez unable to log into benefits portal. Getting 'account locked' error. "
                            + "Needs password reset and verification of account status.",
                    pChen, adminPerson, adminPerson, cmPhone, siHowTo,
                    Date.valueOf(today), false);

            // Ticket 2: Bright Horizons — DCA limits question (open, email)
            seedTicket(em, out, "Bright Horizons \u2014 DCA annual limit question",
                    "Karen White asking about 2026 Dependent Care FSA contribution limits "
                            + "and whether mid-year election changes are allowed.",
                    pWhite, staffMember, staffMember, cmEmail, siHowTo,
                    Date.valueOf(today.minusDays(1)), false);

            // Ticket 3: Cascade — banking update (completed, email)
            seedTicket(em, out, "Cascade Financial \u2014 Banking info update",
                    "Tom Anderson requesting update to company direct deposit banking information "
                            + "for reimbursement payments. Signed authorization form received via secure email.",
                    pAnderson, adminPerson, adminPerson, cmEmail, siBanking,
                    Date.valueOf(today.minusDays(5)), true);

            // Ticket 4: Delta — COBRA enrollment dispute (open, phone, urgent feel)
            seedTicket(em, out, "Delta Medical \u2014 COBRA enrollment dispute",
                    "Former employee Sandra Mitchell claims she did not receive COBRA election notice. "
                            + "Requesting immediate enrollment. Needs review of notification records and vendor mailing logs.",
                    pClark, adminPerson, staffMember, cmPhone, siHowTo,
                    Date.valueOf(today.minusDays(2)), false);

            // Ticket 5: Acme — open enrollment planning (open, email)
            seedTicket(em, out, "Acme Mfg \u2014 Open enrollment planning",
                    "Sarah Chen requesting meeting to discuss upcoming open enrollment timeline, "
                            + "communications plan, and any plan design changes for next plan year.",
                    pChen, staffMember, staffMember, cmEmail, siHowTo,
                    Date.valueOf(today.plusDays(7)), false);

            // ═══════════════════════════════════════════
            //  BPO VENDOR USERS
            // ═══════════════════════════════════════════
            log(out, "<h5 class='mt-3' style='color:#0d5681;'>BPO Vendor Users</h5>");

            Address bpoAddr = seedAddress(em, "500 Tech Center Drive", "Floor 3", "Austin", "TX", "78701");
            Person bpoAdmin = seedPerson(em, out, "Alex", "Rivera", "Operations Manager",
                    "arivera@accelvantage.com", "512-555-0901", bpoAddr, psp);
            Person bpoUser = seedPerson(em, out, "Priya", "Sharma", "Claims Processor",
                    "psharma@accelvantage.com", "512-555-0902", bpoAddr, psp);

            User bpoAdminUser = seedUser(em, out, bpoAdmin, "arivera@accelvantage.com", "demo123");
            User bpoUserUser = seedUser(em, out, bpoUser, "psharma@accelvantage.com", "demo123");

            UserRole urBpoAdmin = EntityLookup.getUserRoleById(em, 102);
            UserRole urBpoUser = EntityLookup.getUserRoleById(em, 103);
            if (urBpoAdmin != null) assignRole(em, bpoAdminUser, urBpoAdmin);
            if (urBpoUser != null) assignRole(em, bpoUserUser, urBpoUser);

            // Seed BPO Registration entity (AccelVantage — all flags true for demo)
            em.getTransaction().begin();
            BpoRegistration bpoReg = new BpoRegistration();
            bpoReg.setPsp(psp);
            bpoReg.setBpoName("AccelVantage");
            bpoReg.setBpoUrl("https://accelvantage.com");
            bpoReg.setActive(true);
            bpoReg.setApproved(true);
            bpoReg.setRequested(true);
            bpoReg.setAccepted(true);
            em.persist(bpoReg);
            em.getTransaction().commit();
            log(out, "✅ BPO Registration: <b>AccelVantage</b> (all flags true)");

            // ═══════════════════════════════════════════
            //  STAFF USER
            // ═══════════════════════════════════════════
            log(out, "<h5 class='mt-3' style='color:#0d5681;'>Staff User</h5>");

            User staffUser = seedUser(em, out, staffMember, "jmartinez@superiorstate.net", "demo123");
            UserRole urPspUser = EntityLookup.getUserRoleById(em, 1);
            if (urPspUser != null) assignRole(em, staffUser, urPspUser);
            seedFilterPresets(em, staffUser);

            // ═══════════════════════════════════════════
            //  CLOSE INITIALIZATION CHECKLIST
            // ═══════════════════════════════════════════
            closeInitializationChecklist(em);

            // ═══════════════════════════════════════════
            //  MARK COMPLETE
            // ═══════════════════════════════════════════
            markSeeded(em);
            log(out, "<strong>Demo data seed complete.</strong>");
    }

    // ═══════════════════════════════════════════════════════════════
    //  SEED GUARD
    // ═══════════════════════════════════════════════════════════════

    private void closeInitializationChecklist(EntityManager em) {
        CheckList cl = em.find(CheckList.class, 29L);
        if (cl != null) {
            em.getTransaction().begin();
            cl.setComplete(true);
            cl.setDateCompleted(java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(1)));
            cl.setCompletedBy(cl.getLoggedBy());
            em.merge(cl);
            em.getTransaction().commit();
        }
    }

    private boolean isAlreadySeeded(EntityManager em) {
        try {
            Query q = em.createQuery("SELECT c FROM Constant c WHERE c.name = 'DEMO_DATA_SEEDED'");
            return !q.getResultList().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void markSeeded(EntityManager em) {
        em.getTransaction().begin();
        Constant c = new Constant();
        c.setName("DEMO_DATA_SEEDED");
        c.setValue("true");
        em.persist(c);
        em.getTransaction().commit();
    }

    // ═══════════════════════════════════════════════════════════════
    //  EMPLOYER / EMPLOYEE
    // ═══════════════════════════════════════════════════════════════

    private Employer seedEmployer(EntityManager em, PrintWriter out,
                                  int id, String name, String email, String contactName, int erKey, boolean billable) {
        Employer existing = EntityLookup.getEmployerById(em, id, true);
        if (existing != null) {
            log(out, "&nbsp;&nbsp;Employer exists: " + name);
            return existing;
        }
        em.getTransaction().begin();
        Employer er = new Employer();
        er.setId(id);
        er.setEmployerName(name);
        er.setActive(true);
        er.setEmail(email);
        er.setContactName(contactName);
        er.setErKey(erKey);
        er.setAltId(id);
        er.setBillable(billable);
        er.setHasPop(true);
        er.setHasPb(true);
        er.setHasCdh(true);
        em.persist(er);
        em.getTransaction().commit();
        log(out, "&nbsp;&nbsp;Created employer: <strong>" + name + "</strong> (ID " + id + ")");
        return er;
    }

    private Employee seedEmployee(EntityManager em, PrintWriter out,
                                  int id, String firstName, String lastName, Employer employer, String email) {
        Employee existing = EntityLookup.getEmployeeById(em, id, true);
        if (existing != null) return existing;

        em.getTransaction().begin();
        Employee ee = new Employee();
        ee.setId(id);
        ee.setFirstName(firstName);
        ee.setLastName(lastName);
        ee.setMmKey(id);
        ee.setActive(true);
        ee.setEmployer(employer);
        ee.setEmail(email);
        em.persist(ee);
        em.getTransaction().commit();
        log(out, "&nbsp;&nbsp;Created employee: " + firstName + " " + lastName
                + " @ " + employer.getEmployerName());
        return ee;
    }

    private void addContactToEmployer(EntityManager em, Employer employer, Employee contact) {
        em.getTransaction().begin();
        employer.addContact(contact);
        em.merge(employer);
        em.getTransaction().commit();
    }

    // ═══════════════════════════════════════════════════════════════
    //  ADDRESS / PERSON
    // ═══════════════════════════════════════════════════════════════

    private Address seedAddress(EntityManager em, String ad1, String ad2,
                                String city, String state, String zip) {
        em.getTransaction().begin();
        Address a = new Address();
        a.setAddress1(ad1);
        a.setAddress2(ad2);
        a.setCity(city);
        a.setState(state);
        a.setZipCode(zip);
        em.persist(a);
        em.getTransaction().commit();
        return a;
    }

    private Person seedPerson(EntityManager em, PrintWriter out,
                              String firstName, String lastName, String title,
                              String email, String phone, Address address, PSP psp) {
        em.getTransaction().begin();
        Person p = new Person();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setFullName(firstName + " " + lastName);
        p.setTitle(title);
        p.setEmail(email);
        p.setPhone(phone);
        p.setAddress(address);
        p.setPsp(psp);
        em.persist(p);
        em.getTransaction().commit();
        log(out, "&nbsp;&nbsp;Created person: <strong>" + firstName + " " + lastName
                + "</strong> (" + title + ")");
        return p;
    }

    private void linkPersonToEmployee(EntityManager em, Person person, Employee employee) {
        em.getTransaction().begin();
        person.setEmployee(employee);
        em.merge(person);
        em.getTransaction().commit();
    }

    // ═══════════════════════════════════════════════════════════════
    //  BENEFITS
    // ═══════════════════════════════════════════════════════════════

    private Benefit seedBenefit(EntityManager em, PrintWriter out,
                                int summitId, String sourceType, String name,
                                Employer employer, PlanType planType, Date nextRenewalDue) {
        Benefit existing = EntityLookup.getBenefitBySummitKey(em, sourceType, summitId);
        if (existing != null) {
            log(out, "&nbsp;&nbsp;Benefit exists: " + name);
            return existing;
        }
        LocalDate renewalDate = nextRenewalDue.toLocalDate();
        Date effectiveDate = Date.valueOf(renewalDate.minusYears(1));

        em.getTransaction().begin();
        Benefit b = new Benefit();
        b.setSummitId(summitId);
        b.setSourceType(sourceType);
        b.setPlanName(name);
        b.setPlanDescription(name);
        b.setEmployer(employer);
        b.setPlanType(planType);
        b.setEffectiveDate(effectiveDate);
        b.setNextRenewalDue(nextRenewalDue);
        b.setActive(true);
        b.setPbBenId(0);
        em.persist(b);
        em.getTransaction().commit();
        log(out, "&nbsp;&nbsp;Created benefit: <strong>" + name + "</strong> (renews "
                + nextRenewalDue + ")");
        return b;
    }

    // ═══════════════════════════════════════════════════════════════
    //  RENEWALS
    // ═══════════════════════════════════════════════════════════════

    private Renewal seedRenewal(EntityManager em, PrintWriter out, String name,
                                Employer employer, Person loggedBy, Person assignedTo,
                                Person primaryContact, Date dueDate) {
        em.getTransaction().begin();
        Renewal r = new Renewal();
        r.setFullName(name);
        r.setEmployer(employer);
        r.setLoggedBy(loggedBy);
        r.setAssignedTo(assignedTo);
        r.setPrimaryContact(primaryContact);
        r.setDueDate(dueDate);
        r.setComplete(false);
        em.persist(r);
        em.getTransaction().commit();

        PSP psp = loggedBy.getPsp();
        String[] steps = {
            "Review current plan design and census",
            "Request renewal rates from carrier",
            "Prepare renewal comparison report",
            "Schedule renewal meeting with client",
            "Finalize plan elections and confirm enrollment"
        };
        CheckList cl = createChecklistForActivity(em, r, psp, steps);
        em.getTransaction().begin();
        r.setCheckList(cl);
        em.merge(r);
        em.getTransaction().commit();

        log(out, "&nbsp;&nbsp;Created renewal: <strong>" + name + "</strong> (due " + dueDate + ")");
        return r;
    }

    private void seedRenewalItem(EntityManager em, Renewal renewal, Benefit benefit, Date dateFor) {
        em.getTransaction().begin();
        RenewalItem ri = new RenewalItem();
        ri.setBenefit(benefit);
        ri.setRenewal(renewal);
        ri.setDateFor(dateFor);
        em.persist(ri);
        em.getTransaction().commit();
    }

    // ═══════════════════════════════════════════════════════════════
    //  SETUPS
    // ═══════════════════════════════════════════════════════════════

    private void seedSetup(EntityManager em, PrintWriter out, String name,
                           Person loggedBy, Person assignedTo, Person primaryContact, Date dueDate) {
        em.getTransaction().begin();
        Setup s = new Setup();
        s.setFullName(name);
        s.setLoggedBy(loggedBy);
        s.setAssignedTo(assignedTo);
        s.setPrimaryContact(primaryContact);
        s.setDueDate(dueDate);
        s.setComplete(false);
        em.persist(s);
        em.getTransaction().commit();

        PSP psp = loggedBy.getPsp();
        String[] steps = {
            "Collect employer information and plan documents",
            "Configure benefit plans in system",
            "Import employee census data",
            "Verify employer banking and billing setup",
            "Send welcome packet to employer contact"
        };
        CheckList cl = createChecklistForActivity(em, s, psp, steps);
        em.getTransaction().begin();
        s.setCheckList(cl);
        em.merge(s);
        em.getTransaction().commit();

        log(out, "&nbsp;&nbsp;Created setup: <strong>" + name + "</strong> (due " + dueDate + ")");
    }

    // ═══════════════════════════════════════════════════════════════
    //  TICKETS
    // ═══════════════════════════════════════════════════════════════

    private void seedTicket(EntityManager em, PrintWriter out, String name, String description,
                            Person contact, Person loggedBy, Person assignedTo,
                            ContactMethod contactMethod, ServiceItem serviceItem,
                            Date dueDate, boolean isComplete) {
        em.getTransaction().begin();
        Ticket t = new Ticket();
        t.setFullName(name);
        t.setDescription(description);
        t.setContact(contact);
        t.setLoggedBy(loggedBy);
        t.setAssignedTo(assignedTo);
        t.setPrimaryContact(contact);
        t.setContactMethod(contactMethod);
        t.setTicketServiceItem(serviceItem);
        t.setDueDate(dueDate);
        t.setComplete(isComplete);
        if (isComplete) {
            t.setDateCompleted(dueDate);
            t.setCompletedBy(assignedTo);
        }
        em.persist(t);
        em.getTransaction().commit();

        PSP psp = loggedBy.getPsp();
        String[] steps = {
            "Review request details and verify contact",
            "Research issue and gather documentation",
            "Resolve issue or escalate as needed",
            "Confirm resolution with contact"
        };
        CheckList cl = createChecklistForActivity(em, t, psp, steps);
        em.getTransaction().begin();
        t.setCheckList(cl);
        em.merge(t);
        em.getTransaction().commit();

        log(out, "&nbsp;&nbsp;Created ticket: <strong>" + name + "</strong>"
                + (isComplete ? " (completed)" : " (open)"));
    }

    // ═══════════════════════════════════════════════════════════════
    //  CHECKLIST (required by activity detail page)
    // ═══════════════════════════════════════════════════════════════

    private CheckList createChecklistForActivity(EntityManager em, Activity activity,
                                                   PSP psp, String[] taskDescriptions) {
        em.getTransaction().begin();
        CheckList cl = new CheckList();
        cl.setAssignedTo(activity);
        cl.setDueDate(activity.getDueDate());
        cl.setFullName(activity.getFullName());
        cl.setLoggedBy(activity.getLoggedBy());
        cl.setComplete(activity.isComplete());
        em.persist(cl);
        em.getTransaction().commit();

        int sortOrder = 10;
        for (String desc : taskDescriptions) {
            em.getTransaction().begin();
            Task task = new Task();
            task.setReUsable(false);
            task.setDescription(desc);
            task.setHasOwner(false);
            task.setSourced(false);
            task.setAllowNonOwner(true);
            task.setHasAutomation(false);
            task.setHasGoTo(false);
            task.setHasInfo(false);
            task.setAllowEarly(true);
            task.setAllowFuture(true);
            task.setPsp(psp);
            em.persist(task);
            em.getTransaction().commit();

            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setComplete(false);
            toDo.setCheckList(cl);
            toDo.setTask(task);
            toDo.setSortOrder(sortOrder);
            em.persist(toDo);
            em.getTransaction().commit();

            sortOrder += 10;
        }

        return cl;
    }

    // ═══════════════════════════════════════════════════════════════
    //  USER / ROLE
    // ═══════════════════════════════════════════════════════════════

    private User seedUser(EntityManager em, PrintWriter out, Person person, String email, String password) {
        em.getTransaction().begin();
        User u = new User();
        u.setPerson(person);
        u.setUserName(email);
        u.setEmail(email);
        u.setEmailVerified(true);
        u.setTempGuid(UUID.randomUUID().toString());
        u.setGuidExpiration(Date.valueOf(LocalDate.now()));
        u.setGuidUsed(true);
        u.setAllowSetPassword(false);
        try {
            String salt = AuthDAO.generateSalt();
            String hash = AuthDAO.generatePasswordHash(password, salt);
            u.setSalt(salt);
            u.setPasswordHash(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        em.persist(u);
        em.getTransaction().commit();
        log(out, "&nbsp;&nbsp;Created user: <strong>" + email + "</strong>");
        return u;
    }

    private void assignRole(EntityManager em, User user, UserRole role) {
        em.getTransaction().begin();
        user.addUserToRole(role);
        em.merge(user);
        em.getTransaction().commit();
    }

    private void seedFilterPresets(EntityManager em, User user) {
        em.getTransaction().begin();

        UserFilterPreset p1 = new UserFilterPreset();
        p1.setUser(user);
        p1.setSlotNumber(1);
        p1.setLabel("My Actionable");
        p1.setViewRenewal(true);
        p1.setViewSetup(true);
        p1.setViewTicket(true);
        p1.setViewOpportunity(true);
        p1.setOwnershipFilter(1);
        p1.setAttentionFilter(1);
        p1.setSortAlphabetically(false);
        em.persist(p1);

        UserFilterPreset p2 = new UserFilterPreset();
        p2.setUser(user);
        p2.setSlotNumber(2);
        p2.setLabel("All Open");
        p2.setViewRenewal(true);
        p2.setViewSetup(true);
        p2.setViewTicket(true);
        p2.setViewOpportunity(true);
        p2.setOwnershipFilter(0);
        p2.setAttentionFilter(0);
        p2.setSortAlphabetically(true);
        em.persist(p2);

        UserFilterPreset p3 = new UserFilterPreset();
        p3.setUser(user);
        p3.setSlotNumber(3);
        p3.setLabel("My Renewals");
        p3.setViewRenewal(true);
        p3.setViewSetup(false);
        p3.setViewTicket(false);
        p3.setViewOpportunity(false);
        p3.setOwnershipFilter(1);
        p3.setAttentionFilter(0);
        p3.setSortAlphabetically(true);
        em.persist(p3);

        em.getTransaction().commit();
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════════════════════════════

    private void log(PrintWriter out, String msg) {
        out.println(msg);
        out.flush();
    }

    private String escapeHtml(String text) {
        if (text == null) return "null";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
