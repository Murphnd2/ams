package net.superiorstate.ams.previous.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
@WebFilter("/*")
public class CsrfFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        HttpSession session = request.getSession();

        // Always create token if missing
        if (session.getAttribute("csrfToken") == null) {
            session.setAttribute("csrfToken", UUID.randomUUID().toString());
        }

        // PROTECT ONLY THE EMAIL-SENDING SERVLET
        if (request.getParameter("sendAutoEmail") != null) {   // we will add this hidden param in step 3
            String token = request.getParameter("csrf");
            if (token == null || !token.equals(session.getAttribute("csrfToken"))) {
                response.sendError(403, "CSRF protection: invalid token");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}