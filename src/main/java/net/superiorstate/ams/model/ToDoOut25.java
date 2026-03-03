package net.superiorstate.ams.model;

import net.superiorstate.ams.model.activity.checklist.CheckList;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDo;
import net.superiorstate.ams.model.general.Automation;
import net.superiorstate.ams.model.general.BpoRegistration;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;

import java.util.List;

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
    private BpoRegistration bpoRegistration;
    private boolean hasGoto;
    private WebLink gotoLink;
    private boolean hasInfo;
    private WebLink infoLink;
    private boolean hasFutureBlock;
    private boolean bpoCompleted;

    private boolean wasComplete;
    // === Pre-computed display state ===
    private boolean isMyTask;
    private boolean isDelegated;
    private boolean isTimeBlocked;
    private boolean isWhoBlocked;
    private String formServlet;
    private String btnIcon;
    private String rowStyle;
    private String rowCssClass;
    private String pointerEvents;
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
        setBpoRegistration(t.getTask().getBpoRegistration());
        setHasGoto(t.getTask().hasGoTo());
        setGotoLink(t.getTask().getGoToLink());
        setHasInfo(t.getTask().hasInfo());
        setInfoLink(t.getTask().getInfoLink());
        setHasFutureBlock(false);
        setBpoCompleted(t.isBpoCompleted());
        this.wasComplete = toDo.isComplete();
    }
    public boolean isMyTask() { return isMyTask; }
    public boolean isDelegated() { return isDelegated; }
    public boolean isTimeBlocked() { return isTimeBlocked; }
    public boolean isWhoBlocked() { return isWhoBlocked; }
    public String getFormServlet() { return formServlet; }
    public String getBtnIcon() { return btnIcon; }
    public String getRowStyle() { return rowStyle; }
    public String getRowCssClass() { return rowCssClass; }
    public String getPointerEvents() { return pointerEvents; }
    public void computeDisplayState(long myPersonId, boolean isAdmin, boolean blockFuture, int openIndex, boolean isMyActivity) {
        this.isMyTask = (hasOwner && taskOwner != null && taskOwner.getId() == myPersonId);
        this.isDelegated = (hasOwner && !this.isMyTask) || (isSourced && bpoRegistration != null);

        this.isTimeBlocked = (openIndex > 0) && (!allowEarly || blockFuture);
        this.isWhoBlocked = !this.isMyTask && !allowNonOwner && (hasOwner || isSourced);

        boolean adminOverride = isAdmin;

        this.formServlet = "CloseToDo25";
        this.btnIcon = "square";
        this.rowStyle = "";
        this.rowCssClass = "";
        this.pointerEvents = "";

        if (isWhoBlocked && isComplete) {
            btnIcon = "x-square-fill"; formServlet = "ReOpenToDo25";
            rowStyle = "text-decoration:line-through;"; rowCssClass = "fst-italic fw-lighter";
            pointerEvents = "pe-none";
        } else if (isComplete) {
            btnIcon = "x-square"; formServlet = "ReOpenToDo25";
            rowStyle = "text-decoration:line-through;"; rowCssClass = "fst-italic fw-lighter";
        } else if (isWhoBlocked) {
            btnIcon = "person-square";
            pointerEvents = "pe-none";
        } else if (isTimeBlocked) {
            btnIcon = "clock-fill";
            pointerEvents = "pe-none";
        } else if (bpoCompleted && !isComplete) {
            btnIcon = "check2-square";
            rowCssClass = "bpo-awaiting-verify";
        } else if (isDelegated) {
            btnIcon = "box-arrow-up-left";
        } else if (!isMyActivity && !isMyTask) {
            btnIcon = "circle";
        }

        if (adminOverride) {
            pointerEvents = "";
        }
    }
    public static void computeAllDisplayStates(List<ToDoOut25> list, long myPersonId, boolean isAdmin, long activityOwnerId) {
        boolean blockFuture = false;
        int openIndex = 0;
        boolean isMyActivity = (myPersonId == activityOwnerId);
        for (ToDoOut25 t : list) {
            if (t.getTask().getId() == 153) continue;
            t.computeDisplayState(myPersonId, isAdmin, blockFuture, openIndex, isMyActivity);
            if (!t.isComplete()) {
                if (!t.allowsFuture()) {
                    blockFuture = true;
                }
                openIndex++;
            }
        }
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

    public BpoRegistration getBpoRegistration() {
        return bpoRegistration;
    }

    public void setBpoRegistration(BpoRegistration bpoRegistration) {
        this.bpoRegistration = bpoRegistration;
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

    public boolean isBpoCompleted() { return bpoCompleted; }
    public void setBpoCompleted(boolean bpoCompleted) { this.bpoCompleted = bpoCompleted; }
}
