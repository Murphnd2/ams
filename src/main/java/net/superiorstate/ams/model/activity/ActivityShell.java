package net.superiorstate.ams.model.activity;

import java.sql.Date;
import java.time.LocalDate;

public class ActivityShell {
    private long activityId;
    private String fullName;
    private String dtype;
    private Date dueDate;
    private boolean isWaitingOnUs;
    private boolean isNeedingFollowUp;
    private int contactLevel;
    private int ownershipLevel;

    public ActivityShell(){}

    public long getActivityId() {
        return activityId;
    }

    public void setActivityId(long activityId) {
        this.activityId = activityId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDtype() {
        return dtype;
    }

    public void setDtype(String dtype) {
        this.dtype = dtype;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isWaitingOnUs() {
        return isWaitingOnUs;
    }

    public void setWaitingOnUs(boolean waitingOnUs) {
        isWaitingOnUs = waitingOnUs;
    }

    public boolean isNeedingFollowUp() {
        return isNeedingFollowUp;
    }

    public void setNeedingFollowUp(boolean needingFollowUp) {
        isNeedingFollowUp = needingFollowUp;
    }

    public int getContactLevel() {
        return contactLevel;
    }

    public void setContactLevel(int contactLevel) {
        this.contactLevel = contactLevel;
    }

    public int getOwnershipLevel() {
        return ownershipLevel;
    }

    public void setOwnershipLevel(int ownershipLevel) {
        this.ownershipLevel = ownershipLevel;
    }

    public String getDateHtml(){
        String className;
        LocalDate dueMonth=LocalDate.of(getDueDate().toLocalDate().getYear(),getDueDate().toLocalDate().getMonthValue(),2);
        LocalDate today = LocalDate.now();
        LocalDate curMonth=LocalDate.of(today.getYear(),today.getMonthValue(),1);
        if(dueMonth.isAfter(curMonth.plusMonths(1L)))
            className = "btn btn-outline-secondary text-lowercase fw-lighter pe-none";
        else if(dueMonth.isAfter(curMonth))
            className = "btn btn-outline-dark pe-none";
        else if(dueMonth.isAfter(curMonth.minusMonths(1L)))
            className = "btn btn-warning text-dark pe-none";
        else className = "btn btn-danger fw-bold pe-none";
        return className;

    }

    public String getPicture(){
        String picture = "collection";
        if(getOwnershipLevel()==1)
            picture = "person";
        else if(getOwnershipLevel()==2)
            picture = "check-lg";
        return picture;
    }

    public String getButtonHtml(){
        String className = "btn btn-";
        String iClass = "bi bi-";
        if(!isWaitingOnUs)
            className += "outline-";
        if(getDtype().equals("Renewal")) {
            className += "primary";
            iClass += "repeat";
        }
        else if(getDtype().equals("Setup")) {
            className += "secondary";
            iClass += "buildings";
        }
        else {
            className +="info";
            iClass += "ticket-detailed";
        }
        className +=" text-truncate";
        String theSpan = "<span class=\"d-none d-lg-inline\">" + getDtype() + "</span>";
        return "<button type=\"submit\" class=\""+className+"\" name=\"btnViewActivity\" style=\"width:21%\" id=\""+getActivityId()+"\" value=\""+getActivityId()+"\"><i class=\""+iClass+"\">&nbsp;"+theSpan+"</i></button>";
    }

    public String getFullNameFormatted(){
        String name = getFullName();
        if(isWaitingOnUs)
            name = name.toUpperCase();
        else
            name = name.toLowerCase();
        String className = "text-muted";
        if(getContactLevel()==1)
            className="fw-bold";
        else if(getContactLevel()==2)
            className ="fw-bold text-danger";
        String span = "<span class=\""+className+" text-truncate\">"+name+"</span>";
        return span;
    }
}
