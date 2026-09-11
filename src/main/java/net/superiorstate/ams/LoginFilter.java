package net.superiorstate.ams;

import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.superiorstate.ams.data.AmsDataLocal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@WebFilter("/*")
public class LoginFilter implements Filter {

    private static final Set<String> ALLOWED_ENDPOINTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "", "/login", "/LogOut", "/ResetLogin", "/AuthenticateUser", "/OneTimeUserLogin",
            "/NeedsHelp", "/HelpUserLogin", "/InitializeDataBase", "/index.jsp", "/EmployerBillingDetail", "/initialize.jsp", "/GoInitialize25",
            "/landing-page.jsp", "/ShowFileUpload", "/AcceptInvite",
            "/RequestQuote"
    )));

    /** Paths that should still work even when DB is uninitialized */
    private static final Set<String> INIT_ALLOWED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "/initialize.jsp", "/GoInitialize25", "/InitializeDataBase"
    )));

    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession(false);

        String path = request.getRequestURI().substring(request.getContextPath().length()).replaceAll("[/]+$", "");

        // Always allow static resources (images, css, js, fonts, icons)
        if (isStaticResource(path)) {
            chain.doFilter(req, res);
            return;
        }

        // Allow API endpoints (authenticated via ApiTokenFilter, not session)
        if (path.startsWith("/api/")) {
            chain.doFilter(req, res);
            return;
        }

        // Check if database is uninitialized
        boolean uninitialized = false;
        try {
            Integer uninit = (Integer) request.getSession().getAttribute("uninitialized");
            uninitialized = (uninit != null && uninit == 0);
        } catch (Exception ignored) {}

        // If uninitialized, only allow init-related paths — redirect everything else to initialize.jsp
        if (uninitialized) {
            if (INIT_ALLOWED.contains(path)) {
                chain.doFilter(req, res);
            } else {
                response.sendRedirect(request.getContextPath() + "/initialize.jsp");
            }
            return;
        }

        // Normal auth check
        boolean isAuthenticated = false;
        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            isAuthenticated = local.isAuthenticated();
        } catch (Exception e) {
        }

        boolean loggedIn = (session != null && isAuthenticated);
        boolean allowedPath = ALLOWED_ENDPOINTS.contains(path) || path.startsWith("/proposal/") || path.startsWith("/apply/") || path.startsWith("/q/") || path.equals("/saveQuestionnaire") || path.startsWith("/tpo") || path.equals("/uploadRateSheet") || path.equals("/saveApplication") || path.startsWith("/video") || path.startsWith("/census-drop/") || path.startsWith("/outlook/");

        if (loggedIn || allowedPath) {
            chain.doFilter(req, res);
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    private boolean isStaticResource(String path) {
        return path.startsWith("/images/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/fonts/")
                || path.startsWith("/webfonts/")
                || path.startsWith("/bootstrap-icons/")
                || path.startsWith("/branding/")
                || path.startsWith("/logo")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".gif")
                || path.endsWith(".svg")
                || path.endsWith(".ico");
    }
}
