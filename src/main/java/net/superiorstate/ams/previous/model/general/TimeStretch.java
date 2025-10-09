package net.superiorstate.ams.previous.model.general;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDateTime;

public class TimeStretch {
    private Long id;
    private Person user;
    private Date inDate;
    private Time inTime;
    private Date outDate;
    private Time outTime;

    public TimeStretch(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getUser() {
        return user;
    }

    public void setUser(Person user) {
        this.user = user;
    }

    public Date getInDate() {
        return inDate;
    }

    public void setInDate(Date inDate) {
        this.inDate = inDate;
    }

    public Time getInTime() {
        return inTime;
    }

    public void setInTime(Time inTime) {
        this.inTime = inTime;
    }

    public Date getOutDate() {
        return outDate;
    }

    public void setOutDate(Date outDate) {
        this.outDate = outDate;
    }

    public Time getOutTime() {
        return outTime;
    }

    public void setOutTime(Time outTime) {
        this.outTime = outTime;
    }

    public int getMinutesWorked(){
        int minutesWorked = 0;
        LocalDateTime timeOut;
        LocalDateTime timeIn = LocalDateTime.of(inDate.toLocalDate(),inTime.toLocalTime());
        try{
            timeOut = LocalDateTime.of(outDate.toLocalDate(),outTime.toLocalTime());
        } catch (Exception e){
            e.printStackTrace();
            return 0;
        }
        minutesWorked = timeOut.compareTo(timeIn);
        return minutesWorked;
    }
}
