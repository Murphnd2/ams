package net.superiorstate.ams.model.activity.ticket;

import jakarta.persistence.*;
import net.superiorstate.ams.model.activity.checklist.sequences.support.ServiceItem;

@Entity
public class TicketSubCategory {

    @Id
    @GeneratedValue
    @Column(name="subcategory_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="category_id")
    private TicketCategory ticketCategory;

    @OneToOne
    @JoinColumn(name="temp_purpose_id")
    private ServiceItem serviceItem;

    @Column(columnDefinition = "varchar(200)")
    private String description;

    @Column(name="is_active")
    private boolean isActive;

    public TicketSubCategory(){}

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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public TicketCategory getTicketCategory() {
        return ticketCategory;
    }

    public void setTicketCategory(TicketCategory ticketCategory) {
        this.ticketCategory = ticketCategory;
    }

    public ServiceItem getServiceItem() {
        return serviceItem;
    }

    public void setServiceItem(ServiceItem serviceItem) {
        this.serviceItem = serviceItem;
    }
    public TicketCategory getNoteCategory() {
        return ticketCategory;
    }

    public void setNoteCategory(TicketCategory ticketCategory) {
        this.ticketCategory = ticketCategory;
    }
}
