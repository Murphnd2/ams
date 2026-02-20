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
