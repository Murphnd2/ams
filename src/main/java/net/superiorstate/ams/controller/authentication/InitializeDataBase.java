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

            // D-05: Validate deployment key — format: {TYPE}-{KEY} or {TYPE}-{KEY}-{DEMOTAG}
            //   Examples: PSP-mykey123, BPO-mykey123, PSP-mykey123-DEMO
            String submittedKey = request.getParameter("deploymentKey");
            String expectedKey = AppConfig.get("DEPLOYMENT_KEY");

            if (expectedKey == null || expectedKey.isBlank()) {
                System.out.println("⛔ InitializeDataBase blocked — no DEPLOYMENT_KEY in ssa.properties");
                response.sendRedirect("index.jsp");
                return;
            }

            // Parse: first segment = system type, second = key, optional third = demo tag
            String systemType = null;
            String baseKey = null;
            String demoTag = null;
            if (submittedKey != null && !submittedKey.isBlank()) {
                String[] parts = submittedKey.split("-", 3);
                if (parts.length >= 2) {
                    systemType = parts[0].toUpperCase();
                    baseKey = parts[1];
                    if (parts.length == 3 && !parts[2].isBlank()) {
                        demoTag = parts[2];
                    }
                }
            }

            if (systemType == null || (!"PSP".equals(systemType) && !"BPO".equals(systemType))) {
                System.out.println("⛔ InitializeDataBase blocked — missing or invalid system type prefix (expected PSP- or BPO-)");
                response.sendRedirect("GoInitialize25");
                return;
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

            // Run initialization based on system type
            try {
                System.out.println("🚀 InitializeDataBase — key validated, type=" + systemType + ", starting initialization...");

                if ("BPO".equals(systemType)) {
                    DatabaseInitializer.initializeBpoDataBase(request, em);
                    System.out.println("✅ InitializeDataBase — BPO initialization complete");
                } else {
                    DatabaseInitializer.initializeDataBase(request, em);
                    System.out.println("✅ InitializeDataBase — PSP initialization complete");

                    // Run optional demo seeder if tag was provided (PSP only)
                    if (demoTag != null && !demoTag.isBlank()) {
                        System.out.println("🎭 Demo tag detected: " + demoTag);
                        DatabaseInitializer.seedDemoData(em, demoTag);
                    }
                }

                // Load global data so a server restart is not required
                AmsDataGlobal global = new AmsDataGlobal();
                global.initializeGlobalData(em);
                getServletContext().setAttribute("global", global);

                // Refresh system type attributes from DB constant
                getServletContext().setAttribute("systemType", AppConfig.getSystemType());
                getServletContext().setAttribute("isBpoSystem", AppConfig.isBpo());
                getServletContext().setAttribute("isPspSystem", AppConfig.isPsp());
                System.out.println("✅ Global data loaded after initialization (systemType=" + AppConfig.getSystemType() + ")");

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
