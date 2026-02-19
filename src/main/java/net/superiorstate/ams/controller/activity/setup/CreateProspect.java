package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataLocal;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Agency;
import net.superiorstate.ams.model.sales.agency.Prospect;

import java.io.IOException;

@WebServlet(name = "CreateProspect", value = "/CreateProspect")
public class CreateProspect extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
        Prospect prospect = null;
        try {
            String prospectName = request.getParameter("prospectName");
            String contactFirst = request.getParameter("contactFirst");
            String contactLast = request.getParameter("contactLast");
            String contactEmail = request.getParameter("contactEmail");
            String contactPhone = request.getParameter("contactPhone");
            long agencyId = Long.parseLong(request.getParameter("agencyId"));

            Agency agency = em.find(Agency.class, agencyId);

            // Get agent from agency (same pattern as GenerateProp)
            Person agent = null;
            if (agency.getAgentList() != null && !agency.getAgentList().isEmpty()) {
                agent = agency.getAgentList().get(0);
            }

            // Create contact person
            em.getTransaction().begin();
            Person contact = new Person();
            contact.setFirstName(contactFirst);
            contact.setLastName(contactLast);
            contact.setEmail(contactEmail);
            contact.setPhone(contactPhone);
            contact.setPsp(local.getCurrentPerson().getPsp());
            em.persist(contact);
            em.getTransaction().commit();

            // Create prospect
            em.getTransaction().begin();
            prospect = new Prospect();
            prospect.setName(prospectName);
            prospect.setContact(contact);
            prospect.setAgent(agent);
            em.persist(prospect);
            em.getTransaction().commit();

            System.out.println("Prospect created: " + prospectName + " (ID=" + prospect.getId() + ")");

        } finally {
            em.close();
        }

        response.sendRedirect("ProposalBuilder" + (prospect != null ? "?selectedProspect=" + prospect.getId() : ""));
    }
}
