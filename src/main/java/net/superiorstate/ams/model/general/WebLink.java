package net.superiorstate.ams.model.general;

import jakarta.persistence.*;
import net.superiorstate.ams.data.dao.AppConstantDAO;
import net.superiorstate.ams.model.activity.checklist.tasks.Task;
import net.superiorstate.ams.model.activity.checklist.tasks.ToDoNote;
import net.superiorstate.ams.model.activity.note.Email;
import net.superiorstate.ams.model.activity.note.Note;

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

    @ManyToOne
    @JoinColumn(name = "todo_note_id")
    private ToDoNote toDoNote;

    @ManyToOne
    @JoinColumn(name = "note_id")
    private Note note;

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
            anchorTag += AppConstantDAO.getWebPath(em) + "ShowFileUpload?doc=" + getLinkPath() +"\"";
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

    public ToDoNote getToDoNote() {
        return toDoNote;
    }

    public void setToDoNote(ToDoNote toDoNote) {
        this.toDoNote = toDoNote;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

}
