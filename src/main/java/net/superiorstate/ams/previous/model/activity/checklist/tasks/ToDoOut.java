package net.superiorstate.ams.previous.model.activity.checklist.tasks;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.activity.checklist.CheckList;
import net.superiorstate.ams.previous.model.general.Automation;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;

@Entity
@Table(name="todo_out_06")
public class ToDoOut {

    @Column(name="sequential_count")
    private int sequenceId;


    @ManyToOne
    @JoinColumn(name="checklist_id")
    private CheckList checkList;

    @Column(name="is_complete")
    private boolean isComplete;

    @Column(name="sort_order")
    private int sortOrder;

    @Id
    @OneToOne
    @JoinColumn(name="todo_id")
    private ToDo toDo;

    @ManyToOne
    @JoinColumn(name="task_id")
    private Task task;

    @Column(name="DESCRIPTION")
    private String description;

    @Column(name="has_automation")
    private boolean hasAutomation;

    @ManyToOne
    @JoinColumn(name="auto_id")
    private Automation automation;

    @Column(name="servlet_name")
    private String servletName;

    @Column(name="automation_text")
    private String automationText;

    @Column(name="allow_early")
    private boolean allowEarly;

    @Column(name="allow_future")
    private boolean allowFuture;

    @Column(name="allow_non_owner")
    private boolean allowNonOwner;

    @Column(name="has_owner")
    private boolean hasOwner;

    @ManyToOne
    @JoinColumn(name="owner_id")
    private Person taskOwner;

    @Column(name="is_sourced")
    private boolean isSourced;

    @ManyToOne
    @JoinColumn(name="source_owner")
    private Person sourceOwner;

    @Column(name="has_goto")
    private boolean hasGoto;

    @ManyToOne
    @JoinColumn(name="goto_link_id")
    private WebLink gotoLink;

    @Column(name="has_info")
    private boolean hasInfo;

    @ManyToOne
    @JoinColumn(name="info_link_id")
    private WebLink infoLink;

    @Column(name="has_future_block")
    private boolean hasFutureBlock;

    public ToDoOut(){}

    public ToDoOut(ToDo t, int index){
        setSequenceId(index);

    }


    public int getSequenceId() {
        return sequenceId;
    }

