package net.superiorstate.ams.controller.authentication;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.AmsDataLocal;

import java.io.IOException;

@WebServlet(name = "login", urlPatterns = {"/login", ""})
public class login extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        routeLogin(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        routeLogin(request, response);
    }

    private void routeLogin(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // If already authenticated, redirect to appropriate home page
        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            if (local != null && local.isAuthenticated()) {
                boolean isBpo = Boolean.TRUE.equals(request.getSession().getAttribute("isBpo"));
                boolean isBpoAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoAdmin"));
                boolean isBpoUser = Boolean.TRUE.equals(request.getSession().getAttribute("isBpoUser"));
                boolean isAgent = Boolean.TRUE.equals(request.getSession().getAttribute("isAgent"));
                boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));

                boolean isPspUser = Boolean.TRUE.equals(request.getSession().getAttribute("isPspUser"));
                boolean isPspAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));

                if (isBpo || isBpoAdmin || isBpoUser) {
                    response.sendRedirect("BpoHome");
                } else if (isPspUser || isPspAdmin) {
                    response.sendRedirect("ViewHome25");
                } else if (isAgent || isAgencyAdmin) {
                    response.sendRedirect("AgentHome");
                } else {
                    response.sendRedirect("ViewHome25");
                }
                return;
            }
        } catch (Exception ignored) {}

        // Unauthenticated — show custom landing or default login
        AmsDataGlobal global = (AmsDataGlobal) getServletContext().getAttribute("global");

        // V068: host-header custom agency landing. When AMS is reached over a NON-PSP host
        // that maps to an agency with a non-blank landing page, serve that agency's white-label
        // landing (PSP header chrome suppressed) instead of the default landing/login. PSP hosts,
        // unmatched hosts, and matched-but-blank hosts fall through untouched to the PSP block below.
        if (global != null) {
            String host = request.getServerName();
            if (!global.isPspHost(host)) {
                String agencyHtml = global.getLandingHtmlForHost(host);
                if (agencyHtml != null && !agencyHtml.isBlank()) {
                    request.setAttribute("landingHtml", agencyHtml);
                    request.setAttribute("whiteLabel", Boolean.TRUE);
                    request.setAttribute("brandName", global.getAgencyNameForHost(host));
                    // Defense-in-depth on this public, pre-login page.
                    response.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
                    response.setHeader("X-Content-Type-Options", "nosniff");
                    request.getRequestDispatcher("/WEB-INF/view/authentication/customLanding25.jsp")
                           .forward(request, response);
                    return;
                }
            }
        }

        if (global != null && global.isUseCustomLanding()) {
            String html = global.getCustomLandingHtml();
            if (html != null && !html.isBlank()) {
                request.setAttribute("landingHtml", html);
                request.getRequestDispatcher("/WEB-INF/view/authentication/customLanding25.jsp")
                       .forward(request, response);
                return;
            }
        }
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}
