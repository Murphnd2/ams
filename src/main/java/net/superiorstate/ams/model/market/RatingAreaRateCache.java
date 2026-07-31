package net.superiorstate.ams.model.market;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rating_area_rate_cache")
public class RatingAreaRateCache {

    public static final String SOURCE_ENV_STAGING = "STAGING";
    public static final String SOURCE_ENV_PRODUCTION = "PRODUCTION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "plan_year", nullable = false)
    private Integer planYear;

    @Column(name = "county_fips", nullable = false)
    private String countyFips;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "uses_tobacco", nullable = false)
    private boolean usesTobacco;

    @Column(name = "market_low_premium")
    private BigDecimal marketLowPremium;

    @Column(name = "market_high_premium")
    private BigDecimal marketHighPremium;

    @Column(name = "lcsp_premium")
    private BigDecimal lcspPremium;

    @Column(name = "benchmark_silver_premium")
    private BigDecimal benchmarkSilverPremium;

    @Column(name = "lowest_bronze_premium")
    private BigDecimal lowestBronzePremium;

    /** V078: on-exchange counterpart to {@link #lcspPremium} — ICHRA affordability (T44) keys on this, not the off-exchange figure. */
    @Column(name = "onex_lcsp_premium")
    private BigDecimal onexLcspPremium;

    /** V078: on-exchange counterpart to {@link #benchmarkSilverPremium}. */
    @Column(name = "onex_benchmark_silver_premium")
    private BigDecimal onexBenchmarkSilverPremium;

    @Column(name = "carrier_count")
    private Integer carrierCount;

    @Column(name = "plan_count")
    private Integer planCount;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "source_env", nullable = false)
    private String sourceEnv;

    public RatingAreaRateCache() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPlanYear() {
        return planYear;
    }

    public void setPlanYear(Integer planYear) {
        this.planYear = planYear;
    }

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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public boolean isUsesTobacco() {
        return usesTobacco;
    }

    public void setUsesTobacco(boolean usesTobacco) {
        this.usesTobacco = usesTobacco;
    }

    public BigDecimal getMarketLowPremium() {
        return marketLowPremium;
    }

    public void setMarketLowPremium(BigDecimal marketLowPremium) {
        this.marketLowPremium = marketLowPremium;
    }

    public BigDecimal getMarketHighPremium() {
        return marketHighPremium;
    }

    public void setMarketHighPremium(BigDecimal marketHighPremium) {
        this.marketHighPremium = marketHighPremium;
    }

    public BigDecimal getLcspPremium() {
        return lcspPremium;
    }

    public void setLcspPremium(BigDecimal lcspPremium) {
        this.lcspPremium = lcspPremium;
    }

    public BigDecimal getBenchmarkSilverPremium() {
        return benchmarkSilverPremium;
    }

    public void setBenchmarkSilverPremium(BigDecimal benchmarkSilverPremium) {
        this.benchmarkSilverPremium = benchmarkSilverPremium;
    }

    public BigDecimal getLowestBronzePremium() {
        return lowestBronzePremium;
    }

    public void setLowestBronzePremium(BigDecimal lowestBronzePremium) {
        this.lowestBronzePremium = lowestBronzePremium;
    }

    public BigDecimal getOnexLcspPremium() {
        return onexLcspPremium;
    }

    public void setOnexLcspPremium(BigDecimal onexLcspPremium) {
        this.onexLcspPremium = onexLcspPremium;
    }

    public BigDecimal getOnexBenchmarkSilverPremium() {
        return onexBenchmarkSilverPremium;
    }

    public void setOnexBenchmarkSilverPremium(BigDecimal onexBenchmarkSilverPremium) {
        this.onexBenchmarkSilverPremium = onexBenchmarkSilverPremium;
    }

    public Integer getCarrierCount() {
        return carrierCount;
    }

    public void setCarrierCount(Integer carrierCount) {
        this.carrierCount = carrierCount;
    }

    public Integer getPlanCount() {
        return planCount;
    }

    public void setPlanCount(Integer planCount) {
        this.planCount = planCount;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public String getSourceEnv() {
        return sourceEnv;
    }

    public void setSourceEnv(String sourceEnv) {
        this.sourceEnv = sourceEnv;
    }
}
