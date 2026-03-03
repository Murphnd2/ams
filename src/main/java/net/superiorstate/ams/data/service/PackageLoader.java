package net.superiorstate.ams.data.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import net.superiorstate.ams.model.general.PSP;
import net.superiorstate.ams.model.sales.application.ApplicationField;
import net.superiorstate.ams.model.sales.application.ApplicationSection;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PackageLoader {

    private static final ObjectMapper mapper = new ObjectMapper();

    public record PackageSummary(String id, String name, String description) {}
    public record PackageLoadResult(int sectionsLoaded, int sectionsSkipped, List<String> skippedKeys) {}

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

    public static PackageLoadResult loadPackage(EntityManager em, String packageId, PSP psp) {
        int sectionsLoaded = 0;
        int sectionsSkipped = 0;
        List<String> skippedKeys = new ArrayList<>();

        // Find the file name from the index
        String fileName = null;
        try (InputStream indexIs = PackageLoader.class.getClassLoader().getResourceAsStream("packages/package-index.json")) {
            if (indexIs == null) return new PackageLoadResult(0, 0, List.of("Package index not found"));
            JsonNode index = mapper.readTree(indexIs);
            for (JsonNode node : index) {
                if (packageId.equals(node.get("id").asText())) {
                    fileName = node.get("file").asText();
                    break;
                }
            }
        } catch (Exception e) {
            return new PackageLoadResult(0, 0, List.of("Error reading package index: " + e.getMessage()));
        }

        if (fileName == null) {
            return new PackageLoadResult(0, 0, List.of("Package not found: " + packageId));
        }

        // Read the package file
        JsonNode packageRoot;
        try (InputStream pkgIs = PackageLoader.class.getClassLoader().getResourceAsStream("packages/" + fileName)) {
            if (pkgIs == null) return new PackageLoadResult(0, 0, List.of("Package file not found: " + fileName));
            packageRoot = mapper.readTree(pkgIs);
        } catch (Exception e) {
            return new PackageLoadResult(0, 0, List.of("Error reading package file: " + e.getMessage()));
        }

        // Load sections within a transaction
        try {
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

                // Create fields
                JsonNode fields = sectionNode.get("fields");
                if (fields != null) {
                    for (JsonNode fieldNode : fields) {
                        String fieldKey = fieldNode.get("fieldKey").asText();

                        // Skip if field already exists (String PK)
                        if (em.find(ApplicationField.class, fieldKey) != null) {
                            continue;
                        }

                        ApplicationField field = new ApplicationField();
                        field.setFieldKey(fieldKey);
                        field.setLabel(fieldNode.get("label").asText());
                        field.setFieldType(fieldNode.get("fieldType").asText());
                        field.setRequired(fieldNode.get("required").asBoolean());
                        field.setSortOrder(fieldNode.get("sortOrder").asInt());
                        field.setApplicationSection(section);

                        if (!fieldNode.get("helpText").isNull()) {
                            field.setHelpText(fieldNode.get("helpText").asText());
                        }
                        if (!fieldNode.get("selectOptions").isNull()) {
                            field.setSelectOptions(fieldNode.get("selectOptions").asText());
                        }

                        em.persist(field);
                    }
                }

                sectionsLoaded++;
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.out.println("PackageLoader: Error loading package " + packageId + ": " + e.getMessage());
            return new PackageLoadResult(0, 0, List.of("Error loading package: " + e.getMessage()));
        }

        return new PackageLoadResult(sectionsLoaded, sectionsSkipped, skippedKeys);
    }
}
