package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationSection;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.Feature;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "ServiceManagerSort", value = "/ServiceManagerSort")
public class ServiceManagerSort extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // T123 hardening: the Service Manager sibling S9-F could not reach. Reached by
        // fetch('ServiceManagerSort', …) at serviceManager25.jsp:1601 rather than a form action,
        // which is why a form-action grep missed it; it reorders LOS, Enhancement,
        // ApplicationSection, Feature and ApplicationField rows and had no server-side check.
        // Deliberately placed BEFORE setContentType/getWriter — sendError after the writer has
        // been acquired can throw IllegalStateException.
        // Same shape as AgencyAction.doPost's V067 guard — deliberately identical, not improved.
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

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
                switch (type) {
                    case "los" -> {
                        long id = Long.parseLong(idStr);
                        LOS los = EntityLookup.getLosById(em, id);
                        if (los != null) {
                            los.setSortOrder(sortOrder);
                            em.merge(los);
                        }
                    }
                    case "enhancement" -> {
                        long id = Long.parseLong(idStr);
                        Enhancement enh = em.find(Enhancement.class, id);
                        if (enh != null) {
                            enh.setSortOrder(sortOrder);
                            em.merge(enh);
                        }
                    }
                    case "appSection" -> {
                        long id = Long.parseLong(idStr);
                        ApplicationSection section = em.find(ApplicationSection.class, id);
                        if (section != null) {
                            section.setSortOrder(sortOrder);
                            em.merge(section);
                        }
                    }
                    case "feature" -> {
                        long id = Long.parseLong(idStr);
                        Feature feature = em.find(Feature.class, id);
                        if (feature != null) {
                            feature.setSortOrder(sortOrder);
                            em.merge(feature);
                        }
                    }
                    case "appField" -> {
                        // ApplicationField PK is String (field_key), not Long
                        ApplicationField field = em.find(ApplicationField.class, idStr);
                        if (field != null) {
                            field.setSortOrder(sortOrder);
                            em.merge(field);
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
