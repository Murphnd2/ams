package net.superiorstate.ams.model.sales.application;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.agency.Proposal;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
public class Application {
    @Id
    @OneToOne
    @JoinColumn(name="proposal_id")
    private Proposal proposal;

    @Column(name="status",columnDefinition = "varchar(20) DEFAULT 'IN_PROGRESS'")
    private String status;

    @Column(name="date_started")
    private Timestamp dateStarted;

    @Column(name="date_submitted")
    private Timestamp dateSubmitted;

    @Column(name="date_reviewed")
    private Timestamp dateReviewed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="reviewed_by")
    private Person reviewedBy;

    @Column(name="review_notes",columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "selected_los_ids", columnDefinition = "varchar(500)")
    private String selectedLosIds;

    @Column(name = "selected_enhancement_ids", columnDefinition = "varchar(500)")
    private String selectedEnhancementIds;

    @OneToMany(mappedBy = "application")
    private List<ApplicationModule> applicationModuleList;

    @OneToMany(mappedBy = "application")
    private List<ApplicationFieldValue> fieldValues;

    @OneToOne(mappedBy = "application")
    private Setup setup;

    public Application(){}

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(Timestamp dateStarted) {
        this.dateStarted = dateStarted;
    }

    public Timestamp getDateSubmitted() {
        return dateSubmitted;
    }

    public void setDateSubmitted(Timestamp dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
    }

    public Timestamp getDateReviewed() {
        return dateReviewed;
    }

    public void setDateReviewed(Timestamp dateReviewed) {
        this.dateReviewed = dateReviewed;
    }

    public Person getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Person reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public List<ApplicationModule> getApplicationModuleList() {
        return applicationModuleList;
    }

    public void setApplicationModuleList(List<ApplicationModule> applicationModuleList) {
        this.applicationModuleList = applicationModuleList;
    }

    public List<ApplicationFieldValue> getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(List<ApplicationFieldValue> fieldValues) {
        this.fieldValues = fieldValues;
    }

    public Setup getSetup() {
        return setup;
    }

    public void setSetup(Setup setup) {
        this.setup = setup;
    }

    public String getSelectedLosIds() {
        return selectedLosIds;
    }

    public void setSelectedLosIds(String selectedLosIds) {
        this.selectedLosIds = selectedLosIds;
    }

    public String getSelectedEnhancementIds() {
        return selectedEnhancementIds;
    }

    public void setSelectedEnhancementIds(String selectedEnhancementIds) {
        this.selectedEnhancementIds = selectedEnhancementIds;
    }

    /** Parse selected LOS IDs into a List. Returns empty list if null/blank. */
    public List<Long> getSelectedLosIdList() {
        if (selectedLosIds == null || selectedLosIds.isBlank()) return Collections.emptyList();
        return Arrays.stream(selectedLosIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong).collect(Collectors.toList());
    }

    /** Parse selected Enhancement IDs into a List. Returns empty list if null/blank. */
    public List<Long> getSelectedEnhancementIdList() {
        if (selectedEnhancementIds == null || selectedEnhancementIds.isBlank()) return Collections.emptyList();
        return Arrays.stream(selectedEnhancementIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong).collect(Collectors.toList());
    }

    /** Check if this application has service selections recorded. */
    public boolean hasServiceSelections() {
        return selectedLosIds != null && !selectedLosIds.isBlank();
    }

}