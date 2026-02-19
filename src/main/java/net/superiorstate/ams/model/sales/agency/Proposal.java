package net.superiorstate.ams.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.sales.application.Application;
import net.superiorstate.ams.model.sales.offering.LOS;

import java.sql.Timestamp;
import java.util.List;

@Entity
public class Proposal {
    @Id
    @GeneratedValue
    @Column(name="proposal_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="prospect_id",nullable = false)
    private Prospect prospect;

    @ManyToOne
    @JoinColumn(name="rate_id",nullable = false)
    private Rate rate;

    @Column(name="date_created",nullable = false,updatable = false,insertable = false,columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp dateCreated;

    @Column(name="is_inactive", columnDefinition = "TINYINT")
    private boolean isInactive;

    @Column(name="application_guid",columnDefinition = "varchar(36)",nullable = false)
    private String applicationGUID;

    @Column(name="status",columnDefinition = "varchar(20) DEFAULT 'CREATED'")
    private String status;

    @ManyToOne
    @JoinColumn(name="created_by")
    private Person createdBy;

    @Column(name="date_sent")
    private Timestamp dateSent;

    @Column(name="date_viewed")
    private Timestamp dateViewed;

    @Column(name="date_applied")
    private Timestamp dateApplied;

    @ManyToMany
    @JoinTable(name="proposalitems",
            joinColumns = @JoinColumn(name="proposal_id"),inverseJoinColumns = @JoinColumn(name="los_id"))
    List<LOS> losList;

    @OneToOne(mappedBy = "proposal")
    private Application application;

    @ManyToOne
    @JoinColumn(name="source_activity_id")
    private Activity sourceActivity;
    public Proposal(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Prospect getProspect() {
        return prospect;
    }

    public void setProspect(Prospect prospect) {
        this.prospect = prospect;
    }

    public Rate getRate() {
        return rate;
    }
    public Activity getSourceActivity() {
        return sourceActivity;
    }

    public void setSourceActivity(Activity sourceActivity) {
        this.sourceActivity = sourceActivity;
    }
    public void setRate(Rate rate) {
        this.rate = rate;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    public boolean isInactive() {
        return isInactive;
    }

    public void setInactive(boolean inactive) {
        isInactive = inactive;
    }

    public String getApplicationGUID() {
        return applicationGUID;
    }

    public void setApplicationGUID(String applicationGUID) {
        this.applicationGUID = applicationGUID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Person getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Person createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getDateSent() {
        return dateSent;
    }

    public void setDateSent(Timestamp dateSent) {
        this.dateSent = dateSent;
    }

    public Timestamp getDateViewed() {
        return dateViewed;
    }

    public void setDateViewed(Timestamp dateViewed) {
        this.dateViewed = dateViewed;
    }

    public Timestamp getDateApplied() {
        return dateApplied;
    }

    public void setDateApplied(Timestamp dateApplied) {
        this.dateApplied = dateApplied;
    }

    public List<LOS> getLosList() {
        return losList;
    }

    public void setLosList(List<LOS> losList) {
        this.losList = losList;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public void addLos(LOS los){
        this.losList.add(los);
        los.getListOfProposalsThatIncludeThisLOS().add(this);
    }
    public void removeLos(LOS los){
        this.losList.remove(los);
        los.getListOfProposalsThatIncludeThisLOS().remove(this);
    }
}