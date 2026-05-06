package net.superiorstate.ams.model.general;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "knowledge_base")
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kb_id")
    private Long id;

    @Column(name = "kb_key", nullable = false, length = 50, unique = true)
    private String kbKey;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private KnowledgeBaseSource source = KnowledgeBaseSource.DB;

    @Column(name = "json_filename", length = 100)
    private String jsonFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "reload_strategy", nullable = false)
    private KnowledgeBaseReloadStrategy reloadStrategy = KnowledgeBaseReloadStrategy.SEARCH;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 100;

    @Column(name = "date_created", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp dateCreated;

    @Column(name = "date_modified", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp dateModified;

    // ── Getters and Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKbKey() { return kbKey; }
    public void setKbKey(String kbKey) { this.kbKey = kbKey; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public KnowledgeBaseSource getSource() { return source; }
    public void setSource(KnowledgeBaseSource source) { this.source = source; }

    public String getJsonFilename() { return jsonFilename; }
    public void setJsonFilename(String jsonFilename) { this.jsonFilename = jsonFilename; }

    public KnowledgeBaseReloadStrategy getReloadStrategy() { return reloadStrategy; }
    public void setReloadStrategy(KnowledgeBaseReloadStrategy reloadStrategy) { this.reloadStrategy = reloadStrategy; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Timestamp getDateCreated() { return dateCreated; }
    public void setDateCreated(Timestamp dateCreated) { this.dateCreated = dateCreated; }

    public Timestamp getDateModified() { return dateModified; }
    public void setDateModified(Timestamp dateModified) { this.dateModified = dateModified; }
}
