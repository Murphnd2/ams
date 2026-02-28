package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import net.superiorstate.ams.data.AmsDataGlobal;
import net.superiorstate.ams.data.resolver.EntityLookup;
import net.superiorstate.ams.model.Constant;
import net.superiorstate.ams.model.general.Address;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.User;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.agency.Agency;

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
            User user = em.find(User.class, person);
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

        log(out, "Captured initialization state for: <strong>" + s.pspName
                + "</strong> (" + s.firstName + " " + s.lastName + " / " + s.email + ")");
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

        // Run the core initialization
        DatabaseInitializer.performInitialization(em);

        log(out, "Database re-initialized.");

        // Restore the admin user's password hash+salt (since we can't recover plaintext)
        if (s.passwordHash != null && s.salt != null) {
            Person person = EntityLookup.getPersonById(em, 104L);
            if (person != null) {
                User user = em.find(User.class, person);
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
