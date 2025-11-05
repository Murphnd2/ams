package net.superiorstate.ams.previous.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.sales.application.Application;
import net.superiorstate.ams.previous.model.sales.offering.LOS;

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

    @ManyToMany
    @JoinTable(name="proposalitems",
            joinColumns = @JoinColumn(name="proposal_id"),inverseJoinColumns = @JoinColumn(name="los_id"))
    List<LOS> losList;

    @OneToOne(mappedBy = "proposal")
    private Application application;

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
