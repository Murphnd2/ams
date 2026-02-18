package net.superiorstate.ams.model;

import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.Automation;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;

public class ToDoOut25 {
    private int sequenceId;
    private CheckList checkList;
    private boolean isComplete;
    private int sortOrder;
    private ToDo toDo;
    private Task task;
    private String description;
    private boolean hasAutomation;
    private Automation automation;
    private String servletName;
    private String automationText;
    private boolean allowEarly;
    private boolean allowFuture;
    private boolean allowNonOwner;
    private boolean hasOwner;
    private Person taskOwner;
    private boolean isSourced;
    private Person sourceOwner;
    private boolean hasGoto;
    private WebLink gotoLink;
    private boolean hasInfo;
    private WebLink infoLink;
    private boolean hasFutureBlock;

    private boolean wasComplete;

    public ToDoOut25(){}

    public ToDoOut25(ToDo t){
        setSequenceId(t.getId().intValue());
        setCheckList(t.getCheckList());
        setComplete(t.isComplete());
        setSortOrder(t.getSortOrder());
        setToDo(t);
        setTask(t.getTask());
        setDescription(t.getTask().getDescription());
        setHasAutomation(t.getTask().isAutomated());
        setAutomation(t.getTask().getAutomation());
        setServletName(t.getTask().getServletName());
        setAutomationText(t.getTask().getAutomationText());
        setAllowEarly(t.getTask().allowEarly());
        setAllowFuture(t.getTask().allowFuture());
        setAllowNonOwner(t.getTask().allowNonOwner());
        setHasOwner(t.getTask().hasOwner());
        setTaskOwner(t.getTask().getOwner());
        setSourced(t.getTask().isSourced());
        setSourceOwner(t.getTask().getSourceOwner());
        setHasGoto(t.getTask().hasGoTo());
        setGotoLink(t.getTask().getGoToLink());
        setHasInfo(t.getTask().hasInfo());
        setInfoLink(t.getTask().getInfoLink());
        setHasFutureBlock(false);
        this.wasComplete = toDo.isComplete();
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    public CheckList getCheckList() {
        return checkList;
    }

    public void setCheckList(CheckList checkList) {
        this.checkList = checkList;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public ToDo getToDo() {
        return toDo;
    }

    public void setToDo(ToDo toDo) {
        this.toDo = toDo;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean hasAutomation() {
        return hasAutomation;
    }

    public void setHasAutomation(boolean hasAutomation) {
        this.hasAutomation = hasAutomation;
    }

    public Automation getAutomation() {
        return automation;
    }

    public void setAutomation(Automation automation) {
        this.automation = automation;
    }

    public String getServletName() {
        return servletName;
    }

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    public String getAutomationText() {
        return automationText;
    }

    public void setAutomationText(String automationText) {
        this.automationText = automationText;
    }

    public boolean allowsEarly() {
        return allowEarly;
    }

    public void setAllowEarly(boolean allowEarly) {
        this.allowEarly = allowEarly;
    }

    public boolean allowsFuture() {
        return allowFuture;
    }

    public void setAllowFuture(boolean allowFuture) {
        this.allowFuture = allowFuture;
    }

    public boolean allowsNonOwner() {
        return allowNonOwner;
    }

    public void setAllowNonOwner(boolean allowNonOwner) {
        this.allowNonOwner = allowNonOwner;
    }

    public boolean hasOwner() {
        return hasOwner;
    }

    public void setHasOwner(boolean hasOwner) {
        this.hasOwner = hasOwner;
    }

    public Person getTaskOwner() {
        return taskOwner;
    }

    public void setTaskOwner(Person taskOwner) {
        this.taskOwner = taskOwner;
    }

    public boolean isSourced() {
        return isSourced;
    }

    public void setSourced(boolean sourced) {
        isSourced = sourced;
    }

    public Person getSourceOwner() {
        return sourceOwner;
    }

    public void setSourceOwner(Person sourceOwner) {
        this.sourceOwner = sourceOwner;
    }

    public boolean hasGoto() {
        return hasGoto;
    }

    public void setHasGoto(boolean hasGoto) {
        this.hasGoto = hasGoto;
    }

    public WebLink getGotoLink() {
        return gotoLink;
    }

    public void setGotoLink(WebLink gotoLink) {
        this.gotoLink = gotoLink;
    }

    public boolean hasInfo() {
        return hasInfo;
    }

    public void setHasInfo(boolean hasInfo) {
        this.hasInfo = hasInfo;
    }

    public WebLink getInfoLink() {
        return infoLink;
    }

    public void setInfoLink(WebLink infoLink) {
        this.infoLink = infoLink;
    }

    public boolean isHasFutureBlock() {
        return hasFutureBlock;
    }

    public void setHasFutureBlock(boolean hasFutureBlock) {
        this.hasFutureBlock = hasFutureBlock;
    }

    public boolean wasComplete() { return wasComplete; }
    public void setWasComplete(boolean b) { this.wasComplete = b; }
}
