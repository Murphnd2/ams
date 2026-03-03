package net.superiorstate.ams.controller.api;

import com.google.gson.Gson;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.model.general.ApprovedVendor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/v1/registry/vendors")
public class VendorRegistryApi extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");

        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        if (emf == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Registry unavailable\"}");
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            List<ApprovedVendor> vendors = em.createQuery(
                            "SELECT v FROM ApprovedVendor v WHERE v.isActive = true ORDER BY v.vendorName",
                            ApprovedVendor.class)
                    .getResultList();

            List<Map<String, String>> result = new ArrayList<>();
            for (ApprovedVendor v : vendors) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("vendorName", v.getVendorName());
                entry.put("vendorUrl", v.getVendorUrl());
                entry.put("description", v.getDescription());
                result.add(entry);
            }

            response.getWriter().write(gson.toJson(result));
        } catch (Exception e) {
            System.err.println("[VENDOR-REGISTRY] Error querying approved vendors: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Registry unavailable\"}");
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"error\": \"Method not allowed\"}");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"error\": \"Method not allowed\"}");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"error\": \"Method not allowed\"}");
    }
}
