package net.superiorstate.ams.previous.model.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.sql.Date;

@Entity
public class BillingMonth {
    @Id
    @GeneratedValue
    @Column(name="billing_month_id")
    private int monthId;

    @Column(name="full_date",unique = true)
    private Date fullDate;

    @Column(name="year")
    private int year;

    @Column(name="month")
    private int month;

    public BillingMonth(){}

    public int getMonthId() {
        return monthId;
    }

    public void setMonthId(int monthId) {
        this.monthId = monthId;
    }

    public Date getFullDate() {
        return fullDate;
    }

    public void setFullDate(Date fullDate) {
        this.fullDate = fullDate;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getMonthName(){
        return getFullDate().toLocalDate().getMonth().toString();
    }

    public String getBillingMonth(){
        return getMonthName() + " " + getYear();
    }
}
