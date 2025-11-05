package net.superiorstate.ams.previous.model.activity.renewal;

import net.superiorstate.ams.previous.model.summit.archive.Employer;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;

public class RenewalEmployer implements Comparable<RenewalEmployer> {
    private Employer employer;
    private Date lastRenewed;
    private Integer stage;

    public RenewalEmployer(){}

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
    }

    public Date getLastRenewed() {
        return lastRenewed;
    }

    public void setLastRenewed(Date lastRenewed) {
        this.lastRenewed = lastRenewed;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    @Override
    public int compareTo(@NotNull RenewalEmployer o) {
        if(this.stage.compareTo(o.getStage())!=0)
            return this.stage.compareTo(o.getStage());
        else return this.employer.getEmployerName().compareTo(o.getEmployer().getEmployerName());
    }

}
