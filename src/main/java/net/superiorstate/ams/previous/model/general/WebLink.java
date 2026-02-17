package net.superiorstate.ams.previous.model.general;

import jakarta.persistence.*;
import net.superiorstate.ams.previous.data.misc.dbA;
import net.superiorstate.ams.previous.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.previous.model.activity.note.Email;

import java.util.List;

@Entity
public class WebLink {
    @Id
    @GeneratedValue
    @Column(name="link_id")
    private Long id;

    @Column(name="description",columnDefinition = "varchar(200)")
    private String plainText;

    @Column(name="link_path",columnDefinition = "varchar(2000)")
    private String linkPath;

    @Column(name="active")
    private boolean active;

    @ManyToOne
    @JoinColumn(name="type_id")
    private LinkType linkType;

    @ManyToOne
    @JoinColumn(name="email_id")
    private Email email;

    @ManyToMany(mappedBy="webLinkList")
    List<Task> listOfTasksWithThisWebLink;

    @ManyToMany(mappedBy="webLinkList")
    List<Assignee> listOfAssigneesWithThisWebLink;


    public WebLink(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlainText() {
        return plainText;
    }

    public void setPlainText(String plainText) {
        this.plainText = plainText;
    }

    public String getLinkPath() {
        return linkPath;
    }

    public void setLinkPath(String linkPath) {
        this.linkPath = linkPath;
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(LinkType linkType) {
        this.linkType = linkType;
    }

    public List<Task> getListOfTasksWithThisWebLink() {
        return listOfTasksWithThisWebLink;
    }

    public void setListOfTasksWithThisWebLink(List<Task> listOfTasksWithThisWebLink) {
        this.listOfTasksWithThisWebLink = listOfTasksWithThisWebLink;
    }


    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }


    public String getExternalAnchorTag(EntityManager em){
        String anchorTag = "<a href = \"";
        if(getLinkType().getId()==1){
            anchorTag += dbA.getWebPath(em) + "ShowFileUpload?doc=" + getLinkPath() +"\"";
        } else if (getLinkType().getId()==2) {
            anchorTag += getLinkPath() + "\"";
        }
        anchorTag +=" target = \"blank\">" + getPlainText() + "</a>";
        return anchorTag;

    }


    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Assignee> getListOfAssigneesWithThisWebLink() {
        return listOfAssigneesWithThisWebLink;
    }


}
