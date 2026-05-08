package net.superiorstate.ams.data.service;

import jakarta.persistence.EntityManager;
import net.superiorstate.ams.data.dao.EmployerInventoryDAO;
import net.superiorstate.ams.model.dto.inventory.BenefitDTO;
import net.superiorstate.ams.model.dto.inventory.EmployerInventoryDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Detects in-scope employer names mentioned in free text and formats their
 * service-scope inventory as a plain-text prompt context block.
 *
 * Name index is lazy-loaded from EmployerInventoryDAO.getAllInScope() on first
 * call and cached for the JVM lifetime (Tomcat restart re-loads).
 *
 * All public methods are best-effort — callers must wrap in try/catch and
 * degrade gracefully when this resolver fails.
 */
public class EmployerContextResolver {

    private static final Logger log = LogManager.getLogger(EmployerContextResolver.class);

    private static final int MAX_MATCHES           = 5;
    private static final int MAX_BENEFITS_PER_ER   = 8;

    /** Lazy cache: lowercase employer name → organizationId. Immutable once set. */
    private static volatile Map<String, Long> nameIndex = null;
    private static final Object CACHE_LOCK = new Object();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Detects in-scope employers mentioned in {@code text} and returns a
     * formatted {@code == EMPLOYER SERVICE SCOPE ==} block, or {@code null}
     * if no employers are detected.
     */
    public static String resolveContext(EntityManager em, String text) {
        List<EmployerInventoryDTO> matches = detectEmployers(em, text);
        if (matches.isEmpty()) return null;
        return formatContextBlock(matches);
    }

    /**
     * Detects in-scope employers mentioned in {@code text}.
     * Uses whole-word boundary matching for short or single-word names to
     * reduce false positives. Caps results at {@value MAX_MATCHES}.
     */
    public static List<EmployerInventoryDTO> detectEmployers(EntityManager em, String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        Map<String, Long> index = getOrLoadNameIndex(em);
        if (index.isEmpty()) return Collections.emptyList();

        String lowerText = text.toLowerCase(Locale.ROOT);
        // LinkedHashSet preserves match order and deduplicates by orgId
        Set<Long> matchedIds = new LinkedHashSet<>();

        for (Map.Entry<String, Long> entry : index.entrySet()) {
            if (matchedIds.size() >= MAX_MATCHES) break;
            String lcName = entry.getKey();
            Long orgId    = entry.getValue();
            if (matchedIds.contains(orgId)) continue;

            boolean hit = needsWordBoundary(lcName)
                    ? Pattern.compile("\\b" + Pattern.quote(lcName) + "\\b", Pattern.CASE_INSENSITIVE)
                              .matcher(text).find()
                    : lowerText.contains(lcName);

            if (hit) matchedIds.add(orgId);
        }

        if (matchedIds.isEmpty()) return Collections.emptyList();

        List<EmployerInventoryDTO> results = new ArrayList<>();
        for (Long orgId : matchedIds) {
            try {
                EmployerInventoryDTO dto = EmployerInventoryDAO.getByOrganizationId(em, orgId);
                if (dto != null) results.add(dto);
            } catch (Exception e) {
                log.warn("EmployerContextResolver: failed to fetch inventory for orgId={}", orgId, e);
            }
        }

        if (!results.isEmpty()) {
            log.info("EmployerContextResolver: detected {} employer(s) via name-matching in text", results.size());
        }
        return results;
    }

