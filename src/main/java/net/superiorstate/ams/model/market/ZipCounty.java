package net.superiorstate.ams.model.market;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One ZIP-to-county mapping (V084/V085). Composite key {@code (zip, countyFips)}
 * because the relation is genuinely many-to-many — in Texas, 686 of 1,992 ZCTAs
 * (34%) straddle a county line.
 * <p>
 * <b>Source and its limitation.</b> Rows are derived from the US Census Bureau's
 * 2020 ZCTA-to-county relationship file (public domain, 17 U.S.C. 105). ZCTAs are
 * Census tabulation areas that <i>approximate</i> USPS ZIP codes — they omit ZIPs
 * with no residential delivery area, notably PO-box-only ones. <b>A valid USPS ZIP
 * may therefore have no row here, and that is a miss, not an invalid input.</b>
 * Callers must present it that way; see {@code ZipCountyResolver}.
 * <p>
 * <b>{@code landAreaRatio} is a share of land area, not of population.</b> It is
 * deliberately not called {@code resRatio} — HUD's residential ratio counts
 * addresses, this counts dirt, and for a ZIP with a town on one side of a county
 * line and ranchland on the other the two disagree. It exists <b>only</b> to order
 * the candidate counties a crossing ZIP presents to an agent. It must never drive
 * an automatic selection, and no figure shown to a user may be derived from it.
 * Null where the source reported no land area — an unknown ratio is not a zero one.
 */
@Entity
@Table(name = "zip_county")
@IdClass(ZipCounty.ZipCountyId.class)
public class ZipCounty {

    @Id
    @Column(name = "zip", columnDefinition = "char(5)")
    private String zip;

    @Id
    @Column(name = "county_fips", columnDefinition = "char(5)")
    private String countyFips;

    /** Land-area share of the ZCTA inside this county, 0–1. Ordering only — never a displayed figure. */
    @Column(name = "land_area_ratio")
    private BigDecimal landAreaRatio;

    public ZipCounty() {}

    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }

    public String getCountyFips() { return countyFips; }
    public void setCountyFips(String countyFips) { this.countyFips = countyFips; }

    public BigDecimal getLandAreaRatio() { return landAreaRatio; }
    public void setLandAreaRatio(BigDecimal landAreaRatio) { this.landAreaRatio = landAreaRatio; }

    @Override
    public String toString() {
        return zip + " -> " + countyFips;
    }

    /** Composite primary key, following the {@code IrsLimit.IrsLimitId} precedent. */
    public static class ZipCountyId implements Serializable {
        private String zip;
        private String countyFips;

        public ZipCountyId() {}

        public ZipCountyId(String zip, String countyFips) {
            this.zip = zip;
            this.countyFips = countyFips;
        }

        public String getZip() { return zip; }
        public void setZip(String zip) { this.zip = zip; }

        public String getCountyFips() { return countyFips; }
        public void setCountyFips(String countyFips) { this.countyFips = countyFips; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ZipCountyId)) return false;
            ZipCountyId other = (ZipCountyId) o;
            return Objects.equals(zip, other.zip) && Objects.equals(countyFips, other.countyFips);
        }

        @Override
        public int hashCode() {
            return Objects.hash(zip, countyFips);
        }
    }
}
