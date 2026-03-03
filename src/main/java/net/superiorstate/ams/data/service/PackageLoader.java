package net.superiorstate.ams.data.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationSection;
import net.superiorstate.ams.model.sales.offering.Enhancement;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.io.InputStream;
import java.util.*;

public class PackageLoader {

    private static final ObjectMapper mapper = new ObjectMapper();

    public record PackageSummary(String id, String name, String description) {}
    public record PackageLoadResult(int sectionsLoaded, int sectionsSkipped, List<String> skippedKeys) {}

    // ── Package listing ─────────────────────────────────────────────────

    public static List<PackageSummary> getAvailablePackages() {
        List<PackageSummary> packages = new ArrayList<>();
        try (InputStream is = PackageLoader.class.getClassLoader().getResourceAsStream("packages/package-index.json")) {
            if (is == null) return packages;
            JsonNode root = mapper.readTree(is);
            for (JsonNode node : root) {
                packages.add(new PackageSummary(
                        node.get("id").asText(),
                        node.get("name").asText(),
                        node.get("description").asText()
                ));
            }
        } catch (Exception e) {
            System.out.println("PackageLoader: Error reading package index: " + e.getMessage());
        }
        return packages;
    }

    /**
     * Returns only packages that have at least one section not yet loaded for this PSP.
     * A package is considered "fully loaded" if every section's templateKey exists
     * as a non-null templateKey on an ApplicationSection for this PSP (regardless of suppressed state).
     */
    public static List<PackageSummary> getAvailablePackagesForPsp(EntityManager em, long pspId) {
        List<PackageSummary> allPackages = getAvailablePackages();
        List<PackageSummary> available = new ArrayList<>();

        for (PackageSummary pkg : allPackages) {
            List<String> templateKeys = getPackageTemplateKeys(pkg.id());
            if (templateKeys.isEmpty()) continue;

            TypedQuery<Long> q = em.createQuery(
                    "SELECT COUNT(s) FROM ApplicationSection s WHERE s.templateKey IN :keys AND s.psp.id = :pspId",
                    Long.class);
            q.setParameter("keys", templateKeys);
            q.setParameter("pspId", pspId);
            long loadedCount = q.getSingleResult();

            if (loadedCount < templateKeys.size()) {
                available.add(pkg);
            }
        }
        return available;
    }

    // ── Package loading ─────────────────────────────────────────────────

