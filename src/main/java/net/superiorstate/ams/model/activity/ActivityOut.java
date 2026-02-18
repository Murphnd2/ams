package net.superiorstate.ams.model.activity;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.summit.archive.Employer;

import java.sql.Date;
import java.time.LocalDate;

@Entity
@Table(name="a_base_05")
public class ActivityOut {
    @Id
    @Column(name="UID")
    private String uid;

    @ManyToOne
    @JoinColumn(name="id")
    private Activity activity;

    @Column(name="DTYPE")
    private String dtype;

    @Column(name="full_name_alt")
    private String fullName;

    @Column(name="due_date")
    private String dueDate;

    @Column(name="is_complete")
    private boolean isComplete;

    @ManyToOne
    @JoinColumn(name="assigned_to_id")
    private Person assignedTo;

    @ManyToOne
    @JoinColumn(name="employer_id")
    private Employer employer;

    @Column(name="contact_status")
    private int contactStatus;

    @Column(name="needs_contact")
    private boolean isNeedingContact;

    @Column(name="waiting_on_us")
    private boolean isWaitingOnUs;

    @Column(name="last_contact")
    private Date lastContactDate;

    @ManyToOne
    @JoinColumn(name="checklist_id")
    private CheckList checkList;

    @ManyToOne
    @JoinColumn(name="task_owner_id")
    private Person taskOwner;

    @ManyToOne
    @JoinColumn(name="source_owner_id")
    private Person sourceOwner;

    public ActivityOut(){}

    public String getUid() {
        return uid;
    }

    public Activity getActivity() {
        return activity;
    }

    public String getDtype() {
        return dtype;
    }

    public String getFullName() {
        return fullName;
    }

    public Date getDueDate() {
        return Date.valueOf(dueDate);
    }

    public boolean isComplete() {
        return isComplete;
    }

    public Person getAssignedTo() {
        return assignedTo;
    }

    public Employer getEmployer() {
        return employer;
    }

    public int getContactStatus() {
        return contactStatus;
    }

    public boolean isNeedingContact() {
        return isNeedingContact;
    }

    public boolean isWaitingOnUs() {
        return isWaitingOnUs;
    }

    public Date getLastContactDate() {
        return lastContactDate;
    }

    public CheckList getCheckList() {
        return checkList;
    }

    public Person getTaskOwner() {
        return taskOwner;
    }

    public Person getSourceOwner() {
        return sourceOwner;
    }

    public int getOwnershipLevel(Long userId) {
        try{
            if(getAssignedTo().getId().equals(userId))
                return 1;
            else if(getTaskOwner().getId().equals(userId))
                return 2;
            else return 0;
        } catch (Exception e){return 0;}

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

    public String getPicture(Long userId){
        try{
            if(getOwnershipLevel(userId)==1)
               return "person";
            else if(getOwnershipLevel(userId)==2)
               return "check-lg";
            return "collection";
        }catch (Exception e){return "collection";}

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
        return "<button type=\"submit\" class=\""+className+"\" name=\"btnViewActivity\" style=\"width:21%\" id=\""+getActivity().getId()+"\" value=\""+getActivity().getId()+"\"><i class=\""+iClass+"\">&nbsp;"+theSpan+"</i></button>";
    }

    public String getFullNameFormatted(){
        String name = getFullName();
        if(isWaitingOnUs)
            name = name.toUpperCase();
        else
            name = name.toLowerCase();
        String className = "text-muted";
        if(getContactStatus()==1)
            className="fw-bold";
        else if(getContactStatus()==2)
            className ="fw-bold text-danger";
        return "<span class=\""+className+" text-truncate\">"+name+"</span>";
    }
}
