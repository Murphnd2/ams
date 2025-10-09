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
            "/NeedsHelp", "/HelpUserLogin", "/InitializeDataBase", "/index.jsp", "/EmployerBillingDetail", "/initialize.jsp", "/GoInitialize25"
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

        boolean isAuthenticated = false;
        try {
            AmsDataLocal local = (AmsDataLocal) request.getSession().getAttribute("local");
            isAuthenticated = local.isAuthenticated();
        } catch (Exception e) {
            // optional: log the error
        }

        boolean loggedIn = (session != null && isAuthenticated);
        boolean allowedPath = isAllowedPath(path);

        if (loggedIn || allowedPath) {
            chain.doFilter(req, res);
            if (loggedIn)
                System.out.println("LoggedIn");
            else
                System.out.println("Not logged in but PATH GOOD");
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    private boolean isAllowedPath(String path) {
        return ALLOWED_ENDPOINTS.contains(path) || isStaticResource(path);
    }

    private boolean isStaticResource(String path) {
        return path.startsWith("/images/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/fonts/")
                || path.startsWith("/webfonts/")
                || path.startsWith("/bootstrap-icons/")
                || path.startsWith("/logo") // e.g., /logo.png, /logoC.png
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".gif")
                || path.endsWith(".svg")
                || path.endsWith(".ico");
    }
}

