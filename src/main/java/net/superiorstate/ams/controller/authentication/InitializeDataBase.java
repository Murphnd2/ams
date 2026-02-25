package net.superiorstate.ams.controller.authentication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.service.DatabaseInitializer;
import net.superiorstate.ams.model.Constant;

import java.io.IOException;

@WebServlet(name = "InitializeDataBase", value = "/InitializeDataBase")
public class InitializeDataBase extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET not supported.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // D-06: Prevent re-initialization
        EntityManagerFactory emf = (EntityManagerFactory) getServletContext().getAttribute("emf");
        EntityManager em = emf.createEntityManager();
        try {
            if (isAlreadyInitialized(em)) {
                System.out.println("⛔ InitializeDataBase blocked — database already initialized");
                response.sendRedirect("index.jsp");
                return;
            }

            // D-05: Validate deployment key (with optional demo tag support)
            String submittedKey = request.getParameter("deploymentKey");
            String expectedKey = AppConfig.get("DEPLOYMENT_KEY");

            if (expectedKey == null || expectedKey.isBlank()) {
                System.out.println("⛔ InitializeDataBase blocked — no DEPLOYMENT_KEY in ssa.properties");
                response.sendRedirect("index.jsp");
                return;
            }

            // Parse optional demo tag: key contains no hyphens by convention.
            // If a hyphen is present, everything before it is the key, everything after is the tag.
            String baseKey = submittedKey;
            String demoTag = null;
            if (submittedKey != null) {
                int hyphen = submittedKey.indexOf('-');
                if (hyphen > 0) {
                    baseKey = submittedKey.substring(0, hyphen);
                    demoTag = submittedKey.substring(hyphen + 1);
                }
            }

            if (!expectedKey.equals(baseKey)) {
                System.out.println("⛔ InitializeDataBase blocked — invalid deployment key");
                response.sendRedirect("GoInitialize25");
                return;
            }

            // Run standard initialization
            System.out.println("🚀 InitializeDataBase — key validated, starting initialization...");
            DatabaseInitializer.initializeDataBase(request, em);
            System.out.println("✅ InitializeDataBase — initialization complete");

            // Run optional demo seeder if tag was provided
            if (demoTag != null && !demoTag.isBlank()) {
                System.out.println("🎭 Demo tag detected: " + demoTag);
                DatabaseInitializer.seedDemoData(em, demoTag);
            }

            // Load global data so a server restart is not required
            AmsDataGlobal global = new AmsDataGlobal();
            global.initializeGlobalData(em);
            getServletContext().setAttribute("global", global);
            System.out.println("✅ Global data loaded after initialization");

            // Fix session so LoginFilter stops redirecting to initialize.jsp
            request.getSession().setAttribute("uninitialized", 1);

        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect("index.jsp");
    }

    private boolean isAlreadyInitialized(EntityManager em) {
        try {
            Query q = em.createQuery("SELECT c FROM Constant c WHERE c.name = :name");
            q.setParameter("name", "SSL_PORT");
            Constant c = (Constant) q.getSingleResult();
            return c != null && "443".equals(c.getValue());
        } catch (Exception e) {
            return false;
        }
    }
}
