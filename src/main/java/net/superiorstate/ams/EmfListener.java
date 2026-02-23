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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;

@WebListener
public class EmfListener implements ServletContextListener, HttpSessionListener, HttpSessionAttributeListener {

    public EmfListener() {}

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // D-01: Load infrastructure config before anything else
        AppConfig.load();

        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("ssaPU");
            sce.getServletContext().setAttribute("emf", emf);

            EntityManager em = null;
            try {
                em = emf.createEntityManager();

                // Guard: if metamodel is empty, JPQL may fail — catch and continue
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
                    System.out.println("✅ Global data loaded");
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

