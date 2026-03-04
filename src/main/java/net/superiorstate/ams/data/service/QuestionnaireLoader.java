package net.superiorstate.ams.data.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import net.superiorstate.ams.model.activity.questionnaire.Questionnaire;
import net.superiorstate.ams.model.activity.questionnaire.QuestionnaireField;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads questionnaire seed data from questionnaire_seeds.json during PSP initialization.
 * Pattern follows PackageLoader: Jackson JSON, duplicate detection via template_key,
 * two-pass persist (entities then scoping), em.flush() after parent persist.
 */
public class QuestionnaireLoader {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String SEED_FILE = "questionnaire/questionnaire_seeds.json";

    public record LoadResult(int loaded, int skipped, List<String> skippedKeys) {}

    /**
     * Loads all questionnaire templates from the seed JSON for the given PSP.
     * Skips any with a template_key that already exists for this PSP.
     */
    public static LoadResult loadQuestionnaires(EntityManager em, PSP psp) {
        int loaded = 0;
        int skipped = 0;
        List<String> skippedKeys = new ArrayList<>();

        // Read seed JSON from classpath
        JsonNode root;
        try (InputStream is = QuestionnaireLoader.class.getClassLoader().getResourceAsStream(SEED_FILE)) {
            if (is == null) {
                System.out.println("QuestionnaireLoader: Seed file not found: " + SEED_FILE);
                return new LoadResult(0, 0, List.of("Seed file not found"));
            }
            root = mapper.readTree(is);
        } catch (Exception e) {
            System.out.println("QuestionnaireLoader: Error reading seed file: " + e.getMessage());
            return new LoadResult(0, 0, List.of("Error reading seed file: " + e.getMessage()));
        }

        JsonNode questionnaires = root.get("questionnaires");
        if (questionnaires == null || !questionnaires.isArray()) {
            return new LoadResult(0, 0, List.of("No questionnaires array in seed file"));
        }

        try {
            // Pass 1: Create questionnaires + fields
            List<ScopingWork> scopingWork = new ArrayList<>();

            em.getTransaction().begin();

            for (JsonNode qNode : questionnaires) {
                String templateKey = qNode.get("template_key").asText();

                // Duplicate detection: skip if template_key already exists for this PSP
                TypedQuery<Long> existsQuery = em.createQuery(
                        "SELECT COUNT(q) FROM Questionnaire q WHERE q.templateKey = :key AND q.psp.id = :pspId",
                        Long.class);
                existsQuery.setParameter("key", templateKey);
                existsQuery.setParameter("pspId", psp.getId());
                if (existsQuery.getSingleResult() > 0) {
                    skipped++;
                    skippedKeys.add(templateKey);
                    continue;
                }

                // Create questionnaire entity
                Questionnaire q = new Questionnaire();
                q.setTemplateKey(templateKey);
                q.setName(qNode.get("name").asText());
                q.setDescription(getTextOrNull(qNode, "description"));
                q.setActivityType(qNode.get("activity_type").asText());
                q.setExternalUrl(getTextOrNull(qNode, "external_url"));
                q.setSortOrder(loaded * 100); // auto-increment by loading order
                q.setSuppressed(false);
                q.setPsp(psp);
                em.persist(q);
                em.flush(); // ensure questionnaire_id assigned before field FKs

                // Flatten sections into fields (native mode only)
                JsonNode sections = qNode.get("sections");
                if (sections != null && sections.isArray()) {
                    int fieldSortOrder = 100;
                    for (JsonNode sectionNode : sections) {
                        String sectionName = getTextOrNull(sectionNode, "section_name");
                        JsonNode fields = sectionNode.get("fields");
                        if (fields != null && fields.isArray()) {
                            for (JsonNode fieldNode : fields) {
                                QuestionnaireField field = new QuestionnaireField();
                                field.setQuestionnaire(q);
                                field.setFieldKey(fieldNode.get("field_key").asText());
                                field.setLabel(getTextOrNull(fieldNode, "label"));
                                field.setFieldType(fieldNode.has("field_type") ? fieldNode.get("field_type").asText() : "TEXT");
                                field.setSelectOptions(getTextOrNull(fieldNode, "select_options"));
                                field.setHelpText(getTextOrNull(fieldNode, "help_text"));
                                field.setSectionName(sectionName);
                                field.setRequired(fieldNode.has("is_required") && fieldNode.get("is_required").asBoolean());
                                field.setSortOrder(fieldSortOrder);
                                field.setSuppressed(false);
                                em.persist(field);
                                fieldSortOrder += 100;
                            }
                        }
                    }
                }

                // Track for pass 2 scoping
                JsonNode scoping = qNode.get("scoping");
                if (scoping != null) {
                    scopingWork.add(new ScopingWork(q, scoping));
                }

                loaded++;
                System.out.println("QuestionnaireLoader: Created '" + q.getName() + "' (" +
                        (q.isExternal() ? "external" : "native") + ")");
            }

            em.getTransaction().commit();

            // Pass 2: Scope linking (separate transaction, matches PackageLoader pattern)
            if (!scopingWork.isEmpty()) {
                em.getTransaction().begin();

                for (ScopingWork work : scopingWork) {
                    linkScoping(em, work.questionnaire, work.scopingNode, psp);
                    em.merge(work.questionnaire);
                }

                em.getTransaction().commit();
            }

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("QuestionnaireLoader: Error loading questionnaires: " + e.getMessage());
            e.printStackTrace();
            return new LoadResult(0, 0, List.of("Error: " + e.getMessage()));
        }

        return new LoadResult(loaded, skipped, skippedKeys);
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private record ScopingWork(Questionnaire questionnaire, JsonNode scopingNode) {}

    /**
     * Links a questionnaire to LOS/Enhancement based on its scoping definition.
     * Looks up LOS by shortText or description. Enhancement by shortText or description.
     * scope_all links to ALL active LOS and Enhancement for this PSP.
     */
    private static void linkScoping(EntityManager em, Questionnaire q, JsonNode scoping, PSP psp) {
        // scope_all: link to all active LOS + Enhancement
        if (scoping.has("scope_all") && scoping.get("scope_all").asBoolean()) {
            List<LOS> allLos = em.createQuery(
                            "SELECT l FROM LOS l WHERE l.psp.id = :pspId AND l.suppressed = false", LOS.class)
                    .setParameter("pspId", psp.getId())
                    .getResultList();
            List<Enhancement> allEnh = em.createQuery(
                            "SELECT e FROM Enhancement e WHERE e.psp.id = :pspId AND e.suppressed = false", Enhancement.class)
                    .setParameter("pspId", psp.getId())
                    .getResultList();
            for (LOS l : allLos) {
                if (!q.getLosList().contains(l)) q.getLosList().add(l);
            }
            for (Enhancement e : allEnh) {
                if (!q.getEnhancementList().contains(e)) q.getEnhancementList().add(e);
            }
            return;
        }

        // LOS scoping by name
        JsonNode losNames = scoping.get("los");
        if (losNames != null && losNames.isArray()) {
            for (JsonNode losName : losNames) {
                String name = losName.asText();
                List<LOS> matches = em.createQuery(
                                "SELECT l FROM LOS l WHERE l.psp.id = :pspId AND l.suppressed = false " +
                                        "AND (l.shortText = :name OR l.description = :name)", LOS.class)
                        .setParameter("pspId", psp.getId())
                        .setParameter("name", name)
                        .getResultList();
                if (matches.isEmpty()) {
                    System.out.println("QuestionnaireLoader: No LOS match for '" + name + "' on PSP " + psp.getId() +
                            " — scoping will be applied when PSP configures this LOS");
                } else {
                    for (LOS l : matches) {
                        if (!q.getLosList().contains(l)) q.getLosList().add(l);
                    }
                }
            }
        }

        // Enhancement scoping by name
        JsonNode enhNames = scoping.get("enhancements");
        if (enhNames != null && enhNames.isArray()) {
            for (JsonNode enhName : enhNames) {
                String name = enhName.asText();
                List<Enhancement> matches = em.createQuery(
                                "SELECT e FROM Enhancement e WHERE e.psp.id = :pspId AND e.suppressed = false " +
                                        "AND (e.shortText = :name OR e.description = :name)", Enhancement.class)
                        .setParameter("pspId", psp.getId())
                        .setParameter("name", name)
                        .getResultList();
                if (matches.isEmpty()) {
                    System.out.println("QuestionnaireLoader: No Enhancement match for '" + name + "' on PSP " + psp.getId() +
                            " — scoping will be applied when PSP configures this Enhancement");
                } else {
                    for (Enhancement e : matches) {
                        if (!q.getEnhancementList().contains(e)) q.getEnhancementList().add(e);
                    }
                }
            }
        }
    }

    /** Reads a text value from a JsonNode, returning null if the field is missing or JSON null. */
    private static String getTextOrNull(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) return null;
        return node.get(fieldName).asText();
    }
}
