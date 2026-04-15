package net.superiorstate.ams.controller.market;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.activity.note.Note;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Prospect;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet(name = "RequestQuote", value = "/RequestQuote")
public class RequestQuote extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (global == null) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "System not initialized");
            return;
        }
        request.setAttribute("losList", global.getLosList());
        request.setAttribute("pspName", global.getPsp() != null ? global.getPsp().getFullName() : "");
        request.setAttribute("logoNavbar", global.getLogoNavbar());
        request.setAttribute("favicon", global.getFavicon());
        request.getRequestDispatcher("/WEB-INF/view/market/requestQuote25.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");
        if (global == null) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "System not initialized");
            return;
        }

        // Extract form fields
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String contactMethod = request.getParameter("contactMethod");
        String companyName = request.getParameter("companyName");
        String employeeCountStr = request.getParameter("employeeCount");
        String[] losIds = request.getParameterValues("losIds");
        String additionalInfo = request.getParameter("additionalInfo");

        // --- Bot detection ---
        // 1. Honeypot: invisible field that only bots fill in
        String honeypot = request.getParameter("website");
        if (honeypot != null && !honeypot.isBlank()) {
            System.out.println("[RequestQuote] Bot detected: honeypot filled. Dropping submission.");
            request.setAttribute("submitted", true);
            setGlobalAttrs(request, global);
            request.getRequestDispatcher("/WEB-INF/view/market/requestQuote25.jsp").forward(request, response);
            return;
        }
        // 2. Time check: form must be open for at least 3 seconds
        String loadedAt = request.getParameter("formLoadedAt");
        if (loadedAt != null && !loadedAt.isBlank()) {
            try {
                long loadTime = Long.parseLong(loadedAt);
                long elapsed = System.currentTimeMillis() - loadTime;
                if (elapsed < 3000) {
                    System.out.println("[RequestQuote] Bot detected: form submitted in " + elapsed + "ms. Dropping submission.");
                    request.setAttribute("submitted", true);
                    setGlobalAttrs(request, global);
                    request.getRequestDispatcher("/WEB-INF/view/market/requestQuote25.jsp").forward(request, response);
                    return;
                }
            } catch (NumberFormatException ignored) {}
        }

        // Validate required fields
        boolean contactValid = "phone".equals(contactMethod) ? !isBlank(phone) : !isBlank(email);
        if (isBlank(firstName) || isBlank(lastName) || isBlank(companyName) || !contactValid) {
            request.setAttribute("error", "Please fill in all required fields.");
            repopulateForm(request, global, firstName, lastName, email, phone, contactMethod,
                    companyName, employeeCountStr, additionalInfo);
            request.getRequestDispatcher("/WEB-INF/view/market/requestQuote25.jsp").forward(request, response);
            return;
        }

        // Check home agency is configured
        Long homeAgencyId = global.getPspHomeAgencyId();
        if (homeAgencyId == null) {
            request.setAttribute("error", "Quote requests are not yet available. Please contact us directly.");
            setGlobalAttrs(request, global);
            request.getRequestDispatcher("/WEB-INF/view/market/requestQuote25.jsp").forward(request, response);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            // Resolve agency manager
            long managerId = global.getAgencyManagerId(homeAgencyId);
            Person manager;
            if (managerId > 0) {
                manager = EntityLookup.getPersonById(em, managerId);
            } else {
                Agency homeAgency = EntityLookup.getAgencyById(em, homeAgencyId);
                manager = (homeAgency != null && homeAgency.getManager() != null)
                        ? homeAgency.getManager()
                        : null;
            }
            if (manager == null) {
                request.setAttribute("error", "Quote requests are not yet available. Please contact us directly.");
                setGlobalAttrs(request, global);
                request.getRequestDispatcher("/WEB-INF/view/market/requestQuote25.jsp").forward(request, response);
                return;
            }

            Agency agency = EntityLookup.getAgencyById(em, homeAgencyId);

            // 1. Create Person contact
            em.getTransaction().begin();
            Person contact = new Person();
            contact.setFirstName(firstName.trim());
            contact.setLastName(lastName.trim());
            contact.setFullName(firstName.trim() + " " + lastName.trim());
            if (email != null && !email.isBlank()) contact.setEmail(email.trim());
            if (phone != null && !phone.isBlank()) contact.setPhone(phone.trim());
            contact.setPsp(global.getPsp());
            em.persist(contact);
            em.getTransaction().commit();

            // 2. Create Prospect
            em.getTransaction().begin();
            Prospect prospect = new Prospect();
            prospect.setName(companyName.trim());
            prospect.setContact(contact);
            prospect.setAgent(manager);
            em.persist(prospect);
            em.getTransaction().commit();

            // 3. Create Opportunity
            Integer employeeCount = null;
            if (employeeCountStr != null && !employeeCountStr.isBlank()) {
                try { employeeCount = Integer.parseInt(employeeCountStr.trim()); } catch (NumberFormatException ignored) {}
            }

            em.getTransaction().begin();
            Opportunity opp = new Opportunity();
            opp.setProspect(prospect);
            opp.setAgency(agency);
            opp.setStage("NEW");
            opp.setAssignedTo(manager);
            opp.setLoggedBy(manager);
            opp.setManagedBy(manager);
            opp.setPrimaryContact(contact);
            opp.setFullName(companyName.trim().toUpperCase());
            opp.setDueDate(Date.valueOf(LocalDate.now().plusDays(30)));
            opp.setComplete(false);
            opp.setEstimatedEmployees(employeeCount);
            em.persist(opp);
            em.getTransaction().commit();

            // 4. Create CheckList
            em.getTransaction().begin();
            CheckList c = new CheckList();
            c.setAssignedTo(opp);
            c.setComplete(false);
            c.setFullName(companyName.trim().toUpperCase() + " Checklist");
            c.setDueDate(opp.getDueDate());
            c.setLoggedBy(manager);
            em.persist(c);
            em.getTransaction().commit();

            // 5. Add default ToDo (task 153 - pre-completed placeholder)
            em.getTransaction().begin();
            ToDo toDo = new ToDo();
            toDo.setTask(EntityLookup.getTaskById(em, 153L));
            toDo.setSortOrder(1000);
            toDo.setCheckList(c);
            toDo.setComplete(true);
            em.persist(toDo);
            em.getTransaction().commit();

            // 6. Link CheckList back to Opportunity
            CheckList checkList = EntityLookup.getCheckListById(em, c.getId());
            em.getTransaction().begin();
            opp.setCheckList(checkList);
            em.persist(opp);
            em.getTransaction().commit();

            // 7. Create Note with quote details (services + additional info)
            String noteText = buildNoteText(losIds, global.getLosList(), additionalInfo, contactMethod);
            if (noteText != null) {
                em.getTransaction().begin();
                Note note = new Note();
                note.setDetail(noteText);
                note.setActivity(opp);
                note.setCreatedBy(manager);
                note.setDateGenerated(Date.valueOf(LocalDate.now()));
                note.setReasonCreated(EntityLookup.getReasonById(em, 1));   // Internal Note
                note.setStatus(EntityLookup.getActivityStatusById(em, 2)); // No Change
                em.persist(note);
                em.getTransaction().commit();
            }

            // 8. Refresh global sales data cache
            global.refreshSalesData(em);
            getServletContext().setAttribute("global", global);

            // Show confirmation
            request.setAttribute("submitted", true);
            request.setAttribute("companyName", companyName.trim());
            request.setAttribute("pspName", global.getPsp() != null ? global.getPsp().getFullName() : "");
            request.setAttribute("logoNavbar", global.getLogoNavbar());
            request.setAttribute("favicon", global.getFavicon());
            request.getRequestDispatcher("/WEB-INF/view/market/requestQuote25.jsp").forward(request, response);

        } catch (Exception e) {
            System.out.println("RequestQuote FAILED: " + e.getMessage());
            e.printStackTrace();
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            request.setAttribute("error", "An error occurred submitting your request. Please try again.");
            setGlobalAttrs(request, global);
            request.getRequestDispatcher("/WEB-INF/view/market/requestQuote25.jsp").forward(request, response);
        } finally {
            em.close();
        }
    }

    private String buildNoteText(String[] losIds, List<LOS> losList, String additionalInfo, String contactMethod) {
        StringBuilder sb = new StringBuilder();
        sb.append("Quote request via website.<br>");
        sb.append("Preferred contact: ").append("phone".equals(contactMethod) ? "Phone" : "Email").append("<br>");

        // Build services line using short names
        if (losIds != null && losIds.length > 0 && losList != null) {
            StringBuilder services = new StringBuilder();
            for (String idStr : losIds) {
                try {
                    long losId = Long.parseLong(idStr);
                    for (LOS los : losList) {
                        if (los.getId() == losId) {
                            if (services.length() > 0) services.append(", ");
                            services.append(los.getShortText());
                            break;
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (services.length() > 0) {
                sb.append("Requested services: ").append(services).append("<br>");
            }
        }

        if (additionalInfo != null && !additionalInfo.isBlank()) {
            sb.append("Additional info: ").append(additionalInfo.trim());
        }

        return sb.toString();
    }

    private void repopulateForm(HttpServletRequest request, AmsDataGlobal global,
                                String firstName, String lastName, String email, String phone,
                                String contactMethod, String companyName, String employeeCount,
                                String additionalInfo) {
        request.setAttribute("firstName", firstName);
        request.setAttribute("lastName", lastName);
        request.setAttribute("email", email);
        request.setAttribute("phone", phone);
        request.setAttribute("contactMethod", contactMethod);
        request.setAttribute("companyName", companyName);
        request.setAttribute("employeeCount", employeeCount);
        request.setAttribute("additionalInfo", additionalInfo);
        setGlobalAttrs(request, global);
    }

    private void setGlobalAttrs(HttpServletRequest request, AmsDataGlobal global) {
        request.setAttribute("losList", global.getLosList());
        request.setAttribute("pspName", global.getPsp() != null ? global.getPsp().getFullName() : "");
        request.setAttribute("logoNavbar", global.getLogoNavbar());
        request.setAttribute("favicon", global.getFavicon());
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
