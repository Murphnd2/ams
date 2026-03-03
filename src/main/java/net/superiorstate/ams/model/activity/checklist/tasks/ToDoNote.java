package net.superiorstate.ams.model.activity.checklist.tasks;

import jakarta.persistence.*;
import net.superiorstate.ams.model.general.Person;
import net.superiorstate.ams.model.general.WebLink;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name="todo_note")
public class ToDoNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="note_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="todo_id")
    private ToDo toDo;

    @ManyToOne
    @JoinColumn(name="created_by_id")
    private Person createdBy;

    @Column(name="created_date", nullable=false)
    private Timestamp createdDate;

    @Column(name="note_text", nullable=false, columnDefinition="TEXT")
    private String noteText;

    @Column(name="source_type", nullable=false, length=10)
    private String sourceType;

    @Column(name="todo_guid", length=36)
    private String todoGuid;

    @Column(name="author_name", length=100)
    private String authorName;

    @OneToMany(mappedBy = "toDoNote")
    private List<WebLink> webLinkList;

    public ToDoNote() {}

    @PrePersist
    private void setDefaults() {
        if (this.createdDate == null) {
            this.createdDate = new Timestamp(System.currentTimeMillis());
        }
        if (this.sourceType == null) {
            this.sourceType = "PSP";
        }
    }

    // --- Getters/Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ToDo getToDo() {
        return toDo;
    }

    public void setToDo(ToDo toDo) {
        this.toDo = toDo;
    }

    public Person getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Person createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTodoGuid() {
        return todoGuid;
    }

    public void setTodoGuid(String todoGuid) {
        this.todoGuid = todoGuid;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public List<WebLink> getWebLinkList() {
        return webLinkList;
    }

    public void setWebLinkList(List<WebLink> webLinkList) {
        this.webLinkList = webLinkList;
    }

    /** Returns the display name: createdBy.fullName if available, else authorName, else sourceType. */
    public String getDisplayAuthor() {
        if (createdBy != null) return createdBy.getFullName();
        if (authorName != null && !authorName.isBlank()) return authorName;
        return sourceType != null ? sourceType + " User" : "Unknown";
    }
}
