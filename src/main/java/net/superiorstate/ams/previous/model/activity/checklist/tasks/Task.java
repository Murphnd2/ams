package net.superiorstate.ams.previous.model.activity.checklist.tasks;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.model.activity.checklist.sequences.support.TaskSequenceTable;
import net.superiorstate.ams.previous.model.general.Automation;
import net.superiorstate.ams.previous.model.general.PSP;
import net.superiorstate.ams.previous.model.general.Person;
import net.superiorstate.ams.previous.model.general.WebLink;

import java.util.List;

@Entity
public class Task {
    @Id
    @GeneratedValue
    @Column(name="task_id")
    private Long id;

    @Column(columnDefinition = "varchar(200)")
    private String description;

    @ManyToOne
    @JoinColumn(name="psp_id")
    private PSP psp;

    @Column(name="has_owner")
    private boolean hasOwner;

    @Column(name="has_info")
    private boolean hasInfo;

    @Column(name="has_goto")
    private boolean hasGoTo;

    @Column(name="is_sourced")
    private boolean isSourced;

    @ManyToOne
    @JoinColumn(name="goto_link_id")
    private WebLink goToLink;

    @ManyToOne
    @JoinColumn(name="info_link_id")
    private WebLink infoLink;

    @ManyToOne
    @JoinColumn(name="source_owner")
    private Person sourceOwner;


    @ManyToOne
    @JoinColumn(name="owner_id")
    private Person owner;

    @ManyToMany
    @JoinTable(name="task_links",joinColumns =@JoinColumn(name="task_id"),inverseJoinColumns = @JoinColumn(name="link_id"))
    List<WebLink> webLinkList;

    @OneToMany(mappedBy="task")
    List<TaskSequenceTable> taskSequenceTables;

    @Column(name="reusable",columnDefinition = "boolean default true")
    private boolean reUsable;

    @OneToOne
    @JoinColumn(name="auto_id")
    private Automation automation;

    @Column(name="has_automation")
    private boolean hasAutomation;

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

    public Task(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PSP getPsp() {
        return psp;
    }

    public void setPsp(PSP psp) {
        this.psp = psp;
    }

    public List<WebLink> getWebLinkList() {
        return webLinkList;
    }

    public void setWebLinkList(List<WebLink> webLinkList) {
        this.webLinkList = webLinkList;
    }

    public List<TaskSequenceTable> getTaskSequenceTables() {
        return taskSequenceTables;
    }

    public void setTaskSequenceTables(List<TaskSequenceTable> taskSequenceTables) {
        this.taskSequenceTables = taskSequenceTables;
    }
    public boolean isReUsable() {
        return reUsable;
    }

    public void setReUsable(boolean reUsable) {
        this.reUsable = reUsable;
    }

    public boolean isAutomated() {
        return hasAutomation;
    }

    public void setHasAutomation(boolean hasAutomation) {
        this.hasAutomation = hasAutomation;
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
    public void addWebLink(WebLink webLink){
        this.webLinkList.add(webLink);
        webLink.getListOfTasksWithThisWebLink().add(this);
    }
    public void removeWebLink(WebLink webLink){
        this.webLinkList.remove(webLink);
        webLink.getListOfTasksWithThisWebLink().remove(this);
    }

    public boolean hasOwner() {
        return hasOwner;
    }

    public void setHasOwner(boolean hasOwner) {
        this.hasOwner = hasOwner;
    }

    public boolean hasInfo() {
        return hasInfo;
    }
    public void setHasInfo(boolean hasInfo) {
        this.hasInfo = hasInfo;
    }
    public boolean hasGoTo() {
        return hasGoTo;
    }
    public void setHasGoTo(boolean hasGoTo) {
        this.hasGoTo = hasGoTo;
    }

    public boolean isSourced() {
        return isSourced;
    }
    public void setSourced(boolean sourced) {
        isSourced = sourced;
    }
    public WebLink getGoToLink() {
        return goToLink;
    }
    public void setGoToLink(WebLink goToLink) {
        this.goToLink = goToLink;
    }
    public WebLink getInfoLink() {
        return infoLink;
    }
    public void setInfoLink(WebLink infoLink) {
        this.infoLink = infoLink;
    }
    public Person getSourceOwner() {
        return sourceOwner;
    }
    public void setSourceOwner(Person sourceOwner) {
        this.sourceOwner = sourceOwner;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        this.owner = owner;
    }

    public boolean hasAutomation() {
        return hasAutomation;
    }
    public boolean allowEarly() {
        return allowEarly;
    }
    public void setAllowEarly(boolean allowEarly) {
        this.allowEarly = allowEarly;
    }
    public boolean allowFuture() {
        return allowFuture;
    }
    public void setAllowFuture(boolean allowFuture) {
        this.allowFuture = allowFuture;
    }

    public boolean allowNonOwner() {
        return allowNonOwner;
    }
    public void setAllowNonOwner(boolean allowNonOwner) {
        this.allowNonOwner = allowNonOwner;
    }

    public Automation getAutomation() {
        return automation;
    }

    public void setAutomation(Automation automation) {
        this.automation = automation;
    }
}
