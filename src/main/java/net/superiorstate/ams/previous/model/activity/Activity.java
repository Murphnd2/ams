package net.superiorstate.ams.previous.model.activity;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.activity.note.Note;
import net.superiorstate.ams.previous.model.general.Assignee;
import net.superiorstate.ams.previous.model.general.Person;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Entity
public class Activity extends Assignee {
    @Column(name="date_created",nullable = false,updatable = false,insertable = false,columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp dateCreated;

    @Column(name="due_date")
    private Date dueDate;
    @ManyToOne
    @JoinColumn(name="created_by_id")
    private Person loggedBy;

    @ManyToOne
    @JoinColumn(name="assigned_to_id")
    private Assignee assignedTo;

    @Column(name="is_complete")
    private boolean isComplete;

    @Column(name="date_completed")
    private Date dateCompleted;

    @ManyToOne
    @JoinColumn(name="completed_by_id")
    private Person completedBy;

    @ManyToOne
    @JoinColumn(name="primary_contact")
    private Person primaryContact;

   @OneToMany(mappedBy = "activity")
   private List<Note> noteList;


    public Activity(){}

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Person getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(Person loggedBy) {
        this.loggedBy = loggedBy;
    }

    public Assignee getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Assignee assignedTo) {
        this.assignedTo = assignedTo;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public Person getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(Person completedBy) {
        this.completedBy = completedBy;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public Date getDueMonth(){
        return Date.valueOf(LocalDate.of(dueDate.toLocalDate().getYear(),dueDate.toLocalDate().getMonthValue(),1));
    }

    public Person getPrimaryContact(){return  primaryContact;}
    public void setPrimaryContact(Person primaryContact) {this.primaryContact = primaryContact;}

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public List<Note> getNoteList() {
        return noteList;
    }

    public void setNoteList(List<Note> noteList) {
        this.noteList = noteList;
    }

    public Date getLastDate(){
        Date lastDate = Date.valueOf(LocalDate.of(2000,1,1));
        if(getNoteList().size()>0){
            for(Note n: getNoteList()){
                if(n.getReasonCreated().isOutbound()){
                    if(n.getDateGenerated().compareTo(lastDate)>=0)
                        lastDate = n.getDateGenerated();
                }
            }
        }
        return lastDate;
    }

    public void addNote(Note note){
        this.getNoteList().add(note);
    }

    public boolean needsFollowUp(){
        LocalDate cutOff = LocalDate.now().minusDays(7);
        if(getLastDate().compareTo(Date.valueOf(cutOff))<0)
            return true;
        return false;
    }

    public boolean getOnUs(){
        boolean onUs = true;
        Long maxPunch = 0L;
        if(getNoteList().size()>0){
            for(Note n:getNoteList()){
                if(n.getStatus().getId()==1 || n.getStatus().getId()==3) {
                    if (n.getId()>maxPunch){
                        onUs = n.getStatus().getId() != 1;
                        maxPunch = n.getId();
                    }
                }
            }
        }
        return onUs;
    }
}
