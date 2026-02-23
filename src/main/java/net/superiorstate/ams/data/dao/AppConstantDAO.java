package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import net.superiorstate.ams.AppConfig;
import net.superiorstate.ams.model.Constant;

import java.sql.Date;

/**
 * Utility class for accessing Constant values from the database.
 */
public abstract class AppConstantDAO {

    /**
     * Retrieves a Constant entity by name.
     *
     * @param em   EntityManager to use
     * @param name name of the constant
     * @return Constant or null if not found
     */
    public static Constant getConstant(EntityManager em, String name) {
        try {
            Query q = em.createQuery("SELECT c FROM Constant c WHERE c.name = :name");
            q.setParameter("name", name);
            return (Constant) q.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves the value of a Constant by name.
     */
    public static String getConstantValue(EntityManager em, String name) {
        Constant c = getConstant(em, name);
        return c != null ? c.getValue() : null;
    }

    /**
     * Retrieves the note of a Constant by name.
     */
    public static String getConstantNote(EntityManager em, String name) {
        Constant c = getConstant(em, name);
        return c != null ? c.getNote() : null;
    }

    /**
     * Gets SAVE_PATH from ssa.properties (infrastructure config).
     */
    public static String getSavePath() {
        return AppConfig.get("SAVE_PATH", "/var/lib/tomcat10/data/");
    }

    /**
     * Gets the value of the "WEB_PATH" constant.
     */
    public static String getWebPath(EntityManager em) {
        return getConstantValue(em, "WEB_PATH");
    }

    /**
     * Gets the value of the "FALSE_CLOSE" constant as a Date.
     */
    public static Date getFalseCloseDate(EntityManager em) {
        String dateString = getConstantValue(em, "FALSE_CLOSE");
        return dateString != null ? Date.valueOf(dateString) : null;
    }
}