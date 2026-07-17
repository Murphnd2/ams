package net.superiorstate.ams;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.model.Constant;

import net.superiorstate.ams.service.InstallationHealthScheduler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebListener
public class EmfListener implements ServletContextListener, HttpSessionListener, HttpSessionAttributeListener {

    private InstallationHealthScheduler healthScheduler;
    private ExecutorService billingExecutor;

    public EmfListener() {}

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // D-01: Load infrastructure config before anything else
        AppConfig.load();

        // Set system type attributes for JSP access via ${applicationScope.xxx}
        sce.getServletContext().setAttribute("systemType", AppConfig.getSystemType());
        sce.getServletContext().setAttribute("isBpoSystem", AppConfig.isBpo());
        sce.getServletContext().setAttribute("isPspSystem", AppConfig.isPsp());

        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("ssaPU");
            sce.getServletContext().setAttribute("emf", emf);

            EntityManager em = null;
            try {
                em = emf.createEntityManager();

                // Guard: if metamodel is empty, JPQL may fail — catch and continue
                // SSL_PORT='443' is used as a DB-seeded sentinel — its presence confirms
                // DatabaseInitializer has run and the schema is bootstrapped. The literal
                // value is not used for SSL configuration.
                Constant c = null;
                try {
                    Query q = em.createQuery("SELECT c FROM Constant c WHERE c.name=:name");
                    q.setParameter("name", "SSL_PORT");
                    c = (Constant) q.getSingleResult();
                } catch (Exception ignored) {
                    // JPQL can fail if entities aren't discovered (e.g., exclude-unlisted-classes=true),
                    // or if the table/row doesn't exist yet. We treat this as "not initialized".
                }

                if (c == null || c.getValue() == null || !"443".equals(c.getValue())) {
                    System.out.println("🔁 Skipping global data load – database not initialized");
                } else {
                    AmsDataGlobal global = new AmsDataGlobal();
                    global.initializeGlobalData(em);
                    sce.getServletContext().setAttribute("global", global);

                    // Monthly Billing Launcher background worker executor — runs on the
                    // production PSP, deliberately NOT gated by AppConfig.isMaster().
                    billingExecutor = Executors.newSingleThreadExecutor(r -> {
                        Thread t = new Thread(r, "billing-pipeline-runner");
                        t.setDaemon(true);
                        return t;
                    });
                    sce.getServletContext().setAttribute("billingExecutor", billingExecutor);

                    // Refresh system type attributes — initializeGlobalData may have
                    // cached the authoritative SYSTEM_TYPE from the DB constant
                    sce.getServletContext().setAttribute("systemType", AppConfig.getSystemType());
                    sce.getServletContext().setAttribute("isBpoSystem", AppConfig.isBpo());
                    sce.getServletContext().setAttribute("isPspSystem", AppConfig.isPsp());
                    sce.getServletContext().setAttribute("isMasterSystem", AppConfig.isMaster());
                    System.out.println("✅ Global data loaded (systemType=" + AppConfig.getSystemType()
                            + ", isMaster=" + AppConfig.isMaster() + ")");

                    // Start background health scheduler on master installations
                    if (AppConfig.isMaster()) {
                        healthScheduler = new InstallationHealthScheduler(emf, global);
                        healthScheduler.start();
                    }
                }
            } finally {
                if (em != null && em.isOpen()) {
                    em.close();
                }
            }
        } catch (Throwable t) {
            // Log the error to catalina.base/logs, but DO NOT fail startup on Linux due to a path issue.
            logStartupError(t);

            // If you prefer fail-fast when EMF can’t be created, uncomment the next line:
            // throw new RuntimeException("Startup failed", t);
        }
    }

    private static void logStartupError(Throwable t) {
        try {
            String logDir = AppConfig.get("LOG_PATH",
                    System.getProperty("catalina.base", ".") + "/logs");
            Path logPath = Paths.get(logDir, "emf_error.log");
            Files.createDirectories(logPath.getParent());
            Files.writeString(
                    logPath,
                    "❌ Exception in contextInitialized:\n" + getStackTrace(t) + "\n",
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // Last resort: print to stderr
            t.printStackTrace();
        }
    }

    private static String getStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Stop health scheduler before closing EMF
        if (healthScheduler != null) {
            healthScheduler.stop();
        }

        if (billingExecutor != null) {
            billingExecutor.shutdownNow();
        }

        EntityManagerFactory emf = (EntityManagerFactory) sce.getServletContext().getAttribute("emf");
        if (emf != null && emf.isOpen()) {
            try {
                emf.close();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        session.setAttribute("thisMonth", getThisMonth());
        session.setAttribute("lastMonth", getLastMonth());
        session.setAttribute("twoMonth", getTwoMonths());

        EntityManagerFactory emf = (EntityManagerFactory) session.getServletContext().getAttribute("emf");
        if (emf == null) {
            session.setAttribute("uninitialized", 0);
            return;
        }

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            // SSL_PORT='443' is used as a DB-seeded sentinel — its presence confirms
            // DatabaseInitializer has run and the schema is bootstrapped. The literal
            // value is not used for SSL configuration.
            Constant c = null;
            try {
                Query q = em.createQuery("SELECT c FROM Constant c WHERE c.name=:name");
                q.setParameter("name", "SSL_PORT");
                c = (Constant) q.getSingleResult();
            } catch (Exception ignored) {
                // Same rationale as above — treat as uninitialized
            }
            session.setAttribute("uninitialized",
                    (c == null || c.getValue() == null || !"443".equals(c.getValue())) ? 0 : 1);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    private LocalDate getThisMonthLd() {
        LocalDate now = LocalDate.now();
        return LocalDate.of(now.getYear(), now.getMonthValue(), 1);
    }

    private Date getThisMonth() {
        return Date.valueOf(getThisMonthLd());
    }

    private Date getLastMonth() {
        return Date.valueOf(getThisMonthLd().minusMonths(1L));
    }

    private Date getTwoMonths() {
        return Date.valueOf(getThisMonthLd().minusMonths(2L));
    }

    @Override public void sessionDestroyed(HttpSessionEvent se) {}
    @Override public void attributeAdded(HttpSessionBindingEvent sbe) {}
    @Override public void attributeRemoved(HttpSessionBindingEvent sbe) {}
    @Override public void attributeReplaced(HttpSessionBindingEvent sbe) {}
}

