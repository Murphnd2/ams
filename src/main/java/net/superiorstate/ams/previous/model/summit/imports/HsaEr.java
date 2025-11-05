package net.superiorstate.ams.previous.model.summit.imports;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.summit.archive.Employer;

@Entity
public class HsaEr {

    @Id
    @Column(name="name")
    private String name;

    @OneToOne
    @JoinColumn(name="organization_id")
    private Employer employer;

    @Column(name="billed_direct")
    private boolean billedDirect;

    public HsaEr(){}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(Employer employer) {
        this.employer = employer;
    }

    public boolean isBilledDirect() {
        return billedDirect;
    }

    public void setBilledDirect(boolean billedDirect) {
        this.billedDirect = billedDirect;
    }
}
