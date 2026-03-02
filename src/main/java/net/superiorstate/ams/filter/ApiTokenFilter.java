package net.superiorstate.ams.filter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.superiorstate.ams.AppConfig;

import java.io.IOException;

@WebFilter(urlPatterns = "/api/*")
public class ApiTokenFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String uri = request.getRequestURI();

        // Skip auth for partnership handshake endpoints
        if (uri.endsWith("/api/v1/partnership/request") || uri.endsWith("/api/v1/partnership/approve")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            sendUnauthorized(response);
            return;
        }

        EntityManagerFactory emf = (EntityManagerFactory) request.getServletContext().getAttribute("emf");
        if (emf == null) {
            sendUnauthorized(response);
            return;
        }

        EntityManager em = emf.createEntityManager();
        try {
            Object partner = lookupPartnerByToken(em, token);
            if (partner == null) {
                sendUnauthorized(response);
                return;
            }
            request.setAttribute("authenticatedPartner", partner);
            chain.doFilter(request, response);
        } finally {
            if (em.isOpen()) em.close();
        }
    }

    private Object lookupPartnerByToken(EntityManager em, String token) {
        try {
            if (AppConfig.isPsp()) {
                // PSP side: look up BpoRegistration by inbound token
                return em.createQuery(
                                "SELECT b FROM BpoRegistration b WHERE b.apiTokenInbound = :token AND b.isActive = true")
                        .setParameter("token", token)
                        .getSingleResult();
            } else if (AppConfig.isBpo()) {
                // BPO side: look up PspClient by inbound token
                return em.createQuery(
                                "SELECT p FROM PspClient p WHERE p.apiTokenInbound = :token AND p.isActive = true")
                        .setParameter("token", token)
                        .getSingleResult();
            }
        } catch (Exception e) {
            // No match found
        }
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Unauthorized\"}");
    }
}
