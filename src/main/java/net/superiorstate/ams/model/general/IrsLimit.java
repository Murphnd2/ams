package net.superiorstate.ams.model.general;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "irslimit")
@IdClass(IrsLimit.IrsLimitId.class)
public class IrsLimit {

    @Id
    @Column(name = "limit_key", columnDefinition = "varchar(50)")
    private String limitKey;

    @Id
    @Column(name = "plan_year")
    private int planYear;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "description", columnDefinition = "varchar(200)", nullable = false)
    private String description;

    public IrsLimit() {}

    public String getLimitKey() { return limitKey; }
    public void setLimitKey(String limitKey) { this.limitKey = limitKey; }

    public int getPlanYear() { return planYear; }
    public void setPlanYear(int planYear) { this.planYear = planYear; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFormattedAmount() {
        return String.format("$%,.0f", amount);
    }

    public static class IrsLimitId implements Serializable {
        private String limitKey;
        private int planYear;

        public IrsLimitId() {}

        public String getLimitKey() { return limitKey; }
        public void setLimitKey(String limitKey) { this.limitKey = limitKey; }
        public int getPlanYear() { return planYear; }
        public void setPlanYear(int planYear) { this.planYear = planYear; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IrsLimitId that = (IrsLimitId) o;
            return planYear == that.planYear && limitKey.equals(that.limitKey);
        }

        @Override
        public int hashCode() {
            return 31 * limitKey.hashCode() + planYear;
        }
    }
}