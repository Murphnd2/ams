package net.superiorstate.ams.data.service;

import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.metamodel.EntityType;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.Constant;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.User;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.agency.Agency;
import org.eclipse.persistence.sessions.server.ServerSession;

import java.io.PrintWriter;
import java.util.List;

/**
 * Utility for database reset operations. Captures initialization state,
 * truncates all tables, and re-initializes from the saved state.
 *
 * Used by ReSeedDb and ReSeedDemoData servlets.
 */
public abstract class DatabaseResetUtil {

    // ═══════════════════════════════════════════════════════════════
    //  SAVED STATE — holds values from the initialization form
    // ═══════════════════════════════════════════════════════════════

    public static class SavedState {
        public String pspName;
        public String firstName;
        public String lastName;
        public String email;
        public String passwordHash;
        public String salt;
        public String address;
        public String city;
        public String state;
        public String zip;
        public String phone;
        public String taxId;
        public String domain;
        public String summitPath;
        public String smtpServer;
        public String smtpPort;
        public String smtpUser;
        public String smtpPassword;
        /** Captured rows from schema_version: [version, description, script_name] */
        public List<Object[]> schemaVersionRows;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CAPTURE — read current init values from database
    // ═══════════════════════════════════════════════════════════════

    public static SavedState captureInitState(EntityManager em, PrintWriter out) {
        SavedState s = new SavedState();

        // From PSP (ID 4)
        PSP psp = EntityLookup.getPspById(em, 4L);
        if (psp != null) {
            s.pspName = psp.getFullName();
        }

        // From Person (ID 104) — primary admin
        Person person = EntityLookup.getPersonById(em, 104L);
        if (person != null) {
            s.firstName = person.getFirstName();
            s.lastName = person.getLastName();
            s.email = person.getEmail();
        }

        // From User — password hash + salt (can't recover plaintext)
        if (person != null) {
            User user = em.find(User.class, person.getId());
            if (user != null) {
                s.passwordHash = user.getPasswordHash();
                s.salt = user.getSalt();
            }
        }

        // From Address (ID 3)
        Address addr = EntityLookup.getAddressById(em, 3L);
        if (addr != null) {
            s.address = addr.getAddress1();
            s.city = addr.getCity();
            s.state = addr.getState();
            s.zip = addr.getZipCode();
        }

        // From Agency (ID 14)
        Agency agency = EntityLookup.getAgencyById(em, 14L);
        if (agency != null) {
            s.phone = agency.getPhone();
            s.taxId = agency.getTaxId();
        }

        // From Constants table
        s.smtpServer = getConstant(em, "SMTP_SERVER");
        s.smtpPort = getConstant(em, "SMTP_PORT");
        s.smtpUser = getConstant(em, "SMTP_USER");
        s.smtpPassword = getConstant(em, "SMTP_PASSWORD");
        s.domain = getConstant(em, "WEB_PATH");
        s.summitPath = getConstant(em, "SUMMIT_PATH");

        // Capture schema_version rows (if the table exists)
        try {
            s.schemaVersionRows = em.createNativeQuery(
                    "SELECT version, description, script_name FROM schema_version ORDER BY version"
            ).getResultList();
        } catch (Exception e) {
            s.schemaVersionRows = List.of();  // table may not exist yet
        }

        log(out, "Captured initialization state for: <strong>" + s.pspName
                + "</strong> (" + s.firstName + " " + s.lastName + " / " + s.email + ")");
        if (s.schemaVersionRows != null && !s.schemaVersionRows.isEmpty()) {
            log(out, "Captured <strong>" + s.schemaVersionRows.size() + "</strong> schema_version rows.");
        }
        return s;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CLEAR — truncate all tables
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public static void clearAllTables(EntityManager em, PrintWriter out) {
        log(out, "Clearing all tables...");

        // Must use a fresh connection — close any managed EntityManager state
        em.clear();

        em.getTransaction().begin();
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        em.getTransaction().commit();

        // Get all table names in the current schema
        List<String> tables = em.createNativeQuery(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'"
        ).getResultList();

        int count = 0;
        for (String table : tables) {
            try {
                em.getTransaction().begin();
                em.createNativeQuery("TRUNCATE TABLE `" + table + "`").executeUpdate();
                em.getTransaction().commit();
                count++;
            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                log(out, "&nbsp;&nbsp;Warning: could not truncate <code>" + table + "</code>: " + e.getMessage());
            }
        }

        em.getTransaction().begin();
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        em.getTransaction().commit();

        log(out, "Truncated <strong>" + count + "</strong> of " + tables.size() + " tables.");
    }

    // ═══════════════════════════════════════════════════════════════
    //  EVICT L2 CACHE — per-class eviction (safe for EclipseLink)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Evicts all entity classes from the L2 shared cache one at a time.
     *
     * IMPORTANT: Do NOT use {@code emf.getCache().evictAll()} — in EclipseLink 3.0.2
     * it corrupts internal descriptor metadata, causing entity-to-table mapping errors
     * (e.g., Person mapped to ASSIGNEE table instead of person table).
     *
     * Per-class eviction via the JPA Metamodel avoids this bug.
     */
    public static void evictEntityCaches(EntityManagerFactory emf) {
        Cache cache = emf.getCache();
        for (EntityType<?> entityType : emf.getMetamodel().getEntities()) {
            Class<?> javaType = entityType.getJavaType();
            if (javaType != null) {
                cache.evict(javaType);
            }
        }

        // Reset EclipseLink's in-memory sequence cache so auto-generated IDs
        // re-read from the (now-truncated) SEQUENCE table instead of using
        // stale pre-allocated values that collide with explicit IDs.
        try {
            ServerSession session = emf.unwrap(ServerSession.class);
            session.getSequencingControl().resetSequencing();
        } catch (Exception e) {
            System.err.println("⚠ Could not reset sequence cache: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  REINITIALIZE — set fields and call DatabaseInitializer
    // ═══════════════════════════════════════════════════════════════

    public static void reinitialize(EntityManager em, SavedState s, PrintWriter out) {
        log(out, "Re-initializing database...");

        // Set DatabaseInitializer static fields from saved state
        DatabaseInitializer.setPspName(s.pspName);
        DatabaseInitializer.setFirstName(s.firstName);
        DatabaseInitializer.setLastName(s.lastName);
        DatabaseInitializer.setEmail(s.email);
        DatabaseInitializer.setPassword("PLACEHOLDER");  // Will be overwritten with saved hash
        DatabaseInitializer.setAddress(s.address);
        DatabaseInitializer.setCity(s.city);
        DatabaseInitializer.setState(s.state);
        DatabaseInitializer.setZip(s.zip);
        DatabaseInitializer.setPhone(s.phone);
        DatabaseInitializer.setTaxId(s.taxId);
        DatabaseInitializer.setDomain(s.domain);
        DatabaseInitializer.setSummitPath(s.summitPath);
        DatabaseInitializer.setSmtpServer(s.smtpServer);
        DatabaseInitializer.setSmtpPort(s.smtpPort);
        DatabaseInitializer.setSmtpUsername(s.smtpUser);
        DatabaseInitializer.setSmtpPassword(s.smtpPassword);

        // Run the core initialization (caller should provide a fresh EM after truncation)
        DatabaseInitializer.performInitialization(em);

        log(out, "Database re-initialized.");

        // Restore schema_version rows
        if (s.schemaVersionRows != null && !s.schemaVersionRows.isEmpty()) {
            try {
                em.getTransaction().begin();
                for (Object[] row : s.schemaVersionRows) {
                    em.createNativeQuery(
                            "INSERT IGNORE INTO schema_version (version, description, script_name) VALUES (?1, ?2, ?3)")
                            .setParameter(1, row[0])
                            .setParameter(2, row[1])
                            .setParameter(3, row[2])
                            .executeUpdate();
                }
                em.getTransaction().commit();
                log(out, "Restored <strong>" + s.schemaVersionRows.size() + "</strong> schema_version rows.");
            } catch (Exception e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                log(out, "Warning: could not restore schema_version: " + e.getMessage());
            }
        }

        // Restore the admin user's password hash+salt (since we can't recover plaintext)
        if (s.passwordHash != null && s.salt != null) {
            Person person = EntityLookup.getPersonById(em, 104L);
            if (person != null) {
                User user = em.find(User.class, person.getId());
                if (user != null) {
                    em.getTransaction().begin();
                    user.setPasswordHash(s.passwordHash);
                    user.setSalt(s.salt);
                    em.merge(user);
                    em.getTransaction().commit();
                    log(out, "Restored admin password credentials.");
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  RELOAD GLOBALS
    // ═══════════════════════════════════════════════════════════════

    public static void reloadGlobals(EntityManager em, AmsDataGlobal global, PrintWriter out) {
        if (global != null) {
            global.initializeGlobalData(em);
            log(out, "Reloaded AmsDataGlobal.");
        } else {
            log(out, "Warning: AmsDataGlobal not available — skip reload.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private static String getConstant(EntityManager em, String name) {
        try {
            Query q = em.createQuery("SELECT c FROM Constant c WHERE c.name = :name");
            q.setParameter("name", name);
            List<Constant> results = q.getResultList();
            return results.isEmpty() ? null : results.get(0).getValue();
        } catch (Exception e) {
            return null;
        }
    }

    private static void log(PrintWriter out, String msg) {
        if (out != null) {
            out.println("<p>" + msg + "</p>");
            out.flush();
        }
    }
}
