package net.superiorstate.ams.model.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * V110 — maps a {@code paycycle_frequency} application answer to a recurrence token, for a
 * future matrix dropdown filter to read.
 * <p>
 * <b>{@code aliasKey}</b> is {@code aliasText} uppercased with all non-alphanumeric characters
 * stripped — computed by {@link net.superiorstate.ams.data.dao.PaycycleFrequencyAliasDAO#normalize}
 * so the seed migration and any caller agree on one implementation. Unique.
 * <p>
 * <b>No code reads this table yet</b> — the matrix dropdown filter and preselection are a
 * separate, later task.
 * <p>
 * <b>An answer with no matching active alias must result in no filtering at all</b> when that
 * later filter is built, never a partial or wrong filter — the table's own comment states this,
 * and it is restated here since this entity is what a caller will query.
 */
@Entity
@Table(name = "paycycle_frequency_alias")
public class PaycycleFrequencyAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "alias_text", nullable = false)
    private String aliasText;

    @Column(name = "alias_key", nullable = false)
    private String aliasKey;

    @Column(name = "recurrence", nullable = false)
    private String recurrence;

    @Column(name = "semimonthly_variant")
    private String semimonthlyVariant;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public PaycycleFrequencyAlias() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getAliasText() { return aliasText; }
    public void setAliasText(String aliasText) { this.aliasText = aliasText; }

    public String getAliasKey() { return aliasKey; }
    public void setAliasKey(String aliasKey) { this.aliasKey = aliasKey; }

    public String getRecurrence() { return recurrence; }
    public void setRecurrence(String recurrence) { this.recurrence = recurrence; }

    public String getSemimonthlyVariant() { return semimonthlyVariant; }
    public void setSemimonthlyVariant(String semimonthlyVariant) { this.semimonthlyVariant = semimonthlyVariant; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
