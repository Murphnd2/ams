package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.sales.application.ApplicationSection;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "ServiceManagerSort", value = "/ServiceManagerSort")
public class ServiceManagerSort extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String type = request.getParameter("type");
        String[] ids = request.getParameterValues("ids[]");

        if (type == null || ids == null || ids.length == 0) {
            out.write("{\"status\":\"error\",\"message\":\"Missing parameters\"}");
            return;
        }

        try {
            em.getTransaction().begin();
            int sortOrder = 100;
            for (String idStr : ids) {
                long id = Long.parseLong(idStr);
                switch (type) {
                    case "los" -> {
                        LOS los = EntityLookup.getLosById(em, id);
                        if (los != null) {
                            los.setSortOrder(sortOrder);
                            em.merge(los);
                        }
                    }
                    case "enhancement" -> {
                        Enhancement enh = em.find(Enhancement.class, id);
                        if (enh != null) {
                            enh.setSortOrder(sortOrder);
                            em.merge(enh);
                        }
                    }
                    case "appSection" -> {
                        ApplicationSection section = em.find(ApplicationSection.class, id);
                        if (section != null) {
                            section.setSortOrder(sortOrder);
                            em.merge(section);
                        }
                    }
                }
                sortOrder += 100;
            }
            em.getTransaction().commit();
            out.write("{\"status\":\"ok\"}");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            out.write("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        } finally {
            em.close();
        }
    }
}
