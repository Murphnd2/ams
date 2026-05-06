package net.superiorstate.ams.model.general;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "knowledge_chunk_history")
public class KnowledgeChunkHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    // Plain Long — no @ManyToOne. History rows survive hard-deletion of their source chunk.
    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    @Column(name = "title_before", length = 200)
    private String titleBefore;

    @Column(name = "content_before", columnDefinition = "TEXT")
    private String contentBefore;

    @Column(name = "keywords_before", length = 500)
    private String keywordsBefore;

    // Boxed Boolean — column is nullable (NULL means "not recorded")
    @Column(name = "is_active_before")
    private Boolean activeBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private KnowledgeChunkChangeType changeType;

    @Column(name = "change_note", length = 500)
    private String changeNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private Person modifiedBy;

    @Column(name = "modified_on", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp modifiedOn;

    // ── Getters and Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }

    public String getTitleBefore() { return titleBefore; }
    public void setTitleBefore(String titleBefore) { this.titleBefore = titleBefore; }

    public String getContentBefore() { return contentBefore; }
    public void setContentBefore(String contentBefore) { this.contentBefore = contentBefore; }

    public String getKeywordsBefore() { return keywordsBefore; }
    public void setKeywordsBefore(String keywordsBefore) { this.keywordsBefore = keywordsBefore; }

    public Boolean getActiveBefore() { return activeBefore; }
    public void setActiveBefore(Boolean activeBefore) { this.activeBefore = activeBefore; }

    public KnowledgeChunkChangeType getChangeType() { return changeType; }
    public void setChangeType(KnowledgeChunkChangeType changeType) { this.changeType = changeType; }

    public String getChangeNote() { return changeNote; }
    public void setChangeNote(String changeNote) { this.changeNote = changeNote; }

    public Person getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(Person modifiedBy) { this.modifiedBy = modifiedBy; }

    public Timestamp getModifiedOn() { return modifiedOn; }
    public void setModifiedOn(Timestamp modifiedOn) { this.modifiedOn = modifiedOn; }
}