    public static PackageLoadResult loadPackage(EntityManager em, String packageId, PSP psp) {
        int sectionsLoaded = 0;
        int sectionsSkipped = 0;
        List<String> skippedKeys = new ArrayList<>();

        String fileName = getPackageFileName(packageId);
        if (fileName == null) {
            return new PackageLoadResult(0, 0, List.of("Package not found: " + packageId));
        }

        JsonNode packageRoot;
        try (InputStream pkgIs = PackageLoader.class.getClassLoader().getResourceAsStream("packages/" + fileName)) {
            if (pkgIs == null) return new PackageLoadResult(0, 0, List.of("Package file not found: " + fileName));
            packageRoot = mapper.readTree(pkgIs);
        } catch (Exception e) {
            return new PackageLoadResult(0, 0, List.of("Error reading package file: " + e.getMessage()));
        }

        try {
            // Pass 1: Create sections + fields (no relationship linking yet)
            List<ApplicationSection> allScopeSections = new ArrayList<>();

            em.getTransaction().begin();

            JsonNode sections = packageRoot.get("sections");
            for (JsonNode sectionNode : sections) {
                String templateKey = sectionNode.get("templateKey").asText();

                // Check if section already exists for this PSP
                TypedQuery<Long> existsQuery = em.createQuery(
                        "SELECT COUNT(s) FROM ApplicationSection s WHERE s.templateKey = :key AND s.psp.id = :pspId",
                        Long.class);
                existsQuery.setParameter("key", templateKey);
                existsQuery.setParameter("pspId", psp.getId());
                if (existsQuery.getSingleResult() > 0) {
                    sectionsSkipped++;
                    skippedKeys.add(templateKey);
                    continue;
                }

                // Create the section
                ApplicationSection section = new ApplicationSection();
                section.setTemplateKey(templateKey);
                section.setName(sectionNode.get("name").asText());
                section.setDescription(sectionNode.get("description").asText());
                section.setScope(sectionNode.get("scope").asText());
                section.setSortOrder(sectionNode.get("sortOrder").asInt());
                section.setSuppressed(false);
                section.setPsp(psp);
                em.persist(section);
                em.flush(); // ensure section_id is assigned before field FK references

                // Track ALL-scope sections for pass 2
                if ("ALL".equals(section.getScope())) {
                    allScopeSections.add(section);
                }

                // Create fields
                JsonNode fields = sectionNode.get("fields");
                if (fields != null) {
                    System.out.println("PackageLoader: Processing " + fields.size() + " fields for section '" + section.getName() + "'");
                    for (JsonNode fieldNode : fields) {
                        String fieldKey = fieldNode.get("fieldKey").asText();

                        // Skip if field already exists (String PK)
                        if (em.find(ApplicationField.class, fieldKey) != null) {
                            System.out.println("PackageLoader: Field '" + fieldKey + "' already exists, skipping");
                            continue;
                        }

                        ApplicationField field = new ApplicationField();
                        field.setFieldKey(fieldKey);
                        field.setLabel(fieldNode.get("label").asText());
                        field.setFieldType(fieldNode.get("fieldType").asText());
                        field.setRequired(fieldNode.get("required").asBoolean());
                        field.setSortOrder(fieldNode.get("sortOrder").asInt());
                        field.setSuppressed(false);
                        field.setApplicationSection(section);

                        if (!fieldNode.get("helpText").isNull()) {
                            field.setHelpText(fieldNode.get("helpText").asText());
                        }
                        if (!fieldNode.get("selectOptions").isNull()) {
                            field.setSelectOptions(fieldNode.get("selectOptions").asText());
                        }

                        em.persist(field);
                        System.out.println("PackageLoader: Created field '" + fieldKey + "'");
                    }
                }

                sectionsLoaded++;
            }

            em.getTransaction().commit();

            // Pass 2: ALL-scope auto-assign to LOS/Enhancements (separate transaction, matches createAppSection pattern)
            if (!allScopeSections.isEmpty()) {
                List<LOS> allLos = em.createQuery(
                                "SELECT l FROM LOS l WHERE l.psp.id = :pspId AND l.suppressed = false", LOS.class)
                        .setParameter("pspId", psp.getId())
                        .getResultList();
                List<Enhancement> allEnh = em.createQuery(
                                "SELECT e FROM Enhancement e WHERE e.psp.id = :pspId AND e.suppressed = false", Enhancement.class)
                        .setParameter("pspId", psp.getId())
                        .getResultList();

                em.getTransaction().begin();
                for (ApplicationSection section : allScopeSections) {
                    for (LOS l : allLos) {
                        if (section.getLosList() == null || !section.getLosList().contains(l)) {
                            if (section.getLosList() == null) section.setLosList(new ArrayList<>());
                            section.getLosList().add(l);
                        }
                    }
                    for (Enhancement e : allEnh) {
                        if (section.getEnhancementList() == null || !section.getEnhancementList().contains(e)) {
                            if (section.getEnhancementList() == null) section.setEnhancementList(new ArrayList<>());
                            section.getEnhancementList().add(e);
                        }
                    }
                    em.merge(section);
                }
                em.getTransaction().commit();
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("PackageLoader: Error loading package " + packageId + ": " + e.getMessage());
            e.printStackTrace();
            return new PackageLoadResult(0, 0, List.of("Error loading package: " + e.getMessage()));
        }

        return new PackageLoadResult(sectionsLoaded, sectionsSkipped, skippedKeys);
    }

    // ── Reset to default ────────────────────────────────────────────────

    /**
     * Resets a package-loaded section to its JSON template defaults.
     * - Existing fields matching JSON: properties restored to defaults
     * - Suppressed fields matching JSON: un-suppressed and properties restored
     * - Missing fields from JSON: created
     * - Extra fields not in JSON (manually added): suppressed
     * - Section properties (name, description, scope, sortOrder) restored
     *
     * @return number of fields restored/created
     */
    public static int resetSectionToDefault(EntityManager em, ApplicationSection section) {
        if (section.getTemplateKey() == null) return 0;

        JsonNode sectionDef = findSectionDefinition(section.getTemplateKey());
        if (sectionDef == null) return 0;

        int fieldsProcessed = 0;

        em.getTransaction().begin();

        // Restore section properties
        section.setName(sectionDef.get("name").asText());
        section.setDescription(sectionDef.get("description").asText());
        section.setScope(sectionDef.get("scope").asText());
        section.setSortOrder(sectionDef.get("sortOrder").asInt());
        em.merge(section);

        // Build a set of JSON field keys for this section
        JsonNode jsonFields = sectionDef.get("fields");
        Set<String> jsonFieldKeys = new HashSet<>();
        if (jsonFields != null) {
            for (JsonNode fieldDef : jsonFields) {
                String fieldKey = fieldDef.get("fieldKey").asText();
                jsonFieldKeys.add(fieldKey);

                ApplicationField existing = em.find(ApplicationField.class, fieldKey);
                if (existing != null) {
                    // Restore properties to JSON defaults
                    existing.setLabel(fieldDef.get("label").asText());
                    existing.setFieldType(fieldDef.get("fieldType").asText());
                    existing.setRequired(fieldDef.get("required").asBoolean());
                    existing.setSortOrder(fieldDef.get("sortOrder").asInt());
                    existing.setHelpText(fieldDef.has("helpText") && !fieldDef.get("helpText").isNull()
                            ? fieldDef.get("helpText").asText() : null);
                    existing.setSelectOptions(fieldDef.has("selectOptions") && !fieldDef.get("selectOptions").isNull()
                            ? fieldDef.get("selectOptions").asText() : null);
                    existing.setSuppressed(false);
                    existing.setApplicationSection(section);
                    em.merge(existing);
                } else {
                    // Create new field
                    ApplicationField field = new ApplicationField();
                    field.setFieldKey(fieldKey);
                    field.setLabel(fieldDef.get("label").asText());
                    field.setFieldType(fieldDef.get("fieldType").asText());
                    field.setRequired(fieldDef.get("required").asBoolean());
                    field.setSortOrder(fieldDef.get("sortOrder").asInt());
                    field.setHelpText(fieldDef.has("helpText") && !fieldDef.get("helpText").isNull()
                            ? fieldDef.get("helpText").asText() : null);
                    field.setSelectOptions(fieldDef.has("selectOptions") && !fieldDef.get("selectOptions").isNull()
                            ? fieldDef.get("selectOptions").asText() : null);
                    field.setSuppressed(false);
                    field.setApplicationSection(section);
                    em.persist(field);
                }
                fieldsProcessed++;
            }
        }

        // Suppress any manually-added fields that are NOT in the JSON template
        List<ApplicationField> currentFields = em.createQuery(
                        "SELECT f FROM ApplicationField f WHERE f.applicationSection.id = :sectionId AND f.suppressed = false",
                        ApplicationField.class)
                .setParameter("sectionId", section.getId())
                .getResultList();
        for (ApplicationField f : currentFields) {
            if (!jsonFieldKeys.contains(f.getFieldKey())) {
                f.setSuppressed(true);
                em.merge(f);
            }
        }

        em.getTransaction().commit();
        return fieldsProcessed;
    }

    // ── Private helpers ─────────────────────────────────────────────────

    /**
     * Scans all package JSON files to find the section definition matching a templateKey.
     */
    private static JsonNode findSectionDefinition(String templateKey) {
        List<PackageSummary> allPackages = getAvailablePackages();
        for (PackageSummary pkg : allPackages) {
            String fileName = getPackageFileName(pkg.id());
            if (fileName == null) continue;
            try (InputStream is = PackageLoader.class.getClassLoader().getResourceAsStream("packages/" + fileName)) {
                if (is == null) continue;
                JsonNode root = mapper.readTree(is);
                JsonNode sections = root.get("sections");
                if (sections != null) {
                    for (JsonNode section : sections) {
                        if (templateKey.equals(section.get("templateKey").asText())) {
                            return section;
                        }
                    }
                }
            } catch (Exception e) {
                // continue to next package
            }
        }
        return null;
    }

    /**
     * Reads a package file and returns the list of templateKey values for its sections.
     */
    private static List<String> getPackageTemplateKeys(String packageId) {
        List<String> keys = new ArrayList<>();
        String fileName = getPackageFileName(packageId);
        if (fileName == null) return keys;

        try (InputStream is = PackageLoader.class.getClassLoader().getResourceAsStream("packages/" + fileName)) {
            if (is == null) return keys;
            JsonNode root = mapper.readTree(is);
            JsonNode sections = root.get("sections");
            if (sections != null) {
                for (JsonNode section : sections) {
                    keys.add(section.get("templateKey").asText());
                }
            }
        } catch (Exception e) {
            System.out.println("PackageLoader: Error reading package keys for " + packageId + ": " + e.getMessage());
        }
        return keys;
    }

    /**
     * Looks up the filename for a packageId from the index.
     */
    private static String getPackageFileName(String packageId) {
        try (InputStream is = PackageLoader.class.getClassLoader().getResourceAsStream("packages/package-index.json")) {
            if (is == null) return null;
            JsonNode index = mapper.readTree(is);
            for (JsonNode node : index) {
                if (packageId.equals(node.get("id").asText())) {
                    return node.get("file").asText();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
