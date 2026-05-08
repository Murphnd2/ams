package net.superiorstate.ams.data.dao;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.model.dto.inventory.BenefitDTO;
import net.superiorstate.ams.model.dto.inventory.EmployerInventoryDTO;
import net.superiorstate.ams.model.summit.archive.Benefit;
import net.superiorstate.ams.model.summit.archive.Employer;
import net.superiorstate.ams.model.summit.imports.HsaEr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only DAO for the employer service-scope inventory endpoint.
 *
 * <p>All methods are static; the class is abstract (not instantiable) following
 * the convention of TicketKnowledgeDAO and ChatbotSkillDAO.
 *
 * <p>Three query budget for {@link #getAllInScope}:
 * <ol>
 *   <li>In-scope employers, ordered by name</li>
 *   <li>All active benefits with FETCH JOINs on PlanType and BillingGroup</li>
 *   <li>All HsaEr rows</li>
 * </ol>
 * Grouping and DTO assembly happen in Java after the three queries return.
 */
public abstract class EmployerInventoryDAO {

    private static final Logger log = LogManager.getLogger(EmployerInventoryDAO.class);

    // ── JPQL strings ─────────────────────────────────────────────────────────

    /**
     * All employers that are "in scope" for service-inventory purposes:
     * any of the CDH / PB / POP flags set, OR at least one HsaEr row exists.
     */
    private static final String JPQL_IN_SCOPE_EMPLOYERS =
            "SELECT e FROM Employer e " +
            "WHERE e.hasCdh = true OR e.hasPb = true OR e.hasPop = true " +
            "OR EXISTS (SELECT h FROM HsaEr h WHERE h.employer.id = e.id) " +
            "ORDER BY e.employerName";

    /**
     * All active benefits across every employer, with PlanType and (optionally)
     * BillingGroup eager-loaded to avoid N+1.  Results are filtered to the
     * desired employers in Java after the query returns.
     *
     * LEFT JOIN FETCH on billingGroup: some PlanTypes have no billing group;
     * those benefits are kept, billingGroup fields become null in the DTO.
     */
    private static final String JPQL_ALL_ACTIVE_BENEFITS =
            "SELECT b FROM Benefit b " +
            "JOIN FETCH b.planType pt " +
            "LEFT JOIN FETCH pt.billingGroup " +
            "WHERE b.isActive = true";

    /** Benefits for a single employer. */
    private static final String JPQL_BENEFITS_BY_EMPLOYER =
            "SELECT b FROM Benefit b " +
            "JOIN FETCH b.planType pt " +
            "LEFT JOIN FETCH pt.billingGroup " +
            "WHERE b.employer.id = :orgId AND b.isActive = true";

    /** HsaEr rows for a single employer. */
    private static final String JPQL_HSA_BY_EMPLOYER =
            "SELECT h FROM HsaEr h WHERE h.employer.id = :orgId";

    /** Employers matching a case-insensitive name fragment. */
    private static final String JPQL_EMPLOYERS_BY_NAME =
            "SELECT e FROM Employer e " +
            "WHERE LOWER(e.employerName) LIKE :fragment " +
            "ORDER BY e.employerName";

    /** Benefits for a set of employer IDs (used by searchByName). */
    private static final String JPQL_BENEFITS_BY_EMPLOYER_IDS =
            "SELECT b FROM Benefit b " +
            "JOIN FETCH b.planType pt " +
            "LEFT JOIN FETCH pt.billingGroup " +
            "WHERE b.isActive = true AND b.employer.id IN :ids";

    /** HsaEr rows for a set of employer IDs (used by searchByName). */
    private static final String JPQL_HSA_BY_EMPLOYER_IDS =
            "SELECT h FROM HsaEr h WHERE h.employer.id IN :ids";

    // ── public API ───────────────────────────────────────────────────────────

    /**
     * Returns all in-scope employers with full service-scope detail.
     *
     * <p>"In scope" = has_cdh OR has_pb OR has_pop OR any HsaEr row exists.
     * Results ordered alphabetically by employer name.
     *
     * <p>Executes exactly three queries; all grouping is done in Java.
     *
     * @param em open EntityManager (caller is responsible for lifecycle)
     * @return list of DTOs, possibly empty; never null
     */
    public static List<EmployerInventoryDTO> getAllInScope(EntityManager em) {
        try {
            // Q1 — in-scope employers
            List<Employer> employers = em.createQuery(JPQL_IN_SCOPE_EMPLOYERS, Employer.class)
                    .getResultList();

            if (employers.isEmpty()) return Collections.emptyList();

            Set<Integer> scopeIds = employers.stream()
                    .map(Employer::getId)
                    .collect(Collectors.toSet());

            // Q2 — all active benefits (filter to scope in Java)
            List<Benefit> allBenefits = em.createQuery(JPQL_ALL_ACTIVE_BENEFITS, Benefit.class)
                    .getResultList();

            // Q3 — all HsaEr rows (filter to scope in Java)
            List<HsaEr> allHsaErs = em.createQuery("SELECT h FROM HsaEr h", HsaEr.class)
                    .getResultList();

            // group by employer ID
            Map<Integer, List<Benefit>> benefitsByEr = new HashMap<>();
            for (Benefit b : allBenefits) {
                if (b.getEmployer() == null) continue;
                int eid = b.getEmployer().getId();
                if (scopeIds.contains(eid)) {
                    benefitsByEr.computeIfAbsent(eid, k -> new ArrayList<>()).add(b);
                }
            }

            Map<Integer, List<HsaEr>> hsaByEr = new HashMap<>();
            for (HsaEr h : allHsaErs) {
                if (h.getEmployer() == null) continue;
                int eid = h.getEmployer().getId();
                if (scopeIds.contains(eid)) {
                    hsaByEr.computeIfAbsent(eid, k -> new ArrayList<>()).add(h);
                }
            }

            // assemble
            List<EmployerInventoryDTO> result = new ArrayList<>(employers.size());
            for (Employer e : employers) {
                List<Benefit> eb = benefitsByEr.getOrDefault(e.getId(), Collections.emptyList());
                List<HsaEr>  eh = hsaByEr.getOrDefault(e.getId(), Collections.emptyList());
                result.add(buildDTO(e, eb, eh));
            }
            return result;

        } catch (Exception ex) {
            log.error("getAllInScope failed", ex);
            return Collections.emptyList();
        }
    }

    /**
     * Returns the service-scope DTO for a single employer, or {@code null} if
     * no employer exists with the given organization ID.
     *
     * @param em    open EntityManager
     * @param orgId value of Employer.id (organization_id column)
     * @return populated DTO, or null if not found
     */
    public static EmployerInventoryDTO getByOrganizationId(EntityManager em, long orgId) {
        try {
            // Q1 — employer record
            Employer employer = em.find(Employer.class, (int) orgId);
            if (employer == null) return null;

            // Q2 — active benefits for this employer
            List<Benefit> benefits = em.createQuery(JPQL_BENEFITS_BY_EMPLOYER, Benefit.class)
                    .setParameter("orgId", (int) orgId)
                    .getResultList();

            // Q3 — HsaEr rows for this employer
            List<HsaEr> hsaErs = em.createQuery(JPQL_HSA_BY_EMPLOYER, HsaEr.class)
                    .setParameter("orgId", (int) orgId)
                    .getResultList();

            return buildDTO(employer, benefits, hsaErs);

        } catch (Exception ex) {
            log.error("getByOrganizationId failed for orgId={}", orgId, ex);
            return null;
        }
    }

    /**
     * Case-insensitive partial-name search across all employers.
     *
     * <p>Does NOT restrict to in-scope employers — returns any employer whose
     * name contains the fragment, so callers can discover employers by name
     * regardless of whether they currently carry CDH/POP/HSA products.
     *
     * @param em           open EntityManager
     * @param nameFragment substring to match (case-insensitive); must be non-blank
     * @return list of matching employer DTOs, ordered by employer name; never null
     */
    public static List<EmployerInventoryDTO> searchByName(EntityManager em, String nameFragment) {
        try {
            String pattern = "%" + nameFragment.toLowerCase(Locale.ROOT) + "%";

            // Q1 — employers matching the name fragment
            List<Employer> employers = em.createQuery(JPQL_EMPLOYERS_BY_NAME, Employer.class)
                    .setParameter("fragment", pattern)
                    .getResultList();

            if (employers.isEmpty()) return Collections.emptyList();

            List<Integer> ids = employers.stream()
                    .map(Employer::getId)
                    .collect(Collectors.toList());

            // Q2 — active benefits for the matched employers
            List<Benefit> benefits = em.createQuery(JPQL_BENEFITS_BY_EMPLOYER_IDS, Benefit.class)
                    .setParameter("ids", ids)
                    .getResultList();

            // Q3 — HsaEr rows for the matched employers
            List<HsaEr> hsaErs = em.createQuery(JPQL_HSA_BY_EMPLOYER_IDS, HsaEr.class)
                    .setParameter("ids", ids)
                    .getResultList();

            // group
            Map<Integer, List<Benefit>> benefitsByEr = new HashMap<>();
            for (Benefit b : benefits) {
                if (b.getEmployer() != null) {
                    benefitsByEr.computeIfAbsent(b.getEmployer().getId(), k -> new ArrayList<>()).add(b);
                }
            }
            Map<Integer, List<HsaEr>> hsaByEr = new HashMap<>();
            for (HsaEr h : hsaErs) {
                if (h.getEmployer() != null) {
                    hsaByEr.computeIfAbsent(h.getEmployer().getId(), k -> new ArrayList<>()).add(h);
                }
            }

            List<EmployerInventoryDTO> result = new ArrayList<>(employers.size());
            for (Employer e : employers) {
                List<Benefit> eb = benefitsByEr.getOrDefault(e.getId(), Collections.emptyList());
                List<HsaEr>  eh = hsaByEr.getOrDefault(e.getId(), Collections.emptyList());
                result.add(buildDTO(e, eb, eh));
            }
            return result;

        } catch (Exception ex) {
            log.error("searchByName failed for fragment='{}'", nameFragment, ex);
            return Collections.emptyList();
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    /**
     * Assembles an {@link EmployerInventoryDTO} from pre-loaded JPA entities.
     * No additional database access.
     */
    private static EmployerInventoryDTO buildDTO(Employer e,
                                                  List<Benefit> benefits,
                                                  List<HsaEr> hsaErs) {
        EmployerInventoryDTO dto = new EmployerInventoryDTO();

        // identity
        dto.setOrganizationId((long) e.getId());
        dto.setEmployerName(e.getEmployerName());
        dto.setActive(e.isActive());
        dto.setBillable(e.isBillable());

        // ── scope flags ────────────────────────────────────────────────────
        // Employer getters: isPop(), isCdh(), isPb() (non-standard naming confirmed in Phase 1)
        List<String> scopeFlags = new ArrayList<>();
        if (e.isCdh())         scopeFlags.add("CDH");
        if (e.isPb())          scopeFlags.add("PB");
        if (e.isPop())         scopeFlags.add("POP");
        if (!hsaErs.isEmpty()) scopeFlags.add("HSA");
        dto.setScopeFlags(scopeFlags);

        // ── service lines ──────────────────────────────────────────────────
        // Insertion-ordered set to deduplicate while preserving first-seen order.
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        for (Benefit b : benefits) {
            if (b.getPlanType() == null || b.getPlanType().getCode() == null) continue;
            String code = b.getPlanType().getCode();
            if ("COBRA".equals(b.getSourceType())) {
                lines.add("COBRA-" + code);
            } else {
                lines.add(code);
            }
        }
        if (e.isPop()) lines.add("POP");

        boolean hasInvoicedHsa   = hsaErs.stream().anyMatch(h -> !h.isBilledDirect());
        boolean hasBilledDirectHsa = hsaErs.stream().anyMatch(HsaEr::isBilledDirect);
        if (hasInvoicedHsa)    lines.add("HSA");
        if (hasBilledDirectHsa) lines.add("HSA-Direct");

        dto.setServiceLines(new ArrayList<>(lines));

        // ── plan codes (distinct) ──────────────────────────────────────────
        List<String> planCodes = benefits.stream()
                .filter(b -> b.getPlanType() != null && b.getPlanType().getCode() != null)
                .map(b -> b.getPlanType().getCode())
                .distinct()
                .collect(Collectors.toList());
        dto.setPlanCodes(planCodes);

        // ── billing groups (distinct, nulls excluded) ──────────────────────
        List<String> billingGroups = benefits.stream()
                .filter(b -> b.getPlanType() != null
                        && b.getPlanType().getBillingGroup() != null
                        && b.getPlanType().getBillingGroup().getDescription() != null)
                .map(b -> b.getPlanType().getBillingGroup().getDescription())
                .distinct()
                .collect(Collectors.toList());
        dto.setBillingGroups(billingGroups);

        // ── counts ─────────────────────────────────────────────────────────
        dto.setActiveBenefitCount(benefits.size());
        dto.setCdhCount((int) benefits.stream()
                .filter(b -> "CDH".equals(b.getSourceType())).count());
        dto.setCobraCount((int) benefits.stream()
                .filter(b -> "COBRA".equals(b.getSourceType())).count());

        // ── debit cards ────────────────────────────────────────────────────
        // Benefit.isHasCards() — column "hasCards" (camelCase, confirmed Phase 1)
        dto.setHasDebitCards(benefits.stream().anyMatch(Benefit::isHasCards));

        // ── date aggregates ────────────────────────────────────────────────
        dto.setEarliestBenefitEffective(benefits.stream()
                .map(Benefit::getEffectiveDate)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .map(EmployerInventoryDAO::toIsoDate)
                .orElse(null));

        dto.setNextRenewalMin(benefits.stream()
                .map(Benefit::getNextRenewalDue)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .map(EmployerInventoryDAO::toIsoDate)
                .orElse(null));

        dto.setNextRenewalMax(benefits.stream()
                .map(Benefit::getNextRenewalDue)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(EmployerInventoryDAO::toIsoDate)
                .orElse(null));

        // ── HsaInfo ────────────────────────────────────────────────────────
        EmployerInventoryDTO.HsaInfo hsaInfo = new EmployerInventoryDTO.HsaInfo();
        hsaInfo.setActive(!hsaErs.isEmpty());
        hsaInfo.setInvoiced(hasInvoicedHsa);
        hsaInfo.setBilledDirect(hasBilledDirectHsa);
        hsaInfo.setHsaerNames(hsaErs.stream()
                .map(HsaEr::getName)
                .collect(Collectors.toList()));
        dto.setHsa(hsaInfo);

        // ── benefit detail list ────────────────────────────────────────────
        dto.setBenefits(benefits.stream()
                .map(EmployerInventoryDAO::toBenefitDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    /** Maps a JPA {@link Benefit} entity to a {@link BenefitDTO}. */
    private static BenefitDTO toBenefitDTO(Benefit b) {
        BenefitDTO dto = new BenefitDTO();
        dto.setBenefitId(b.getId());
        dto.setSourceType(b.getSourceType());
        dto.setActive(b.isActive());
        dto.setHasCards(b.isHasCards());
        dto.setPlanName(b.getPlanName());
        dto.setPlanDescription(b.getPlanDescription());
        dto.setSummitId(b.getSummitId());
        dto.setBenIdPb(b.getPbBenId());

        dto.setEffectiveDate(toIsoDate(b.getEffectiveDate()));
        dto.setTerminationDate(toIsoDate(b.getTerminationDate()));
        dto.setNextRenewalDue(toIsoDate(b.getNextRenewalDue()));
        dto.setLastRenewed(toIsoDate(b.getLastRenewed()));
        dto.setPlanYearStart(toIsoDate(b.getPlanYearStart()));
        dto.setPlanYearEnd(toIsoDate(b.getPlanYearEnd()));

        if (b.getPlanType() != null) {
            dto.setCode(b.getPlanType().getCode());
            dto.setPlanTypeName(b.getPlanType().getPlanTypeName());
            if (b.getPlanType().getBillingGroup() != null) {
                dto.setBillingGroupId(b.getPlanType().getBillingGroup().getId());
                dto.setBillingGroup(b.getPlanType().getBillingGroup().getDescription());
            }
        }

        return dto;
    }

    /**
     * Converts a {@code java.sql.Date} to an ISO-8601 string (yyyy-MM-dd).
     * Returns {@code null} if the input is null.
     */
    private static String toIsoDate(Date d) {
        return d == null ? null : d.toLocalDate().toString();
    }
}
