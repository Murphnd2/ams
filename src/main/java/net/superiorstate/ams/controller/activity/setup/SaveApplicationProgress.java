package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.model.sales.agency.Proposal;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationFieldValue;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet(name = "SaveApplicationProgress", value = "/saveApplication")
@MultipartConfig
public class SaveApplicationProgress extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String guid = request.getParameter("guid");
        if (guid == null || guid.isEmpty()) {
            response.setStatus(400);
            out.print("{\"error\":\"Missing GUID\"}");
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        try {
            Query q = em.createQuery("SELECT p FROM Proposal p LEFT JOIN FETCH p.losList LEFT JOIN FETCH p.application WHERE p.applicationGUID = :guid");
            q.setParameter("guid", guid);
            List<Proposal> results = q.getResultList();
            if (results.isEmpty()) {
                response.setStatus(404);
                out.print("{\"error\":\"Proposal not found\"}");
                return;
            }
            Proposal proposal = results.get(0);

            // Create or get application
            Application application = proposal.getApplication();
            if (application == null) {
                em.getTransaction().begin();
                application = new Application();
                application.setProposal(proposal);
                application.setStatus("IN_PROGRESS");
                application.setDateStarted(Timestamp.from(Instant.now()));
                em.persist(application);
                em.getTransaction().commit();
            }

            // Get applicable fields
            List<Long> losIds = proposal.getLosList().stream()
                    .map(LOS::getId).collect(Collectors.toList());

            Query fq = em.createQuery(
                    "SELECT DISTINCT f FROM ApplicationField f " +
                            "JOIN f.applicationSection s " +
                            "LEFT JOIN s.losList los " +
                            "WHERE s.scope = 'ALL' OR los.id IN :losIds");
            fq.setParameter("losIds", losIds);
            List<ApplicationField> fields = fq.getResultList();

            // Save values
            em.getTransaction().begin();
            for (ApplicationField field : fields) {
                String paramValue = request.getParameter(field.getFieldKey());

                if ("CHECKBOX".equals(field.getFieldType())) {
                    String[] values = request.getParameterValues(field.getFieldKey());
                    paramValue = (values != null) ? String.join("|", values) : null;
                }

                // Find existing value
                Query existQ = em.createQuery(
                        "SELECT v FROM ApplicationFieldValue v " +
                                "WHERE v.application = :app AND v.applicationField = :field");
                existQ.setParameter("app", application);
                existQ.setParameter("field", field);
                List<ApplicationFieldValue> existing = existQ.getResultList();

                if (paramValue != null && !paramValue.trim().isEmpty()) {
                    if (!existing.isEmpty()) {
                        existing.get(0).setFieldValue(paramValue.trim());
                        em.merge(existing.get(0));
                    } else {
                        ApplicationFieldValue fv = new ApplicationFieldValue();
                        fv.setApplication(application);
                        fv.setApplicationField(field);
                        fv.setFieldValue(paramValue.trim());
                        em.persist(fv);
                    }
                } else if (!existing.isEmpty()) {
                    // Clear previously saved value if now empty
                    em.remove(existing.get(0));
                }
            }
            em.getTransaction().commit();

            out.print("{\"status\":\"saved\",\"timestamp\":\"" + Instant.now().toString() + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"error\":\"Save failed\"}");
        } finally {
            em.close();
        }
    }
}