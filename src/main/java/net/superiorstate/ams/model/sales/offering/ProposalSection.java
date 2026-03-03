package net.superiorstate.ams.model.sales.offering;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.PSP;

import java.sql.Timestamp;

@Entity
@Table(name = "proposal_section")
public class ProposalSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "psp_id", nullable = false)
    private PSP psp;

    @Column(name = "section_type", columnDefinition = "varchar(20)", nullable = false)
    private String sectionType;

    @Column(name = "title", columnDefinition = "varchar(200)")
    private String title;

    @Column(name = "html_content", columnDefinition = "text")
    private String htmlContent;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "date_created", insertable = false, updatable = false)
    private Timestamp dateCreated;

    @Column(name = "date_modified", insertable = false, updatable = false)
    private Timestamp dateModified;

    public ProposalSection() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PSP getPsp() {
        return psp;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }

    public String getSectionType() {
        return sectionType;
    }

    public void setSectionType(String sectionType) {
        this.sectionType = sectionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public Timestamp getDateModified() {
        return dateModified;
    }
}
