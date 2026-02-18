package net.superiorstate.ams.model.general;

import jakarta.persistence.*;

import java.sql.Date;
import java.sql.Time;

@Entity
public class TimeLog {
    @Id
    @GeneratedValue
    @Column(name="log_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="person_id")
    private Person person;

    @Column(name="date")
    private Date punchDate;

    @Column(name="time")
    private Time punchTime;

    @Column(name="is_in")
    private boolean isIn;

    public TimeLog(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Date getPunchDate() {
        return punchDate;
    }

    public void setPunchDate(Date punchDate) {
        this.punchDate = punchDate;
    }

    public Time getPunchTime() {
        return punchTime;
    }

    public void setPunchTime(Time punchTime) {
        this.punchTime = punchTime;
    }

    public boolean isIn() {
        return isIn;
    }

    public void setIn(boolean in) {
        isIn = in;
    }
}
