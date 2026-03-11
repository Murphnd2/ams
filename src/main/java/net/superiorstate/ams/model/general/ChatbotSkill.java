package net.superiorstate.ams.model.general;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chatbot_skill")
public class ChatbotSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psp_id", nullable = false)
    private PSP psp;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "trigger_keywords", length = 500)
    private String triggerKeywords;

    @Column(name = "accepts_file_upload", nullable = false)
    private boolean acceptsFileUpload;

    @Column(name = "accepted_mime_types", length = 200)
    private String acceptedMimeTypes;

    @Column(name = "model", nullable = false, length = 100)
    private String model = "claude-haiku-4-5-20251001";

    @Column(name = "max_tokens", nullable = false)
    private int maxTokens = 1024;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_admin_only", nullable = false)
    private boolean adminOnly;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 100;

    // ── Getters and Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PSP getPsp() { return psp; }
    public void setPsp(PSP psp) { this.psp = psp; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public String getTriggerKeywords() { return triggerKeywords; }
    public void setTriggerKeywords(String triggerKeywords) { this.triggerKeywords = triggerKeywords; }

    public boolean isAcceptsFileUpload() { return acceptsFileUpload; }
    public void setAcceptsFileUpload(boolean acceptsFileUpload) { this.acceptsFileUpload = acceptsFileUpload; }

    public String getAcceptedMimeTypes() { return acceptedMimeTypes; }
    public void setAcceptedMimeTypes(String acceptedMimeTypes) { this.acceptedMimeTypes = acceptedMimeTypes; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isAdminOnly() { return adminOnly; }
    public void setAdminOnly(boolean adminOnly) { this.adminOnly = adminOnly; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    // ── Helper Methods ──

    /**
     * Checks if a given MIME type is accepted by this skill.
     * Returns true if accepted_mime_types is null/empty (accepts anything when file upload enabled)
     * or if the MIME type is in the comma-separated list.
     */
    public boolean acceptsMimeType(String mimeType) {
        if (acceptedMimeTypes == null || acceptedMimeTypes.isBlank()) return true;
        if (mimeType == null) return false;
        String[] types = acceptedMimeTypes.split(",");
        for (String t : types) {
            if (t.trim().equalsIgnoreCase(mimeType.trim())) return true;
        }
        return false;
    }

    /**
     * Returns trigger keywords as a Set of lowercase strings.
     */
    public Set<String> getTriggerKeywordSet() {
        Set<String> set = new HashSet<>();
        if (triggerKeywords == null || triggerKeywords.isBlank()) return set;
        for (String kw : triggerKeywords.split(",")) {
            String trimmed = kw.trim().toLowerCase();
            if (!trimmed.isEmpty()) set.add(trimmed);
        }
        return set;
    }
}
