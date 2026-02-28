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

            // Validate form field lengths before initialization
            String validationError = validateFormFields(request);
            if (validationError != null) {
                request.setAttribute("initError", validationError);
                request.getRequestDispatcher("initialize.jsp").forward(request, response);
                return;
            }

            // Run standard initialization
            try {
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
            } catch (Exception e) {
                System.out.println("⛔ InitializeDataBase failed: " + e.getMessage());
                request.setAttribute("initError", "Initialization failed: " + e.getMessage());
                request.getRequestDispatcher("initialize.jsp").forward(request, response);
                return;
            }

        } finally {
            if (em.isOpen()) em.close();
        }

        response.sendRedirect("index.jsp");
    }

    private String validateFormFields(HttpServletRequest request) {
        String[][] checks = {
            {"pspName",   "Company Name",    "200"},
            {"phone",     "Phone",           "12"},
            {"taxId",     "Tax ID",          "10"},
            {"address",   "Street Address",  "100"},
            {"city",      "City",            "50"},
            {"state",     "State",           "2"},
            {"zipCode",   "Zip",             "10"},
            {"firstName", "First Name",      "50"},
            {"lastName",  "Last Name",       "80"},
            {"email",     "Email",           "100"},
        };
        for (String[] check : checks) {
            String value = request.getParameter(check[0]);
            if (value != null && value.length() > Integer.parseInt(check[2])) {
                return check[1] + " exceeds maximum length of " + check[2] + " characters.";
            }
        }
        return null;
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
