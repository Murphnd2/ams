package net.superiorstate.ams.previous.model.activity.ticket;

import jakarta.persistence.*;

@Entity
public class TicketCategory {
    @Id
    @GeneratedValue
    @Column(name="category_id")
    private Long id;
    @Column(columnDefinition = "varchar(200)")
    private String description;

    @Column(name="short_text")
    private String shortText;

    @Column(name="active")
    private boolean active;

    public TicketCategory(){}

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

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
