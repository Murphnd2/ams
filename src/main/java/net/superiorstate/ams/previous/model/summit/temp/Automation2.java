package net.superiorstate.ams.previous.model.summit.temp;

public class Automation2 {
    private Long taskId;
    private String subject;
    private String message;
    private boolean isReminder;
    private boolean shouldClose;
    private int statusChangeFlag;

    private boolean nonStandard;

    public Automation2(){}

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isReminder() {
        return isReminder;
    }

    public void setReminder(boolean reminder) {
        isReminder = reminder;
    }

    public boolean isNonStandard() {
        return nonStandard;
    }

    public void setNonStandard(boolean nonStandard) {
        this.nonStandard = nonStandard;
    }

    public boolean shouldClose() {
        return shouldClose;
    }

    public void setShouldClose(boolean shouldClose) {
        this.shouldClose = shouldClose;
    }

    public int getStatusChangeFlag() {
        return statusChangeFlag;
    }

    public void setStatusChangeFlag(int statusChangeFlag) {
        this.statusChangeFlag = statusChangeFlag;
    }
}
