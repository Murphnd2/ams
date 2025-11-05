package net.superiorstate.ams.previous.model.activity.checklist.tasks;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.general.Person;

import java.sql.Date;

@Entity
public class ToDo {

    @Id
    @GeneratedValue
    @Column(name="todo_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="task_id")
    private Task task;

    @Column(name="sort_order")
    private int sortOrder;

    @Column(name="is_complete")
    private boolean isComplete;

    @Column(name="date_completed")
    private Date dateCompleted;

    @ManyToOne
    @JoinColumn(name="completed_by_id")
    private Person completedBy;

    @ManyToOne
    @JoinColumn(name="checklist_id")
    private CheckList checkList;

    public ToDo(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
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

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public CheckList getCheckList() {
        return checkList;
    }

    public void setCheckList(CheckList checkList) {
        this.checkList = checkList;
    }

    public String getWebDescription(String yOrN_isOnlyOwner, String fontSize, String isOwner){
        if((!yOrN_isOnlyOwner.equals("Y") || isOwner.equals("1")) && getTask().hasGoTo() && getTask().getGoToLink()!=null && getTask().getGoToLink().getLinkPath()!=null && !isComplete()){
            return "<a href=\"" + getTask().getGoToLink().getLinkPath() + "\" target=\"_blank\" style=\"font-size:" + fontSize + "\">" + getTask().getDescription() + "</a>";
        } else return "<span style=\"font-size:" + fontSize + "\">" + getTask().getDescription() + "</span>";
    }

    public String getWebDescription(boolean allowNonOwner1, String fontSize, boolean isOwner1){
        if((allowNonOwner1 || isOwner1) && getTask().hasGoTo() && getTask().getGoToLink()!=null && getTask().getGoToLink().getLinkPath()!=null && !isComplete()){
            return "<a href=\"" + getTask().getGoToLink().getLinkPath() + "\" target=\"_blank\" style=\"font-size:" + fontSize + "\">" + getTask().getDescription() + "</a>";
        } else return "<span style=\"font-size:" + fontSize + "\">" + getTask().getDescription() + "</span>";
    }

    public String getToDoButton(String yOrN_isActive, long toDoId, String biCode, String hasOwner, String isSourced){
        String btn = "";
        if(!isComplete() && yOrN_isActive.equals("Y") && (hasOwner.equals("Y") || isSourced.equals("Y"))){
            btn = "<button type=\"submit\" class=\"btn btn-outline-cb border-white border-0 m-0 p-0\" name=\"btnToDo\" id=\"btn" + toDoId + "\" value=\"" + toDoId + "\">";
            btn +="<i class=\"bi bi-box-arrow-up-left\" style=\"font-size: 1.4rem\"></i></button>";
        } else if(!isComplete() && yOrN_isActive.equals("Y")){
            btn = "<button type=\"submit\" class=\"btn btn-outline-cb border-white border-0 m-0 p-0\" name=\"btnToDo\" id=\"btn" + toDoId + "\" value=\"" + toDoId + "\">";
            btn +="<i class=\"bi bi-square\" style=\"font-size: 1.4rem\"></i></button>";
        } else if(!isComplete()){
            btn = "<button type=\"submit\" class=\"btn btn-outline-cb border-white border-0 m-0 pe-none p-0\" name=\"btnToDo\" id=\"btn" + toDoId + "\" value=\"" + toDoId + "\" disabled >";
            btn +="<i class=\"bi bi-" + biCode + "\" style=\"font-size: 1.4rem\"></i></button>";
        } else if(yOrN_isActive.equals("Y")){
            btn  ="<button type=\"submit\" class=\"btn btn-outline-cb border-white border-0 m-0 p-0\" name=\"btnToDo\" id=\"btn" + toDoId + "\" value=\"" + toDoId + "\">";
            btn +="<i class=\"bi bi-x-square\" style=\"font-size: 1.4rem\"></i></button>";
        } else {
            btn  ="<button type=\"submit\" class=\"btn btn-outline-cb border-white border-0 pe-none m-0 p-0\" name=\"btnToDo\" id=\"btn" + toDoId + "\" disabled value=\"" + toDoId + "\">";
            btn +="<i class=\"bi bi-x-square\" style=\"font-size: 1.4rem\"></i></button>";
        }
        return btn;
    }

    public String getFormServlet(){
        if(isComplete())
            return "ReOpenToDo";
        return "CloseToDo";
    }
}
