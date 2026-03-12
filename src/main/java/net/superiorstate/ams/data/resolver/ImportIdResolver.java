package net.superiorstate.ams.data.resolver;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.superiorstate.ams.model.imports.ImportIdMapping;
import net.superiorstate.ams.model.imports.ImportProvider;
import net.superiorstate.ams.model.summit.archive.PlanType;

import java.util.List;

/**
 * Resolves external IDs from import providers to internal AMS primary keys
 * via the import_id_mapping cross-reference table. Provides the indirection
 * layer needed for multi-provider coexistence without PK collisions.
 */
public abstract class ImportIdResolver {

    public static final String PLAN_TYPE = "PLAN_TYPE";
    public static final String EMPLOYER  = "EMPLOYER";
    public static final String EMPLOYEE  = "EMPLOYEE";
    public static final String BENEFIT   = "BENEFIT";

    // ── Core resolution ──────────────────────────────────────────────────

    /**
     * Resolve an external ID to an internal AMS ID via import_id_mapping.
     * Returns null if no mapping exists.
     */
    public static Integer resolveInternalId(EntityManager em, int providerId,
                                             String entityType, String externalId) {
        try {
            ImportIdMapping m = em.createNamedQuery("ImportIdMapping.resolve", ImportIdMapping.class)
                    .setParameter("providerId", providerId)
                    .setParameter("entityType", entityType)
                    .setParameter("externalId", externalId)
                    .getSingleResult();
            return m.getInternalId();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Resolve an external ID to the mapped AMS entity.
     * Returns null if no mapping exists.
     */
    public static <T> T resolveEntity(EntityManager em, Class<T> entityClass,
                                       int providerId, String entityType, String externalId) {
        Integer internalId = resolveInternalId(em, providerId, entityType, externalId);
        if (internalId == null) return null;
        return em.find(entityClass, internalId);
    }

    // ── Recording ────────────────────────────────────────────────────────

    /**
     * Record a new external-to-internal mapping. If a mapping already exists
     * for this (provider, entityType, externalId), it is updated.
     * Caller must manage the transaction.
     */
    public static void recordMapping(EntityManager em, ImportProvider provider,
                                      String entityType, String externalId,
                                      int internalId, boolean isPrimary) {
        ImportIdMapping existing;
        try {
            existing = em.createNamedQuery("ImportIdMapping.resolve", ImportIdMapping.class)
                    .setParameter("providerId", provider.getId())
                    .setParameter("entityType", entityType)
                    .setParameter("externalId", externalId)
                    .getSingleResult();
            existing.setInternalId(internalId);
            existing.setPrimary(isPrimary);
            em.merge(existing);
        } catch (NoResultException e) {
            ImportIdMapping mapping = new ImportIdMapping(provider, entityType, externalId, internalId);
            mapping.setPrimary(isPrimary);
            em.persist(mapping);
        }
    }

    // ── PK allocation ────────────────────────────────────────────────────

    /**
     * Allocate the next available internal PK for a given entity type.
     * Uses MAX(pk) + 1 to avoid collisions with existing data.
     */
    public static int allocateInternalId(EntityManager em, String entityType) {
        String sql = switch (entityType) {
            case EMPLOYER  -> "SELECT COALESCE(MAX(organization_id), 0) + 1 FROM employer";
            case EMPLOYEE  -> "SELECT COALESCE(MAX(employee_id), 0) + 1 FROM employee";
            case PLAN_TYPE -> "SELECT COALESCE(MAX(PlanType_ID), 0) + 1 FROM plantype";
            default -> throw new IllegalArgumentException("Cannot allocate PK for entity: " + entityType);
        };
        Number maxId = (Number) em.createNativeQuery(sql).getSingleResult();
        return maxId.intValue();
    }

    /**
     * Check if a given internal ID already exists in the archive table.
     */
    public static boolean internalIdExists(EntityManager em, String entityType, int candidateId) {
        String sql = switch (entityType) {
            case EMPLOYER  -> "SELECT COUNT(*) FROM employer WHERE organization_id = ?1";
            case EMPLOYEE  -> "SELECT COUNT(*) FROM employee WHERE employee_id = ?1";
            case PLAN_TYPE -> "SELECT COUNT(*) FROM plantype WHERE PlanType_ID = ?1";
            default -> throw new IllegalArgumentException("Unknown entity: " + entityType);
        };
        Number count = (Number) em.createNativeQuery(sql)
                .setParameter(1, candidateId)
                .getSingleResult();
        return count.intValue() > 0;
    }

    // ── Plan type matching cascade ───────────────────────────────────────

    /**
     * Resolve a plan type using the cascading match algorithm:
     * 1) Identity match via import_id_mapping
     * 2) Code match via import_plan_type_mapping
     * 3) Exact name match against existing plan types
     * 4) No match → returns null (caller should create new)
     *
     * Returns the resolved PlanType, or null if no match found.
     * Sets matchInfo[0] to a description of how the match was made.
     */
    public static PlanType resolvePlanType(EntityManager em, ImportProvider provider,
                                            String externalId, String code, String name,
                                            String[] matchInfo) {
        // 1) Identity match via cross-reference
        PlanType pt = resolveEntity(em, PlanType.class, provider.getId(), PLAN_TYPE, externalId);
        if (pt != null) {
            if (matchInfo != null && matchInfo.length > 0) matchInfo[0] = "identity";
            return pt;
        }

        // 2) Code match via import_plan_type_mapping
        if (code != null && !code.isEmpty()) {
            try {
                PlanType mapped = em.createQuery(
                        "SELECT m.targetPlanType FROM ImportPlanTypeMapping m " +
                        "WHERE (m.provider.id = :pid OR m.provider IS NULL) " +
                        "AND LOWER(m.sourcePlanCode) = LOWER(:code) " +
                        "AND m.targetPlanType IS NOT NULL " +
                        "ORDER BY m.provider.id DESC", PlanType.class)  // provider-specific first, then system default
                        .setParameter("pid", provider.getId())
                        .setParameter("code", code)
                        .setMaxResults(1)
                        .getSingleResult();
                if (mapped != null) {
                    if (matchInfo != null && matchInfo.length > 0) matchInfo[0] = "code";
                    return mapped;
                }
            } catch (NoResultException ignored) {}
        }

        // 3) Exact name match
        if (name != null && !name.isEmpty()) {
            try {
                List<PlanType> matches = em.createQuery(
                        "SELECT pt FROM PlanType pt WHERE LOWER(pt.planTypeName) = LOWER(:name)", PlanType.class)
                        .setParameter("name", name)
                        .getResultList();
                if (matches.size() == 1) {
                    if (matchInfo != null && matchInfo.length > 0) matchInfo[0] = "name";
                    return matches.get(0);
                }
                // Multiple matches → ambiguous, do not auto-link
                if (matches.size() > 1) {
                    if (matchInfo != null && matchInfo.length > 0)
                        matchInfo[0] = "ambiguous(" + matches.size() + " matches)";
                    return null;
                }
            } catch (Exception ignored) {}
        }

        // 4) No match
        if (matchInfo != null && matchInfo.length > 0) matchInfo[0] = "none";
        return null;
    }

    // ── Query helpers ────────────────────────────────────────────────────

    /**
     * Find all mappings for a given internal record across all providers.
     */
    public static List<ImportIdMapping> findMappingsByInternalId(EntityManager em,
                                                                  String entityType, int internalId) {
        return em.createNamedQuery("ImportIdMapping.findByInternal", ImportIdMapping.class)
                .setParameter("entityType", entityType)
                .setParameter("internalId", internalId)
                .getResultList();
    }

    /**
     * Find all mappings for a given provider and entity type.
     */
    public static List<ImportIdMapping> findMappingsByProvider(EntityManager em,
                                                                int providerId, String entityType) {
        return em.createNamedQuery("ImportIdMapping.findByProvider", ImportIdMapping.class)
                .setParameter("providerId", providerId)
                .setParameter("entityType", entityType)
                .getResultList();
    }

    /**
     * Set is_primary=false on all existing mappings for a given internal record,
     * then set is_primary=true on the mapping for the specified provider.
     * Used during provider transitions. Caller must manage the transaction.
     */
    public static void transferPrimary(EntityManager em, String entityType,
                                        int internalId, int newPrimaryProviderId) {
        List<ImportIdMapping> mappings = findMappingsByInternalId(em, entityType, internalId);
        for (ImportIdMapping m : mappings) {
            boolean shouldBePrimary = (m.getProvider().getId() == newPrimaryProviderId);
            if (m.isPrimary() != shouldBePrimary) {
                m.setPrimary(shouldBePrimary);
                em.merge(m);
            }
        }
    }

    // ── Integrity checks ──────────────────────────────────────────────────

    /**
     * Find orphaned mappings — mappings whose internal_id does not exist
     * in the corresponding archive table. Returns a list of mapping IDs.
     */
    public static List<ImportIdMapping> findOrphanedMappings(EntityManager em, String entityType) {
        String existsCheck = switch (entityType) {
            case EMPLOYER  -> "SELECT 1 FROM employer WHERE organization_id = ?1";
            case EMPLOYEE  -> "SELECT 1 FROM employee WHERE employee_id = ?1";
            case PLAN_TYPE -> "SELECT 1 FROM plantype WHERE PlanType_ID = ?1";
            case BENEFIT   -> "SELECT 1 FROM benefit WHERE benefit_id = ?1";
            default -> throw new IllegalArgumentException("Unknown entity: " + entityType);
        };

        List<ImportIdMapping> allMappings = em.createQuery(
                "SELECT m FROM ImportIdMapping m WHERE m.entityType = :et", ImportIdMapping.class)
                .setParameter("et", entityType)
                .getResultList();

        List<ImportIdMapping> orphans = new java.util.ArrayList<>();
        for (ImportIdMapping m : allMappings) {
            List<?> exists = em.createNativeQuery(existsCheck)
                    .setParameter(1, m.getInternalId())
                    .getResultList();
            if (exists.isEmpty()) orphans.add(m);
        }
        return orphans;
    }

    /**
     * Find duplicate mappings — cases where multiple mappings from the same provider
     * point to the same internal record for a given entity type.
     * Returns groups of mappings keyed by internal_id.
     */
    public static java.util.Map<Integer, List<ImportIdMapping>> findDuplicateMappings(
            EntityManager em, int providerId, String entityType) {
        List<ImportIdMapping> mappings = findMappingsByProvider(em, providerId, entityType);

        java.util.Map<Integer, List<ImportIdMapping>> byInternal = new java.util.LinkedHashMap<>();
        for (ImportIdMapping m : mappings) {
            byInternal.computeIfAbsent(m.getInternalId(), k -> new java.util.ArrayList<>()).add(m);
        }

        // Keep only groups with more than one mapping
        byInternal.entrySet().removeIf(e -> e.getValue().size() <= 1);
        return byInternal;
    }

    /**
     * Count mappings grouped by entity type for a given provider.
     * Useful for import run summary reporting.
     */
    public static java.util.Map<String, Long> countMappingsByEntityType(EntityManager em, int providerId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                "SELECT m.entityType, COUNT(m) FROM ImportIdMapping m " +
                "WHERE m.provider.id = :pid GROUP BY m.entityType")
                .setParameter("pid", providerId)
                .getResultList();

        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Object[] row : rows) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }
}