    public CheckList getCheckList() {
        return checkList;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public ToDo getToDo() {
        return toDo;
    }

    public Task getTask() {
        return task;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasAutomation() {
        return hasAutomation;
    }

    public Automation getAutomation() {
        return automation;
    }

    public String getServletName() {
        return servletName;
    }

    public String getAutomationText() {
        return automationText;
    }

    public boolean allowsEarly() {
        return allowEarly;
    }

    public boolean allowsFuture() {
        return allowFuture;
    }

    public boolean allowsNonOwner() {
        return allowNonOwner;
    }

    public boolean hasOwner() {
        return hasOwner;
    }

    public Person getTaskOwner() {
        return taskOwner;
    }

    public boolean isSourced() {
        return isSourced;
    }

    public Person getSourceOwner() {
        return sourceOwner;
    }

    public boolean hasGoto() {
        return hasGoto;
    }

    public WebLink getGotoLink() {
        return gotoLink;
    }

    public boolean hasInfo() {
        return hasInfo;
    }

    public WebLink getInfoLink() {
        return infoLink;
    }

    public boolean hasFutureBlock() {
        return hasFutureBlock;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setToDo(ToDo toDo) {
        this.toDo = toDo;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHasAutomation(boolean hasAutomation) {
        this.hasAutomation = hasAutomation;
    }

    public void setAutomation(Automation automation) {
        this.automation = automation;
    }

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    public void setAutomationText(String automationText) {
        this.automationText = automationText;
    }

    public void setAllowEarly(boolean allowEarly) {
        this.allowEarly = allowEarly;
    }

    public void setAllowFuture(boolean allowFuture) {
        this.allowFuture = allowFuture;
    }

    public void setAllowNonOwner(boolean allowNonOwner) {
        this.allowNonOwner = allowNonOwner;
    }

    public void setHasOwner(boolean hasOwner) {
        this.hasOwner = hasOwner;
    }

    public void setTaskOwner(Person taskOwner) {
        this.taskOwner = taskOwner;
    }

    public void setSourced(boolean sourced) {
        isSourced = sourced;
    }

    public void setSourceOwner(Person sourceOwner) {
        this.sourceOwner = sourceOwner;
    }

    public void setHasGoto(boolean hasGoto) {
        this.hasGoto = hasGoto;
    }

    public void setGotoLink(WebLink gotoLink) {
        this.gotoLink = gotoLink;
    }

    public void setHasInfo(boolean hasInfo) {
        this.hasInfo = hasInfo;
    }

    public void setInfoLink(WebLink infoLink) {
        this.infoLink = infoLink;
    }

    public void setHasFutureBlock(boolean hasFutureBlock) {
        this.hasFutureBlock = hasFutureBlock;
    }

    public boolean isTimeBlocked(){
        return getSequenceId() != 1 && (!allowsEarly() || hasFutureBlock());
    }

    public boolean isWhoBlocked(Person currentUser){
        if(allowsNonOwner() || (!hasOwner() && !isSourced()) || (hasOwner() && getTaskOwner()==null) || (isSourced() && getSourceOwner()==null))
            return false;
        else if((getTaskOwner()!=null && currentUser.getId().equals(getTaskOwner().getId())) || (getSourceOwner()!=null && currentUser.getId().equals(getSourceOwner().getId())))
            return false;
        else return !allowsNonOwner();
    }

    public String getToDoButton(Person u, Person o){
        if(isWhoBlocked(u) && isComplete())
            return getButtonString("x-square-fill","pe-none");
        else if(isComplete())
            return getButtonString("x-square","");
        else if(isWhoBlocked(u))
            return getButtonString("person-square","pe-none");
        else if(isTimeBlocked())
            return getButtonString("clock-fill","pe-none");
        else if(isDelegated(u))
            return getButtonString("box-arrow-up-left","");
        else if(!isMyActivity(u,o) && !isMyTask(u)){
                return getButtonString("circle","");
        } else return getButtonString("square","");
    }

    private boolean isMyActivity(Person u, Person o){
        return o.getId().equals(u.getId());
    }

    private boolean isMyTask(Person u){
        if(hasOwner()&& getTaskOwner()!=null && getTaskOwner().getId().equals(u.getId()))
            return true;
        if(isSourced() && getSourceOwner()!=null && getSourceOwner().getId().equals(u.getId()))
            return true;
        return false;
    }
    private boolean isDelegated(Person u){
        if(hasOwner() && getTaskOwner()!=null && !getTaskOwner().getId().equals(u.getId()))
            return true;
        if(isSourced() && getSourceOwner() != null && !getSourceOwner().getId().equals(u.getId()))
            return true;
        return false;
    }

    public String getToButton(Person currentUser, Person activityOwner){
        if(isWhoBlocked(currentUser) && isComplete()){
            return getButtonString("x-square","pe-none");
        } else if(isWhoBlocked(currentUser)){
            return getButtonString("person-circle","pe-none");
        } else if(isComplete()){
            return getButtonString("x-square","");
        } else if(isTimeBlocked()){
            return getButtonString("clock","pe-none");
        } else if(hasOwner() && getTaskOwner()!=null){
            if(getTaskOwner().equals(currentUser) && !activityOwner.equals(currentUser)){
                return getButtonString("diamond","");
            } else if(getTaskOwner().equals(currentUser)){
                return getButtonString("square","");
            } else if(!getTaskOwner().equals(currentUser) && activityOwner.equals(currentUser)) {
                return getButtonString("box-arrow-up-left","");
            } else {
                return getButtonString("circle","");
            }
        } else if(isSourced() && getSourceOwner()!=null){
            if(getSourceOwner().equals(currentUser) && !activityOwner.equals(currentUser)){
                return getButtonString("diamond","");
            } else if(getSourceOwner().equals(currentUser)){
                return getButtonString("square","");
            } else if(!getSourceOwner().equals(currentUser) && activityOwner.equals(currentUser)) {
                return getButtonString("box-arrow-up-left","");
            } else {
                return getButtonString("circle","");
            }
        } else {
            return getButtonString("square","");
        }
    }

    public String getDescriptionLink(Person currentUser, String fontSize){
        String strikeThrough = "";
        if(isComplete())
            strikeThrough = "; text-decoration: line-through ";
        String textOnly = "<span style=\"font-size:" + fontSize + strikeThrough+ "\">" + getTask().getDescription() + "</span>";
        if(!hasGoto() || getGotoLink()==null || getGotoLink().getLinkPath()==null || getGotoLink().getLinkPath().equals(""))
            return textOnly;
        if(!isWhoBlocked(currentUser))
            return "<a href=\"" + getTask().getGoToLink().getLinkPath() + "\" target=\"_blank\" style=\"font-size:" + fontSize + strikeThrough + "\">" + getTask().getDescription() + "</a>";
        return textOnly;
    }

    public String getFormServlet(){
        if(isComplete())
            return "reOpenToDoOut";
        return "closeToDoOut";
    }

    public String getHelpButton(Person currentUser){
        if(!hasInfo() || getInfoLink()==null || getInfoLink().getLinkPath()==null || getInfoLink().getLinkPath().equals("") )
            return "";
        if(!allowsNonOwner() && hasOwner() && getTaskOwner()!=null && !getTaskOwner().getId().equals(currentUser.getId()))
            return "";
        if(!allowsNonOwner() && isSourced() && getSourceOwner()!=null)
            return "";
        if(isComplete())
            return "";
        return "<a class=\"btn btn-outline-qm m-0 p-0 mt-1 ps-1 pe-1\" href=\"" + getInfoLink().getLinkPath() + "\" target=\"_blank\">" +
                "<i class=\"bi bi-question-lg\"></i></a>";
    }

    public String getManageButton(Person currentUser, boolean isPspAdmin){
        String peNone = "pe-none";
        if(isPspAdmin || (!hasOwner() && !isSourced()) || (hasOwner() && getTaskOwner()!=null && getTaskOwner().getId().equals(currentUser.getId())))
            peNone = "";
        if(isComplete())
            return "";
        String formString = "<form method=\"post\" action=\"manageTask\" id=\"fm" + getToDo().getId() + "\">" +
                            "<input type=\"text\" name=\"toDoId\" id=\"tdId" + getToDo().getId() + "\" value=\"" + getToDo().getId() + "\" hidden>" +
                            "<button type=\"submit\" class=\"btn btn-outline-auto m-0 p-0 ps-1 pe-1 " + peNone + " mt-1\">" +
                            "<i class=\"bi bi-tools\"></i></button></form>";
        return formString;
    }
    private String getButtonString(String icon, String pe_none){
        String disabled = "";
        if(pe_none.equals("pe-none"))
            disabled = "disabled";
        String btn;
        btn  ="<button type=\"submit\" class=\"btn btn-outline-cb border-white border-0 m-0 p-0\" "+disabled+ " name=\"btnToDo\" id=\"btn"
                + getToDo().getId() + "\" value=\"" + getToDo().getId() + "\">"
                + "<i class=\"bi bi-" + icon + "\" style=\"font-size: 1.4rem\"></i></button>";
        return btn;
    }

}
