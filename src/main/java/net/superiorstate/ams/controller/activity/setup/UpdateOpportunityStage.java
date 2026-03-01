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
