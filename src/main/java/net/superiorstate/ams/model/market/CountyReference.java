package net.superiorstate.ams.model.market;

import jakarta.persistence.*;

/**
 * National county reference data: FIPS code, state, county name, and one
 * representative ZIP per county (V076). Seeded for Texas only; other states
 * are added by later migrations of the same shape.
 */
@Entity
@Table(name = "county_reference")
public class CountyReference {

    @Id
    @Column(name = "county_fips")
    private String countyFips;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "county_name", nullable = false)
    private String countyName;

    @Column(name = "representative_zip", nullable = false)
    private String representativeZip;

    public CountyReference() {}

    public String getCountyFips() {
        return countyFips;
    }

    public void setCountyFips(String countyFips) {
        this.countyFips = countyFips;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public String getRepresentativeZip() {
        return representativeZip;
    }

    public void setRepresentativeZip(String representativeZip) {
        this.representativeZip = representativeZip;
    }

    @Override
    public String toString() {
        return countyName + ", " + state + " (" + countyFips + ")";
    }
}
