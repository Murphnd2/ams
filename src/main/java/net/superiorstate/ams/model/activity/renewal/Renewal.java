package net.superiorstate.ams.model.activity.renewal;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.Activity;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.util.List;

@Entity
public class Renewal extends Activity {
    @ManyToOne
    @JoinColumn(name="employer_id")
    private Employer employer;

    @OneToMany(mappedBy = "renewal")
    private List<RenewalItem> renewalItemList;

    @OneToOne
    @JoinColumn(name="checklist_id")
    private CheckList checkList;

    public Renewal(){}

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
    }

    public List<RenewalItem> getRenewalItemList() {
        return renewalItemList;
    }

    public void setRenewalItemList(List<RenewalItem> renewalItemList) {
        this.renewalItemList = renewalItemList;
    }

    public CheckList getCheckList() {
        return checkList;
    }

    public void setCheckList(CheckList checkList) {
        this.checkList = checkList;
    }


}
