package net.superiorstate.ams.model.sales.agency;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * One age-band row of an {@link ProposalIchraSnapshot} (build-plan item 6, V079).
 * Only populated for {@code mode = AGE_BAND}; zero rows for a {@code RANGE} snapshot.
 */
@Entity
@Table(name = "proposal_ichra_snapshot_band")
public class ProposalIchraSnapshotBand {

    @Id
    @GeneratedValue
    @Column(name = "band_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "snapshot_id", nullable = false)
    private ProposalIchraSnapshot snapshot;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "lives", nullable = false)
    private Integer lives;

    @Column(name = "floor_premium", nullable = false)
    private BigDecimal floorPremium;

    @Column(name = "net_per_employee", nullable = false)
    private BigDecimal netPerEmployee;

    @Column(name = "band_net", nullable = false)
    private BigDecimal bandNet;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public ProposalIchraSnapshotBand() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProposalIchraSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(ProposalIchraSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getLives() {
        return lives;
    }

    public void setLives(Integer lives) {
        this.lives = lives;
    }

    public BigDecimal getFloorPremium() {
        return floorPremium;
    }

    public void setFloorPremium(BigDecimal floorPremium) {
        this.floorPremium = floorPremium;
    }

    public BigDecimal getNetPerEmployee() {
        return netPerEmployee;
    }

    public void setNetPerEmployee(BigDecimal netPerEmployee) {
        this.netPerEmployee = netPerEmployee;
    }

    public BigDecimal getBandNet() {
        return bandNet;
    }

    public void setBandNet(BigDecimal bandNet) {
        this.bandNet = bandNet;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