    /**
     * Formats a list of employer DTOs into a plain-text, indented context block
     * suitable for direct insertion into a Claude prompt.
     */
    public static String formatContextBlock(List<EmployerInventoryDTO> matches) {
        StringBuilder sb = new StringBuilder();
        sb.append("== EMPLOYER SERVICE SCOPE ==\n\n");
        sb.append("The following employer(s) were identified as relevant. ")
          .append("Use this data when drafting:\n\n");

        int num = 1;
        for (EmployerInventoryDTO dto : matches) {
            sb.append("Employer ").append(num++).append(": ").append(dto.getEmployerName()).append("\n");

            if (dto.getServiceLines() != null && !dto.getServiceLines().isEmpty()) {
                sb.append("  Active services: ")
                  .append(String.join(", ", dto.getServiceLines())).append("\n");
            }
            if (dto.getPlanCodes() != null && !dto.getPlanCodes().isEmpty()) {
                sb.append("  Plan codes: ")
                  .append(String.join(", ", dto.getPlanCodes())).append("\n");
            }

            EmployerInventoryDTO.HsaInfo hsa = dto.getHsa();
            if (hsa != null && hsa.isActive()) {
                String desc = hsa.isBilledDirect() ? "billed direct from HSA" : "invoiced";
                sb.append("  HSA: ").append(desc).append("\n");
            } else {
                sb.append("  HSA: none\n");
            }

            sb.append("  Debit cards: ").append(dto.isHasDebitCards() ? "yes" : "no").append("\n");

            String earliest = dto.getEarliestBenefitEffective();
            String latest   = dto.getNextRenewalMax();
            if (earliest != null || latest != null) {
                sb.append("  Renewal window: ")
                  .append(earliest != null ? earliest : "unknown")
                  .append(" through ")
                  .append(latest != null ? latest : "unknown")
                  .append("\n");
            }

            sb.append("  Active benefit count: ").append(dto.getActiveBenefitCount()).append("\n");

            List<BenefitDTO> benefits = dto.getBenefits();
            if (benefits != null && !benefits.isEmpty()) {
                int totalActive = 0;
                for (BenefitDTO b : benefits) {
                    if (b.isActive()) totalActive++;
                }
                if (totalActive > 0) {
                    sb.append("  Active benefit detail:\n");
                    int shown = 0;
                    for (BenefitDTO b : benefits) {
                        if (!b.isActive()) continue;
                        if (shown >= MAX_BENEFITS_PER_ER) break;
                        sb.append("    - ");
                        sb.append(b.getPlanName() != null ? b.getPlanName() : b.getPlanTypeName());
                        if (b.getCode() != null) sb.append(" (").append(b.getCode()).append(")");
                        sb.append(" — ").append(b.getSourceType());
                        if (b.getEffectiveDate()  != null) sb.append(", effective ").append(b.getEffectiveDate());
                        if (b.getNextRenewalDue() != null) sb.append(", renews ").append(b.getNextRenewalDue());
                        sb.append("\n");
                        shown++;
                    }
                    if (totalActive > shown) {
                        sb.append("    ...and ").append(totalActive - shown).append(" more\n");
                    }
                }
            }

            sb.append("\n");
        }
        return sb.toString();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Short (≤4 chars) or single-word names need a word-boundary check to avoid
     * matching as substrings inside other words (e.g., "GPC" inside "AGPC Corp").
     */
    private static boolean needsWordBoundary(String lcName) {
        return lcName.length() <= 4 || !lcName.contains(" ");
    }

    private static Map<String, Long> getOrLoadNameIndex(EntityManager em) {
        if (nameIndex != null) return nameIndex;
        synchronized (CACHE_LOCK) {
            if (nameIndex != null) return nameIndex;
            try {
                List<EmployerInventoryDTO> all = EmployerInventoryDAO.getAllInScope(em);
                // LinkedHashMap preserves insertion order for deterministic matching
                Map<String, Long> idx = new LinkedHashMap<>();
                for (EmployerInventoryDTO dto : all) {
                    if (dto.getEmployerName() != null && dto.getOrganizationId() != null) {
                        idx.put(dto.getEmployerName().toLowerCase(Locale.ROOT), dto.getOrganizationId());
                    }
                }
                nameIndex = Collections.unmodifiableMap(idx);
                log.info("EmployerContextResolver: cached {} in-scope employer names", nameIndex.size());
            } catch (Exception e) {
                log.error("EmployerContextResolver: failed to load employer name index; " +
                          "context resolution will be unavailable until next restart", e);
                nameIndex = Collections.emptyMap();
            }
        }
        return nameIndex;
    }
}
