package net.superiorstate.ams.model;

import java.sql.Date;

/**
 * Row DTO for {@code /AgentSetupList} and the Mockup-B "My Tasks" sidebar.
 * Extracted to a top-level class (was an inner class) so JSP EL can
 * reliably resolve its getters.
 */
public class AgentSetupRow {

    private Long id;
    private String name;
    private Date dueDate;
    private String prospectName;
    private String sellingAgentName;
    private Long sellingAgentId;
    private boolean sellingAgentIsMe;
    private int totalToDos;
    private int doneToDos;
    private int myOpenToDos;
    private int scopedOpenToDos;
    private int progressPct;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public String getProspectName() { return prospectName; }
    public void setProspectName(String prospectName) { this.prospectName = prospectName; }

    public String getSellingAgentName() { return sellingAgentName; }
    public void setSellingAgentName(String sellingAgentName) { this.sellingAgentName = sellingAgentName; }

    public Long getSellingAgentId() { return sellingAgentId; }
    public void setSellingAgentId(Long sellingAgentId) { this.sellingAgentId = sellingAgentId; }

    public boolean isSellingAgentIsMe() { return sellingAgentIsMe; }
    public void setSellingAgentIsMe(boolean sellingAgentIsMe) { this.sellingAgentIsMe = sellingAgentIsMe; }

    public int getTotalToDos() { return totalToDos; }
    public void setTotalToDos(int totalToDos) { this.totalToDos = totalToDos; }

    public int getDoneToDos() { return doneToDos; }
    public void setDoneToDos(int doneToDos) { this.doneToDos = doneToDos; }

    public int getMyOpenToDos() { return myOpenToDos; }
    public void setMyOpenToDos(int myOpenToDos) { this.myOpenToDos = myOpenToDos; }

    public int getScopedOpenToDos() { return scopedOpenToDos; }
    public void setScopedOpenToDos(int scopedOpenToDos) { this.scopedOpenToDos = scopedOpenToDos; }

    public int getProgressPct() { return progressPct; }
    public void setProgressPct(int progressPct) { this.progressPct = progressPct; }

    @Override
    public String toString() {
        return "AgentSetupRow{id=" + id + ", name=" + name + ", due=" + dueDate
                + ", prospect=" + prospectName + ", sellingAgent=" + sellingAgentName
                + ", sellingAgentIsMe=" + sellingAgentIsMe
                + ", total=" + totalToDos + ", done=" + doneToDos
                + ", myOpen=" + myOpenToDos + ", scoped=" + scopedOpenToDos
                + ", progressPct=" + progressPct + "}";
    }
}
