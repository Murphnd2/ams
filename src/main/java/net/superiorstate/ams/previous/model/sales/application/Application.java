package net.superiorstate.ams.previous.model.sales.application;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.activity.ticket.setup.Setup;
import net.superiorstate.ams.previous.model.sales.agency.Proposal;

import java.util.List;

@Entity
public class Application {
    @Id
    @OneToOne
    @JoinColumn(name="proposal_id")
    private Proposal proposal;

    @OneToMany(mappedBy = "application")
    private List<ApplicationModule> applicationModuleList;

    @OneToMany(mappedBy = "application")
    private List<ApplicationData> applicationDataList;

    @OneToOne(mappedBy = "application")
    private Setup setup;

    public Application(){}

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public List<ApplicationModule> getApplicationModuleList() {
        return applicationModuleList;
    }

    public void setApplicationModuleList(List<ApplicationModule> applicationModuleList) {
        this.applicationModuleList = applicationModuleList;
    }

    public List<ApplicationData> getApplicationDataList() {
        return applicationDataList;
    }

    public void setApplicationDataList(List<ApplicationData> applicationDataList) {
        this.applicationDataList = applicationDataList;
    }

    public Setup getSetup() {
        return setup;
    }

    public void setSetup(Setup setup) {
        this.setup = setup;
    }

}
