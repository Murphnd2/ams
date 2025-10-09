package net.superiorstate.ams.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

import java.sql.Date;
import java.util.List;

public abstract class Cleaner {

    protected final EntityManager em;

    public Cleaner(EntityManager em) {
        this.em = em;
    }

    public void wipeTables(List<String> tables) {
        try {
            em.getTransaction().begin();
            disableForeignKeyChecks();

            for (String table : tables) {
                truncateTable(table);
            }

            enableForeignKeyChecks();
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    protected void truncateTable(String tableName) {
        Query q = em.createNativeQuery("TRUNCATE TABLE " + tableName);
        q.executeUpdate();
    }

    protected void disableForeignKeyChecks() {
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
    }

    protected void enableForeignKeyChecks() {
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
    }

    /**
     * Deletes from an entity where a specified attribute equals the given monthId.
     *
     * @param entityName       the JPA entity name (e.g., "BillingGrid")
     * @param monthIdAttribute the full attribute name (e.g., "billingMonth.monthId")
     * @param monthIdValue     the integer value to match against
     */
    public void deleteByMonthId(String entityName, String monthIdAttribute, int monthIdValue) {
        String jpql = "DELETE FROM " + entityName + " e WHERE e." + monthIdAttribute + " = :monthId";
        Query q = em.createQuery(jpql);
        q.setParameter("monthId", monthIdValue);
        q.executeUpdate();
    }
    public void deleteByMonthDate(String entityName, String dateAttribute, Date monthDate) {
        String jpql = "DELETE FROM " + entityName + " e WHERE e." + dateAttribute + " = :monthDate";
        Query q = em.createQuery(jpql);
        q.setParameter("monthDate", monthDate);
        q.executeUpdate();
    }

}
