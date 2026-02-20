package net.superiorstate.ams.controller.activity.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.sales.agency.PriceItem;

import java.io.IOException;

@WebServlet(name = "PriceItemAction", value = "/PriceItemAction")
public class PriceItemAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        response.setContentType("application/json");

        String action = request.getParameter("action");

        try {
            switch (action) {

                case "reorder" -> {
                    // Expects comma-separated list of price item IDs in new order
                    String orderStr = request.getParameter("order");
                    if (orderStr != null && !orderStr.isEmpty()) {
                        String[] ids = orderStr.split(",");
                        em.getTransaction().begin();
                        for (int i = 0; i < ids.length; i++) {
                            long piId = Long.parseLong(ids[i].trim());
                            PriceItem pi = EntityLookup.getPriceItemById(em, piId);
                            if (pi != null) {
                                pi.setSortOrder((i + 1) * 100);
                                em.merge(pi);
                            }
                        }
                        em.getTransaction().commit();
                    }
                    response.getWriter().write("{\"status\":\"ok\"}");
                }

                case "toggleSuppress" -> {
                    long piId = Long.parseLong(request.getParameter("priceItemId"));
                    PriceItem pi = EntityLookup.getPriceItemById(em, piId);
                    if (pi != null) {
                        pi.setSuppressed(!pi.isSuppressed());
                        em.getTransaction().begin();
                        em.merge(pi);
                        em.getTransaction().commit();
                        response.getWriter().write("{\"status\":\"ok\",\"suppressed\":" + pi.isSuppressed() + "}");
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