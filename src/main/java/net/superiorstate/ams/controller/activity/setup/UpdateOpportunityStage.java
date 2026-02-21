package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.activity.Opportunity;

import java.io.IOException;

@WebServlet(name = "UpdateOpportunityStage", value = "/UpdateOpportunityStage")
public class UpdateOpportunityStage extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            long oppId = Long.parseLong(request.getParameter("oppId"));
            String stage = request.getParameter("stage");

            Opportunity opp = EntityLookup.getOpportunityById(em, oppId);
            if (opp != null && stage != null) {
                em.getTransaction().begin();
                opp.setStage(stage);
                if ("WON".equals(stage) || "LOST".equals(stage)) {
                    opp.setComplete(true);
                }
                em.persist(opp);
                em.getTransaction().commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
        }
        response.sendRedirect("AgentHome");
    }
}
