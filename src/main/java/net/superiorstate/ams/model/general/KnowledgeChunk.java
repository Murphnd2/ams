package net.superiorstate.ams.model.general;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kb_id", nullable = false)
    private KnowledgeBase knowledgeBase;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "section", length = 100)
    private String section;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "keywords", length = 500)
    private String keywords;

    @Column(name = "account_type", length = 50)
    private String accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_type", nullable = false)
    private KnowledgeChunkType chunkType;

    @Column(name = "effective_start")
    private LocalDate effectiveStart;

    @Column(name = "effective_end")
    private LocalDate effectiveEnd;

    @Column(name = "source_citation", length = 500)
    private String sourceCitation;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private KnowledgeChunkVisibility visibility = KnowledgeChunkVisibility.INTERNAL;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "date_created", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp dateCreated;

    @Column(name = "date_modified", insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp dateModified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private Person modifiedBy;

    // ── Getters and Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KnowledgeBase getKnowledgeBase() { return knowledgeBase; }
    public void setKnowledgeBase(KnowledgeBase knowledgeBase) { this.knowledgeBase = knowledgeBase; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public KnowledgeChunkType getChunkType() { return chunkType; }
    public void setChunkType(KnowledgeChunkType chunkType) { this.chunkType = chunkType; }

    public LocalDate getEffectiveStart() { return effectiveStart; }
    public void setEffectiveStart(LocalDate effectiveStart) { this.effectiveStart = effectiveStart; }

    public LocalDate getEffectiveEnd() { return effectiveEnd; }
    public void setEffectiveEnd(LocalDate effectiveEnd) { this.effectiveEnd = effectiveEnd; }

    public String getSourceCitation() { return sourceCitation; }
    public void setSourceCitation(String sourceCitation) { this.sourceCitation = sourceCitation; }

    public KnowledgeChunkVisibility getVisibility() { return visibility; }
    public void setVisibility(KnowledgeChunkVisibility visibility) { this.visibility = visibility; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Timestamp getDateCreated() { return dateCreated; }
    public void setDateCreated(Timestamp dateCreated) { this.dateCreated = dateCreated; }

    public Timestamp getDateModified() { return dateModified; }
    public void setDateModified(Timestamp dateModified) { this.dateModified = dateModified; }

    public Person getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(Person modifiedBy) { this.modifiedBy = modifiedBy; }
}
