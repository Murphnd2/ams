package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Opportunity;
import net.superiorstate.ams.model.general.Person;

import java.io.IOException;
import java.sql.Date;

@WebServlet(name = "UpdateOpportunityStage", value = "/UpdateOpportunityStage")
public class UpdateOpportunityStage extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long oppId = Long.parseLong(request.getParameter("oppId"));
            Opportunity opp = EntityLookup.getOpportunityById(em, oppId);
            if (opp == null) return;

            em.getTransaction().begin();

            // Update stage if provided
            String stage = request.getParameter("stage");
            if (stage != null) {
                opp.setStage(stage);
                if ("WON".equals(stage) || "LOST".equals(stage)) {
                    opp.setComplete(true);
                }
            }

            // Update managedBy if provided
            String managedByParam = request.getParameter("managedById");
            if (managedByParam != null) {
                if (managedByParam.isEmpty() || "0".equals(managedByParam)) {
                    opp.setManagedBy(null);
                } else {
                    Person manager = EntityLookup.getPersonById(em, Long.parseLong(managedByParam));
                    if (manager != null) {
                        opp.setManagedBy(manager);
                    }
                }
            }

            // Update estimatedEmployees if provided
            String eeParam = request.getParameter("estimatedEmployees");
            if (eeParam != null) {
                if (eeParam.isBlank()) {
                    opp.setEstimatedEmployees(null);
                } else {
                    try { opp.setEstimatedEmployees(Integer.parseInt(eeParam.trim())); } catch (NumberFormatException ignored) {}
                }
            }

            // Update estimatedValue if provided
            String valParam = request.getParameter("estimatedValue");
            if (valParam != null) {
                if (valParam.isBlank()) {
                    opp.setEstimatedValue(null);
                } else {
                    try { opp.setEstimatedValue(Double.parseDouble(valParam.trim())); } catch (NumberFormatException ignored) {}
                }
            }

            // Update expectedCloseDate if provided
            String closeDateParam = request.getParameter("expectedCloseDate");
            if (closeDateParam != null) {
                if (closeDateParam.isBlank()) {
                    opp.setExpectedCloseDate(null);
                } else {
                    try { opp.setExpectedCloseDate(Date.valueOf(closeDateParam.trim())); } catch (IllegalArgumentException ignored) {}
                }
            }

            em.persist(opp);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }

        // Support AJAX calls (from activity detail) vs redirect (from agent pipeline)
        String ajax = request.getParameter("ajax");
        if ("true".equals(ajax)) {
            response.setContentType("application/json");
            response.getWriter().write("{\"ok\":true}");
        } else {
            response.sendRedirect("AgentHome");
        }
    }
}
