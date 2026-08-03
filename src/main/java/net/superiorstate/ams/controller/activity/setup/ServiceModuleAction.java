package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.sales.offering.ServiceModule;

import java.io.IOException;

@WebServlet(name = "ServiceModuleAction", value = "/ServiceModuleAction")
public class ServiceModuleAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // T123 hardening: the second Service Manager sibling. ⚠️ This servlet has NO caller
        // anywhere in the tree — no JSP, no JS, no server-side dispatch (verified S9-G) — yet it
        // remains mapped at /ServiceModuleAction and mutates ServiceModule rows (reorder,
        // toggleSuppress). An unguarded, mapped, mutating endpoint with no UI is arguably worse
        // than one with a UI, because nothing would look wrong if it were exercised.
        // Deliberately placed BEFORE setContentType — sendError after the writer has been
        // acquired can throw IllegalStateException.
        // Same shape as AgencyAction.doPost's V067 guard — deliberately identical, not improved.
        boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
        if (!isPspAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        response.setContentType("application/json");

        String action = request.getParameter("action");

        try {
            switch (action) {

                case "reorder" -> {
                    String orderStr = request.getParameter("order");
                    if (orderStr != null && !orderStr.isEmpty()) {
                        String[] ids = orderStr.split(",");
                        em.getTransaction().begin();
                        for (int i = 0; i < ids.length; i++) {
                            int smId = Integer.parseInt(ids[i].trim());
                            ServiceModule sm = EntityLookup.getServiceModuleById(em, smId);
                            if (sm != null) {
                                sm.setSortOrder((i + 1) * 100);
                                em.merge(sm);
                            }
                        }
                        em.getTransaction().commit();
                    }
                    response.getWriter().write("{\"status\":\"ok\"}");
                }

                case "toggleSuppress" -> {
                    int smId = Integer.parseInt(request.getParameter("moduleId"));
                    ServiceModule sm = EntityLookup.getServiceModuleById(em, smId);
                    if (sm != null) {
                        sm.setSuppressed(!sm.isSuppressed());
                        em.getTransaction().begin();
                        em.merge(sm);
                        em.getTransaction().commit();
                        response.getWriter().write("{\"status\":\"ok\",\"suppressed\":" + sm.isSuppressed() + "}");
                    } else {
                        response.getWriter().write("{\"status\":\"error\",\"message\":\"Not found\"}");
                    }
                }

                default -> response.getWriter().write("{\"status\":\"error\",\"message\":\"Unknown action\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        } finally {
            em.close();
        }
    }
}
