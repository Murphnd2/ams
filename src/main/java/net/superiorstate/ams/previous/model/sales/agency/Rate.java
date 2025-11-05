package net.superiorstate.ams.previous.model.sales.agency;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.general.PSP;

import java.util.List;

@Entity
public class Rate implements Comparable<Rate> {
    @Id
    @GeneratedValue
    @Column(name="rate_id")
    private Long id;

    @Column(name="description", columnDefinition = "varchar(100)")
    private String description;

    @Column
    private boolean isSuppressed;

    @ManyToOne
    @JoinColumn(name="psp_id")
    private PSP psp;

    @OneToMany(mappedBy = "rate")
    private List<RateTable> rateTable;

    @ManyToMany(mappedBy = "agencyRateList")
    private List<Agency> listOfAgenciesWithThisRate;

    public Rate(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSuppressed() {
        return isSuppressed;
    }

    public void setSuppressed(boolean suppressed) {
        isSuppressed = suppressed;
    }

    public PSP getPsp() {
        return psp;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }

    public List<RateTable> getRateTable() {
        return rateTable;
    }

    public void setRateTable(List<RateTable> rateTable) {
        this.rateTable = rateTable;
    }

    public List<Agency> getListOfAgenciesWithThisRate() {
        return listOfAgenciesWithThisRate;
    }

    public void setListOfAgenciesWithThisRate(List<Agency> listOfAgenciesWithThisRate) {
        this.listOfAgenciesWithThisRate = listOfAgenciesWithThisRate;
    }
    @Override
    public int compareTo(Rate r) {
        if(this.description.compareTo(r.getDescription()) !=0){
            return this.description.compareTo(r.getDescription());
        } else {
            return this.id.compareTo(r.getId());
        }
    }
}
